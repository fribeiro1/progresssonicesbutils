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
package com.progress.codeshare.esbextension.messagePart;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.saxon.Controller;
import net.sf.saxon.expr.XPathContext;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.sonicsw.xq.XQMessage;
import com.sonicsw.xq.XQMessageException;
import com.sonicsw.xq.XQPart;
import com.sonicsw.xq.service.common.ServiceConstants;

public final class MessagePartExtension {
	private static final DocumentBuilderFactory FACTORY = DocumentBuilderFactory
			.newInstance();

	private static final String PARAM_MSG_PART_INDEX = "msgPartIndex";

	private DocumentBuilder builder;

	public MessagePartExtension() throws ParserConfigurationException {
		builder = FACTORY.newDocumentBuilder();
	}

	public Object getProperty(final XPathContext ctx, final String name)
			throws XQMessageException {
		final Controller controller = ctx.getController();

		final XQMessage msg = (XQMessage) controller
				.getParameter(ServiceConstants.XQMessage);

		final XQPart part = msg.getPart(((Integer) controller
				.getParameter(PARAM_MSG_PART_INDEX)).intValue());

		return part.getHeader().getValue(name);
	}

	public Object getProperty(final XPathContext ctx, final String name,
			final int index) throws XQMessageException {
		final Controller controller = ctx.getController();

		final XQMessage msg = (XQMessage) controller
				.getParameter(ServiceConstants.XQMessage);

		final XQPart part = msg.getPart(index);

		return part.getHeader().getValue(name);
	}

	public String getStringContent(final XPathContext ctx, final int index)
			throws XQMessageException {
		final Controller controller = ctx.getController();

		final XQMessage msg = (XQMessage) controller
				.getParameter(ServiceConstants.XQMessage);

		final XQPart part = msg.getPart(index);

		return (String) part.getContent();
	}

	public String getStringContent(final XPathContext ctx, final String index)
			throws XQMessageException {
		final Controller controller = ctx.getController();

		final XQMessage msg = (XQMessage) controller
				.getParameter(ServiceConstants.XQMessage);

		final XQPart part = msg.getPart(index);

		return (String) part.getContent();
	}

	public Document getXMLContent(final XPathContext ctx, final int index)
			throws XQMessageException {

		try {
			final Controller controller = ctx.getController();

			final XQMessage msg = (XQMessage) controller
					.getParameter(ServiceConstants.XQMessage);

			final XQPart part = msg.getPart(index);

			return builder.parse(new InputSource(new StringReader((String) part
					.getContent())));
		} catch (final Exception e) {
			throw new XQMessageException(e);
		}

	}

	public Document getXMLContent(final XPathContext ctx, final String index)
			throws XQMessageException {

		try {
			final Controller controller = ctx.getController();

			final XQMessage msg = (XQMessage) controller
					.getParameter(ServiceConstants.XQMessage);

			final XQPart part = msg.getPart(index);

			return builder.parse(new InputSource(new StringReader((String) part
					.getContent())));
		} catch (final Exception e) {
			throw new XQMessageException(e);
		}

	}

	public void setProperty(final XPathContext ctx, final String name,
			final String value) throws XQMessageException {
		final Controller controller = ctx.getController();

		final XQMessage msg = (XQMessage) controller
				.getParameter(ServiceConstants.XQMessage);

		final XQPart part = msg.getPart(((Integer) controller
				.getParameter(PARAM_MSG_PART_INDEX)).intValue());

		part.getHeader().setValue(name, value);
	}

	public void setProperty(final XPathContext ctx, final String name,
			final String value, final int index) throws XQMessageException {
		final Controller controller = ctx.getController();

		final XQMessage msg = (XQMessage) controller
				.getParameter(ServiceConstants.XQMessage);

		final XQPart part = msg.getPart(index);

		part.getHeader().setValue(name, value);
	}

}