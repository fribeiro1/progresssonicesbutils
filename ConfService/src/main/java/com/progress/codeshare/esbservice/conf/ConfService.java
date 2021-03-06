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
package com.progress.codeshare.esbservice.conf;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import com.sonicsw.xq.XQConstants;
import com.sonicsw.xq.XQEnvelope;
import com.sonicsw.xq.XQInitContext;
import com.sonicsw.xq.XQMessage;
import com.sonicsw.xq.XQParameters;
import com.sonicsw.xq.XQServiceContext;
import com.sonicsw.xq.XQServiceEx;
import com.sonicsw.xq.XQServiceException;

public class ConfService implements XQServiceEx {
	private static String PARAM_FILE = "file";

	public void destroy() {
	}

	public void init(XQInitContext ctx) {
	}

	public void service(XQServiceContext ctx) throws XQServiceException {

		try {
			Properties conf = new Properties();

			XQParameters params = ctx.getParameters();

			String file = params.getParameter(PARAM_FILE,
					XQConstants.PARAM_STRING);

			conf.load(new BufferedInputStream(new ByteArrayInputStream(file
					.getBytes())));

			Collection keySet = conf.keySet();

			while (ctx.hasNextIncoming()) {
				XQEnvelope env = ctx.getNextIncoming();

				XQMessage msg = env.getMessage();

				Iterator keyIterator = keySet.iterator();

				while (keyIterator.hasNext()) {
					String key = (String) keyIterator.next();

					msg.setHeaderValue(key, conf.get(key));
				}

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