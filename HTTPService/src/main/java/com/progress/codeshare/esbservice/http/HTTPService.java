/*
 * Copyright 2011 Fernando Ribeiro
 * 
 * This file is part of Progress Sonic ESB Utils.
 *
 * Progress Sonic ESB Utils is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Progress Sonic ESB Utils is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with Progress Sonic ESB Utils. If not, see <http://www.gnu.org/licenses/>.
 */
package com.progress.codeshare.esbservice.http;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import com.sonicsw.xq.XQConstants;
import com.sonicsw.xq.XQEnvelope;
import com.sonicsw.xq.XQInitContext;
import com.sonicsw.xq.XQMessage;
import com.sonicsw.xq.XQMessageFactory;
import com.sonicsw.xq.XQParameters;
import com.sonicsw.xq.XQPart;
import com.sonicsw.xq.XQServiceContext;
import com.sonicsw.xq.XQServiceEx;
import com.sonicsw.xq.XQServiceException;

public class HTTPService implements XQServiceEx {
	private static String METHOD_DELETE = "DELETE";
	private static String METHOD_GET = "GET";
	private static String METHOD_POST = "POST";
	private static String METHOD_PUT = "PUT";

	private static String PARAM_MESSAGE_PART = "messagePart";
	private static String PARAM_METHOD = "method";
	private static String PARAM_URI = "uri";

	private static Pattern PATTERN_HEADER = Pattern
			.compile("SonicESB_HTTPHeader_(.*)");
	private static Pattern PATTERN_PARAM = Pattern
			.compile("SonicESB_HTTPParam_.(.*)");

	private static String PREFIX_HEADER = "SonicESB_HTTPHeader_";

	public void destroy() {
	}

	public void init(XQInitContext ctx) {
	}

