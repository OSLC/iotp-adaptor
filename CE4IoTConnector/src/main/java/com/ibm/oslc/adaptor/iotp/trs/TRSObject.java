/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
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
 *    Ernest Mah - Initial implementation
 *    David Terry - TRS 2.0 compliant implementation
 *******************************************************************************/

package com.ibm.oslc.adaptor.iotp.trs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.cookie.MalformedCookieException;
import org.eclipse.lyo.core.trs.Base;
import org.eclipse.lyo.core.trs.ChangeLog;
import org.eclipse.lyo.core.trs.Creation;
import org.eclipse.lyo.core.trs.Deletion;
import org.eclipse.lyo.core.trs.Modification;
import org.eclipse.lyo.core.trs.Page;
import org.eclipse.lyo.core.trs.TRSConstants;
import org.eclipse.lyo.oslc4j.core.OSLC4JUtils;
import org.eclipse.lyo.server.oauth.core.utils.UnauthorizedException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ibm.oslc.adaptor.bmx.impl.BluemixClient;
import com.ibm.oslc.adaptor.iotp.impl.IoTAPIImplementation;
import com.ibm.oslc.adaptor.iotp.impl.IoTPClient;

/**
 * @author jamsden
 * 
 * The IoT Platform does not support any plugin or extensibility mechanism
 * to add code to detect changes in the IoT Platform resources, nor does it
 * provide any event API to notify of platform changes. So the only way to
 * detect create, delete and modification events on IoT Platform resources
 * is to periodically read all resources of all organizations, and compare
 * resource snapshots to see what changed. This could be inefficient and
 * present an undesirable load on the IoT Platform server. But it is the 
 * only option for a TRS provider. Such a provider should be polled 
 * infrequently in production environments. 
 * 
 */

public class TRSObject {
	
	/**
	 * Mutex
	 */
	private static String mutex = "";//$NON-NLS-1$
	private static int basePagenum = 1;
	private static Base baseResources = null;
	private static Base lastSnapshot = null;
	private static ChangeLog changeLogs = null;
	private static int changeOrder = 0;
	
	private static Map<String, Date> lastUpdateDateTimes = new HashMap<String, Date>();
	
