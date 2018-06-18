<!DOCTYPE html>
<%--
 Copyright (c) 2011, 2012 IBM Corporation and others.

 All rights reserved. This program and the accompanying materials
 are made available under the terms of the Eclipse Public License v1.0
 and Eclipse Distribution License v. 1.0 which accompanies this distribution.

 The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 and the Eclipse Distribution License is available at
 http://www.eclipse.org/org/documents/edl-v10.php.

 Contributors:

  Sam Padgett     - initial API and implementation
  Michael Fiedler - adapted for OSLC4J
  Jad El-khoury   - initial implementation of code generator (https://bugs.eclipse.org/bugs/show_bug.cgi?id=422448)

--%>

<%@page import="org.eclipse.lyo.oslc4j.core.model.ServiceProvider"%>
<%@page import="java.util.List" %>
<%@page import="com.ibm.oslc.adaptor.iotp.resources.Device"%>
<%@page import="org.eclipse.lyo.oslc4j.core.OSLC4JUtils"%>
<%@page import="javax.ws.rs.core.UriBuilder"%>

<%@ page contentType="text/html" language="java" pageEncoding="UTF-8" %>

<%
  String creatorUri = (String) request.getAttribute("creatorUri");
  String iotId = (String) request.getAttribute("iotId");
%>

        <table style="clear: both;">
          <tr>
            <td><%= Device.typeIdToHtmlForCreation(request)%></td>
          </tr>
          <tr>
            <td><%= Device.identifierToHtmlForCreation(request)%></td>
          </tr>
          <tr>
            <td><%= Device.descriptionToHtmlForCreation(request)%></td>
          </tr>
          <tr>
          	<td><label for="authToken">Authentication Token: </label>
          	<input type="text" name="authToken" style="width: 400px" id="authToken"></td>
          </tr>
          <tr>
            <td>
              <input type="button"
                value="Submit"
                onclick="javascript: create(creatorUri)">
              <input type="reset">
            </td>
          </tr>
        </table>
        <div style="width: 500px;">
        </div>
