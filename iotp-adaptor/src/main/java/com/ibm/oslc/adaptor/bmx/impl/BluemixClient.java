package com.ibm.oslc.adaptor.bmx.impl;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.eclipse.lyo.server.oauth.core.utils.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.oslc.adaptor.iotp.impl.Constants;

/**
 * A simple test program to login to Bluemix and get organizations, spaces,
 * applications and Node-RED flows. Note that the login credentials are your IBM
 * public credentials, not IBM internal.
 * 
 * This implementation uses cloudfoundry-client-lib, an older synchronous Java
 * API that works. But it is slow and it is causing version issues with the
 * servlet-api. Adding this to the pom.xml file cause the CE4IoTConnector server
 * to pickup the wrong version of the servlet-api, and there are missing methods
 * that cause the program to terminate.
 * 
 * @author jamsden
 *
 */
public final class BluemixClient {

	// Used to store instances of this IoTClient in the HTTP session
	public static final String BMXCLIENT_ATTRIBUTE = "com.ibm.oslc.adaptor.bmx.BluemixClient";

	private static final Logger log = LoggerFactory.getLogger(BluemixClient.class);
	private HttpClient client = null;
	private Header accept = null;
	private Header authorization = null;
	private Header nodeREDAuthorization = null;
	private final String bmxUri = Constants.BLUEMIX_URI;
	private JsonParser parser = new JsonParser();
	private Gson gson = new Gson();
	private String user = null;
	private String password = null;
	private SSLContext sslContext = null;
	
	public BluemixClient(String userId, String password) throws NoSuchAlgorithmException, KeyManagementException {
		this.user = userId;
		this.password = password;
		
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

		client = HttpClientBuilder.create().setSSLContext(sslContext).build();
	}
	
