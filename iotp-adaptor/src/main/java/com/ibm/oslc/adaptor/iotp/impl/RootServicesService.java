/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 *  
 *  The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 *  and the Eclipse Distribution License is available at
 *  http://www.eclipse.org/org/documents/edl-v10.php.
 *  
 *  Contributors:
 *  
 *     IBM Corporation - initial API and implementation
 *     Samuel Padgett  - fix rootservices about URI
 *******************************************************************************/
package com.ibm.oslc.adaptor.iotp.impl;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.lyo.oslc4j.core.OSLC4JUtils;

import com.ibm.oslc.adaptor.iotp.servlet.ServiceProviderCatalogSingleton;
import com.ibm.oslc.adaptor.iotp.servlet.ServletListener;


/**
 * Jazz Root Services Service, see:
 *	https://jazz.net/wiki/bin/view/Main/RootServicesSpec
 *	https://jazz.net/wiki/bin/view/Main/RootServicesSpecAddendum2
 */
public class RootServicesService extends HttpServlet {    	

	private static final long serialVersionUID = -8125286361811879744L;

	/**
	 * Return a Rational Jazz compliant root services document
	 * 
	 * See https://jazz.net/wiki/bin/view/Main/RootServicesSpec
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        
        request.setAttribute("baseUri", OSLC4JUtils.getPublicURI());
        request.setAttribute("catalogUri", ServiceProviderCatalogSingleton.getUri().toString());
        request.setAttribute("oauthDomain",OSLC4JUtils.getPublicURI());
        request.setAttribute("about", OSLC4JUtils.getPublicURI() + request.getServletPath());
		final RequestDispatcher rd = request.getRequestDispatcher("/rootservices_rdfxml.jsp"); 
		rd.forward(request, response);
		response.flushBuffer();
	}
}
 