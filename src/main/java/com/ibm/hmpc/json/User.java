package com.ibm.hmpc.json;

import java.util.List;

public class User {
	private String username;
	private List<Object> roles_links;
	private String id;
	private String name;
	private List<Role> roles;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Role> getRoles() {
		return roles;
	}

	public void setRoles(List<Role> roles) {
		this.roles = roles;
	}

	public List<Object> getRoles_links() {
		return roles_links;
	}

	public void setRoles_links(List<Object> roles_links) {
		this.roles_links = roles_links;
	}
}
