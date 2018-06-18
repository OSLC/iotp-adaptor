package com.ibm.oslc.adaptor.bmx.impl;

public class Organization {
	private String guid;
	private String name;
	private String spaces_url;
	private Metadata metadata;

	public String getGuid() {
		return guid;
	}

	public void setGuid(String url) {
		this.guid = url;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getSpaces_url() {
		return spaces_url;
	}
	
	public void setSpaces_url(String spaces_url) {
		this.spaces_url = spaces_url;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
}
