package com.ibm.oslc.adaptor.bmx.impl;

import java.util.ArrayList;

public class NodeREDApplication extends Application {
	private String bearerToken;
	private ArrayList<String> flowNames;
	
	public NodeREDApplication(Application app) {
		this.setGuid(app.getGuid());
		this.setName(app.getName());
		this.setState(app.getState());
		this.setMetadata(app.getMetadata());
	}

	public ArrayList<String> getFlowNames() {
		if (flowNames == null) flowNames = new ArrayList<String>();
		return flowNames;
	}

	public void setFlowNames(ArrayList<String> flowNames) {
		this.flowNames = flowNames;
	}

	public String getBearerToken() {
		return bearerToken;
	}

	public void setBearerToken(String bearerToken) {
		this.bearerToken = bearerToken;
	}
}