	public void service(XQServiceContext ctx) throws XQServiceException {

		try {
			XQMessageFactory factory = ctx.getMessageFactory();

			XQParameters params = ctx.getParameters();

			int messagePart = params.getIntParameter(PARAM_MESSAGE_PART,
					XQConstants.PARAM_STRING);

			String method = params.getParameter(PARAM_METHOD,
					XQConstants.PARAM_STRING);

			String uri = params.getParameter(PARAM_URI,
					XQConstants.PARAM_STRING);

			while (ctx.hasNextIncoming()) {
				XQEnvelope env = ctx.getNextIncoming();

				XQMessage origMsg = env.getMessage();

				XQMessage newMsg = factory.createMessage();

				HttpClient client = new HttpClient();

				Iterator headerIterator = origMsg.getHeaderNames();

				if (METHOD_DELETE.equals(method)) {
					HttpMethodBase req = new DeleteMethod(uri);

					/*
					 * Copy all XQ headers and extract HTTP headers and
					 * parameters
					 */
					while (headerIterator.hasNext()) {
						String header = (String) headerIterator.next();

						newMsg.setHeaderValue(header, origMsg
								.getHeaderValue(header));

						Matcher matcher = PATTERN_HEADER.matcher(header);

						if (matcher.find())
							req.addRequestHeader(matcher.group(1),
									(String) origMsg.getHeaderValue(matcher
											.group()));

					}

					client.executeMethod(req);

					/* Transform all HTTP to XQ headers */
					Header[] headers = req.getResponseHeaders();

					for (int i = 0; i < headers.length; i++)
						newMsg.setHeaderValue(PREFIX_HEADER
								+ headers[i].getName(), headers[i].getValue());

					XQPart newPart = newMsg.createPart();

					newPart.setContentId("Result");

					newPart.setContent(new String(req.getResponseBody()), req
							.getResponseHeader("Content-Type").getValue());

					newMsg.addPart(newPart);
				} else if (METHOD_GET.equals(method)) {
					HttpMethodBase req = new GetMethod();

					List paramList = new ArrayList();

					/*
					 * Copy all XQ headers and extract HTTP headers and
					 * parameters
					 */
					while (headerIterator.hasNext()) {
						String header = (String) headerIterator.next();

						newMsg.setHeaderValue(header, origMsg
								.getHeaderValue(header));

						Matcher headerMatcher = PATTERN_HEADER
								.matcher(header);

						if (headerMatcher.find()) {
							req.addRequestHeader(headerMatcher.group(1),
									(String) origMsg
											.getHeaderValue(headerMatcher
													.group()));

							continue;
						}

						Matcher paramMatcher = PATTERN_PARAM
								.matcher(header);

						if (paramMatcher.find())
							paramList.add(new NameValuePair(paramMatcher
									.group(1), (String) origMsg
									.getHeaderValue(paramMatcher.group())));

					}

					req.setQueryString((NameValuePair[]) paramList
							.toArray(new NameValuePair[] {}));

					client.executeMethod(req);

					/* Transform all HTTP to XQ headers */
					Header[] headers = req.getResponseHeaders();

					for (int i = 0; i < headers.length; i++)
						newMsg.setHeaderValue(PREFIX_HEADER
								+ headers[i].getName(), headers[i].getValue());

					XQPart newPart = newMsg.createPart();

					newPart.setContentId("Result");

					newPart.setContent(new String(req.getResponseBody()), req
							.getResponseHeader("Content-Type").getValue());

					newMsg.addPart(newPart);
				} else if (METHOD_POST.equals(method)) {
					PostMethod req = new PostMethod(uri);

					/*
					 * Copy all XQ headers and extract HTTP headers and
					 * parameters
					 */
					while (headerIterator.hasNext()) {
						String header = (String) headerIterator.next();

						newMsg.setHeaderValue(header, origMsg
								.getHeaderValue(header));

						Matcher headerMatcher = PATTERN_HEADER
								.matcher(header);

						if (headerMatcher.find()) {
							req.addRequestHeader(headerMatcher.group(1),
									(String) origMsg
											.getHeaderValue(headerMatcher
													.group()));

							continue;
						}

						Matcher paramMatcher = PATTERN_PARAM
								.matcher(header);

						if (paramMatcher.find())
							req.addParameter(new NameValuePair(paramMatcher
									.group(1), (String) origMsg
									.getHeaderValue(paramMatcher.group())));

					}

					XQPart origPart = origMsg.getPart(messagePart);

					req.setRequestEntity(new StringRequestEntity(
							(String) origPart.getContent(), origPart
									.getContentType(), null));

					client.executeMethod(req);

					/* Transform all HTTP to XQ headers */
					Header[] headers = req.getResponseHeaders();

					for (int i = 0; i < headers.length; i++)
						newMsg.setHeaderValue(PREFIX_HEADER
								+ headers[i].getName(), headers[i].getValue());

					XQPart newPart = newMsg.createPart();

					newPart.setContentId("Result");

					newPart.setContent(new String(req.getResponseBody()), req
							.getResponseHeader("Content-Type").getValue());

					newMsg.addPart(newPart);
				} else if (METHOD_PUT.equals(method)) {
					EntityEnclosingMethod req = new PutMethod(uri);

					/* Copy all XQ headers and extract HTTP headers */
					while (headerIterator.hasNext()) {
						String header = (String) headerIterator.next();

						newMsg.setHeaderValue(header, origMsg
								.getHeaderValue(header));

						Matcher matcher = PATTERN_HEADER.matcher(header);

						if (matcher.find())
							req.addRequestHeader(matcher.group(1),
									(String) origMsg.getHeaderValue(matcher
											.group()));

					}

					XQPart origPart = origMsg.getPart(messagePart);

					req.setRequestEntity(new StringRequestEntity(
							(String) origPart.getContent(), origPart
									.getContentType(), null));

					client.executeMethod(req);

					/* Transform all HTTP to XQ headers */
					Header[] headers = req.getResponseHeaders();

					for (int i = 0; i < headers.length; i++)
						newMsg.setHeaderValue(PREFIX_HEADER
								+ headers[i].getName(), headers[i].getValue());

					XQPart newPart = newMsg.createPart();

					newPart.setContentId("Result");

					newPart.setContent(new String(req.getResponseBody()), req
							.getResponseHeader("Content-Type").getValue());

					newMsg.addPart(newPart);
				}

				env.setMessage(newMsg);

				Iterator addressIterator = env.getAddresses();

				if (addressIterator.hasNext())
					ctx.addOutgoing(env);

			}

		} catch (Exception e) {
			throw new XQServiceException(e);
		}

	}

	public void start() {
	}

	public void stop() {
	}

}