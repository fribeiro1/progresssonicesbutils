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
package com.progress.codeshare.esbservice.dirAuth;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;

import com.progress.codeshare.esbservice.dirAuth.model.Control;
import com.progress.codeshare.esbservice.dirAuth.model.DirAuth;
import com.progress.codeshare.esbservice.dirAuth.model.DirAuthDocument;
import com.progress.codeshare.esbservice.dirAuth.model.DirAuthResponse;
import com.progress.codeshare.esbservice.dirAuth.model.DirAuthResponseDocument;
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

public class DirAuthService implements XQServiceEx {

	private static class BasicControl implements Control {
		private static long serialVersionUID = 2349027930790221141L;

		private boolean criticality = false;

		private String id;

		private byte[] value;

		public BasicControl(String id) {
			this.id = id;
		}

		public BasicControl(String id, boolean criticality, byte[] value) {
			this.id = id;
			this.criticality = criticality;
			this.value = value;
		}

		public byte[] getEncodedValue() {
			return value;
		}

		public String getID() {
			return id;
		}

		public boolean isCritical() {
			return criticality;
		}

	}

	private static String PARAM_INITIAL_CONTEXT_FACTORY = "initialContextFactory";
	private static String PARAM_KEEP_ORIGINAL_PART = "keepOriginalPart";
	private static String PARAM_MESSAGE_PART = "messagePart";

	private static String PARAM_PROVIDER_URL = "providerUrl";

	private Hashtable CONF = new Hashtable();

	public void destroy() {
	}

	public void init(XQInitContext ctx) {
		XQParameters params = ctx.getParameters();

		CONF.put(Context.INITIAL_CONTEXT_FACTORY,
				params.getParameter(PARAM_INITIAL_CONTEXT_FACTORY, XQConstants.PARAM_STRING));
		CONF.put(Context.PROVIDER_URL, params.getParameter(PARAM_PROVIDER_URL, XQConstants.PARAM_STRING));
	}

	public void service(XQServiceContext ctx) throws XQServiceException {

		try {
			XQMessageFactory factory = ctx.getMessageFactory();

			XQParameters params = ctx.getParameters();

			int messagePart = params.getIntParameter(PARAM_MESSAGE_PART, XQConstants.PARAM_STRING);

			boolean keepOriginalPart = params.getBooleanParameter(PARAM_KEEP_ORIGINAL_PART,
					XQConstants.PARAM_STRING);

			while (ctx.hasNextIncoming()) {
				XQEnvelope env = ctx.getNextIncoming();

				XQMessage origMsg = env.getMessage();

				XQMessage newMsg = factory.createMessage();

				/*
				 * Copy all headers of the original message to the new message
				 */
				Iterator headerIterator = origMsg.getHeaderNames();

				while (headerIterator.hasNext()) {
					String name = (String) headerIterator.next();

					newMsg.setHeaderValue(name, origMsg.getHeaderValue(name));
				}

				Iterator addressIterator = env.getAddresses();

				for (int i = 0; i < origMsg.getPartCount(); i++) {

					/* Decide whether to process the part or not */
					if ((messagePart == i) || (messagePart == XQConstants.ALL_PARTS)) {
						XQPart origPart = origMsg.getPart(i);

						/* Decide whether to keep the original part or not */
						if (keepOriginalPart) {
							origPart.setContentId("original_part_" + messagePart);

							newMsg.addPart(origPart);
						}

						XQPart newPart = newMsg.createPart();

						newPart.setContentId("Result-" + messagePart);

						DirAuthDocument reqDoc = DirAuthDocument.Factory.parse((String) origPart.getContent());

						DirAuth req = reqDoc.getDirAuth();

						CONF.put(Context.SECURITY_AUTHENTICATION, req.getMethod());
						CONF.put(Context.SECURITY_CREDENTIALS, req.getCredentials());
						CONF.put(Context.SECURITY_PRINCIPAL, req.getPrincipal());

						Control[] reqCtrlArr = req.getControlArray();

						List reqCtrlList = new ArrayList();

						for (int j = 0; j < reqCtrlArr.length; j++)
							reqCtrlList.add(new BasicControl(reqCtrlArr[i].getID()));

						DirAuthResponseDocument resDoc = DirAuthResponseDocument.Factory.newInstance();

						DirAuthResponse res = DirAuthResponse.Factory.newInstance();

						try {
							new InitialLdapContext(CONF, (javax.naming.ldap.Control[]) reqCtrlList
									.toArray(new javax.naming.ldap.Control[] {}));

							res.setAuthenticated(true);
						} catch (NamingException e) {
							res.setAuthenticated(false);
						}

						resDoc.setDirAuthResponse(res);

						newPart.setContent(resDoc.toString(), XQConstants.CONTENT_TYPE_XML);

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

		} catch (Exception e) {
			throw new XQServiceException(e);
		}

	}

	public void start() {
	}

	public void stop() {
	}

}