/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *
 *     Jim Amsden     - initial API and implementation for CE4IoTConnector
 *     
 *******************************************************************************/
package com.ibm.oslc.adaptor.iotp.impl;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.eclipse.lyo.server.oauth.consumerstore.FileSystemConsumerStore;
import org.eclipse.lyo.server.oauth.core.Application;
import org.eclipse.lyo.server.oauth.core.AuthenticationException;
import org.eclipse.lyo.server.oauth.core.OAuthConfiguration;
import org.eclipse.lyo.server.oauth.core.OAuthRequest;
import org.eclipse.lyo.server.oauth.core.token.LRUCache;
import org.eclipse.lyo.server.oauth.core.token.SimpleTokenStrategy;
import org.eclipse.lyo.server.oauth.core.utils.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.oslc.adaptor.bmx.impl.BluemixClient;
import com.ibm.oslc.adaptor.iotp.CE4IoTConnectorManager;

import net.oauth.OAuth;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthProblemException;
import net.oauth.http.HttpMessage;
import net.oauth.server.OAuthServlet;

/** A JEE filter that is used to create an authentication challenge on access to protected resources.
 * The actual authentication is delegated to the IoT Platform since it actually manages the resources.
 * This ensures the user credentials, privileges and authentication are done by the data sources
 * owning the resources. This filter simply delegates user management to the IoT Platform login.
 * 
 * @author jamsden
 *
 */
public class CredentialsFilter implements Filter {


	
    public static final String CREDENTIALS_ATTRIBUTE = "com.ibm.oslc.adaptor.iotp.Credentials";
    private static final String ADMIN_SESSION_ATTRIBUTE = "com.ibm.oslc.adaptor.iotp.AdminSession";
    public static final String JAZZ_INVALID_EXPIRED_TOKEN_OAUTH_PROBLEM = "invalid_expired_token";
    public static final String OAUTH_REALM = "IoTPlatform";
		
	private static LRUCache<String, IoTPClient> iotKeyToConnectorCache = new LRUCache<String, IoTPClient>(200);
	private static LRUCache<String, BluemixClient> bmxKeyToConnectorCache = new LRUCache<String, BluemixClient>(200);
	
	private static Logger log = LoggerFactory.getLogger(CredentialsFilter.class);

	
	@Override
	public void destroy() {
		

	}
	


