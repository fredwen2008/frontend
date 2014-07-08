package com.ibm.hmpc.json;

public class Auth {
	private String tenantName;
	private String tenantId;
	private PasswordCredentials passwordCredentials;
	private Token token;

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public Token getToken() {
		return token;
	}

	public void setToken(Token token) {
		this.token = token;
	}

	public String getTenantName() {
		return tenantName;
	}

	public void setTenantName(String tenantName) {
		this.tenantName = tenantName;
	}

	public PasswordCredentials getPasswordCredentials() {
		return passwordCredentials;
	}

	public void setPasswordCredentials(PasswordCredentials passwordCredentials) {
		this.passwordCredentials = passwordCredentials;
	}

	/*
	 * @Override public String toString() { return "{tenantName:" + tenantName +
	 * ",passwordCredentials:" + passwordCredentials+"}"; }
	 */
}
