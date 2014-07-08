package com.ibm.hmpc.json;

import java.util.List;

public class Metadata {
	private String is_admin;
	private List<String> roles;

	public String getIs_admin() {
		return is_admin;
	}

	public void setIs_admin(String is_admin) {
		this.is_admin = is_admin;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}
}
