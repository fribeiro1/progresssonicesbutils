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
package com.progress.codeshare.esbservice.dirSearch;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;

import com.progress.codeshare.esbservice.dirSearch.model.Attribute;
import com.progress.codeshare.esbservice.dirSearch.model.Attributes;
import com.progress.codeshare.esbservice.dirSearch.model.Controls;
import com.progress.codeshare.esbservice.dirSearch.model.DirSearch;
import com.progress.codeshare.esbservice.dirSearch.model.DirSearchDocument;
import com.progress.codeshare.esbservice.dirSearch.model.DirSearchResponse;
import com.progress.codeshare.esbservice.dirSearch.model.DirSearchResponseDocument;
import com.progress.codeshare.esbservice.dirSearch.model.Result;
import com.progress.codeshare.esbservice.dirSearch.model.Scope;
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

public class DirSearchService implements XQServiceEx {
	private static String PARAM_KEEP_ORIGINAL_PART = "keepOriginalPart";
	private static String PARAM_MESSAGE_PART = "messagePart";
	private static String PARAM_INITIAL_CONTEXT_FACTORY = "initialContextFactory";
	private static String PARAM_PROVIDER_URL = "providerUrl";
	private static String PARAM_SECURITY_AUTHENTICATION = "securityAuthentication";
	private static String PARAM_SECURITY_CREDENTIALS = "securityCredentials";
	private static String PARAM_SECURITY_PRINCIPAL = "securityPrincipal";

	private Hashtable CONF = new Hashtable();

	public void destroy() {
	}

	public void init(XQInitContext ctx) {
		XQParameters params = ctx.getParameters();

		CONF.put(Context.INITIAL_CONTEXT_FACTORY, params.getParameter(
				PARAM_INITIAL_CONTEXT_FACTORY, XQConstants.PARAM_STRING));
		CONF.put(Context.PROVIDER_URL, params.getParameter(PARAM_PROVIDER_URL,
				XQConstants.PARAM_STRING));
		CONF.put(Context.SECURITY_AUTHENTICATION, params.getParameter(
				PARAM_SECURITY_AUTHENTICATION, XQConstants.PARAM_STRING));
		CONF.put(Context.SECURITY_CREDENTIALS, params.getParameter(
				PARAM_SECURITY_CREDENTIALS, XQConstants.PARAM_STRING));
		CONF.put(Context.SECURITY_PRINCIPAL, params.getParameter(
				PARAM_SECURITY_PRINCIPAL, XQConstants.PARAM_STRING));
	}

	public void service(XQServiceContext servCtx)
			throws XQServiceException {

		try {
			XQMessageFactory factory = servCtx.getMessageFactory();

			XQParameters params = servCtx.getParameters();

			int messagePart = params.getIntParameter(PARAM_MESSAGE_PART,
					XQConstants.PARAM_STRING);

			boolean keepOriginalPart = params.getBooleanParameter(
					PARAM_KEEP_ORIGINAL_PART, XQConstants.PARAM_STRING);

			DirContext dirCtx = new InitialLdapContext(CONF, null);

			while (servCtx.hasNextIncoming()) {
				XQEnvelope env = servCtx.getNextIncoming();

				XQMessage origMsg = env.getMessage();

				XQMessage newMsg = factory.createMessage();

				/* Copy all headers of the original message to the new message */
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
							origPart.setContentId("original_part_"
									+ messagePart);

							newMsg.addPart(origPart);
						}

						XQPart newPart = newMsg.createPart();

						DirSearchDocument reqDoc = DirSearchDocument.Factory
								.parse((String) origPart.getContent());

						DirSearch req = reqDoc.getDirSearch();

						Controls reqControls = req.getControls();

						Attributes reqAttrs = reqControls.getAttributes();

						Scope.Enum reqScope = reqControls.getScope();

						SearchControls dirControls = new SearchControls();

						if (Scope.OBJECT_SCOPE == reqScope)
							dirControls
									.setSearchScope(SearchControls.OBJECT_SCOPE);
						else if (Scope.ONELEVEL_SCOPE == reqScope)
							dirControls
									.setSearchScope(SearchControls.ONELEVEL_SCOPE);
						else if (Scope.SUBTREE_SCOPE == reqScope)
							dirControls
									.setSearchScope(SearchControls.SUBTREE_SCOPE);

						if (reqAttrs != null) {
							Attribute[] reqAttrArr = reqAttrs
									.getAttributeArray();

							List reqAttrList = new ArrayList();

							for (int j = 0; j < reqAttrArr.length; j++)
								reqAttrList.add(reqAttrArr[j].getID());

							dirControls
									.setReturningAttributes((String[]) reqAttrList
											.toArray(new String[] {}));
						}

						NamingEnumeration dirResultEnum = dirCtx.search(
								req.getContext(), req.getFilterExpression(),
								req.getFilterArgumentArray(), dirControls);

						DirSearchResponseDocument resDoc = DirSearchResponseDocument.Factory
								.newInstance();

						DirSearchResponse res = resDoc
								.addNewDirSearchResponse();

						while (dirResultEnum.hasMoreElements()) {
							Result resResult = res.addNewResult();

							SearchResult dirResult = (SearchResult) dirResultEnum
									.nextElement();

							resResult.setID(dirResult.getNameInNamespace());

							javax.naming.directory.Attributes dirAttrList = dirResult
									.getAttributes();

							NamingEnumeration dirAttrEnum = dirAttrList
									.getAll();

							while (dirAttrEnum.hasMoreElements()) {
								Attribute resAttr = resResult
										.addNewAttribute();

								javax.naming.directory.Attribute dirAttr = (javax.naming.directory.Attribute) dirAttrEnum
										.nextElement();

								resAttr.setID(dirAttr.getID());

								NamingEnumeration dirValueEnum = dirAttr
										.getAll();

								while (dirValueEnum.hasMoreElements()) {
									Object value = dirValueEnum
											.nextElement();

									if (value != null)
										resAttr.addValue(value.toString());

								}

							}

						}

						resDoc.setDirSearchResponse(res);

						newPart.setContent(resDoc.toString(),
								XQConstants.CONTENT_TYPE_XML);

						newMsg.addPart(newPart);
					}

					/* Break when done */
					if (messagePart == i)
						break;

				}

				env.setMessage(newMsg);

				if (addressIterator.hasNext())
					servCtx.addOutgoing(env);

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