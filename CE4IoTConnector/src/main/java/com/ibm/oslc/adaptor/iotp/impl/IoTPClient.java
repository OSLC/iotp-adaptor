/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2018. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************//**
 *****************************************************************************
 Copyright (c) 2015-16 IBM Corporation and other Contributors.
 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Public License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/epl-v10.html
 Contributors:
 Sathiskumar Palaniappan - Initial Contribution
 *****************************************************************************
 *
 */
package com.ibm.oslc.adaptor.iotp.impl;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.ConnectionClosedException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.impl.cookie.DefaultCookieSpec;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.server.oauth.core.utils.UnauthorizedException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.oslc.adaptor.iotp.impl.Constants;


/**
 * A simple IoT Platform Java client API that uses Lightweight Third party
 * Authentication (LPTA) for single signon. The IoT Platform support this
 * through user login and the LtpaToken2 cookie which enables IoT Platform REST
 * API access for all resources of all organizations for which the logged-in
 * user is a member.
 * 
 * This implementation's login method is based on the current IoT Platform web application
 * login sequence used in the browser. This is not a good practice and should be 
 * replaced with the Cloud IAM API as soon as it is supported by the IoT Platform
 * 
 * TODO: Update login method to use Cloud IAM.
 * 
 * @author jamsden
 *
 */
public class IoTPClient {

	private static Logger log = LoggerFactory.getLogger(IoTPClient.class);

	// Used to store instances of this IoTClient in the HTTP session
	public static final String IOTPCLIENT_ATTRIBUTE = "com.ibm.oslc.adaptor.iotp.impl.IoTPClient";

	private SSLContext sslContext = null;

	private String platformBase;
	private String apiVersion;

	// End user session information
	private HttpClient client;
	private CookieStore cookieStore;
	private String user;
	private String password;
	private Registry<CookieSpecProvider> cookieReg = null;
	private RequestConfig config = null;

	// Enum for content-type header
	public enum ContentType {
		text("text/plain"), json("application/json"), xml("application/xml"), bin("application/octet-stream");

		ContentType(String type) {
			mType = type;
		}

		public String getType() {
			return mType;
		}

		private String mType;

	} // ending enum

