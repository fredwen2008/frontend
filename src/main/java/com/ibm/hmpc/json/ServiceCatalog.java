package com.ibm.hmpc.json;

import java.util.List;

public class ServiceCatalog {
	private String type;
	private String name;
	private List<Endpoint> endpoints;
	private List<Object> endpoints_links;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Endpoint> getEndpoints() {
		return endpoints;
	}

	public void setEndpoints(List<Endpoint> endpoints) {
		this.endpoints = endpoints;
	}

	public List<Object> getEndpoints_links() {
		return endpoints_links;
	}

	public void setEndpoints_links(List<Object> endpoints_links) {
		this.endpoints_links = endpoints_links;
	}
}
