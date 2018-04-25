/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2017. All Rights Reserved. 
 *
 * Note to U.S. Government Users Restricted Rights:  Use, duplication or 
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.oslc.adaptor.iotp.impl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.ibm.oslc.adaptor.bmx.impl.BluemixClient;
import com.ibm.oslc.adaptor.bmx.impl.Organization;
import com.ibm.oslc.adaptor.iotp.BmxServiceProviderInfo;
import com.ibm.oslc.adaptor.iotp.IotpServiceProviderInfo;

/**
 * The implementation class to access the IoT API for IoTPlatform
 *
 */
public class IoTAPIImplementation {

	public static final String API_PARAMETER_AUTHENTICATION_TOKEN = "Authentication-Token";
	public static final String API_PARAMETER_API_KEY = "API-Key";
	public static final String API_PARAMETER_ORGANIZATION_ID = "Organization-ID";
	private static Logger log = LoggerFactory.getLogger(IoTAPIImplementation.class);


	/** Get information about the ServiceProviders from the IoT Platform organizations.
	 * 
	 * @param httpServletRequest
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws IoTFCReSTException
	 */
	public static IotpServiceProviderInfo[] getIotpServiceProviderInfos(HttpServletRequest httpServletRequest)
			throws KeyManagementException, NoSuchAlgorithmException {
		IotpServiceProviderInfo[] spcs = new IotpServiceProviderInfo[0];

		// Get all the organizations accessible by the functional user
		// Create a service provider for each organization

		IotpServiceProviderInfo serviceProviderInfo;
		int index;
		try {
			IoTPClient iotpClient = (IoTPClient)httpServletRequest.getSession().getAttribute(IoTPClient.IOTPCLIENT_ATTRIBUTE);
			JsonArray orgs = iotpClient.getOrganizations();
			spcs = new IotpServiceProviderInfo[orgs.size()];
			index = 0;
			for (JsonElement org: orgs) {
				serviceProviderInfo = new IotpServiceProviderInfo();
				serviceProviderInfo.name = org.getAsJsonObject().get("id").getAsString();
				serviceProviderInfo.iotId = serviceProviderInfo.name;
				serviceProviderInfo.platformBase = getPlatformBase();
				serviceProviderInfo.apiVersion = getApiVersion();
				spcs[index] = serviceProviderInfo;
				index++;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error("Unable to read the IoT Platform organizations: {}", e.getMessage());
		}

		return spcs;
	}
	
	/** Get information about the ServiceProviders from the Bluemix organizations.
	 * 
	 * @param httpServletRequest
	 * @return
	 * @throws KeyManagementException
	 * @throws NoSuchAlgorithmException
	 * @throws IoTFCReSTException
	 */
	public static BmxServiceProviderInfo[] getBmxServiceProviderInfos(HttpServletRequest httpServletRequest)
			throws KeyManagementException, NoSuchAlgorithmException {
		BmxServiceProviderInfo[] spcs = new BmxServiceProviderInfo[0];

		// Get all the organizations accessible by the functional user
		// Create a service provider for each organization

		BmxServiceProviderInfo serviceProviderInfo;
		int index;
		try {
			BluemixClient bmxClient = (BluemixClient)httpServletRequest.getSession().getAttribute(BluemixClient.BMXCLIENT_ATTRIBUTE);
			ArrayList<Organization> bmxOrgs = bmxClient.getOrganizations();
			spcs = new BmxServiceProviderInfo[bmxOrgs.size()];
			index = 0;
			for (Organization org : bmxOrgs) {
				serviceProviderInfo = new BmxServiceProviderInfo();
				serviceProviderInfo.name = org.getName();
				serviceProviderInfo.bmxId = org.getGuid();
				serviceProviderInfo.bmxOrg = org;
				spcs[index] = serviceProviderInfo;
				index++;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			log.error("Unable to read the Bluemix organizations: {}", e.getMessage());
		}

		return spcs;
	}
	
	public static String getPlatformBase() {
		String platformBase = System.getProperty("iot.platform.base");
		if (platformBase == null) {
			log.warn("System property iot.platform.base is not set, using default");
			platformBase = "internetofthings.ibmcloud.com";
		}
		return platformBase;
	}
	
	public static String getApiVersion() {
		String apiVersion = System.getProperty("iot.platform.api.version");
		if (apiVersion == null) {
			log.warn("System property iot.platform.api.version is not set, using default");
			apiVersion = "v0002";
		}
		return apiVersion;
	}

	/**
	 * @param httpServletRequest
	 * @param serviceProviderId
	 * @return
	 * @throws Exception
	 */
	public static final IotpServiceProviderInfo getIotpServiceProviderInfo(final HttpServletRequest httpServletRequest,
			final String serviceProviderId) throws Exception {
		// Need to re-read the organizations in case another was added
		IotpServiceProviderInfo[] serviceProviderInfos = getIotpServiceProviderInfos(httpServletRequest);
		for (IotpServiceProviderInfo serviceProviderInfo : serviceProviderInfos) {
			if (serviceProviderInfo.iotId.equals(serviceProviderId)) {
				return serviceProviderInfo;
			}
		}
		throw new Exception("ServiceProvider for Id " + serviceProviderId + "  not found.");
	}
	
	/**
	 * @param httpServletRequest
	 * @param serviceProviderId
	 * @return
	 * @throws Exception
	 */
	public static final BmxServiceProviderInfo getBmxServiceProviderInfo(final HttpServletRequest httpServletRequest,
			final String serviceProviderId) throws Exception {
		// Need to re-read the organizations in case another was added
		BmxServiceProviderInfo[] serviceProviderInfos = getBmxServiceProviderInfos(httpServletRequest);
		for (BmxServiceProviderInfo serviceProviderInfo : serviceProviderInfos) {
			if (serviceProviderInfo.bmxId.equals(serviceProviderId)) {
				return serviceProviderInfo;
			}
		}
		throw new Exception("ServiceProvider for Id " + serviceProviderId + "  not found.");
	}

}
