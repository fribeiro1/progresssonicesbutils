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
package com.progress.codeshare.esbservice.lineReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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

public final class LineReaderService implements XQServiceEx {
	private static final String PARAM_FILE = "file";

	public void destroy() {
	}

	public void init(XQInitContext ctx) {
	}

	public void service(final XQServiceContext ctx) throws XQServiceException {
		BufferedReader reader = null;

		try {
			final XQParameters params = ctx.getParameters();

			final String file = params.getParameter(PARAM_FILE,
					XQConstants.PARAM_STRING);

			reader = new BufferedReader(new FileReader(file));

			String line = reader.readLine();

			final XQMessageFactory factory = ctx.getMessageFactory();

			final XQEnvelope env = ctx.getFirstIncoming();

			final Iterator addressIterator = env.getAddresses();

			while (line != null) {
				final XQMessage msg = factory.createMessage();

				final XQPart part = msg.createPart();

				part.setContentId("Result");

				part.setContent(line, XQConstants.CONTENT_TYPE_TEXT);

				msg.addPart(part);

				if (addressIterator.hasNext())
					ctx.addOutgoing(msg);

				line = reader.readLine();
			}

		} catch (final Exception e) {
			throw new XQServiceException(e);
		} finally {

			if (reader != null) {

				try {
					reader.close();
				} catch (final IOException e) {
					throw new XQServiceException(e);
				}

			}

		}
	}

	public void start() {
	}

	public void stop() {
	}

}