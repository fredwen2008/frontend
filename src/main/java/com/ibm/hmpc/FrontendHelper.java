package com.ibm.hmpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.ibm.hmpc.json.A_Access;
import com.ibm.hmpc.json.A_Auth;
import com.ibm.hmpc.json.Access;
import com.ibm.hmpc.json.Auth;
import com.ibm.hmpc.json.Endpoint;
import com.ibm.hmpc.json.ServiceCatalog;
import com.ibm.hmpc.json.Token;

public class FrontendHelper {

	private static Logger log = LoggerFactory.getLogger(FrontendHelper.class);
	private static Map<String, String> frontendHostports;
	private static final String[] serviceTypes = { "compute", "computev3", "ec2", "identity", "image", "s3", "volume", "volumev2", "network" };
	private static final String[] regionTypes = { "public", "internal", "admin" };

	static void _addHostportToDB(java.sql.Connection dbConnection, String cloudid, String service, String region, String url) throws SQLException {
		String existHostport = _findHostportFromDB(dbConnection, cloudid, service, region);
		String newHostport = _findHostportFromURL(url);
		if (newHostport == null) {
			log.warn("Endpoint: " + url + " is invalid");
			return;
		}
		if (existHostport == null) {
			PreparedStatement statement = dbConnection.prepareStatement("insert into hostport (hostport,service,region,cloudid) values(?,?,?,?)");
			statement.setString(1, newHostport);
			statement.setString(2, service);
			statement.setString(3, region);
			statement.setString(4, cloudid.toLowerCase());
			statement.executeUpdate();
		} else if (!existHostport.equals(newHostport)) {
			PreparedStatement statement = dbConnection.prepareStatement("update hostport set hostport=? where service=? and region=? and cloudid=?");
			statement.setString(1, newHostport);
			statement.setString(2, service);
			statement.setString(3, region);
			statement.setString(4, cloudid.toLowerCase());
			statement.executeUpdate();
		}
	}

	static String _createRandomString() {
		Random random = new Random(System.currentTimeMillis());
		long randomLong = random.nextLong();
		return Long.toHexString(randomLong);
	}

	static Map _findAllHostPortsByCloudidFromDB(java.sql.Connection dbConnection, String cloudid) throws SQLException {
		Map<String, List<Map<String, String>>> ret = new HashMap<String, List<Map<String, String>>>();
		List<Map<String, String>> public_hostports = new ArrayList<Map<String, String>>();
		List<Map<String, String>> internal_hostports = new ArrayList<Map<String, String>>();
		List<Map<String, String>> admin_hostports = new ArrayList<Map<String, String>>();
		ret.put("public", public_hostports);
		ret.put("internal", internal_hostports);
		ret.put("admin", admin_hostports);

		PreparedStatement statement = dbConnection.prepareStatement("select hostport,service,region from hostport where cloudid=?");
		statement.setString(1, cloudid.toLowerCase());
		ResultSet resultset = statement.executeQuery();
		while (resultset.next()) {
			Map<String, String> row = new HashMap<String, String>();
			row.put("hostport", resultset.getString(1));
			row.put("service", resultset.getString(2));
			row.put("region", resultset.getString(3));
			if (row.get("region").equals("public")) {
				public_hostports.add(row);
			} else if (row.get("region").equals("internal")) {
				internal_hostports.add(row);
			} else if (row.get("region").equals("admin")) {
				admin_hostports.add(row);
			}
		}
		return ret;
	}

	static String _findHostportFromDB(java.sql.Connection dbConnection, String cloudid, String service, String region) throws SQLException {
		PreparedStatement statement = dbConnection.prepareStatement("select hostport from hostport where cloudid=? and service=? and region=?");
		statement.setString(1, cloudid);
		statement.setString(2, service);
		statement.setString(3, region);
		ResultSet resultset = statement.executeQuery();
		while (resultset.next()) {
			return (String) resultset.getString(1);
		}
		return null;
	}