	private ContentType defaultContentType = ContentType.json;

	
	/** Create an LtpaIoTPClient using for the server URI, userId and password.
	 * 
	 * @param userId
	 * @param password
	 * @throws NoSuchAlgorithmException
	 * @throws KeyManagementException
	 */
	public IoTPClient(String userId, String password) throws NoSuchAlgorithmException, KeyManagementException {

		this.platformBase = IoTAPIImplementation.getPlatformBase();
		this.apiVersion = IoTAPIImplementation.getApiVersion();
		this.user = userId;
		this.password = password;

		class LooseCookieSpec extends DefaultCookieSpec {
		    @Override
		    public void validate(Cookie arg0, CookieOrigin arg1) throws MalformedCookieException {
		        //allow all cookies 
		    }
		}

		class LooseSpecProvider implements CookieSpecProvider {
		    @Override
		    public CookieSpec create(HttpContext context) {
		        return new LooseCookieSpec();
		    }
		}

		cookieReg = RegistryBuilder.<CookieSpecProvider>create()
	            .register("loose", new LooseSpecProvider())
	            .build();

		// the path for JSESSIONID cookie varies from /idaas/ to /authsvc/ 
		// so we need to accept all cookies
		cookieStore = new BasicCookieStore();
		config = RequestConfig.custom()
				.setConnectTimeout(30 * 1000)
				.setSocketTimeout(30 * 1000)
				.setRedirectsEnabled(false)
				.setCookieSpec("loose").build();

		TrustManager[] trustAllCerts = null;
		boolean trustAll = false;

		// TODO: when would trustAll be set to true?
		if (trustAll) {
			trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}

				public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}
			} };
		}

		sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(null, trustAllCerts, null);
		
		client = HttpClientBuilder.create()
				.setDefaultCookieStore(cookieStore)
				.setDefaultCookieSpecRegistry(cookieReg)
				.setDefaultRequestConfig(config)
				.setSSLContext(sslContext).build();
	}
	
	
	/**
	 * Login to the IoT Platform with the userId and password provided in the constructor.
	 * This will set the LtpaToken2 cookie which will be used to authenticate other IoT Platform REST API calls.
	 * 
	 * The URI for login will look like: https://internetofthings.ibmcloud.com/ibmssologin 
	 * Basic Authentication is used
	 * @throws IoTFCReSTException 
	 * 
	 */
	public void login() throws UnauthorizedException {
		
		String state = null;

		String blueidlogin;
		String blueidlogin_redirect;
		String ibm_security_logout;
		String basicldapuser;

		String submitUser;
		String submitUserPwd;
		String submitUserPwd_redirect, submitUserPwd_redirect_rd, staging_platform, logout_redirect;

		int staging_platform_response, getOrgs_response, logout_redirect_response;
		String statusCode;
		int responseCode = 0;

		blueidlogin=Constants.BLUEID_LOGIN;
		ibm_security_logout=Constants.IBM_SECURITY_LOGOUT;
		basicldapuser=Constants.BASIC_LDAP_USER;
		submitUser=Constants.SUBMIT_USER_URI;
		submitUserPwd=Constants.SUBMIT_USER_PWD_URI;
		
		
		HttpResponse response = null;
		
		boolean result = true;
		try {
			log.debug("------ 2 ---- GET {}", blueidlogin);
			response = getRequest(blueidlogin);
			blueidlogin_redirect = response.getFirstHeader("Location").getValue();
			// Save the rejected LptaToken2 cookie for use later in step 10
			CookieSpec cookieSpec = new DefaultCookieSpec();
			CookieOrigin origin = new CookieOrigin(Constants.IOT_PLATFORM_BASE, 443, "/", true);
			List<Cookie> cookies = cookieSpec.parse(response.getFirstHeader("Set-Cookie"), origin);
			Cookie ltpaToken2 = cookies.get(0);
			EntityUtils.consume(response.getEntity()); // throw away the content

			log.debug("------ 3 ---- GET {}", blueidlogin_redirect);
			response = getRequest(blueidlogin_redirect);
			EntityUtils.consume(response.getEntity()); // throw away the content

			log.debug("------ 4 ---- GET {}", ibm_security_logout);
			response = getRequest(ibm_security_logout);
			EntityUtils.consume(response.getEntity()); // throw away the content

			blueidlogin_redirect = blueidlogin_redirect.replaceFirst("oidc/endpoint/default/authorize",
					"mtfim/sps/idaas/login");
			
			log.debug("------ 5 ---- GET {}", blueidlogin_redirect);
			response = getRequest(blueidlogin_redirect);
			EntityUtils.consume(response.getEntity()); // throw away the content
			// remove the JSESSIONID cookie with path=/idaas/
			List<Cookie> latestCookies = cookieStore.getCookies();
			cookieStore.clear();
			for (Cookie aCookie : latestCookies) {
				//if (!(aCookie.getName().equals("JSESSIONID") && aCookie.getPath().equals("/idaas/") || aCookie.getName().equals("LtpaToken2"))) cookieStore.addCookie(aCookie);
				if (!(aCookie.getName().equals("JSESSIONID") && aCookie.getPath().equals("/idaas/"))) cookieStore.addCookie(aCookie);
			}

			log.debug("------ 6 ---- GET {}", basicldapuser);
			response = getRequest(basicldapuser);
			Document doc = Jsoup.parseBodyFragment(EntityUtils.toString(response.getEntity()));
			Elements elements = doc.select("body").first().children();
			for (Element el : elements) {
				if (el.tagName().equals("div")) {
					Elements x = el.getElementsByTag("div").tagName("form").eq(3);
					String[] y = x.toString().split(" ");
					for (int i = 0; i <= y.length; i++) {
						if (y[i].contains("action")) {
							String[] k = y[i].split("=");
							state = k[2];
							state = state.substring(0, state.length() - 2);
							break;
						}
					}
				}
			}
						
			log.debug("------ 8 ---- POST {}", submitUserPwd);
			BasicClientCookie userIdCookie = new BasicClientCookie("useribmid", user.replaceAll("@", "%40"));
			userIdCookie.setDomain(Constants.USERID_DOMAIN);
			userIdCookie.setPath("/");
			cookieStore.addCookie(userIdCookie);
			submitUserPwd = submitUserPwd + state;
			response = postRequest(submitUserPwd);
			submitUserPwd_redirect = response.getFirstHeader("Location").getValue();
			EntityUtils.consume(response.getEntity()); // throw away the content

			log.debug("------ 9 ---- GET {}", submitUserPwd_redirect);
			response = getRequest(submitUserPwd_redirect);
			submitUserPwd_redirect_rd = response.getFirstHeader("Location").getValue();
			EntityUtils.consume(response.getEntity()); // throw away the content

			log.debug("------ 10 ---- should have updated LtpaToken2 after: GET {}", submitUserPwd_redirect_rd);
			// Clear out all the cookies
			cookieStore.clear();
			// Add the rejected LtpaToken2 from above, it will be replaced with the one we need for authentication
			cookieStore.addCookie(ltpaToken2);
			getRequest(submitUserPwd_redirect_rd);
			EntityUtils.consume(response.getEntity()); // throw away the content

			log.debug("------ 11 ---- GET {}", "https://internetofthings.ibmcloud.com");
			getRequest(Constants.IOT_PLATFORM_BASE_URL);
			EntityUtils.consume(response.getEntity()); // throw away the content
			HttpClientUtils.closeQuietly(response);
			client = HttpClientBuilder.create()
					.setDefaultCookieStore(cookieStore)
					.setDefaultCookieSpecRegistry(cookieReg)
					.setDefaultRequestConfig(config).build();
												
			// TODO: do IoT Platform logout on shutdown
			String logout = "https://"+IoTAPIImplementation.getPlatformBase()+"/logout";
			//String logout_redirect = getRequest(logout, cookie_header);

			// getRequest(logout_redirect, cookie_header);

		} catch (Exception e) {
			log.error("blueidlogin caught execption:");
			log.error("Login failed!", e.getStackTrace());
			HttpClientUtils.closeQuietly(response);
			throw new UnauthorizedException("Could not login to Watson IoT Platform");
		}
	}
	
	public void logoff() {
		log.info("Logging out from Watson IoT Platform");
		HttpClientUtils.closeQuietly(client);		
	}

	/** Do an HTTP GET request for the IoT Platform based on user authentication. This ensures all the
	 * required headers and cookies are set for the user's session
	 * 
	 * @param location
	 * @param cookie_header
	 * @return
	 */
	/**
	 * @param location
	 * @param cookie_header
	 * @return
	 */
	public HttpResponse getRequest(String location) {
		
		String url = location;

		HttpGet get = new HttpGet(url);
		HttpResponse response = null;

		try {
			response = client.execute(get);
		} catch (ConnectionClosedException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
		catch(ConnectTimeoutException e)
		{
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
		catch(SocketTimeoutException e)
		{
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}

		catch (ArrayIndexOutOfBoundsException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);

		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException(e);
		}
		return response;
	}
	
	
	/** Execute an HTTP POST request with the proper user session headers and cookies.
	 * 
	 * @param location
	 * @param cookie_header
	 * @param userId
	 * @param password
	 * @return
	 */
	public HttpResponse postRequest(String location) {
		
		String url = location;
		HttpResponse response = null;

		HttpPost post = new HttpPost(url);
		try {
			post.addHeader("Content-Type", "application/x-www-form-urlencoded");

			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("operation", "verify"));
			urlParameters.add(new BasicNameValuePair("login-form-type", "pwd"));
			urlParameters.add(new BasicNameValuePair("username", user));
			urlParameters.add(new BasicNameValuePair("redirectURL", " "));
			urlParameters.add(new BasicNameValuePair("password", password));
			
			log.debug("urlParameters are : " + urlParameters);
			post.setEntity(new UrlEncodedFormEntity(urlParameters));

			response = client.execute(post);
			log.debug("    Response: "+response.getStatusLine().toString());
			//log.debug("POST Response Body:\n"+EntityUtils.toString(response.getEntity()));
		} catch(ConnectTimeoutException e)
		{
			log.error(e.getMessage(), e);
			throw new RuntimeException();
		}
		catch(SocketTimeoutException e)
		{
			log.error(e.getMessage(), e);
			throw new RuntimeException();
		}
		catch (ConnectionClosedException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException();
		}

		catch (ArrayIndexOutOfBoundsException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException();

		} catch (IOException e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException();
			
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			throw new RuntimeException();
		}
		return response;
	}


	/** Create a new IoT Platform resource.
	 * 
	 * @param orgId organization managing the resource
	 * @param url URI fragment of the resource to be deleted, e.g., device/types/{typeId}
	 * @param resource the resource to create
	 * @return the created resource
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public JsonElement createIoTResource(String orgId, String url, JsonElement resource) throws URISyntaxException, IOException {
		JsonElement result = null;

		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).append(".").append(platformBase).append("/api/").append(apiVersion).append("/").append(url);
		String uri = sb.toString();

		HttpPost post = new HttpPost(uri);
		post.setEntity(new StringEntity(resource.toString()));
		post.addHeader("Content-Type", "application/json");
		post.addHeader("Accept", "application/json");
		try {
			HttpResponse response = client.execute(post);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
				result = (new JsonParser()).parse(EntityUtils.toString(response.getEntity()));
			} else {
				log.error("Could not create resource: {}, status is {}", uri, response.getStatusLine().getStatusCode());
			}
		} catch (Exception e) {
			log.warn("POST {}: {}", uri, e.getMessage());
			throw e;
		}
		return result;
	}

	/** Read an IoT Platform resource.
	 * 
	 * @param orgId organization managing the resource
	 * @param url URI fragment of the resource to be deleted, e.g., device/types/{typeId}
	 * @return The JsonElement resource representation
	 * 
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public JsonElement readIoTResource(String orgId, String url) throws URISyntaxException, IOException {
		JsonElement result = null;

		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).append(".").append(platformBase).append("/api/").append(apiVersion).append("/").append(url);
		String uri = sb.toString();

		HttpGet get = new HttpGet(uri);
		get.addHeader("Content-Type", "application/json");
		get.addHeader("Accept", "application/json");
		
		try {
			HttpResponse response = client.execute(get);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				result = (new JsonParser()).parse(EntityUtils.toString(response.getEntity()));
			} else {
				log.error("Could not read resource: {}", uri);
				HttpClientUtils.closeQuietly(response);
			}
		} catch (Exception e) {
			log.warn("GET {}: {}", uri, e.getMessage());
			throw e;
		}
		return result;
	}

	/** Update an IoT Platform resource
	 * 
	 * @param orgId organization managing the resource
	 * @param url URI fragment of the resource to be deleted, e.g., device/types/{typeId}
	 * @return the updated resource
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public JsonElement updateIoTResource(String orgId, String url, JsonObject resource) throws URISyntaxException, IOException {
		JsonElement result = null;
		
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).append(".").append(platformBase).append("/api/").append(apiVersion).append("/").append(url);
		String uri = sb.toString();
		
		HttpPut put = new HttpPut(uri);
		put.setEntity(new StringEntity(resource.toString()));
		put.addHeader("Content-Type", "application/json");
		put.addHeader("Accept", "application/json");
		try {
			HttpResponse response = client.execute(put);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				result = (new JsonParser()).parse(EntityUtils.toString(response.getEntity()));				
			} else {
				log.warn("Could not update resource: {}", uri);
			} 
		} catch (Exception e) {
			log.warn("PUT {}: {}", uri, e.getMessage());
			throw e;
		}
		return result;
	}

	/** Delete an IoT Platform resource.
	 * 
	 * @param orgId organization managing the resource
	 * @param url URI fragment of the resource to be deleted, e.g., device/types/{typeId}
	 * @return true if the resource is successfully deleted, false otherwise
	 * @throws URISyntaxException
	 * @throws IOException
	 */
	public Boolean deleteIoTResource(String orgId, String url) throws URISyntaxException, IOException {
		Boolean result = Boolean.FALSE;
		
		StringBuilder sb = new StringBuilder("https://");
		sb.append(orgId).append(".").append(platformBase).append("/api/").append(apiVersion).append("/").append(url);
		String uri = sb.toString();

		HttpDelete delete = new HttpDelete(uri);
		delete.addHeader("Content-Type", "application/json");
		delete.addHeader("Accept", "application/json");
		try {
			HttpResponse response = client.execute(delete);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
				result = Boolean.TRUE;
			} else {
				log.warn("Could not delete resource: {}", uri);
				HttpClientUtils.closeQuietly(response);
			}
		} catch (Exception e) {
			log.warn("DELETE {}: {}", uri, e.getMessage());
			throw e;
		}
		return result;
	}

		
	
	/** Get the organizations the user of this login session is a member of
	 * 
	 * @return JsonArray of organizations
	 * @throws Exception 
	 * @throws IoTFCReSTException
	 */
	public JsonArray getOrganizations() throws Exception {
		JsonArray result = null;
		StringBuilder sb = new StringBuilder("https://");
		sb.append(platformBase).append("/api/").append(apiVersion).append("/auth/organizations");
		String uri = sb.toString();
	
		HttpGet get = new HttpGet(uri);
		get.addHeader("Content-Type", "application/json");
		get.addHeader("Accept", "application/json");
		
		try {
			HttpResponse response = client.execute(get);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
				HttpClientUtils.closeQuietly(response);
				this.login();
				response = client.execute(get);
			}
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				JsonElement jsonResponse = new JsonParser().parse(new InputStreamReader(response.getEntity().getContent()));
				result = jsonResponse.getAsJsonArray();
			} else {
				log.error("Could not read organizations: {} got: {}", uri, response.getStatusLine().getStatusCode());
				HttpClientUtils.closeQuietly(response);
			}
		} catch (Exception e) {
			log.warn("GET Organizations {}: {}", uri, e.getMessage());
			throw e;
		}
		return result;
	
	}
}
