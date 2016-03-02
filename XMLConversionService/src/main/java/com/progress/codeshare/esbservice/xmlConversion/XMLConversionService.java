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
package com.progress.codeshare.esbservice.xmlConversion;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;

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
import com.unidex.xflat.XmlConvert;

public final class XMLConversionService implements XQServiceEx {
	private static final String MODE_FLAT_TO_FLAT = "Flat to Flat";
	private static final String MODE_FLAT_TO_XML = "Flat to XML";
	private static final String MODE_XML_TO_FLAT = "XML to Flat";

	private static final String PARAM_KEEP_ORIGINAL_PART = "keepOriginalPart";
	private static final String PARAM_MESSAGE_PART = "messagePart";
	private static final String PARAM_MODE = "mode";
	private static final String PARAM_SRC_SCHEMA = "srcSchema";
	private static final String PARAM_TARGET_SCHEMA = "targetSchema";

	public void destroy() {
	}

	public void init(XQInitContext ctx) {
	}

	public void service(final XQServiceContext ctx) throws XQServiceException {

		try {
			final XQParameters params = ctx.getParameters();

			final String mode = params.getParameter(PARAM_MODE,
					XQConstants.PARAM_STRING);

			final XQMessageFactory msgFactory = ctx.getMessageFactory();

			final int messagePart = params.getIntParameter(PARAM_MESSAGE_PART,
					XQConstants.PARAM_STRING);

			final boolean keepOriginalPart = params.getBooleanParameter(
					PARAM_KEEP_ORIGINAL_PART, XQConstants.PARAM_STRING);

			final String srcSchema = params.getParameter(PARAM_SRC_SCHEMA,
					XQConstants.PARAM_STRING);

			final XmlConvert converter = new XmlConvert(new StringReader(
					srcSchema), false);

			if (MODE_FLAT_TO_FLAT.equals(mode)) {
				final String targetSchema = params.getParameter(
						PARAM_TARGET_SCHEMA, XQConstants.PARAM_STRING);

				while (ctx.hasNextIncoming()) {
					final XQEnvelope env = ctx.getNextIncoming();

					final XQMessage origMsg = env.getMessage();

					final XQMessage newMsg = msgFactory.createMessage();

					final Iterator headerIterator = origMsg.getHeaderNames();

					/* Copy all headers from the original message to the new message */
					while (headerIterator.hasNext()) {
						final String name = (String) headerIterator.next();

						newMsg.setHeaderValue(name, origMsg
								.getHeaderValue(name));
					}

					final Iterator addressIterator = env.getAddresses();

					for (int i = 0; i < origMsg.getPartCount(); i++) {

						/* Decide whether to process the part or not */
						if ((messagePart == i)
								|| (messagePart == XQConstants.ALL_PARTS)) {
							final XQPart origPart = origMsg.getPart(i);

							/* Decide whether to keep the original part or not */
							if (keepOriginalPart) {
								origPart.setContentId("original_part_" + i);

								newMsg.addPart(origPart);
							}

							final XQPart newPart = newMsg.createPart();

							final Writer writer = new StringWriter();

							final String content = (String) origPart
									.getContent();

							converter.flatToFlat(new StringReader(content),
									new StringReader(targetSchema), writer);

							newPart.setContent(writer.toString(),
									XQConstants.CONTENT_TYPE_TEXT);

							newMsg.addPart(newPart);
						}

						/* Break when done */
						if (messagePart == i)
							break;

					}

					env.setMessage(newMsg);

					if (addressIterator.hasNext())
						ctx.addOutgoing(env);

				}

			} else if (MODE_FLAT_TO_XML.equals(mode)) {

				while (ctx.hasNextIncoming()) {
					final XQEnvelope env = ctx.getNextIncoming();

					final XQMessage origMsg = env.getMessage();

					final XQMessage newMsg = msgFactory.createMessage();

					final Iterator nameIterator = origMsg.getHeaderNames();

					while (nameIterator.hasNext()) {
						final String name = (String) nameIterator.next();

						newMsg.setHeaderValue(name, origMsg
								.getHeaderValue(name));
					}

					final Iterator addressIterator = env.getAddresses();

					for (int i = 0; i < origMsg.getPartCount(); i++) {

						/* Decide whether to process the part or not */
						if ((messagePart == i)
								|| (messagePart == XQConstants.ALL_PARTS)) {
							final XQPart origPart = origMsg.getPart(i);

							/* Decide whether to keep the original part or not */
							if (keepOriginalPart) {
								origPart.setContentId("original_part_" + i);

								newMsg.addPart(origPart);
							}

							final XQPart newPart = newMsg.createPart();

							final Writer writer = new StringWriter();

							final String content = (String) origPart
									.getContent();

							converter.flatToXml(new StringReader(content),
									writer);

							newPart.setContent(writer.toString(),
									XQConstants.CONTENT_TYPE_XML);

							newMsg.addPart(newPart);
						}

						/* Break when done */
						if (messagePart == i)
							break;

					}

					env.setMessage(newMsg);

					if (addressIterator.hasNext())
						ctx.addOutgoing(env);

				}

			} else if (MODE_XML_TO_FLAT.equals(mode)) {

				while (ctx.hasNextIncoming()) {
					final XQEnvelope env = ctx.getNextIncoming();

					final XQMessage origMsg = env.getMessage();

					final XQMessage newMsg = msgFactory.createMessage();

					final Iterator nameIterator = origMsg.getHeaderNames();

					while (nameIterator.hasNext()) {
						final String name = (String) nameIterator.next();

						newMsg.setHeaderValue(name, origMsg
								.getHeaderValue(name));
					}

					final Iterator addressIterator = env.getAddresses();

					for (int i = 0; i < origMsg.getPartCount(); i++) {

						/* Decide whether to process the part or not */
						if ((messagePart == i)
								|| (messagePart == XQConstants.ALL_PARTS)) {
							final XQPart origPart = origMsg.getPart(i);

							/* Decide whether to keep the original part or not */
							if (keepOriginalPart) {
								origPart.setContentId("original_part_" + i);

								newMsg.addPart(origPart);
							}

							final XQPart newPart = newMsg.createPart();

							final Writer writer = new StringWriter();

							final String content = (String) origPart
									.getContent();

							converter.xmlToFlat(new StringReader(content),
									writer);

							newPart.setContent(writer.toString(),
									XQConstants.CONTENT_TYPE_TEXT);

							newMsg.addPart(newPart);
						}

						/* Break when done */
						if (messagePart == i)
							break;

					}

					env.setMessage(newMsg);

					if (addressIterator.hasNext())
						ctx.addOutgoing(env);

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