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
package com.progress.codeshare.esbservice.setMessageProperty;

import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.trans.IndependentContext;
import net.sf.saxon.xpath.NamespaceContextImpl;

import org.xml.sax.InputSource;

import com.sonicsw.xq.XQConstants;
import com.sonicsw.xq.XQEnvelope;
import com.sonicsw.xq.XQInitContext;
import com.sonicsw.xq.XQMessage;
import com.sonicsw.xq.XQParameters;
import com.sonicsw.xq.XQPart;
import com.sonicsw.xq.XQServiceContext;
import com.sonicsw.xq.XQServiceEx;
import com.sonicsw.xq.XQServiceException;

public final class SetMessagePropertyService implements XQServiceEx {
	private static final DateFormat FORMAT = new SimpleDateFormat();

	private static final String MODE_CONSTANT = "Constant";
	private static final String MODE_CONTENT = "Content";
	private static final String MODE_DATE_TIME = "Date & Time";
	private static final String MODE_XPATH = "XPath";

	private static final String PARAM_CONSTANT = "constant";
	private static final String PARAM_DATE_TIME = "dateTime";
	private static final String PARAM_MESSAGE_PART = "messagePart";
	private static final String PARAM_MODE = "mode";
	private static final String PARAM_NAME = "name";
	private static final String PARAM_NAMESPACES = "namespaces";
	private static final String PARAM_XPATH = "xpath";

	private static final Pattern PATTERN_NAMESPACE = Pattern
			.compile("([-._:A-Za-z0-9]*)=([^,]*),?");

	public void destroy() {
	}

	public void init(XQInitContext ctx) {
	}

	public void service(final XQServiceContext ctx) throws XQServiceException {

		try {
			final XQParameters params = ctx.getParameters();

			final String mode = params.getParameter(PARAM_MODE,
					XQConstants.PARAM_STRING);

			final int messagePart = params.getIntParameter(PARAM_MESSAGE_PART,
					XQConstants.PARAM_STRING);

			final String name = params.getParameter(PARAM_NAME,
					XQConstants.PARAM_STRING);

			if (MODE_CONSTANT.equals(mode)) {
				final String constant = params.getParameter(PARAM_CONSTANT,
						XQConstants.PARAM_STRING);

				if (constant != null) {

					while (ctx.hasNextIncoming()) {
						final XQEnvelope env = ctx.getNextIncoming();

						final XQMessage msg = env.getMessage();

						msg.setHeaderValue(name, constant);

						final Iterator addressIterator = env.getAddresses();

						if (addressIterator.hasNext())
							ctx.addOutgoing(env);

					}

				}

			} else if (MODE_CONTENT.equals(mode)) {

				while (ctx.hasNextIncoming()) {
					final XQEnvelope env = ctx.getNextIncoming();

					final XQMessage msg = env.getMessage();

					final XQPart part = msg.getPart(messagePart);

					msg.setHeaderValue(name, (String) part.getContent());

					final Iterator addressIterator = env.getAddresses();

					if (addressIterator.hasNext())
						ctx.addOutgoing(env);

				}

			} else if (MODE_DATE_TIME.equals(mode)) {
				final Date dateTime = FORMAT.parse(params.getParameter(
						PARAM_DATE_TIME, XQConstants.PARAM_STRING));

				while (ctx.hasNextIncoming()) {
					final XQEnvelope env = ctx.getNextIncoming();

					final XQMessage msg = env.getMessage();

					msg.setHeaderValue(name, dateTime.toString());

					final Iterator addressIterator = env.getAddresses();

					if (addressIterator.hasNext())
						ctx.addOutgoing(env);

				}

			} else if (MODE_XPATH.equals(mode)) {
				final String expr = params.getParameter(PARAM_XPATH,
						XQConstants.PARAM_STRING);

				if (expr != null) {
					final XPathFactory factory = XPathFactory
							.newInstance(NamespaceConstant.OBJECT_MODEL_SAXON);

					final XPath xpath = factory.newXPath();

					final String namespaces = params.getParameter(
							PARAM_NAMESPACES, XQConstants.PARAM_STRING);

					if (namespaces != null) {
						/* Configure the namespaces */
						final Matcher matcher = PATTERN_NAMESPACE
								.matcher(namespaces);

						final IndependentContext resolver = new IndependentContext();

						while (matcher.find())
							resolver.declareNamespace(matcher.group(1), matcher
									.group(2));

						xpath.setNamespaceContext(new NamespaceContextImpl(
								resolver));
					}

					while (ctx.hasNextIncoming()) {
						final XQEnvelope env = ctx.getNextIncoming();

						final XQMessage msg = env.getMessage();

						final XQPart part = msg.getPart(messagePart);

						msg.setHeaderValue(name, xpath.evaluate(expr,
								new InputSource(new StringReader((String) part
										.getContent()))));

						final Iterator addressIterator = env.getAddresses();

						if (addressIterator.hasNext())
							ctx.addOutgoing(env);

					}

				}

			}

		} catch (final Exception e) {
			throw new XQServiceException(e);
		}

	}

	public void start() {
	}

	public void stop() {
	}

}