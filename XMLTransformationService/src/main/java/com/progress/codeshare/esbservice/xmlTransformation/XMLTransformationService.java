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
package com.progress.codeshare.esbservice.xmlTransformation;

import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

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
import com.sonicsw.xq.service.common.ServiceConstants;

public class XMLTransformationService implements XQServiceEx {
	private static String PARAM_KEEP_ORIGINAL_PART = "keepOriginalPart";
	private static String PARAM_MESSAGE_PART = "messagePart";
	private static String PARAM_MSG_PART_INDEX = "msgPartIndex";
	private static String PARAM_STYLESHEET = "stylesheet";

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

			boolean keepOriginalPart = params.getBooleanParameter(
					PARAM_KEEP_ORIGINAL_PART, XQConstants.PARAM_STRING);

			String stylesheet = params.getParameter(PARAM_STYLESHEET,
					XQConstants.PARAM_STRING);

			TransformerFactory transformerFactory = TransformerFactory
					.newInstance();

			Transformer transformer = transformerFactory
					.newTransformer(new StreamSource(new StringReader(
							stylesheet)));

			while (ctx.hasNextIncoming()) {
				XQEnvelope env = ctx.getNextIncoming();

				XQMessage origMsg = env.getMessage();

				XQMessage newMsg = msgFactory.createMessage();

				/* Copy all headers of the original message to the new message */
				Iterator headerIterator = origMsg.getHeaderNames();

				while (headerIterator.hasNext()) {
					String name = (String) headerIterator.next();

					newMsg.setHeaderValue(name, origMsg.getHeaderValue(name));
				}

				/*
				 * Pass the original message to the transformer to support
				 * HeaderExtension
				 */
				transformer.setParameter(ServiceConstants.XQMessage, origMsg);

				Iterator addressIterator = env.getAddresses();

				for (int i = 0; i < origMsg.getPartCount(); i++) {

					/* Decide whether to process the part or not */
					if ((messagePart == i)
							|| (messagePart == XQConstants.ALL_PARTS)) {
						/*
						 * Pass the message part index to the transformer to
						 * support MessagePartExtension
						 */
						transformer.setParameter(PARAM_MSG_PART_INDEX,
								new Integer(i));

						XQPart origPart = origMsg.getPart(i);

						/* Decide whether to keep the original part or not */
						if (keepOriginalPart) {
							origPart.setContentId("original_part_" + i);

							newMsg.addPart(origPart);
						}

						XQPart newPart = newMsg.createPart();

						newPart.setContentId("Result-" + i);

						Writer out = new StringWriter();

						String content = (String) origPart.getContent();

						transformer.transform(new StreamSource(
								new StringReader(content)), new StreamResult(
								out));

						newPart.setContent(out.toString(),
								XQConstants.CONTENT_TYPE_XML);

						newMsg.addPart(newPart);
					}

					/* Break when done */
					if (messagePart == i)
						break;

				}

				/* Remove all parameters from the transformer */
				transformer.clearParameters();

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