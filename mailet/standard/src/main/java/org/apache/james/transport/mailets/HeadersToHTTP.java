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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.mailet.Mail;
import org.apache.mailet.base.GenericMailet;

/**
 * Serialise the email and pass it to an HTTP call
 * 
 * Sample configuration:
 * 
 <mailet match="All" class="HeadersToHTTP">
   <url>http://192.168.0.252:3000/alarm</url>
   <parameterKey>Test</parameterKey>
   <parameterValue>ParameterValue</parameterValue>
   <passThrough>true</passThrough>
 </mailet>

 * 
 */
public class HeadersToHTTP extends GenericMailet {

	/**
	 * The name of the header to be added.
	 */
	private String url;
	private String parameterKey = null;
	private String parameterValue = null;
	private boolean passThrough = true;

	/**
	 * Initialize the mailet.
	 */
	public void init() throws MessagingException {

		passThrough = (getInitParameter("passThrough", "true").compareToIgnoreCase("true") == 0);
		String targetUrl = getInitParameter("url");
		parameterKey = getInitParameter("parameterKey");
		parameterValue = getInitParameter("parameterValue");

		// Check if needed config values are used
		if (targetUrl == null || targetUrl.equals("")) {
			throw new MessagingException("Please configure a targetUrl (\"url\")");
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
				+ ". "
				+ ( ((parameterKey==null) || (parameterKey.length()<1)) ? "I will not add any fields to the post. " : "I will prepend: "	+ parameterKey + "=" + parameterValue + ". ")
				+ (passThrough ? "Messages will pass through." : "Messages will be ghosted."));
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
			log(mail.getName() + "HeadersToHTTP: Starting");
			MimeMessage message = mail.getMessage();
			HashSet<NameValuePair> pairs = getNameValuePairs(message);
			log(mail.getName() + "HeadersToHTTP: " + pairs.size() + " named value pairs found");
			String result = httpPost(pairs);
			if (passThrough == true) {
				addHeader(mail, true, result);
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
			message.setHeader("X-headerToHTTP", (success ? "Succeeded" : "Failed"));
			if (!success && errorMessage!=null && errorMessage.length()>0) {
				message.setHeader("X-headerToHTTPFailure", errorMessage);
			}
			message.saveChanges();
		} catch (MessagingException e) {
			log(e.getMessage());
		}
	}

	private String httpPost(HashSet<NameValuePair> pairs) throws HttpException, IOException {

		HttpClient client = new HttpClient();
		PostMethod post = new PostMethod(url);

		post.setRequestBody(setToArray(pairs));
		
		int statusCode = client.executeMethod(post);
		String result = statusCode + ": " + post.getStatusLine().toString();
		log("HeadersToHTTP: " + result);
		
		return result;
		
	}

	private NameValuePair[] setToArray(HashSet<NameValuePair> pairs) {
		NameValuePair[] r =  new NameValuePair[pairs.size()];
		int i = 0;
		for(NameValuePair p : pairs) {
			r[i] = p;
			i = i + 1;
		}
		return r;
	}
	
	private HashSet<NameValuePair> getNameValuePairs(MimeMessage message) throws UnsupportedEncodingException, MessagingException {

		// to_address
		// from
		// reply to
		// subject
		
		HashSet<NameValuePair> pairs = new HashSet<NameValuePair>();
		
    	if (message!=null) {
	    	if (message.getSender()!=null) {
	    		pairs.add( new NameValuePair( "from", message.getSender().toString() ) );
	    	}
	    	if (message.getReplyTo()!=null) {
	    		pairs.add( new NameValuePair( "reply_to", message.getReplyTo().toString() ) );
	    	}
	    	if (message.getMessageID()!=null) {
	    		pairs.add( new NameValuePair( "message_id", message.getMessageID() ) );
	    	}
	    	if (message.getSubject()!=null) {
	    		pairs.add( new NameValuePair( "subject", message.getSubject() ) );
	    	}
	    	pairs.add( new NameValuePair( "size", Integer.toString(message.getSize()) ) );
	    }

		pairs.add( new NameValuePair( parameterKey, parameterValue) );
    			
    	return pairs;
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