	static String _findHostportFromURL(String url) {
		Pattern pattern = Pattern.compile("^http://(\\S+?)/|^http://(\\S+?)$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(url);
		if (matcher.find()) {
			if (matcher.group(1) != null) {
				return matcher.group(1);
			} else {
				return matcher.group(2);
			}
		}
		return null;
	}

	static Map _findHostportRowFromList(String hostport, List hostports) {
		for (Iterator iter = hostports.iterator(); iter.hasNext();) {
			Map row = (Map) iter.next();
			if (row.get("hostport").toString().equals(hostport)) {
				return row;
			}
		}
		return null;
	}

	static String _findOssPasswordFromDB(java.sql.Connection dbConnection, String cloudid, String username, String password) throws SQLException {
		PreparedStatement statement = dbConnection.prepareStatement("select oss_password from user where cloudid=? and lower(username)=? and password=?");
		statement.setString(1, cloudid.toLowerCase());
		statement.setString(2, username.toLowerCase());
		statement.setString(3, password);
		ResultSet resultset = statement.executeQuery();
		if (resultset.next()) {
			return resultset.getString(1);
		}
		return null;
	}

	static A_Access _parseAccess(String json) {
		Gson gson = new Gson();
		try {
			A_Access access = gson.fromJson(json, A_Access.class);
			return access;
		} catch (JsonSyntaxException e) {
			log.warn(e.getMessage());
			return null;
		}
	}

	static A_Auth _parseAuth(String json) {
		Gson gson = new Gson();
		try {
			A_Auth auth = gson.fromJson(json, A_Auth.class);
			return auth;
		} catch (JsonSyntaxException e) {
			log.warn(e.getMessage());
			return null;
		}
	}

	static String _serializeAuth(A_Auth auth) {
		Gson gson = new Gson();
		return gson.toJson(auth);
	}

	public static List convertHeaderToList(String header) {
		List<String[]> rtn = new ArrayList<String[]>();
		String lines[] = header.split("\n");
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			String kv[] = line.split("\\s*:\\s*");
			rtn.add(kv);
		}
		return rtn;
	}

	public static Connection createBusConnection(ConnectionFactory bussource) throws JMSException {
		Connection conn = bussource.createConnection();
		conn.start();
		return conn;
	}

	public static java.sql.Connection createDBConnection(DataSource datasource) throws SQLException {
		return datasource.getConnection();
	}

