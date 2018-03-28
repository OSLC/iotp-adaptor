// Start of user code Copyright
/*******************************************************************************
 * Copyright (c) 2017 Jad El-khoury.
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
 *     Jad El-khoury        - initial implementation
 *     
 *******************************************************************************/
// End of user code

package com.ibm.oslc.adaptor.iotp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;

import org.eclipse.lyo.oslc4j.core.model.Link;
import org.eclipse.lyo.oslc4j.core.OSLC4JUtils;
import com.ibm.oslc.adaptor.iotp.resources.Space;
import com.ibm.oslc.adaptor.iotp.resources.Requirement;
import com.ibm.oslc.adaptor.iotp.resources.ThingTypeMapping;
import com.ibm.oslc.adaptor.iotp.resources.PhysicalInterface;
import com.ibm.oslc.adaptor.iotp.resources.Rule;
import com.ibm.oslc.adaptor.iotp.resources.App;
import com.ibm.oslc.adaptor.iotp.resources.CFService;
import com.ibm.oslc.adaptor.iotp.resources.Person;
import com.ibm.oslc.adaptor.iotp.resources.ThingType;
import com.ibm.oslc.adaptor.iotp.resources.Resource;
import com.ibm.oslc.adaptor.iotp.resources.MetaProperty;
import com.ibm.oslc.adaptor.iotp.resources.EventType;
import com.ibm.oslc.adaptor.iotp.resources.Schema;
import com.ibm.oslc.adaptor.iotp.resources.DeviceInfo;
import com.ibm.oslc.adaptor.iotp.resources.MetaData;
import com.ibm.oslc.adaptor.iotp.resources.Flow;
import com.ibm.oslc.adaptor.iotp.resources.Device;
import com.ibm.oslc.adaptor.iotp.resources.ChangeRequest;
import com.ibm.oslc.adaptor.iotp.resources.DeviceTypeMapping;
import com.ibm.oslc.adaptor.iotp.resources.DeviceType;
import com.ibm.oslc.adaptor.iotp.resources.LogicalInterface;
import com.ibm.oslc.adaptor.iotp.resources.Discussion;
import com.ibm.oslc.adaptor.iotp.resources.Thing;
import com.ibm.oslc.adaptor.iotp.resources.NodeREDApp;

// Start of user code imports
// End of user code

// Start of user code pre_class_code
// End of user code

public class CE4IoTConnectorResourcesFactory {

    // Start of user code class_attributes
    // End of user code
    
    // Start of user code class_methods
    // End of user code

    //methods for ThingType resource
    public static ThingType createThingType(final String iotId, final String thingTypeId)
           throws URISyntaxException
    {
        return new ThingType(constructURIForThingType(iotId, thingTypeId));
    }
    
    public static URI constructURIForThingType(final String iotId, final String thingTypeId)
    {
        String basePath = OSLC4JUtils.getServletURI();
        Map<String, Object> pathParameters = new HashMap<String, Object>();
        pathParameters.put("iotId", iotId);
        pathParameters.put("thingTypeId", thingTypeId);
        String instanceURI = "iotp/{iotId}/resources/thingTypes/{thingTypeId}";
    
        final UriBuilder builder = UriBuilder.fromUri(basePath);
        return builder.path(instanceURI).buildFromMap(pathParameters);
    }
    
    public static Link constructLinkForThingType(final String iotId, final String thingTypeId , final String label)
    {
        return new Link(constructURIForThingType(iotId, thingTypeId), label);
    }
    
    public static Link constructLinkForThingType(final String iotId, final String thingTypeId)
    {
        return new Link(constructURIForThingType(iotId, thingTypeId));
    }
    

    //methods for Space resource
    public static Space createSpace(final String bmxId, final String spaceId)
           throws URISyntaxException
    {
        return new Space(constructURIForSpace(bmxId, spaceId));
    }
    
    public static URI constructURIForSpace(final String bmxId, final String spaceId)
    {
        String basePath = OSLC4JUtils.getServletURI();
        Map<String, Object> pathParameters = new HashMap<String, Object>();
        pathParameters.put("bmxId", bmxId);
        pathParameters.put("spaceId", spaceId);
        String instanceURI = "bmx/{bmxId}/resources/spaces/{spaceId}";
    
        final UriBuilder builder = UriBuilder.fromUri(basePath);
        return builder.path(instanceURI).buildFromMap(pathParameters);
    }
    
    public static Link constructLinkForSpace(final String bmxId, final String spaceId , final String label)
    {
        return new Link(constructURIForSpace(bmxId, spaceId), label);
    }
    
    public static Link constructLinkForSpace(final String bmxId, final String spaceId)
    {
        return new Link(constructURIForSpace(bmxId, spaceId));
    }
    

