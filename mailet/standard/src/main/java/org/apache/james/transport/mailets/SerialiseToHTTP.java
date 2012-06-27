/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/
package org.apache.james.transport.mailets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;

import org.apache.mailet.base.GenericMailet;
import org.apache.mailet.Mail;

/**
 * Serialise the email and pass it to an HTTP call
 * 
 * Sample configuration:
 * 
 * <mailet match="All" class="SerialiseToHTTP">
 * 		<name>URL</name> <value>url where serialised message will be posted</value>
 * 		<name>ParameterKey</name> <value>An arbitrary parameter be added to the post</value>
 * 		<name>ParameterValue</name> <value>A value for the arbitrary parameter</value>
 * 		<name>MessageKeyName</name> <value>Field name for the serialised message</value>
 * 		<name>passThrough</name> <value>true or false</value>
 * </mailet>
 * 
 */
public class SerialiseToHTTP extends GenericMailet {

	/**
	 * The name of the header to be added.
	 */
	private String url;
	private String parameterKey = null;
	private String parameterValue = null;
	private String messageKeyName = "message";
	private boolean passThrough = true;

	/**
	 * Initialize the mailet.
	 */
	public void init() throws MessagingException {

		passThrough = (getInitParameter("passThrough", "true").compareToIgnoreCase("true") == 0);
		String targetUrl = getInitParameter("url");
		parameterKey = getInitParameter("parameterKey");
		parameterValue = getInitParameter("parameterValue");
		String m = getInitParameter("MessageKeyName");
		if (m != null) {
			messageKeyName = m;
		}

		// Check if needed config values are used
		if (targetUrl == null || targetUrl.equals("")) {
			throw new MessagingException(
					"Please configure a targetUrl (\"url\")");
		} else {
			try {
				// targetUrl = targetUrl + ( targetUrl.contains("?") ? "&" :
				// "?") + parameterKey + "=" + parameterValue;
				url = new URL(targetUrl).toExternalForm();
			} catch (MalformedURLException e) {
				throw new MessagingException(
						"Unable to contruct URL object from url");
			}
		}

		// record the result
		log("I will attempt to deliver serialised messages to "
				+ targetUrl
				+ " as "
				+ messageKeyName
				+ ". "
				+ (parameterKey==null || parameterKey.length()<2 ? "I will not add any fields to the post. " : "I will prepend: "	+ parameterKey + "=" + parameterValue + ". ")
				+ (passThrough ? "Messages will pass through."
						: "Messages will be ghosted."));
	}

	/**
	 * Takes the message, serialises it and sends it to the URL
	 * 
	 * @param mail
	 *            the mail being processed
	 * 
	 * @throws MessagingException
	 *             if an error arises during message processing
	 */
	public void service(Mail mail) {
		try {
			MimeMessage message = mail.getMessage();
			String serialisedMessage = getSerialisedMessage(message);
			NameValuePair[] nameValuePairs = getNameValuePairs(serialisedMessage);
			String result = httpPost(nameValuePairs);
			if (passThrough == true) {
				addHeader(mail, (result == null || result.length() == 0), result);
			} else {
				mail.setState(Mail.GHOST);
			}
		} catch (javax.mail.MessagingException me) {
			log(me.getMessage());
			addHeader(mail, false, me.getMessage());
		} catch (IOException e) {
			log(e.getMessage());
			addHeader(mail, false, e.getMessage());
		}
	}

	private void addHeader(Mail mail, boolean success, String errorMessage) {
		try {
			MimeMessage message = mail.getMessage();
			message.setHeader("X-toHTTP", (success ? "Succeeded" : "Failed"));
			if (!success && errorMessage!=null && errorMessage.length()>0) {
				message.setHeader("X-toHTTPFailure", errorMessage);
			}
			message.saveChanges();
		} catch (MessagingException e) {
			log(e.getMessage());
		}
	}

	private String getSerialisedMessage(MimeMessage message)
			throws IOException, MessagingException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		message.writeTo(os);
		return os.toString();
	}

	private String httpPost(NameValuePair[] data) {

		String response = null;
		HttpClient client = new HttpClient();
		PostMethod post = new PostMethod(url);

		if( data.length>1 && data[1]!=null ) {
			log( data[1].getName() + "::" + data[1].getValue() );
		}
		post.setRequestBody(data);

		try {
			int statusCode = client.executeMethod(post);

			if (statusCode != HttpStatus.SC_OK) {
				log("POST failed: " + post.getStatusLine());
				response = post.getStatusLine().toString();
			}// else {
			//	byte[] responseBody = post.getResponseBody();
			//	response = new String(responseBody);
			//}

		} catch (HttpException e) {
			log("Fatal protocol violation: " + e.getMessage());
			response = "Fatal protocol violation: " + e.getMessage();
		} catch (IOException e) {
			log("Fatal transport error: " + e.getMessage());
			response = "Fatal transport error: " + e.getMessage();
		} finally {
			post.releaseConnection();
		}

		return response;
	}

	private NameValuePair[] getNameValuePairs(String message) throws UnsupportedEncodingException {

    	int l = 1;
    	if (parameterKey!=null && parameterKey.length()>0) {
    		l = 2;
    	}
    	
    	NameValuePair[] data = new NameValuePair[l];
    	data[0] = new NameValuePair( messageKeyName, message);
    	if (l==2) {
    		data[1] = new NameValuePair( parameterKey, parameterValue);
    	}
    	
    	return data;
    }

	/**
	 * Return a string describing this mailet.
	 * 
	 * @return a string describing this mailet
	 */
	public String getMailetInfo() {
		return "HTTP POST serialised message";
	}

}
