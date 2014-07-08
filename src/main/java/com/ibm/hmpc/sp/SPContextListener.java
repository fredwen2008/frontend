package com.ibm.hmpc.sp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.ibm.hmpc.FrontendHelper;

public class SPContextListener implements ServletContextListener {

	List<SPWorker> workers = new ArrayList<SPWorker>();

	private void starSPThread(ConnectionFactory bussource, String tenant) throws JMSException {
		SPWorker worker = new SPWorker(bussource, tenant);
		workers.add(worker);
		worker.start();
	}

	private void stopAllSPThreads() {
		for (Iterator<SPWorker> iter = workers.iterator(); iter.hasNext();) {
			SPWorker worker = (SPWorker) iter.next();
			worker.setToQuit(true);
			try {
				worker.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void contextInitialized(ServletContextEvent sce) {
		// start service provider threads
		String webAppPath = sce.getServletContext().getRealPath("/");
		String spProp = webAppPath + "WEB-INF/sp.properties";
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(new File(spProp)));
			ConnectionFactory bussource = FrontendHelper.initJMSConnectionFactory(properties);
			starSPThread(bussource, "001");
			starSPThread(bussource, "002");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	public void contextDestroyed(ServletContextEvent sce) {
		// stop service provider threads
		stopAllSPThreads();
	}

}
