package com.ibm.hmpc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.TestCase;

import com.google.gson.Gson;
import com.ibm.hmpc.json.A_Access;
import com.ibm.hmpc.json.A_Auth;

public class TestFrontend extends TestCase {
	public String readFile(String fileName) throws IOException {
		File file = new File(fileName);
		BufferedReader input = new BufferedReader(new FileReader(file));
		StringBuffer buffer = new StringBuffer();
		String text;
		while ((text = input.readLine()) != null)
			buffer.append(text + "\n");
		input.close();
		return buffer.toString().trim();

	}

	public void testLoadJson1() throws IOException{
		Gson gson = new Gson();
		String json = readFile("d:/temp/2.txt");
		A_Auth auth = gson.fromJson(json, A_Auth.class);
		auth.getAuth().getTenantName();
	}
	
	public void testLoadJson2() throws IOException{
		Gson gson = new Gson();
		String json = readFile("d:/temp/1.txt");
		A_Access access = gson.fromJson(json, A_Access.class);
		access.getAccess().getMetadata();
	}
}
