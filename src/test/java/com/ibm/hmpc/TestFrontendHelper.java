package com.ibm.hmpc;

import junit.framework.TestCase;

public class TestFrontendHelper extends TestCase {
	public void testFindHostport(){
		String aa = FrontendHelper._findHostportFromURL("http://10.1.1.1:100/");
		System.out.print(aa);
	}
	
	
	
/*	public void testFindHostportFromURL(){
		String str = FrontendHelper.findHostportFromURL("http://123123:23423/adfasdf");
		System.out.print(str);		
	}*/
/*	
	public void test_getAllHostPorts(){
		FrontendHelper._getAllHostPorts();
	}*/
}


