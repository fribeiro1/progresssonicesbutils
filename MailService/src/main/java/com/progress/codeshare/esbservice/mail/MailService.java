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
package com.progress.codeshare.esbservice.mail;

import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.sonicsw.xq.XQConstants;
import com.sonicsw.xq.XQEnvelope;
import com.sonicsw.xq.XQHeader;
import com.sonicsw.xq.XQInitContext;
import com.sonicsw.xq.XQMessage;
import com.sonicsw.xq.XQParameters;
import com.sonicsw.xq.XQPart;
import com.sonicsw.xq.XQServiceContext;
import com.sonicsw.xq.XQServiceEx;
import com.sonicsw.xq.XQServiceException;

public final class MailService implements XQServiceEx {
	private static final String PARAM_BCC = "bcc";
	private static final String PARAM_CC = "cc";
	private static final String PARAM_FROM = "from";
	private static final String PARAM_HOST = "host";
	private static final String PARAM_PORT = "port";
	private static final String PARAM_REPLY_TO = "replyTo";
	private static final String PARAM_SUBJECT = "subject";
	private static final String PARAM_TO = "to";

	private static final Pattern PATTERN_HEADER = Pattern
			.compile("com\\.progress\\.codeshare\\.esbservice\\.mail\\.(.*)");

	private static final String PROP_HOST = "mail.host";
	private static final String PROP_PORT = "mail.port";

	private final Properties CONF = new Properties();

	public void destroy() {
	}

	public void init(final XQInitContext ctx) {
		final XQParameters params = ctx.getParameters();

		CONF.put(PROP_HOST, params.getParameter(PARAM_HOST,
				XQConstants.PARAM_STRING));

		final String port = params.getParameter(PARAM_PORT,
				XQConstants.PARAM_STRING);

		if (port != null)
			CONF.put(PROP_PORT, port);

	}

	public void service(final XQServiceContext ctx) throws XQServiceException {

		try {
			final Session session = Session.getDefaultInstance(CONF);

			final XQParameters params = ctx.getParameters();

			final String bcc = params.getParameter(PARAM_BCC,
					XQConstants.PARAM_STRING);

			final String cc = params.getParameter(PARAM_CC,
					XQConstants.PARAM_STRING);

			final String from = params.getParameter(PARAM_FROM,
					XQConstants.PARAM_STRING);

			final String replyTo = params.getParameter(PARAM_REPLY_TO,
					XQConstants.PARAM_STRING);

			final String subject = params.getParameter(PARAM_SUBJECT,
					XQConstants.PARAM_STRING);

			final String to = params.getParameter(PARAM_TO,
					XQConstants.PARAM_STRING);

			while (ctx.hasNextIncoming()) {
				final XQEnvelope env = ctx.getNextIncoming();

				final XQMessage msg = env.getMessage();

				/* Copy all headers of the message to the mail message */
				final Iterator headerIterator = msg.getHeaderNames();

				final Message mail = new MimeMessage(session);

				while (headerIterator.hasNext()) {
					final String name = (String) headerIterator.next();

					final Matcher matcher = PATTERN_HEADER.matcher(name);

					if (matcher.find())
						mail.setHeader(matcher.group(1), (String) msg
								.getHeaderValue(matcher.group()));

				}

				/* Set/Override the bcc: header */
				if (bcc != null)
					mail.setRecipient(RecipientType.BCC, new InternetAddress(
							bcc));

				/* Set/Override the cc: header */
				if (cc != null)
					mail
							.setRecipient(RecipientType.CC,
									new InternetAddress(cc));

				/* Set/Override the From: header */
				if (from != null)
					mail.setFrom(new InternetAddress(from));

				/* Set/Override the Reply-To: header */
				if (replyTo != null)
					mail
							.setReplyTo(new Address[] { new InternetAddress(
									replyTo) });

				/* Set/Override the Subject: header */
				if (subject != null)
					mail.setSubject(subject);

				/* Set/Override the To: header */
				if (to != null)
					mail
							.setRecipient(RecipientType.TO,
									new InternetAddress(to));

				final Multipart multipart = new MimeMultipart();

				/* Map all parts of the message to the mail message */
				for (int i = 0; i < msg.getPartCount(); i++) {
					final XQPart msgPart = msg.getPart(i);

					final XQHeader header = msgPart.getHeader();

					final Iterator keyIterator = header.getKeys();

					final BodyPart bodyPart = new MimeBodyPart();

					while (keyIterator.hasNext()) {
						final String key = (String) keyIterator.next();

						final Matcher matcher = PATTERN_HEADER.matcher(key);

						if (matcher.find())
							bodyPart.addHeader(matcher.group(1), bodyPart
									.getHeader(matcher.group())[0]);

					}

					bodyPart.setContent(msgPart.getContent(), msgPart
							.getContentType());

					multipart.addBodyPart(bodyPart);
				}

				mail.setContent(multipart);

				Transport.send(mail);

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