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
package com.progress.codeshare.esbservice.splitMessagePart;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.sf.saxon.om.ListIterator;
import net.sf.saxon.om.LookaheadIterator;
import net.sf.saxon.om.NamespaceConstant;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.IndependentContext;
import net.sf.saxon.xpath.NamespaceContextImpl;

import org.xml.sax.InputSource;

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

public class SplitMessagePartService implements XQServiceEx {
	private static String PARAM_KEEP_ORIGINAL_PART = "keepOriginalPart";
	private static String PARAM_MESSAGE_PART = "messagePart";
	private static String PARAM_NAMESPACES = "namespaces";
	private static String PARAM_XPATH = "xpath";

	private static Pattern PATTERN_NAMESPACE = Pattern
			.compile("([-._:A-Za-z0-9]+)=([^,]+),?");

	public void destroy() {
	}

	public void init(XQInitContext ctx) {
	}

	public void service(XQServiceContext ctx) throws XQServiceException {

		try {
			XQMessageFactory msgFactory = ctx.getMessageFactory();

			XQParameters params = ctx.getParameters();

			int messagePart = params.getIntParameter(PARAM_MESSAGE_PART,
					XQConstants.PARAM_STRING);

			XPathFactory xpathFactory = XPathFactory
					.newInstance(NamespaceConstant.OBJECT_MODEL_SAXON);

			XPath xpath = xpathFactory.newXPath();

			String namespaces = params.getParameter(PARAM_NAMESPACES,
					XQConstants.PARAM_STRING);

			if (namespaces != null) {
				/* Configure the namespaces */
				IndependentContext resolver = new IndependentContext();

				Matcher matcher = PATTERN_NAMESPACE.matcher(namespaces);

				while (matcher.find())
					resolver.declareNamespace(matcher.group(1), matcher
							.group(2));

				xpath.setNamespaceContext(new NamespaceContextImpl(resolver));
			}

			String expr = params.getParameter(PARAM_XPATH,
					XQConstants.PARAM_STRING);

			boolean keepOriginalPart = params.getBooleanParameter(
					PARAM_KEEP_ORIGINAL_PART, XQConstants.PARAM_STRING);

			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();

			Transformer transformer = transformerFactory.newTransformer();

			while (ctx.hasNextIncoming()) {
				XQEnvelope env = ctx.getNextIncoming();

				XQMessage origMsg = env.getMessage();

				XQMessage newMsg = msgFactory.createMessage();

				/* Copy all headers from the original message to the new message */
				Iterator headerIterator = origMsg.getHeaderNames();

				while (headerIterator.hasNext()) {
					String name = (String) headerIterator.next();

					newMsg.setHeaderValue(name, origMsg.getHeaderValue(name));
				}

				Iterator addressIterator = env.getAddresses();

				for (int i = 0; i < origMsg.getPartCount(); i++) {

					/* Decide whether to process the part or not */
					if ((messagePart == i)
							|| (messagePart == XQConstants.ALL_PARTS)) {
						XQPart origPart = origMsg.getPart(i);

						/* Decide whether to keep the original part or not */
						if (keepOriginalPart) {
							origPart.setContentId("original_part_" + i);

							newMsg.addPart(origPart);
						}

						List resultList = (List) xpath.evaluate(expr,
								new InputSource(new StringReader(
										(String) origPart.getContent())),
								XPathConstants.NODESET);

						if (resultList != null) {
							LookaheadIterator resultIterator = new ListIterator(
									resultList);

							/* Create new parts from the results */
							while (resultIterator.hasNext()) {
								XQPart newPart = newMsg.createPart();

								newPart.setContentId("Result-" + i + "_"
										+ resultIterator.position());

								StringWriter writer = new StringWriter();

								transformer.transform((NodeInfo) resultIterator
										.next(), new StreamResult(writer));

								newPart.setContent(writer.toString(),
										XQConstants.CONTENT_TYPE_XML);

								newMsg.addPart(newPart);
							}

						}

					}

					/* Break when done */
					if (messagePart == i)
						break;

				}

				env.setMessage(newMsg);

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