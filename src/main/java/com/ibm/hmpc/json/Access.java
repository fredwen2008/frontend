package com.ibm.hmpc.json;

import java.util.List;

public class Access {
	private Token token;
	private List<ServiceCatalog> serviceCatalog;
	private User user;
	private Metadata metadata;

	public Token getToken() {
		return token;
	}

	public void setToken(Token token) {
		this.token = token;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public List<ServiceCatalog> getServiceCatalog() {
		return serviceCatalog;
	}

	public void setServiceCatalog(List<ServiceCatalog> serviceCatalog) {
		this.serviceCatalog = serviceCatalog;
	}
}