    //methods for EventType resource
    public static EventType createEventType(final String iotId, final String eventTypeId)
           throws URISyntaxException
    {
        return new EventType(constructURIForEventType(iotId, eventTypeId));
    }
    
    public static URI constructURIForEventType(final String iotId, final String eventTypeId)
    {
        String basePath = OSLC4JUtils.getServletURI();
        Map<String, Object> pathParameters = new HashMap<String, Object>();
        pathParameters.put("iotId", iotId);
        pathParameters.put("eventTypeId", eventTypeId);
        String instanceURI = "iotp/{iotId}/resources/eventTypes/{eventTypeId}";
    
        final UriBuilder builder = UriBuilder.fromUri(basePath);
        return builder.path(instanceURI).buildFromMap(pathParameters);
    }
    
    public static Link constructLinkForEventType(final String iotId, final String eventTypeId , final String label)
    {
        return new Link(constructURIForEventType(iotId, eventTypeId), label);
    }
    
    public static Link constructLinkForEventType(final String iotId, final String eventTypeId)
    {
        return new Link(constructURIForEventType(iotId, eventTypeId));
    }
    

    //methods for Requirement resource
    

    //methods for Schema resource
    public static Schema createSchema(final String iotId, final String schemaId)
           throws URISyntaxException
    {
        return new Schema(constructURIForSchema(iotId, schemaId));
    }
    
    public static URI constructURIForSchema(final String iotId, final String schemaId)
    {
        String basePath = OSLC4JUtils.getServletURI();
        Map<String, Object> pathParameters = new HashMap<String, Object>();
        pathParameters.put("iotId", iotId);
        pathParameters.put("schemaId", schemaId);
        String instanceURI = "iotp/{iotId}/resources/schemas/{schemaId}";
    
        final UriBuilder builder = UriBuilder.fromUri(basePath);
        return builder.path(instanceURI).buildFromMap(pathParameters);
    }
    
    public static Link constructLinkForSchema(final String iotId, final String schemaId , final String label)
    {
        return new Link(constructURIForSchema(iotId, schemaId), label);
    }
    
    public static Link constructLinkForSchema(final String iotId, final String schemaId)
    {
        return new Link(constructURIForSchema(iotId, schemaId));
    }
    

    //methods for PhysicalInterface resource
    public static PhysicalInterface createPhysicalInterface(final String iotId, final String physicalInterfaceId)
           throws URISyntaxException
    {
        return new PhysicalInterface(constructURIForPhysicalInterface(iotId, physicalInterfaceId));
    }
    
    public static URI constructURIForPhysicalInterface(final String iotId, final String physicalInterfaceId)
    {
        String basePath = OSLC4JUtils.getServletURI();
        Map<String, Object> pathParameters = new HashMap<String, Object>();
        pathParameters.put("iotId", iotId);
        pathParameters.put("physicalInterfaceId", physicalInterfaceId);
        String instanceURI = "iotp/{iotId}/resources/physicalInterfaces/{physicalInterfaceId}";
    
        final UriBuilder builder = UriBuilder.fromUri(basePath);
        return builder.path(instanceURI).buildFromMap(pathParameters);
    }
    
    public static Link constructLinkForPhysicalInterface(final String iotId, final String physicalInterfaceId , final String label)
    {
        return new Link(constructURIForPhysicalInterface(iotId, physicalInterfaceId), label);
    }
    
    public static Link constructLinkForPhysicalInterface(final String iotId, final String physicalInterfaceId)
    {
        return new Link(constructURIForPhysicalInterface(iotId, physicalInterfaceId));
    }
    

    //methods for Rule resource
    public static Rule createRule(final String iotId, final String ruleId)
           throws URISyntaxException
    {
        return new Rule(constructURIForRule(iotId, ruleId));
    }
    
    public static URI constructURIForRule(final String iotId, final String ruleId)
    {
        String basePath = OSLC4JUtils.getServletURI();
        Map<String, Object> pathParameters = new HashMap<String, Object>();
        pathParameters.put("iotId", iotId);
        pathParameters.put("ruleId", ruleId);
        String instanceURI = "iotp/{iotId}/resources/rules/{ruleId}";
    
        final UriBuilder builder = UriBuilder.fromUri(basePath);
        return builder.path(instanceURI).buildFromMap(pathParameters);
    }
    
    public static Link constructLinkForRule(final String iotId, final String ruleId , final String label)
    {
        return new Link(constructURIForRule(iotId, ruleId), label);
    }
    
    public static Link constructLinkForRule(final String iotId, final String ruleId)
    {
        return new Link(constructURIForRule(iotId, ruleId));
    }
    

    //methods for Device resource
    public static Device createDevice(final String iotId, final String typeId, final String deviceId)
           throws URISyntaxException
    {
        return new Device(constructURIForDevice(iotId, typeId, deviceId));
    }
    
