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
package com.progress.codeshare.esbservice.fileReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
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

public final class FileReaderService implements XQServiceEx {
	private static final String PARAM_FILE = "file";

	public void destroy() {
	}

	public void init(XQInitContext ctx) {
	}

	public void service(final XQServiceContext ctx) throws XQServiceException {

		try {
			final XQMessageFactory factory = ctx.getMessageFactory();

			final XQParameters params = ctx.getParameters();

			final String file = params.getParameter(PARAM_FILE,
					XQConstants.PARAM_STRING);

			final StringBuffer buf = new StringBuffer();

			final Reader reader = new BufferedReader(new FileReader(file));

			int i = reader.read();

			while (i != -1) {
				buf.append((char) i);

				i = reader.read();
			}

			reader.close();

			final String content = buf.toString();

			while (ctx.hasNextIncoming()) {
				final XQEnvelope env = ctx.getNextIncoming();

				final XQMessage origMsg = env.getMessage();

				final XQMessage newMsg = factory.createMessage();

				/* Copy all headers of the original message to the new message */
				final Iterator headerIterator = origMsg.getHeaderNames();

				while (headerIterator.hasNext()) {
					final String name = (String) headerIterator.next();

					newMsg.setHeaderValue(name, origMsg.getHeaderValue(name));
				}

				final XQPart newPart = newMsg.createPart();

				newPart.setContentId("Result");

				newPart.setContent(content.toString(),
						XQConstants.CONTENT_TYPE_TEXT);

				newMsg.addPart(newPart);

				env.setMessage(newMsg);

				final Iterator addressIterator = env.getAddresses();

				if (addressIterator.hasNext())
					ctx.addOutgoing(env);

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