	private static final SimpleDateFormat XSD_DATETIME_FORMAT;
	static {
		XSD_DATETIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");//$NON-NLS-1$
		XSD_DATETIME_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));//$NON-NLS-1$
	}
	private static DateFormat createDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
	private static DateFormat updateDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	
	private static Properties configProperties = new Properties();
	static {
		try {
			configProperties.load(TRSObject.class.getResourceAsStream("/config.properties"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	/** Builds both the base resources and change log
	 * 
	 * @param httpServletRequest
	 * @throws IOException 
	 */
	public static void buildBaseResourcesAndChangeLogs(HttpServletRequest httpServletRequest) throws URISyntaxException, IOException {
		synchronized (mutex) {
			TRSObject.buildBaseResourcesAndChangeLogsWithScanning(httpServletRequest);
		}
	}

	/** Generates and returns the Base resources by page number.
	 * 
	 * @param pagenum
	 * @param httpServletRequest
	 * @return  the Base resources
	 * @throws URISyntaxException
	 * @throws IOException 
	 */
	public static Base getBaseResource(String pagenum, HttpServletRequest httpServletRequest) throws URISyntaxException, IOException {
		synchronized (mutex) {
			TRSObject.buildBaseResourcesAndChangeLogsWithScanning(httpServletRequest);
			return baseResources;
		}
	}

	/** Generates and returns the ChangeLog by page number.
	 * 
	 * @param pagenum
	 * @param httpServletRequest
	 * @return the ChangeLog
	 * @throws URISyntaxException
	 * @throws IOException 
	 */
	public static ChangeLog getChangeLog(String pagenum, HttpServletRequest httpServletRequest)  throws URISyntaxException, IOException {
		synchronized (mutex) {
			TRSObject.buildBaseResourcesAndChangeLogsWithScanning(httpServletRequest);
			// changeLogs might be null
			return changeLogs;
		}
	}

	/** Internal method to build the base resources and change log for all tracked resources.
	 * 
	 * @param httpServletRequest
	 * @throws IOException 
	 */
	private static void buildBaseResourcesAndChangeLogsWithScanning(HttpServletRequest httpServletRequest)  throws URISyntaxException, IOException {
		IoTPClient client = (IoTPClient)httpServletRequest.getSession().getAttribute(IoTPClient.IOTPCLIENT_ATTRIBUTE);
		if (baseResources == null) {
			// rebuild the complete base and restart the change log - this is a reindex operation
			baseResources = getBase(client);  // establish the initial base resources
	        // Get the resource's last update date and save it with the base
	        for (URI member: baseResources.getMembers()) {
	        		JsonElement resource = client.readIoTResource(getOrgId(member), getResourceRelativeURI(member));
	        		lastUpdateDateTimes.put(member.toString(), getUpdated(resource.getAsJsonObject()));
	        }
		}
		updateChangeLog(client, baseResources);  // the change log accumulates create, delete and modify change events on the base			
	}	

	private static Base getBase(IoTPClient client) throws URISyntaxException, IOException {
	 	Base base = new Base();
	 	JsonArray orgs = null;
		try {
			orgs = client.getOrganizations();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        for (JsonElement org: orgs) {
        		String orgId = org.getAsJsonObject().get("id").getAsString();

        		// Create the TRS Base
	    		base.setAbout(URI.create(OSLC4JUtils.getPublicURI()
	    				+ "/services/trs/" + TRSConstants.TRS_TERM_BASE));//$NON-NLS-1$
	    		base.setCutoffEvent(new URI(TRSConstants.RDF_NIL));

	    		// The base has to have at least one page
	    		Page page = new Page();
	    		page.setAbout(URI.create(OSLC4JUtils.getPublicURI()
	    				+ "/services/trs/" + TRSConstants.TRS_TERM_BASE + "/"+String.valueOf(basePagenum)));//$NON-NLS-1$);
	    		page.setNextPage(new URI(TRSConstants.RDF_NIL));
	    		page.setPageOf(base);

	    		base.setNextPage(page);

	    		// Now read all the device types in the organization and add them to the base resources
            JsonArray deviceTypes = client.readIoTResource(orgId, "device/types").getAsJsonObject().get("results").getAsJsonArray();
            for (JsonElement deviceType: deviceTypes) {
            		String deviceTypeId = deviceType.getAsJsonObject().get("id").getAsString();
            		URI uri = URI.create(OSLC4JUtils.getPublicURI()+"/services/iotp/"+orgId+"/resources/deviceTypes/"+deviceTypeId);
            		base.getMembers().add(uri);
            }
        }
		return base;
	}

	/** Get the updated Date of a DeviceType JSON element.
	 * @param resource
	 * @return
	 */
	private static Date getUpdated(JsonObject resource) {
		Date updated = null;
		JsonElement element = null;
		try {
			element = resource.get("updatedDateTime");
			if (element != null) updated = updateDateFormat.parse(element.getAsString());
		} catch (ParseException exc) {
			// the updated format is the same as the created format when the device type is first deployed
			try {
				if (updated == null) updated = createDateFormat.parse(element.getAsString());
			} catch (ParseException e) {				
			}
		}
		return updated;
	}


	/** Calculates the ChangeLog by comparing the current IoT Platform resources against 
	 * the last time they were read and finding any that were created, deleted or modified.
	 * @param base
	 * @return
	 * @throws IOException 
	 * @throws URISyntaxException 
	 */
	private static void updateChangeLog(IoTPClient client, Base base) throws URISyntaxException, IOException {
		// is this the first time the change log is being updated?
		if (lastSnapshot == null) {
			changeLogs = new ChangeLog();
			changeLogs.setAbout(URI.create(OSLC4JUtils.getPublicURI()
	    				+ "/services/trs/" + TRSConstants.TRS_TERM_CHANGE_LOG));
			lastSnapshot = base;
			return;
		}
		// Get the latest set of resources
		Base latestSnapshot = getBase(client);
		changeOrder++; // increment the order number for this segment of change events in the change log
		List<URI> resourcesToRemove = new ArrayList<URI>(lastSnapshot.getMembers()); // assume we remove all resources
		for (URI member: latestSnapshot.getMembers()) {
			if (!lastSnapshot.getMembers().contains(member)) {
				// Got a new resource since last check
				Creation createEvent = new Creation();
				Date now = new Date();
				URI eventURI = URI.create("urn:urn-3:" + "trs-provider" + ":" + XSD_DATETIME_FORMAT.format(now) + ":" + changeOrder);
				createEvent.setAbout(eventURI);
				createEvent.setChanged(member);
				createEvent.setOrder(changeOrder);
				changeLogs.getChange().add(createEvent);
				JsonElement resource = client.readIoTResource(getOrgId(member), getResourceRelativeURI(member));
				lastUpdateDateTimes.put(member.toString(), getUpdated(resource.getAsJsonObject()));
			} else {
				// resource was in last snapshot, so don't remove it
				resourcesToRemove.remove(member);
				// See if the resource changed since the last snapshot
				JsonElement resource = client.readIoTResource(getOrgId(member), getResourceRelativeURI(member));
				Date updated = getUpdated(resource.getAsJsonObject());
				if (updated.after((Date)lastUpdateDateTimes.get(member.toString()))) {
					// Its been changed
        				lastUpdateDateTimes.put(member.toString(), updated);
        				Modification modEvent = new Modification();
        				Date now = new Date();
        				URI eventURI = URI.create("urn:urn-3:" + "trs-provider" + ":" + XSD_DATETIME_FORMAT.format(now) + ":" + changeOrder);
        				modEvent.setAbout(eventURI);
        				modEvent.setChanged(member);
        				modEvent.setOrder(changeOrder);
        				changeLogs.getChange().add(modEvent);
				}
			}
		}
		// Remove all the resources that were deleted since the last snapshot
		for (URI member: resourcesToRemove) {
			Deletion deleteEvent = new Deletion();
			Date now = new Date();
			URI eventURI = URI.create("urn:urn-3:" + "trs-provider" + ":" + XSD_DATETIME_FORMAT.format(now) + ":" + changeOrder);
			deleteEvent.setAbout(eventURI);
			deleteEvent.setChanged(member);
			deleteEvent.setOrder(changeOrder);
			changeLogs.getChange().add(deleteEvent);
			lastUpdateDateTimes.remove(member.toString());
		}
		lastSnapshot = latestSnapshot;
	}
	
	
	private static String getOrgId(URI oslcUri) {
		String result = null;
		Matcher matcher = Pattern.compile(".*/services/iotp/(.*?)/").matcher(oslcUri.toString());
		if (matcher.find()) result = matcher.group(1);
		return result;
	}

	private static String getResourceRelativeURI(URI oslcUri) {
		String result = null;
		Matcher matcher = Pattern.compile(".*/resources/deviceTypes/(.*)").matcher(oslcUri.toString());
		if (matcher.find()) result = "device/types/"+matcher.group(1);
		return result;
	}

	public static void setClient(HttpServletRequest httpServletRequest) throws KeyManagementException, NoSuchAlgorithmException, UnauthorizedException, MalformedCookieException, IOException {
		IoTPClient iotConnector = null;
		BluemixClient bmxConnector = null;
		String trs_user = configProperties.getProperty("trs_user");
		String trs_user_password=configProperties.getProperty("trs_user_password");
		
		String iotpPlatformBase = IoTAPIImplementation.getPlatformBase();
		iotConnector = new IoTPClient(trs_user, trs_user_password);
		
		iotConnector.login();		
		bmxConnector = new BluemixClient(trs_user, trs_user_password);
		bmxConnector.login();
		httpServletRequest.getSession().setAttribute(IoTPClient.IOTPCLIENT_ATTRIBUTE, iotConnector);
		httpServletRequest.getSession().setAttribute(BluemixClient.BMXCLIENT_ATTRIBUTE, bmxConnector);
		
	}

	public static void logOffClient(HttpServletRequest httpServletRequest) {
		IoTPClient client = (IoTPClient)httpServletRequest.getSession().getAttribute(IoTPClient.IOTPCLIENT_ATTRIBUTE);
		client.logoff();
		BluemixClient bmxClient = (BluemixClient)httpServletRequest.getSession().getAttribute(BluemixClient.BMXCLIENT_ATTRIBUTE);
		bmxClient.logoff();
	}


}