    public static URI constructURIForDevice(final String iotId, final String typeId, final String deviceId)
    {
        String basePath = OSLC4JUtils.getServletURI();
        Map<String, Object> pathParameters = new HashMap<String, Object>();
        pathParameters.put("iotId", iotId);
        pathParameters.put("typeId", typeId);
        pathParameters.put("deviceId", deviceId);
        String instanceURI = "iotp/{iotId}/resources/devices/{typeId}/devices/{deviceId}";
    
        final UriBuilder builder = UriBuilder.fromUri(basePath);
        return builder.path(instanceURI).buildFromMap(pathParameters);
    }
    
    public static Link constructLinkForDevice(final String iotId, final String typeId, final String deviceId , final String label)
    {
        return new Link(constructURIForDevice(iotId, typeId, deviceId), label);
    }
    
    public static Link constructLinkForDevice(final String iotId, final String typeId, final String deviceId)
    {
        return new Link(constructURIForDevice(iotId, typeId, deviceId));
    }
    

    //methods for ChangeRequest resource
    

    //methods for DeviceType resource
    public static DeviceType createDeviceType(final String iotId, final String deviceTypeId)
           throws URISyntaxException
    {
        return new DeviceType(constructURIForDeviceType(iotId, deviceTypeId));
    }
    
    public static URI constructURIForDeviceType(final String iotId, final String deviceTypeId)
    {
        String basePath = OSLC4JUtils.getServletURI();
        Map<String, Object> pathParameters = new HashMap<String, Object>();
        pathParameters.put("iotId", iotId);
        pathParameters.put("deviceTypeId", deviceTypeId);
        String instanceURI = "iotp/{iotId}/resources/deviceTypes/{deviceTypeId}";
    
        final UriBuilder builder = UriBuilder.fromUri(basePath);
        return builder.path(instanceURI).buildFromMap(pathParameters);
    }
    
    public static Link constructLinkForDeviceType(final String iotId, final String deviceTypeId , final String label)
    {
        return new Link(constructURIForDeviceType(iotId, deviceTypeId), label);
    }
    
    public static Link constructLinkForDeviceType(final String iotId, final String deviceTypeId)
    {
        return new Link(constructURIForDeviceType(iotId, deviceTypeId));
    }
    

    //methods for LogicalInterface resource
    public static LogicalInterface createLogicalInterface(final String iotId, final String logicalInterfaceId)
           throws URISyntaxException
    {
        return new LogicalInterface(constructURIForLogicalInterface(iotId, logicalInterfaceId));
    }
    
    public static URI constructURIForLogicalInterface(final String iotId, final String logicalInterfaceId)
    {
        String basePath = OSLC4JUtils.getServletURI();
        Map<String, Object> pathParameters = new HashMap<String, Object>();
        pathParameters.put("iotId", iotId);
        pathParameters.put("logicalInterfaceId", logicalInterfaceId);
        String instanceURI = "iotp/{iotId}/resources/logicalInterfaces/{logicalInterfaceId}";
    
        final UriBuilder builder = UriBuilder.fromUri(basePath);
        return builder.path(instanceURI).buildFromMap(pathParameters);
    }
    
    public static Link constructLinkForLogicalInterface(final String iotId, final String logicalInterfaceId , final String label)
    {
        return new Link(constructURIForLogicalInterface(iotId, logicalInterfaceId), label);
    }
    
    public static Link constructLinkForLogicalInterface(final String iotId, final String logicalInterfaceId)
    {
        return new Link(constructURIForLogicalInterface(iotId, logicalInterfaceId));
    }
    

    //methods for Thing resource
    

    //methods for NodeREDApp resource
    public static NodeREDApp createNodeREDApp(final String bmxId, final String nodeREDAppId)
           throws URISyntaxException
    {
        return new NodeREDApp(constructURIForNodeREDApp(bmxId, nodeREDAppId));
    }
    
    public static URI constructURIForNodeREDApp(final String bmxId, final String nodeREDAppId)
    {
        String basePath = OSLC4JUtils.getServletURI();
        Map<String, Object> pathParameters = new HashMap<String, Object>();
        pathParameters.put("bmxId", bmxId);
        pathParameters.put("nodeREDAppId", nodeREDAppId);
        String instanceURI = "bmx/{bmxId}/resources/nodeREDApps/{nodeREDAppId}";
    
        final UriBuilder builder = UriBuilder.fromUri(basePath);
        return builder.path(instanceURI).buildFromMap(pathParameters);
    }
    
    public static Link constructLinkForNodeREDApp(final String bmxId, final String nodeREDAppId , final String label)
    {
        return new Link(constructURIForNodeREDApp(bmxId, nodeREDAppId), label);
    }
    
    public static Link constructLinkForNodeREDApp(final String bmxId, final String nodeREDAppId)
    {
        return new Link(constructURIForNodeREDApp(bmxId, nodeREDAppId));
    }
    

}