	/**
	 * Check for OAuth or BasicAuth credentials and challenge if not found.
	 * 
	 * Store the IoTPClient and BluemixClient in the HttpSession for retrieval in the REST services. The IoTPClient
	 * has the LtpaToken2 cookie used to authenticate the user's access to the IoT Platform REST APIs. 
	 * This cookie is retireved on Watson IoT Platform login. BluemixClient
	 * uses bearer token authroization.
	 */
	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
			FilterChain chain) throws IOException, ServletException {
		
		if(servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse) {
			HttpServletRequest request = (HttpServletRequest) servletRequest;
			HttpServletResponse response = (HttpServletResponse) servletResponse;
		
			//Don't protect requests to oauth service.   TODO: possibly do this in web.xml
			if (! request.getPathInfo().startsWith("/oauth"))
			{
			
				// First check if this is an OAuth request.
				try {
					try {
						OAuthMessage message = OAuthServlet.getMessage(request, null);
						if (message.getToken() != null) {
							OAuthRequest oAuthRequest = new OAuthRequest(request);
							oAuthRequest.validate();
							IoTPClient iotConnector = iotKeyToConnectorCache.get(message.getToken());
							BluemixClient bmxConnector = bmxKeyToConnectorCache.get(message.getToken());
							if (iotConnector == null || bmxConnector == null) {
								throw new OAuthProblemException(OAuth.Problems.TOKEN_REJECTED);
							}
			
							request.getSession().setAttribute(IoTPClient.IOTPCLIENT_ATTRIBUTE, iotConnector);
							request.getSession().setAttribute(BluemixClient.BMXCLIENT_ATTRIBUTE, bmxConnector);
						}
					} catch (OAuthProblemException e) {
						if (OAuth.Problems.TOKEN_REJECTED.equals(e.getProblem())) {
							throwInvalidExpiredException(e);
						} else {
							throw e;
						}
					}
				} catch (OAuthException e) {
					OAuthServlet.handleException(response, e, OAUTH_REALM);
					return;
				}
                
				
				// This is not an OAuth request. Check for basic access authentication.
				HttpSession session = request.getSession();
				IoTPClient iotConnector = (IoTPClient) session.getAttribute(IoTPClient.IOTPCLIENT_ATTRIBUTE);
				BluemixClient bmxConnector = (BluemixClient)session.getAttribute(BluemixClient.BMXCLIENT_ATTRIBUTE);
				if (iotConnector == null || bmxConnector == null) {
					try {
						Credentials credentials = (Credentials) session.getAttribute(CREDENTIALS_ATTRIBUTE);
						if (credentials == null) {
							credentials = HttpUtils.getCredentials(request);
							if (credentials == null) {
								throw new UnauthorizedException();
							}
						}
						iotConnector = new IoTPClient(credentials.getUsername(), credentials.getPassword());
						iotConnector.login();
						session.setAttribute(IoTPClient.IOTPCLIENT_ATTRIBUTE, iotConnector);

						bmxConnector = new BluemixClient(credentials.getUsername(), credentials.getPassword());
						bmxConnector.login();
						session.setAttribute(BluemixClient.BMXCLIENT_ATTRIBUTE, bmxConnector);

						session.setAttribute(CREDENTIALS_ATTRIBUTE, credentials);
				
					} catch (UnauthorizedException e) {
						HttpUtils.sendUnauthorizedResponse(response, e);
						log.info("Sending authentication challenge to get credentials: {}", e.getMessage());
						return;
					} catch (KeyManagementException | NoSuchAlgorithmException ce) {
						throw new ServletException(ce);
					}
				}					
			}
		}

		chain.doFilter(servletRequest, servletResponse);
	}
	

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		OAuthConfiguration config = OAuthConfiguration.getInstance();

		// Validates a user's ID and password.
		config.setApplication(new Application() {
			@Override
			public void login(HttpServletRequest request, String id, String password) 
					throws AuthenticationException {
				try {
					// Login to the API Platform
					log.info("Doing application login: {}", id);
					IoTPClient iotpClient = new IoTPClient(id, password);
					iotpClient.login();
					request.setAttribute(IoTPClient.IOTPCLIENT_ATTRIBUTE, iotpClient);

					// Login to the Bluemix Platform
					BluemixClient bmxClient = new BluemixClient(id, password);
					bmxClient.login();
					request.setAttribute(BluemixClient.BMXCLIENT_ATTRIBUTE, bmxClient);

					Credentials creds = new Credentials();
					creds.setUsername(id);
					creds.setPassword(password);
                    request.getSession().setAttribute(CREDENTIALS_ATTRIBUTE, creds);
                    // TODO: determine if the IoT Platform user is an administrator
                    request.getSession().setAttribute(ADMIN_SESSION_ATTRIBUTE, Boolean.TRUE);
				} catch (Exception e) {
					log.error("Login failed: {}", e.getCause().getMessage());
					throw new AuthenticationException(e.getCause().getMessage(), e);
				}
			}

			@Override
			public String getName() {
				// Display name for this application.
				return "CE4IoTConnector";
			}

			@Override
			public boolean isAdminSession(HttpServletRequest request) {
				return Boolean.TRUE.equals(request.getSession().getAttribute(
						ADMIN_SESSION_ATTRIBUTE));
			}

			@Override
			public String getRealm(HttpServletRequest request) {
				return CE4IoTConnectorManager.REALM;
			}

			@Override
			public boolean isAuthenticated(HttpServletRequest request) {
				IoTPClient iotpc = (IoTPClient) request.getSession().getAttribute(IoTPClient.IOTPCLIENT_ATTRIBUTE);
				BluemixClient bmxc = (BluemixClient) request.getSession().getAttribute(BluemixClient.BMXCLIENT_ATTRIBUTE);
				if (iotpc == null || bmxc == null) {
					return false;
				}
				
				request.setAttribute(IoTPClient.IOTPCLIENT_ATTRIBUTE, iotpc);
				request.setAttribute(BluemixClient.BMXCLIENT_ATTRIBUTE, bmxc);
				return true;
			}
		});

		/*
		 * Override some SimpleTokenStrategy methods so that we can keep the
		 * Connection associated with the OAuth tokens.
		 */
		config.setTokenStrategy(new SimpleTokenStrategy() {
			@Override
			public void markRequestTokenAuthorized(
					HttpServletRequest httpRequest, String requestToken)
					throws OAuthProblemException {
				iotKeyToConnectorCache.put(requestToken,	(IoTPClient) httpRequest.getAttribute(IoTPClient.IOTPCLIENT_ATTRIBUTE));
				bmxKeyToConnectorCache.put(requestToken,	(BluemixClient) httpRequest.getAttribute(BluemixClient.BMXCLIENT_ATTRIBUTE));
				super.markRequestTokenAuthorized(httpRequest, requestToken);
			}

			@Override
			public void generateAccessToken(OAuthRequest oAuthRequest)
					throws OAuthProblemException, IOException {
				String requestToken = oAuthRequest.getMessage().getToken();
				IoTPClient iotpc = iotKeyToConnectorCache.remove(requestToken);
				BluemixClient bmxc = bmxKeyToConnectorCache.remove(requestToken);
				super.generateAccessToken(oAuthRequest);
				log.info("caching clients at token {} = {}. {}", oAuthRequest.getAccessor().accessToken, iotpc, bmxc);
				iotKeyToConnectorCache.put(oAuthRequest.getAccessor().accessToken, iotpc);
				bmxKeyToConnectorCache.put(oAuthRequest.getAccessor().accessToken, bmxc);
			}
		});

		try {
			// For now, hard-code the consumers.
			config.setConsumerStore(new FileSystemConsumerStore("CE4IoTConnectorOAuthStore.xml"));
		} catch (Throwable t) {
			log.error("Error initializing the OAuth consumer store: " +  t.getMessage());
		
		}

	}
	
	/**
	 * Jazz requires a exception with the magic string "invalid_expired_token" to restart
	 * OAuth authentication
	 * @param e
	 * @return
	 * @throws OAuthProblemException 
	 */
	private void throwInvalidExpiredException(OAuthProblemException e) throws OAuthProblemException {
		OAuthProblemException ope = new OAuthProblemException(JAZZ_INVALID_EXPIRED_TOKEN_OAUTH_PROBLEM);
		ope.setParameter(HttpMessage.STATUS_CODE, new Integer(
				HttpServletResponse.SC_UNAUTHORIZED));
		throw ope;
	}
	
	
}
