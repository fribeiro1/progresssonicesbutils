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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.quartz.JobDataMap;
import org.quartz.jobs.ee.jms.JmsMessageFactory;

public final class SchMessageFactory implements JmsMessageFactory {

	public Message createMessage(final JobDataMap jobDataMap, final Session ses) {
		Message result = null;

		try {
			result = ses.createMessage();
		} catch (final JMSException e) {
			e.printStackTrace();
		}

		return result;
	}

}