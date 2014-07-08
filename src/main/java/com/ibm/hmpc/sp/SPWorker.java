package com.ibm.hmpc.sp;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;




import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.hmpc.FrontendHelper;

public class SPWorker extends Thread {
	
	private static Logger log = LoggerFactory.getLogger(SPWorker.class);  
	private String queueName;
	private ConnectionFactory busSource;
	private boolean toQuit = false;

	public SPWorker(ConnectionFactory _busSource, String _queueName) throws JMSException {
		super("SPWorker " + _queueName);
		busSource = _busSource;
		queueName = _queueName;
	}

	@Override
	public void run() {
		Connection busConnection = null;
		Session session = null;
		Destination request_queue;
		MessageProducer producer = null;
		MessageConsumer consumer = null;
		Socket clientSocket = null;
		while (toQuit == false) {
			try {
				if (busConnection == null) {
					Thread.sleep(1000);
					busConnection = FrontendHelper.createBusConnection(busSource);
					session = busConnection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
					request_queue = session.createQueue(queueName);
					consumer = session.createConsumer(request_queue);
				}

				MapMessage requestMsg = (MapMessage) consumer.receive(5000);
				if (requestMsg == null) {
					continue;
				}
				requestMsg.acknowledge();
				Destination response_queue = requestMsg.getJMSReplyTo();
				String correlationID = requestMsg.getJMSCorrelationID();

				MapMessage responseMsg = session.createMapMessage();
				producer = session.createProducer(response_queue);

				String spHostport = requestMsg.getString("request_host");
				String method = requestMsg.getString("request_method");
				String spUri = requestMsg.getString("request_uri");
				String spHeader = requestMsg.getString("request_header");
				String requestBody = requestMsg.getString("request_body");

				String hostport[] = spHostport.split(":");
				String host = hostport[0];
				String port = hostport[1];

				clientSocket = new Socket(host, Integer.parseInt(port));
				// clientSocket.setSoTimeout(5000);
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
				String request = method + " " + spUri + " HTTP/1.1\n" + spHeader + "\n\n" + requestBody + "\n";
				log.debug("############## Request:\n"+request);
				out.print(request);
				out.flush();

				StringBuilder header = new StringBuilder();
				StringBuilder body = new StringBuilder();
				boolean ishead = true;
				while (true) {
					String line = in.readLine();
					if (line == null)
						break;
					if (line.equals("")) {
						ishead = false;
					} else if (ishead) {
						if (header.length() > 0)
							header.append("\n");
						header.append(line);
					} else {
						if (body.length() > 0)
							body.append("\n");
						body.append(line);
					}
				}
				log.debug("############## Response:\n"+header+"\n"+body);
				responseMsg.setJMSCorrelationID(correlationID);
				responseMsg.setString("response_header", header.toString());
				responseMsg.setString("response_body", body.toString());
				producer.send(responseMsg);
				producer.close();
			} catch (InterruptedException e) {
			} catch (NumberFormatException e) {
			} catch (UnknownHostException e) {
			} catch (IOException e) {
			} catch (JMSException e) {
				try {
					if (producer != null)
						producer.close();
					if (consumer != null)
						consumer.close();
					if (session != null)
						session.close();
					if (busConnection != null)
						busConnection.close();
				} catch (JMSException e1) {
				} finally {
					busConnection = null;
				}
			} finally {
				try {
					if (clientSocket != null)
						clientSocket.close();
					clientSocket = null;
				} catch (IOException e) {
				}
			}
		}
	}

	public void setToQuit(boolean toQuit) {
		this.toQuit = toQuit;
	}

}
