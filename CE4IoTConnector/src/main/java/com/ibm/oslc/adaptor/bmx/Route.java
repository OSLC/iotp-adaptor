package com.ibm.oslc.adaptor.bmx;

public class Route {
	public String guid;
	public String host;
	public String port;
	public String path;
	public String domain_url;
	private Metadata metadata;
	
	public String getGuid() {
		return guid;
	}
	
	public void setGuid(String guid) {
		this.guid = guid;
	}
	
	public String getHost() {
		return host + ".mybluemix.net";
	}
	
	public void setHost(String host) {
		this.host = host;
	}
	
	public String getPort() {
		return port;
	}
	
	public void setPort(String port) {
		this.port = port;
	}
	
	public String getPath() {
		return path;
	}
	
	public void setPath(String path) {
		this.path = path;
	}
	
	public String getDomain_url() {
		return domain_url;
	}
	
	public void setDomain_url(String domain_url) {
		this.domain_url = domain_url;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
