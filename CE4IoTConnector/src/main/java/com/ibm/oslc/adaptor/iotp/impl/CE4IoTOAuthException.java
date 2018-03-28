/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation.
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
 *******************************************************************************/
package com.ibm.oslc.adaptor.iotp.impl;

import org.eclipse.lyo.server.oauth.core.utils.UnauthorizedException;

import net.oauth.OAuthException;

/**
 * A special unauthorized exception indicating an OAuth problem.
 * 
 * @author Samuel Padgett <spadgett@us.ibm.com>
 */
public class CE4IoTOAuthException extends UnauthorizedException {
	public CE4IoTOAuthException(OAuthException e) {
		super(e);
	}
}