	public void login() throws UnauthorizedException
{
		HttpResponse response = null;
		try {
			log.info("Logging user: {} into Bluemix", user);

			// Get the authorization endpoint
			HttpGet get = new HttpGet(bmxUri + "/v2/info");
			response = client.execute(get);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				log.error("Could not get authorization endpoint: " + response.getStatusLine().toString());
				throw new UnauthorizedException("Could not access Bluemix authorization endpoint");
			}
			String authEndpointURI = parser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject()
					.get("authorization_endpoint").getAsString();
			log.info("Bluemix Authorization Endpoint URI: {}", authEndpointURI);

			// Get the bearer token used in the Authorization header for subsequent requests
			HttpPost post = new HttpPost(authEndpointURI + "/oauth/token");
			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("response_type", "token"));
			urlParameters.add(new BasicNameValuePair("grant_type", "password"));
			urlParameters.add(new BasicNameValuePair("username", user));
			urlParameters.add(new BasicNameValuePair("password", password));
			post.setEntity(new UrlEncodedFormEntity(urlParameters));
			post.addHeader("Accept", "application/json");
			post.addHeader("Content-Type", "application/x-www-form-urlencoded");
			post.addHeader("Authorization", "Basic Y2Y6");
			response = client.execute(post);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				log.error("Could not get Bluemix authentication token: " + response.getStatusLine().toString());
				throw new UnauthorizedException("Could not access Bluemix authorization endpoint");
			}
			JsonObject result = parser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
			HttpClientUtils.closeQuietly(response);
			String token_type = result.get("token_type").getAsString();
			String access_token = result.get("access_token").getAsString();
			String bearerToken = token_type + " " + access_token;

			// These headers will be used on all subsequent HTTP requests
			accept = new BasicHeader("Accept", "application/json");
			authorization = new BasicHeader("Authorization", bearerToken);
		} catch (Exception e) {
			log.error("exception tring to login to Bluemix: {}", e.getMessage());
			throw new UnauthorizedException("Could not login to Bluemix");
		}		
	}
	
	public String nodeREDLogin(String route) {
		HttpResponse response = null;
		String bearerToken = null;
		try {
			// Get the authorization scheme
			HttpGet get = new HttpGet(route + "/auth/login");
			response = client.execute(get);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				log.error("Could not get authorization endpoint: " + response.getStatusLine().toString());
				System.exit(-1);
			}
			JsonElement authScheme = parser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject()
					.get("type");
			if (authScheme == null || !"credentials".equals(authScheme.getAsString())) return null; // No active authentication

			// Get the access token used in the Authorization header for subsequent requests
			HttpPost post = new HttpPost(route + "/auth/token");
			List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
			urlParameters.add(new BasicNameValuePair("client_id", "node-red-admin"));
			urlParameters.add(new BasicNameValuePair("grant_type", "password"));
			urlParameters.add(new BasicNameValuePair("scope", "*"));
			urlParameters.add(new BasicNameValuePair("username", user));
			urlParameters.add(new BasicNameValuePair("password", password));
			post.setEntity(new UrlEncodedFormEntity(urlParameters));
			post.addHeader("Accept", "application/json");
			post.addHeader("Content-Type", "application/x-www-form-urlencoded");
			response = client.execute(post);
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				log.error("Could not get Node-RED access token: " + response.getStatusLine().toString());
				return null;
			}
			JsonObject result = parser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
			HttpClientUtils.closeQuietly(response);
			String token_type = result.get("token_type").getAsString();
			String access_token = result.get("access_token").getAsString();
			bearerToken = token_type + " " + access_token;
			log.info("Node-RED bearer token for {}: {}", route, bearerToken);
		} catch (Exception e) {
			log.error("exception trying to login to Node-RED app: {}", e.getMessage());
			e.printStackTrace();
		}		
		return bearerToken;
	}
	

	public void logoff() {
		HttpClientUtils.closeQuietly(client);		
	}
	
	public ArrayList<Service> getServices(Organization org) throws ClientProtocolException, IOException {
		ArrayList<Service> results = new ArrayList<Service>();
		HttpGet get = new HttpGet(bmxUri + "/v2/organizations/" + org.getGuid() + "/services");
		get.addHeader(accept);
		get.addHeader(authorization);
		HttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			log.error("Could not get services: " + response.getStatusLine().toString());
			return results;
		}
		JsonObject serviceEntity = parser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
		HttpClientUtils.closeQuietly(response);

		for (JsonElement serviceElement : serviceEntity.get("resources").getAsJsonArray()) {
			Service service = gson.fromJson(serviceElement.getAsJsonObject().get("entity"), Service.class);
			service.setGuid(
					serviceElement.getAsJsonObject().get("metadata").getAsJsonObject().get("guid").getAsString());
			results.add(service);
		}
		return results;
	}

	public ArrayList<Application> getApplications(Space space) throws ClientProtocolException, IOException {
		// TODO: this is getting all the applications, not the ones for an organization
		// and space (or all spaces)
		ArrayList<Application> results = new ArrayList<Application>();
		HttpGet get = new HttpGet(bmxUri + "/v2/spaces/" + space.getGuid() + "/apps");
		get.addHeader(accept);
		get.addHeader(authorization);
		HttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			log.error("Could not get apps: " + response.getStatusLine().toString());
			return results;
		}
		JsonObject appEntity = parser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
		HttpClientUtils.closeQuietly(response);

		for (JsonElement appElement : appEntity.get("resources").getAsJsonArray()) {
			Application app = gson.fromJson(appElement.getAsJsonObject().get("entity"), Application.class);
			Metadata metadata = gson.fromJson(appElement.getAsJsonObject().get("metadata"), Metadata.class);
			app.setMetadata(metadata);
			app.setGuid(appElement.getAsJsonObject().get("metadata").getAsJsonObject().get("guid").getAsString());

			results.add(app);
		}
		return results;
	}
	
	public NodeREDApplication getNodeREDApplication(String appId) throws ClientProtocolException, IOException {
		NodeREDApplication result = null;
		HttpGet get = new HttpGet(bmxUri + "/v2/apps/" + appId);
		get.addHeader(accept);
		get.addHeader(authorization);
		HttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			log.error("Could not get app: {}, error: {} ", appId, response.getStatusLine().toString());
			return result;
		}
		JsonObject spaceEntity = parser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
		HttpClientUtils.closeQuietly(response);
		result = gson.fromJson(spaceEntity.get("entity"), NodeREDApplication.class);
		Metadata metadata = gson.fromJson(spaceEntity.getAsJsonObject().get("metadata"), Metadata.class);
		result.setMetadata(metadata);
		result.setGuid(spaceEntity.get("metadata").getAsJsonObject().get("guid").getAsString());
		return result;
	}



	public ArrayList<NodeREDApplication> getNodeREDApplications(Space space)
			throws ClientProtocolException, IOException {
		ArrayList<NodeREDApplication> results = new ArrayList<NodeREDApplication>();
		for (Application app : getApplications(space)) {
			boolean isNodeRED = false;

			// get the routes for the app
			for (Route route : getRoutes(app)) {
				try {
					URI uri = new URI("https", route.getHost(), route.getPath() + "/red", null);
					HttpHead req = new HttpHead(uri);
					HttpResponse resp = client.execute(req);
					isNodeRED = resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
					HttpClientUtils.closeQuietly(resp);
					if (isNodeRED) {
						NodeREDApplication nodeREDApp = new NodeREDApplication(app);
						results.add(nodeREDApp);
						// Get the flows for the Node-RED app
						nodeREDApp.setBearerToken(nodeREDLogin(uri.toString()));
						HttpGet getFlows = new HttpGet(uri + "/flows");
						getFlows.addHeader("Node-RED-API-Version", "v2");
						if (nodeREDApp.getBearerToken() != null) getFlows.addHeader("Authorization", nodeREDApp.getBearerToken());
						// TODO: need to handle authentication for protected Node-RED apps
						try {
							resp = client.execute(getFlows);
							if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
								JsonElement result = (new JsonParser()).parse(EntityUtils.toString(resp.getEntity()));
								for (JsonElement flow : result.getAsJsonObject().get("flows").getAsJsonArray()) {
									JsonObject node = flow.getAsJsonObject();
									if (Objects.equals(node.get("type").getAsString(), "tab")) {
										nodeREDApp.getFlowNames().add(flow.getAsJsonObject().get("label").getAsString());
									}
								}
							}
							HttpClientUtils.closeQuietly(resp);
						} catch (IOException e) {
							e.printStackTrace();
						}

						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return results;
	}


	public Space getSpace(String spaceId) throws ClientProtocolException, IOException {
		Space result = null;
		HttpGet get = new HttpGet(bmxUri + "/v2/spaces/" + spaceId);
		get.addHeader(accept);
		get.addHeader(authorization);
		HttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			log.error("Could not get space: {}, error: {} ", spaceId, response.getStatusLine().toString());
			return result;
		}
		JsonObject spaceEntity = parser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
		HttpClientUtils.closeQuietly(response);
		result = gson.fromJson(spaceEntity.get("entity"), Space.class);
		Metadata metadata = gson.fromJson(spaceEntity.getAsJsonObject().get("metadata"), Metadata.class);
		result.setMetadata(metadata);
		result.setGuid(spaceEntity.get("metadata").getAsJsonObject().get("guid").getAsString());
		return result;
	}

	public ArrayList<Space> getSpaces(Organization org) throws ClientProtocolException, IOException {
		ArrayList<Space> results = new ArrayList<Space>();
		HttpGet get = new HttpGet(bmxUri + "/v2/organizations/" + org.getGuid() + "/spaces");
		get.addHeader(accept);
		get.addHeader(authorization);
		HttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			log.error("Could not get spaces: " + response.getStatusLine().toString());
			return results;
		}
		JsonObject spaceEntity = parser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
		HttpClientUtils.closeQuietly(response);

		for (JsonElement spaceElement : spaceEntity.get("resources").getAsJsonArray()) {
			Space space = gson.fromJson(spaceElement.getAsJsonObject().get("entity"), Space.class);
			Metadata metadata = gson.fromJson(spaceElement.getAsJsonObject().get("metadata"), Metadata.class);
			space.setMetadata(metadata);
			space.setGuid(spaceElement.getAsJsonObject().get("metadata").getAsJsonObject().get("guid").getAsString());
			results.add(space);
		}
		return results;
	}

	public ArrayList<Route> getRoutes(Application app) throws ClientProtocolException, IOException {
		ArrayList<Route> results = new ArrayList<Route>();
		HttpGet get = new HttpGet(bmxUri + "/v2/apps/" + app.getGuid() + "/routes");
		get.addHeader(accept);
		get.addHeader(authorization);
		HttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			log.error("Could not get routes: " + response.getStatusLine().toString());
			return results;
		}
		JsonObject routeEntity = parser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
		HttpClientUtils.closeQuietly(response);

		for (JsonElement routeElement : routeEntity.get("resources").getAsJsonArray()) {
			Route route = gson.fromJson(routeElement.getAsJsonObject().get("entity"), Route.class);
			Metadata metadata = gson.fromJson(routeElement.getAsJsonObject().get("metadata"), Metadata.class);
			route.setMetadata(metadata);
			route.setGuid(routeElement.getAsJsonObject().get("metadata").getAsJsonObject().get("guid").getAsString());
			results.add(route);
		}
		return results;
	}

	public ArrayList<Organization> getOrganizations() throws ClientProtocolException, IOException {
		ArrayList<Organization> results = new ArrayList<Organization>();
		HttpGet get = new HttpGet(bmxUri + "/v2/organizations");
		get.addHeader(accept);
		get.addHeader(authorization);
		HttpResponse response = client.execute(get);
		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			log.error("Could not get organizations: " + response.getStatusLine().toString());
			return results;
		}
		JsonObject orgsEntity = parser.parse(EntityUtils.toString(response.getEntity())).getAsJsonObject();
		HttpClientUtils.closeQuietly(response);

		for (JsonElement orgElement : orgsEntity.get("resources").getAsJsonArray()) {
			Organization org = gson.fromJson(orgElement.getAsJsonObject().get("entity"), Organization.class);
			Metadata metadata = gson.fromJson(orgElement.getAsJsonObject().get("metadata"), Metadata.class);
			org.setMetadata(metadata);
			org.setGuid(orgElement.getAsJsonObject().get("metadata").getAsJsonObject().get("guid").getAsString());
			results.add(org);
		}
		return results;
	}
}