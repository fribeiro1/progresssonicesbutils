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

public final class DirSearchService implements XQServiceEx {
	private static final String PARAM_KEEP_ORIGINAL_PART = "keepOriginalPart";
	private static final String PARAM_MESSAGE_PART = "messagePart";
	private static final String PARAM_INITIAL_CONTEXT_FACTORY = "initialContextFactory";
	private static final String PARAM_PROVIDER_URL = "providerUrl";
	private static final String PARAM_SECURITY_AUTHENTICATION = "securityAuthentication";
	private static final String PARAM_SECURITY_CREDENTIALS = "securityCredentials";
	private static final String PARAM_SECURITY_PRINCIPAL = "securityPrincipal";

	private final Hashtable CONF = new Hashtable();

	public void destroy() {
	}

	public void init(final XQInitContext ctx) {
		final XQParameters params = ctx.getParameters();

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

	public void service(final XQServiceContext servCtx)
			throws XQServiceException {

		try {
			final XQMessageFactory factory = servCtx.getMessageFactory();

			final XQParameters params = servCtx.getParameters();

			final int messagePart = params.getIntParameter(PARAM_MESSAGE_PART,
					XQConstants.PARAM_STRING);

			final boolean keepOriginalPart = params.getBooleanParameter(
					PARAM_KEEP_ORIGINAL_PART, XQConstants.PARAM_STRING);

			final DirContext dirCtx = new InitialLdapContext(CONF, null);

			while (servCtx.hasNextIncoming()) {
				final XQEnvelope env = servCtx.getNextIncoming();

				final XQMessage origMsg = env.getMessage();

				final XQMessage newMsg = factory.createMessage();

				/* Copy all headers of the original message to the new message */
				final Iterator headerIterator = origMsg.getHeaderNames();

				while (headerIterator.hasNext()) {
					final String name = (String) headerIterator.next();

					newMsg.setHeaderValue(name, origMsg.getHeaderValue(name));
				}

				final Iterator addressIterator = env.getAddresses();

				for (int i = 0; i < origMsg.getPartCount(); i++) {

					/* Decide whether to process the part or not */
					if ((messagePart == i)
							|| (messagePart == XQConstants.ALL_PARTS)) {
						final XQPart origPart = origMsg.getPart(i);

						/* Decide whether to keep the original part or not */
						if (keepOriginalPart) {
							origPart.setContentId("original_part_"
									+ messagePart);

							newMsg.addPart(origPart);
						}

						final XQPart newPart = newMsg.createPart();

						final DirSearchDocument reqDoc = DirSearchDocument.Factory
								.parse((String) origPart.getContent());

						final DirSearch req = reqDoc.getDirSearch();

						final Controls reqControls = req.getControls();

						final Attributes reqAttrs = reqControls.getAttributes();

						final Scope.Enum reqScope = reqControls.getScope();

						final SearchControls dirControls = new SearchControls();

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
							final Attribute[] reqAttrArr = reqAttrs
									.getAttributeArray();

							final List reqAttrList = new ArrayList();

							for (int j = 0; j < reqAttrArr.length; j++)
								reqAttrList.add(reqAttrArr[j].getID());

							dirControls
									.setReturningAttributes((String[]) reqAttrList
											.toArray(new String[] {}));
						}

						final NamingEnumeration dirResultEnum = dirCtx.search(
								req.getContext(), req.getFilterExpression(),
								req.getFilterArgumentArray(), dirControls);

						final DirSearchResponseDocument resDoc = DirSearchResponseDocument.Factory
								.newInstance();

						final DirSearchResponse res = resDoc
								.addNewDirSearchResponse();

						while (dirResultEnum.hasMoreElements()) {
							final Result resResult = res.addNewResult();

							final SearchResult dirResult = (SearchResult) dirResultEnum
									.nextElement();

							resResult.setID(dirResult.getNameInNamespace());

							final javax.naming.directory.Attributes dirAttrList = dirResult
									.getAttributes();

							final NamingEnumeration dirAttrEnum = dirAttrList
									.getAll();

							while (dirAttrEnum.hasMoreElements()) {
								final Attribute resAttr = resResult
										.addNewAttribute();

								final javax.naming.directory.Attribute dirAttr = (javax.naming.directory.Attribute) dirAttrEnum
										.nextElement();

								resAttr.setID(dirAttr.getID());

								final NamingEnumeration dirValueEnum = dirAttr
										.getAll();

								while (dirValueEnum.hasMoreElements()) {
									final Object value = dirValueEnum
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

		} catch (final Exception e) {
			throw new XQServiceException(e);
		}

	}

	public void start() {
	}

	public void stop() {
	}

}