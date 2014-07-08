package com.ibm.hmpc;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FrontendServlet extends HttpServlet {

	private static final long serialVersionUID = 6856054436283969404L;
	private static Logger log = LoggerFactory.getLogger(FrontendServlet.class);
	private ConnectionFactory busSource;
	private Connection busConnection = null;
	private DataSource dbsource;

	@Override
	public void init(ServletConfig config) throws ServletException {
		ServletContext sc = config.getServletContext();
		String webAppPath = sc.getRealPath("/");


		String frontendProp = webAppPath + "WEB-INF/frontend.properties";

		try {
			log.info("Initializing frontend with: " + frontendProp);
			Properties properties = new Properties();
			properties.load(new FileInputStream(new File(frontendProp)));

			log.info("Initializing hostport of frontend");
			if (FrontendHelper.initFrontendHostport(properties) == false) {
				throw new ServletException("FrontendServlet initializaion failed");
			}

			log.info("Initializing DB connection");
			dbsource = FrontendHelper.initDBConnectionPool(properties);
			log.info("Initializing JMS connection");
			busSource = FrontendHelper.initJMSConnectionFactory(properties);
			busConnection = FrontendHelper.createBusConnection(busSource);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new ServletException("FrontendServlet initializaion failed");
		}
		super.init(config);
	}

	@Override
	public void service(ServletRequest arg0, ServletResponse arg1) throws ServletException, IOException {
		java.sql.Connection dbConnection = null;
		Session busSession = null;
		Map responseMessage;
		Map msg;

		HttpServletRequest request = (HttpServletRequest) arg0;
		HttpServletResponse response = (HttpServletResponse) arg1;
		String requestBody = FrontendHelper.getHttpRequestBody(request);
		String method = request.getMethod();
		String cloudid = request.getParameter("cloudid");
		String service = request.getParameter("service");
		String region = request.getParameter("region");
		String script = request.getParameter("script");

		if (cloudid == null || cloudid.equals("")) {
			throw new ServletException("Cloudid is missing");
		}
		if (service == null || service.equals("")) {
			throw new ServletException("Service is missing");
		} else if (!FrontendHelper.validService(service)) {
			throw new ServletException("Service: " + service + " is invalid");
		}
		if (region == null || region.equals("")) {
			throw new ServletException("Region is missing");
		} else if (!FrontendHelper.validRegion(region)) {
			throw new ServletException("Region: " + region + " is invalid");
		}

		try {
			log.info("Connecting to DB");
			dbConnection = FrontendHelper.createDBConnection(dbsource);
			log.info("Verifying request");
			if (!FrontendHelper.validRequest(dbConnection, method, service, script)) {
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			synchronized (this) {
				log.info("Creating JMS connection session");
				if (busConnection == null) {
					busConnection = FrontendHelper.createBusConnection(busSource);
				}
			}
			busSession = busConnection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

			if (service.equals("identity") && script.endsWith("/tokens")) {
				msg = FrontendHelper.handleTokenRequest(dbConnection, request, requestBody);
				if (msg == null) {
					throw new ServletException("Handle token request failed");
				}
				responseMessage = FrontendHelper.sendReceive(busSession, cloudid, msg);
				if (responseMessage == null) {
					throw new ServletException("Handle token request failed");
				}
				FrontendHelper.handleTokenResponse(dbConnection, responseMessage, cloudid);
			} else {
				msg = FrontendHelper.handleOtherRequest(dbConnection, request, requestBody);
				responseMessage = FrontendHelper.sendReceive(busSession, cloudid, msg);
				if (responseMessage == null) {
					throw new ServletException("Handle other request failed");
				}
			}
			busSession.close();
			String responseBody = FrontendHelper.generateResponseBody(dbConnection, (String) responseMessage.get("response_body"), cloudid);
			List responseHeader = FrontendHelper.convertHeaderToList((String) responseMessage.get("response_header"));
			for (Iterator iter = responseHeader.iterator(); iter.hasNext();) {
				String kv[] = (String[]) iter.next();
				if (kv.length == 1) {
					Pattern pattern = Pattern.compile("HTTP/\\S+\\s+(\\d+)\\s*(\\S*)", Pattern.CASE_INSENSITIVE);
					Matcher matcher = pattern.matcher(kv[0]);
					if (matcher.find()) {
						String errCode = matcher.group(1);
						response.setStatus(Integer.parseInt(errCode));
					}
				} else {
					if (!kv[0].toLowerCase().equals("content-length"))
						response.setHeader(kv[0], kv[1]);
				}
			}
			dbConnection.close();
			response.getWriter().write(responseBody);
		} catch (JMSException e) {
			log.error(e.getMessage());
			throw new ServletException("FrontendServlet encounts JMS error");
		} catch (SQLException e) {
			log.error(e.getMessage());
			throw new ServletException("FrontendServlet encounts DB error");
		} finally {
			if (busSession != null) {
				try {
					busSession.close();
					busConnection.close();
				} catch (JMSException e) {
				} finally {
					busConnection = null;
				}
			}
			if (dbConnection != null) {
				try {
					dbConnection.close();
				} catch (SQLException e) {
				}
			}
		}
	}
}
