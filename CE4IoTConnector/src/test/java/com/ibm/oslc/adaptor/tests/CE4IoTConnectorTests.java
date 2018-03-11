/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2017. All Rights Reserved. 
 *
 * Note to U.S. Government Users Restricted Rights:  Use, duplication or 
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.oslc.adaptor.tests;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.protocol.RequestDefaultHeaders;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.wink.client.ClientResponse;
import org.apache.ws.commons.util.Base64;
import org.eclipse.lyo.client.oslc.OSLCConstants;
import org.eclipse.lyo.client.oslc.OslcClient;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.oslc.adaptor.iotp.resources.Oslc_iotDomainConstants;
import com.ibm.oslc.adaptor.iotp.resources.DeviceType;

//@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class CE4IoTConnectorTests {

	private static final Logger logger = Logger.getLogger(CE4IoTConnectorTests.class.getName());
	private static final String serverUrl = "https://rlia4iot.raleigh.ibm.com:9443/iotp";
	private static final String userId = "someuser@ibm.com";
	private static final String password = "********";
	private static final String orgId = "some organization";
	private static String queryCapability = null;
	private static String creationFactory = null;
	private static OslcClient client = null;

	/**
	 * @return the suite of tests being tested
	 */
	/*
	 * public static TestSuite suite() { return new TestSuite(
	 * CE4IoTConnectorTests.class ); }
	 */

	@BeforeClass
	public static void initialize() {
		try {
			client = new OslcClient();
			// Add a receptor to set the Authorization header as a default header for all requests
			// This is using deprecated code because OSLC4J OslcClient needs to be updated at some point
			Header header = new BasicHeader(HttpHeaders.AUTHORIZATION,  "Basic "+Base64.encode((userId+":"+password).getBytes()));
			List<Header> headers = new ArrayList<Header>();
			headers.add(header);
			RequestDefaultHeaders defaultHeaders = new RequestDefaultHeaders(headers);
			((DefaultHttpClient)client.getHttpClient()).addRequestInterceptor(defaultHeaders);

			String catalogUrl = serverUrl + "/services/catalog/singleton";
			
			String orgTitle = "IoT Platform Service Provider: "+orgId+"(/"+orgId+")";
			String serviceProviderUrl = client.lookupServiceProviderUrl(catalogUrl, orgTitle);

			queryCapability = client.lookupQueryCapability(serviceProviderUrl,
					Oslc_iotDomainConstants.IOT_PLATFORM_NAMSPACE, Oslc_iotDomainConstants.TYPE_DEVICETYPE);

			creationFactory = client.lookupCreationFactory(serviceProviderUrl,
					Oslc_iotDomainConstants.IOT_PLATFORM_NAMSPACE, Oslc_iotDomainConstants.TYPE_DEVICETYPE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testDeviceType() throws Exception {
		String uri = creationFactory.replace("devicetype", "iotDeviceTypes") + "/TestDeviceType";
		
		// TODO: Delete the resource if it exists

		// Create
		DeviceType deviceType = new DeviceType(new URI(uri));
		deviceType.setIdentifier("TestDeviceType");
		deviceType.setTitle("TestDeviceType");
		// TODO: add values to all the supported properties
		deviceType.setDescription("This is a test device that will be deleted");
		ClientResponse response = client.createResource(creationFactory, deviceType, OSLCConstants.CT_RDF);
		assertEquals(response.getStatusCode(), HttpStatus.SC_CREATED);
		logger.info("Created DeviceType: " + deviceType.getTitle());
		response.consumeContent();

		// Read
		deviceType = client.getResource(uri).getEntity(DeviceType.class);
		assertEquals("TestDeviceType", deviceType.getIdentifier());
		// TODO: Check all the read properties to be sure they are what was set when the resource was created
		logger.info("Read DeviceType: " + deviceType.getTitle());
		response.consumeContent();

		// Update
		deviceType.setDescription("This is the updated description");
		// TODO: make changes to more of the properties
		response = client.updateResource(uri, deviceType, OSLCConstants.CT_RDF);
		assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
		logger.info("Updated DeviceType: " + deviceType.getTitle());
		response.consumeContent();
		
		// Read back the DeviceType that was just updated
		deviceType = client.getResource(uri).getEntity(DeviceType.class);
		// TODO: Check that all of the updated properties have the expected values
		assertEquals(deviceType.getDescription(), "This is the updated description");
		logger.info("Read updated DeviceType: " + deviceType.getTitle());
		response.consumeContent();

		// Delete
		response = client.deleteResource(uri);
		assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
		logger.info("Deleted DeviceType: " + uri);
		response.consumeContent();

	}
}
