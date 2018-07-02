# iot-adapter

A project to provide a sample OSLC adaptor and TRS provider for the IBM Watson IoT Platform and Bluemix, developed using the eclipse/Lyo Designer code generator. This sample code is used in the [OSLC Developer Guide](http://oslc.github.io/developing-oslc-applications/).

For additional information, see:

 * [iotp-adaptor User Guide](http://oslc.github.io/developing-oslc-applications/iotp_adaptor/userGuide/user-guide) is a simple user's guide for installing, configuring, administering and using iotp-adapter with the CE tools.
 * [iotp-adaptor Developer Guide](http://oslc.github.io/developing-oslc-applications/iotp_adaptor/developer-guide) provides the complete documentation on how the server was developed.

## Synopsis

The iotp-adaptor project provides sample code that demonstrates how to build a aimple OSLC adaptor on an existing data source, in this case, IBM Watson IoT Platform resources (DeviceType, Device, LogicalInterface, PhysicalInterface, Rule, etc.) iotp-adapter demonstrates how to use eclipse [Lyo Designer](https://wiki.eclipse.org/Lyo/ToolchainModellingAndCodeGenerationWorkshop) to model an OSLC toolchain and generate an OSLC server implementation that can be easily adapted to existing data sources. The generated server provides the following OSLC capabilities:

* **CRUD operations** - on resources using RDF resource representations (provides predictable resource formats, rich semantics, and goos support for links)
* **Service discovery** - ServiceProviderCatalog, ServiceProvicers (containers of managed resources), Services that describe what OSLC capabilities are provided on what resources
* **Query Capability** – persistent independent query capability for integration
* **Delegated dialogs** – to allow an application to create and select resources in another application for the purpose of establishing links
* **Resource preview** – to provide icons and labels in order to view a link to a resource managed by another tool
* **Tracked Resource Sets** – in order to efficiently contribute data from many data sources into a single repository for cross-tool views, queries and reporting
 

## Motivation

The motivation for this project is to capture all the information you will need to create an OSLC adaptor for your data sources that will integrate with the IBM jazz.net base Continuous Engineering solution tools (RDNG, RTC, RQM, etc.). There is really no better way to capture this information than through a concrete worked example. The sample code shows you how to:

* Develop an OSLC toolchain model using Lyo Designer and generate the application
* How to implement the generated ConnectorManager interface to the adapted data sources
* Provide a rootservices document for discovering server discovery capabilities
* Establish Consumer/Friend relationships using OAuth to allow servers to interact
* Create project area artifact container associations to enable linking between jazz.net CE tools and OSLC resources provided by adaptors
* Know what link types are available in each of the applications based on the chosen artifact container association
* Understand the specific integration requirements of each CE application (RDNG, RTC and RQM) that you need to know to get the integrations working


There's also a lot of reusable code here, in particular the CredentialsFilter class that deals with authentication. 


## Installation

CE4IoTConnector is an OSLC server, implemented as a dynamic Web application in a standard JEE WAR file that can be deployed to a JEE enabled Web Server such as WebSphere Liberty, Tomcat, Jetty, etc. See the [User Guide](http://oslc.github.io/developing-oslc-applications/iotp_adaptor/userGuide/user-guide), section [Intallation and Configuration[(http://oslc.github.io/developing-oslc-applications/iotp_adaptor/userGuide/install-and-config.html) for details. 

## Tests

There are some sample JUnit test cases that test the OSLC CRUD capabilities. 

## Dependencies

This distribution has the following dependencies:

* jetty-io 9.4.6.v20170531 (eclipse jetty)
* httpclient 4.5.2
* ws-commons-util 1.0.2
* oslc4j-core 2.2.0 (eclipse/Lyo OSLC4J)
* oslc4j-jena-provider 2.2.0 (eclipse/Lyo OSLC4J)
* oslc4j-wink 2.2.0 (eclipse/Lyo OSLC4J)
* oslc4j-json4j-provider 2.2.0 (eclipse OSLC4J)
* oauth-core 2.2.0 (eclipse/Lyo OSLC4J)
* oauth-consumer-store 2.2.0/Lyo (eclipse OSLC4J)
* oauth-webapp 2.2.0 (eclipse/Lyo OSLC4J)
* oslc-java-client 2.2.0 (for JUnit testing)  (eclipse/Lyo OSLC4J)
* java.servlet-api 3.1.0
* javax.servlet.jsp.jstl-api 1.2.1
* taglibs-standard-impl 1.2.5
* slf4j-log4j12 1.6.4
* junit 4.13
* jsoup 1.8.3 
* gson 2.8.1
* dojo-war 1.12.2
* JRE 1.8

## Contributors

Contributors:

* Jim Amsden (IBM)
* Ralph Schoon (IBM)

## License

Licensed under the [Eclipse Public License](./CE4IoTConnector/license.txt).