	public static String generateResponseBody(java.sql.Connection dbConnection, String body, String cloudid) throws SQLException {
		StringBuilder newbody = new StringBuilder();
		Map allHostports = _findAllHostPortsByCloudidFromDB(dbConnection, cloudid);

		int beginIndex = 0;
		Pattern pattern = Pattern.compile("\"http://(\\S+?)/(\\S*?)\"|\"http://(\\S+?)\"", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(body);
		while (matcher.find()) {
			newbody.append(body.substring(beginIndex, matcher.start()));
			String url = matcher.group();
			String hostport = matcher.group(1);
			String ending = matcher.group(2);
			if (hostport == null) {
				hostport = matcher.group(3);
				ending = "";
			}
			Map row = _findHostportRowFromList(hostport, (List) allHostports.get("public"));
			if (row == null) {
				row = _findHostportRowFromList(hostport, (List) allHostports.get("internal"));
				if (row == null) {
					row = _findHostportRowFromList(hostport, (List) allHostports.get("admin"));
				}
			}

			if (row != null) {
				String service = (String) row.get("service");
				String region = (String) row.get("region");
				String frontendHostport;
				if (frontendHostports.containsKey(region)) {
					frontendHostport = (String) frontendHostports.get(region);
				} else {
					frontendHostport = (String) frontendHostports.get("public");
				}
				if (frontendHostport != null) {
					url = "\"http://" + frontendHostport + "/frontend/" + cloudid + "/" + service + "/" + region + "/" + ending + "\"";
				}
			}
			newbody.append(url);
			beginIndex = matcher.end();
		}
		newbody.append(body.substring(beginIndex));
		return newbody.toString();
	}

	public static String generateSPHeader(HttpServletRequest request, String hostport, String body) {
		StringBuilder header = new StringBuilder();
		for (Enumeration enu = request.getHeaderNames(); enu.hasMoreElements();) {
			String key = (String) enu.nextElement();
			if (key.toLowerCase().equals("connection") || key.toLowerCase().equals("content-length"))
				continue;
			if (header.length() > 0) {
				header.append("\n");
			}
			if (key.toLowerCase().equals("host")) {
				header.append("Host: ").append(hostport);
			} else {
				header.append(key).append(": ").append(request.getHeader(key));
			}
		}

		if (body != null && body.length() > 0) {
			if (header.length() > 0) {
				header.append("\n");
			}
			header.append("content-length").append(": ").append(body.length());
		}
		return header.toString();
	}

	public static String generateSPHostport(java.sql.Connection dbConnection, String cloudid, String service, String region) throws SQLException {
		Map allHostports = _findAllHostPortsByCloudidFromDB(dbConnection, cloudid);
		List rows = (List) allHostports.get(region);

		for (Iterator iter = rows.iterator(); iter.hasNext();) {
			Map row = (Map) iter.next();
			if (row.get("service").equals(service)) {
				return (String) row.get("hostport");
			}
		}
		log.warn("Cannot find hostport for service: " + service + " region: " + region + " cloudid: " + cloudid);
		return null;
	}

	public static String generateSPRequestBody(java.sql.Connection dbConnection, String body, String cloudid) throws SQLException {
		if (body == null) {
			return null;
		}
		StringBuilder newbody = new StringBuilder();
		Map allHostports = _findAllHostPortsByCloudidFromDB(dbConnection, cloudid);

		int beginIndex = 0;
		Pattern pattern = Pattern.compile("\"http://(\\S+?)/(\\S+?)/(\\S+?)/(\\S+?)/(\\S*?)\"", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(body);
		while (matcher.find()) {
			newbody.append(body.substring(beginIndex, matcher.start()));
			String url = matcher.group();
			//String hostport = matcher.group(1);
			String service = matcher.group(3);
			String region = matcher.group(4);
			String script = matcher.group(5);
			String spHostport = null;
			if (!validService(service)) {
				log.warn("URL: " + url + " does not contain valid service");
			} else if (!validRegion(region)) {
				log.warn("URL: " + url + " does not contain valid region");
			} else {
				Map row = (Map) allHostports.get(region);
				if (row.containsKey(service)) {
					spHostport = (String) row.get("hostport");
				} else {
					log.warn("Cannot find hostport for service: " + service + " region: " + region + " cloud: " + cloudid);
				}
			}
			if (spHostport != null) {
				url = "\"http://" + spHostport + "/" + script + "/\"";
			} else {
				log.warn("Cannot find correspoind OSS hostport.");
			}
			newbody.append(url);
			beginIndex = matcher.end();
		}
		newbody.append(body.substring(beginIndex));
		return newbody.toString();
	}

	public static String generateSPRequestURI(HttpServletRequest request) {
		String uri = "";
		String query = request.getQueryString();
		String kvs[] = query.split("&");
		for (int i = 0; i < kvs.length; i++) {
			String kv[] = kvs[i].split("=");
			String key = kv[0];
			String value = kv[1];
			if (key.equals("cloudid") || key.equals("service") || key.equals("region") || key.equals("script"))
				continue;
			if (uri.length() == 0) {
				uri += "?";
			} else {
				uri += "&";
			}
			uri += key + "=" + value;
		}
		String script = request.getParameter("script");
		uri = script + uri;
		return uri;
	}

	public static String getHttpRequestBody(HttpServletRequest request) throws IOException {
		String line;
		StringBuilder buffer = new StringBuilder();
		BufferedReader reader = request.getReader();

		while ((line = reader.readLine()) != null) {
			if (buffer.length() > 0)
				buffer.append("\n");
			buffer.append(line);
		}
		return buffer.toString();
	}

	@SuppressWarnings("unchecked")
	public static Map handleOtherRequest(java.sql.Connection dbConnection, HttpServletRequest request, String requestBody) throws SQLException {
		String cloudid = request.getParameter("cloudid");
		String service = request.getParameter("service");
		String region = request.getParameter("region");
		String token = request.getHeader("X-Auth-Token");

		if (token == null || token.equals("")) {
			log.warn("Token is missing");
			return null;
		}

		String spHostport = generateSPHostport(dbConnection, cloudid, service, region);
		if (spHostport == null) {
			log.warn("Cannot find corresponding hostport for service: " + service + " region: " + region + " cloud: " + cloudid);
			return null;
		}

		if (requestBody.length() > 0) {
			// TODO what request body contains url?
			requestBody = FrontendHelper.generateSPRequestBody(dbConnection, requestBody, cloudid);
		}
		String spUri = generateSPRequestURI(request);
		String spHeader = generateSPHeader(request, spHostport, requestBody);
		Map msg = new HashMap();
		msg.put("request_host", spHostport);
		msg.put("request_method", request.getMethod());
		msg.put("request_uri", spUri);
		msg.put("request_header", spHeader);
		msg.put("request_body", requestBody);
		return msg;
	}

	@SuppressWarnings("unchecked")
	public static Map handleTokenRequest(java.sql.Connection dbConnection, HttpServletRequest request, String requestBody) throws SQLException,
	        ServletException {

		String cloudid = request.getParameter("cloudid");
		String service = request.getParameter("service");
		String region = request.getParameter("region");

		A_Auth aAuth = _parseAuth(requestBody);
		if (aAuth == null) {
			log.warn("Parse request body to A_Auth object failed");
			return null;
		}
		Auth auth = aAuth.getAuth();
		if (auth == null) {
			log.warn("Parse request body to Auth object failed");
			return null;
		}

		if (auth.getPasswordCredentials() != null) {
			String username = auth.getPasswordCredentials().getUsername();
			if (username == null || username.equals("")) {
				log.warn("Username is missing");
				return null;
			}
			String password = auth.getPasswordCredentials().getPassword();
			if (password == null || password.equals("")) {
				log.warn("Password is missing");
				return null;
			}
			String ossPassword = FrontendHelper._findOssPasswordFromDB(dbConnection, cloudid, username, password);
			if (ossPassword == null) {
				log.warn("Username/password authentication failed");
				return null;
			}
			auth.getPasswordCredentials().setPassword(ossPassword);
		} else if (auth.getToken() != null) {

		}

		requestBody = _serializeAuth(aAuth);
		if (requestBody == null || requestBody.equals("")) {
			log.warn("Serialize Auth object to json failed");
			return null;
		}
		String spHostport = generateSPHostport(dbConnection, cloudid, service, region);
		if (spHostport == null) {
			log.warn("Cannot get corresponding hostport");
			return null;
		}
		String spUri = generateSPRequestURI(request);

		String spHeader = generateSPHeader(request, spHostport, requestBody);

		Map msg = new HashMap();
		msg.put("request_host", spHostport);
		msg.put("request_method", request.getMethod());
		msg.put("request_uri", spUri);
		msg.put("request_header", spHeader);
		msg.put("request_body", requestBody);
		return msg;
	}

	public static void handleTokenResponse(java.sql.Connection dbConnection, Map responseMessage, String cloudid) throws SQLException {
		String responseBody = (String) responseMessage.get("response_body");
		A_Access aAccess = FrontendHelper._parseAccess(responseBody);
		if (aAccess == null) {
			log.warn("Parse response body to A_Access object failed");
			return;
		}
		Access access = aAccess.getAccess();
		if (access == null) {
			log.warn("Parse response body to Access object failed");
			return;
		}
		Token token = access.getToken();
		if (token == null) {
			log.warn("Parse response body to Token object failed");
			return;
		}

		String id = token.getId();
		if (id == null || id.equals("")) {
			log.warn("Parse response body to token id failed");
			return;
		}

		List catalogs = access.getServiceCatalog();
		if (catalogs == null) {
			log.warn("Parse response body to ServiceCatalog object failed");
			return;
		}
		for (Iterator iter = catalogs.iterator(); iter.hasNext();) {
			ServiceCatalog catalog = (ServiceCatalog) iter.next();
			String service = catalog.getType();
			List endpoints = catalog.getEndpoints();
			for (Iterator iter1 = endpoints.iterator(); iter1.hasNext();) {
				Endpoint endpoint = (Endpoint) iter1.next();
				String adminURL = endpoint.getAdminURL();
				_addHostportToDB(dbConnection, cloudid, service, "admin", adminURL);
				String internalURL = endpoint.getInternalURL();
				_addHostportToDB(dbConnection, cloudid, service, "internal", internalURL);
				String publicURL = endpoint.getPublicURL();
				_addHostportToDB(dbConnection, cloudid, service, "public", publicURL);
			}
		}
	}

	public static DataSource initDBConnectionPool(Properties properties) throws IOException {
		String db_driver = properties.getProperty("DB_DRIVER");
		String db_username = properties.getProperty("DB_USERNAME");
		String db_password = properties.getProperty("DB_PASSWORD");
		String db_url = properties.getProperty("DB_URL");

		PoolProperties p = new PoolProperties();
		p.setDriverClassName(db_driver);
		p.setUsername(db_username);
		p.setPassword(db_password);
		p.setUrl(db_url);
		p.setJmxEnabled(true);
		p.setTestWhileIdle(false);
		p.setTestOnBorrow(true);
		p.setValidationQuery("SELECT 1");
		p.setTestOnReturn(false);
		p.setValidationInterval(30000);
		p.setTimeBetweenEvictionRunsMillis(30000);
		p.setMaxActive(10);
		p.setInitialSize(10);
		p.setMaxWait(10000);
		p.setRemoveAbandonedTimeout(600);
		p.setMinEvictableIdleTimeMillis(30000);
		p.setMinIdle(10);
		p.setLogAbandoned(true);
		p.setRemoveAbandoned(true);
		p.setMaxIdle(10);
		p.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;" + "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
		DataSource datasource = new DataSource();
		datasource.setPoolProperties(p);
		return datasource;
	}

	public static boolean initFrontendHostport(Properties properties) {
		frontendHostports = new HashMap<String, String>();
		String public_hostport = properties.getProperty("PUBLIC_HOSTPORT");
		if (public_hostport == null) {
			String err = "*** Mandatory propertity PUBLIC_HOST of is missing";
			log.error(err);
			return false;
		}

		frontendHostports.put("public", public_hostport);

		String internal_hostport = properties.getProperty("INTERNAL_HOSTPORT");
		if (internal_hostport == null) {
			log.warn("Optional propertity INTERNAL_HOSTPORT of is missing, so using propertity PUBLIC_HOSTPORT instead");
			internal_hostport = public_hostport;
		}
		frontendHostports.put("internal", internal_hostport);

		String admin_hostport = properties.getProperty("ADMIN_HOSTPORT");
		if (admin_hostport == null) {
			log.warn("Optional propertity ADMIN_HOSTPORT of is missing, so using propertity PUBLIC_HOSTPORT instead");
			admin_hostport = public_hostport;
		}
		frontendHostports.put("admin", admin_hostport);
		return true;
	}

	public static ConnectionFactory initJMSConnectionFactory(Properties properties) {
		String msgbus_username = properties.getProperty("MSGBUS_USERNAME");
		String msgbus_password = properties.getProperty("MSGBUS_PASSWORD");
		String msgbus_url = properties.getProperty("MSGBUS_URL");

		ConnectionFactory bussource = new ActiveMQConnectionFactory(msgbus_username, msgbus_password, msgbus_url);
		return bussource;
	}

	public static Map sendReceive(Session session, String cloudid, Map requestMap) throws JMSException {
		String requestCorrelationId = _createRandomString();
		String queueName = cloudid;
		MapMessage requestMessage = session.createMapMessage();
		for (Iterator iter = requestMap.entrySet().iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			requestMessage.setString(key, value);
		}

		Destination request_queue = session.createQueue(queueName);
		Destination reply_queue = session.createTemporaryQueue();
		MessageProducer producer = session.createProducer(request_queue);
		MessageConsumer consumer = session.createConsumer(reply_queue);

		requestMessage.setJMSCorrelationID(requestCorrelationId);
		requestMessage.setJMSReplyTo(reply_queue);

		producer.send(requestMessage);

		MapMessage responseMessage = (MapMessage) consumer.receive(5000);
		if (responseMessage == null) {
			log.error("Timeout when gettting response from message bus");
			return null;
		}
		responseMessage.acknowledge();
		Map<String, String> responseMap = new HashMap<String, String>();
		String responseCorrelationId = responseMessage.getJMSCorrelationID();
		if (!responseCorrelationId.equals(requestCorrelationId)) {
			log.warn("Request id is " + requestCorrelationId + " ,but response id is " + responseCorrelationId);
		}

		String responseHeader = responseMessage.getString("response_header");
		String responseBody = responseMessage.getString("response_body");
		if (responseHeader == null || responseHeader.equals("")) {
			log.warn("Response header is missing");
		}

		responseMap.put("response_header", responseHeader);
		responseMap.put("response_body", responseBody);
		return responseMap;
	}

	public static boolean validRegion(String region) {

		for (int i = 0; i < regionTypes.length; i++) {
			if (regionTypes[i].equals(region)) {
				return true;
			}
		}
		return false;
	}

	public static boolean validRequest(java.sql.Connection dbConnection, String method, String service, String script) throws SQLException {

		PreparedStatement statement = dbConnection.prepareStatement("select regex from block where service=? and method=?");
		statement.setString(1, service.toLowerCase());
		statement.setString(2, method.toLowerCase());
		ResultSet resultset = statement.executeQuery();
		while (resultset.next()) {
			String regex = resultset.getString(1);
			Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(script);
			if (matcher.find()) {
				log.warn("Request: " + script + " with method: " + method + " is blocked by regex[" + regex + "]");
				return false;
			}
		}
		return true;
	}

	public static boolean validService(String service) {

		for (int i = 0; i < serviceTypes.length; i++) {
			if (serviceTypes[i].equals(service)) {
				return true;
			}
		}
		return false;
	}

	private FrontendHelper() {
	}
}
