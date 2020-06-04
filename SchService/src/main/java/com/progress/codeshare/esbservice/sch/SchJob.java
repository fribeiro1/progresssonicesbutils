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
package com.progress.codeshare.esbservice.sch;

import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.jobs.ee.jms.JmsMessageFactory;

public class SchJob implements Job {

	public void execute(JobExecutionContext jobCtx)
			throws JobExecutionException {
		Connection conn = null;

		Session sess = null;

		MessageProducer producer = null;

		try {
			JobDetail detail = jobCtx.getJobDetail();

			JobDataMap dataMap = detail.getJobDataMap();

			Hashtable env = new Hashtable();

			env.put(Context.INITIAL_CONTEXT_FACTORY,
					"com.sonicsw.jndi.mfcontext.MFContextFactory");

			if (dataMap.containsKey(SchConstants.PROP_DOMAIN))
				env.put("com.sonicsw.jndi.mfcontext.domain", dataMap
						.get(SchConstants.PROP_DOMAIN));

			Context namingCtx = new InitialContext(env);

			ConnectionFactory connFactory = (ConnectionFactory) namingCtx
					.lookup(dataMap
							.getString(SchConstants.PROP_CONNECTION_FACTORY));

			String user = dataMap.getString(SchConstants.PROP_USER);

			String password = dataMap
					.getString(SchConstants.PROP_PASSWORD);

			if ((user != null) && (password != null))
				conn = connFactory.createConnection(user, password);
			else
				conn = connFactory.createConnection();

			sess = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

			Destination destination = (Destination) namingCtx
					.lookup(dataMap.getString(SchConstants.PROP_DESTINATION));

			producer = sess.createProducer(destination);

			JmsMessageFactory messageFactory = new SchMessageFactory();

			Message msg = messageFactory.createMessage(dataMap, sess);

			producer.send(msg);
		} catch (Exception e) {
			throw new JobExecutionException(e);
		} finally {

			if (producer != null) {

				try {
					producer.close();
				} catch (JMSException e) {
					throw new JobExecutionException(e);
				} finally {

					if (sess != null) {

						try {
							sess.close();
						} catch (JMSException e) {
							throw new JobExecutionException(e);
						} finally {

							if (conn != null) {

								try {
									conn.close();
								} catch (JMSException e) {
									throw new JobExecutionException(e);
								}

							}

						}

					}

				}

			}

			if (sess != null) {

				try {
					sess.close();
				} catch (JMSException e) {
					throw new JobExecutionException(e);
				} finally {

					if (conn != null) {

						try {
							conn.close();
						} catch (JMSException e) {
							throw new JobExecutionException(e);
						}

					}

				}

			}

			if (conn != null) {

				try {
					conn.close();
				} catch (JMSException e) {
					throw new JobExecutionException(e);
				}

			}

		}

	}

}