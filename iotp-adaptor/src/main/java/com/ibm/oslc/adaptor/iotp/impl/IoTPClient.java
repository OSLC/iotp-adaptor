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
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.cookie.DefaultCookieSpec;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.server.oauth.core.utils.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


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
	private String access_token;

	// End user session information
	private HttpClient client;
	private CookieStore cookieStore;
	private String user;
	private String password;
	private Registry<CookieSpecProvider> cookieReg = null;
	private RequestConfig config = null;
	
	private static Properties clientProperties = new Properties();
	static {
		try {
			clientProperties.load(IoTPClient.class.getResourceAsStream("/config.properties"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


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
		String apiKey = clientProperties.getProperty("apiKey");

		String result = "login failed";
		HttpResponse response = null;
		int status_code = 0;

		try {
			HttpClient client = HttpClientBuilder.create().build();

			URIBuilder builder = new URIBuilder("https://iam.cloud.ibm.com/identity/token");
			builder.setParameter("grant_type", "urn:ibm:params:oauth:grant-type:apikey");
			builder.addParameter("apikey", apiKey);

			HttpPost post = new HttpPost(builder.build());
			Header authHeader = new BasicHeader("Content-Type",  "application/x-www-form-urlencoded");
			post.addHeader(authHeader);
			response = client.execute(post);

			status_code = response.getStatusLine().getStatusCode();
			if (status_code != HttpStatus.SC_OK) {
				log.error("Couldn't get access_token: "+status_code);
				log.info(EntityUtils.toString(response.getEntity()));
			}
			JsonObject json_response = (new JsonParser()).parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
			access_token = json_response.get("access_token").getAsString();
		} catch (Exception e) {
			e.printStackTrace();
			throw new UnauthorizedException("Cannot login to Watson IoT Platform: "+status_code);
		}
	}
	
	public void logoff() {
		log.info("Logging out from Watson IoT Platform");
		HttpClientUtils.closeQuietly(client);		
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
		post.setHeader("Authorization", "Bearer "+access_token);

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
		get.setHeader("Authorization", "Bearer "+access_token);

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
		put.setHeader("Authorization", "Bearer "+access_token);

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
		delete.setHeader("Authorization", "Bearer "+access_token);

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
		get.setHeader("Authorization", "Bearer "+access_token);

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
			}
			HttpClientUtils.closeQuietly(response);
		} catch (Exception e) {
			log.warn("GET Organizations {}: {}", uri, e.getMessage());
			throw e;
		}
		return result;
	
	}
}
