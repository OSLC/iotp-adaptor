/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2018. All Rights Reserved. 
 *
 * Note to U.S. Government Users Restricted Rights:  Use, duplication or 
 * disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.oslc.adaptor.iotp.impl;

public interface Constants {
	
	//default string values for IOT Watson Platform API
	public static String IOT_PLATFORM_BASE="internetofthings.ibmcloud.com";
	public static String IOT_PLATFORM_BASE_URL="https://internetofthings.ibmcloud.com";
	public static String IOT_PLATFORM_APP_VERSION="v0002";
	public static String BLUEID_LOGIN="https://internetofthings.ibmcloud.com/blueidlogin";
	public static String IBM_SECURITY_LOGOUT="https://idaas.iam.ibm.com/idaas/mtfim/sps/idaas/login/ibm_security_logout";
	public static String BASIC_LDAP_USER="https://idaas.iam.ibm.com/idaas/mtfim/sps/authsvc?PolicyId=urn:ibm:security:authentication:asf:basicldapuser";
	public static String SUBMIT_USER_URI="https://idaas.iam.ibm.com/v1/mgmt/idaas/user/identitysources";
	public static String SUBMIT_USER_PWD_URI="https://idaas.iam.ibm.com/authsvc/mtfim/sps/authsvc?StateId=";
	public static String USERID_DOMAIN="idaas.iam.ibm.com";
	
	//Default strings for Bluemix platform API
	public static String BLUEMIX_URI="https://api.ng.bluemix.net";
	
	//Properties file details
	public static String CONFIG_PROPERTIES = "config.properties";
	public static String POM_PROPERTIES = "META-INF/maven/com.ibm.oslc.adaptor/iotp/pom.properties";
	
	//Properties file error messages
	public static String UNABLE_TO_LOAD_PROPERTIES_FILE="Unable to load the properties found in the \"{0}\" file. Please ensure this file exists in the classpath and that your application server has read access to this file.";
	public static String UNABLE_TO_LOAD_POM_PROPERTIES_FILE="Unable to load the build properties found in the \"{0}\" file. Please ensure this file exists in the classpath and that your application server has read access to this file.";
	

}
