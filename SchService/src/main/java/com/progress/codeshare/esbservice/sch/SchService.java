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

import java.util.Iterator;
import java.util.TimeZone;

import org.apache.xmlbeans.XmlCursor;
import org.quartz.JobDataMap;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.progress.codeshare.esbservice.sch.model.CronTrigger;
import com.progress.codeshare.esbservice.sch.model.Job;
import com.progress.codeshare.esbservice.sch.model.JobDetail;
import com.progress.codeshare.esbservice.sch.model.SchConfig;
import com.progress.codeshare.esbservice.sch.model.SchConfigDocument;
import com.progress.codeshare.esbservice.sch.model.SimpleTrigger;
import com.sonicsw.xq.XQConstants;
import com.sonicsw.xq.XQEnvelope;
import com.sonicsw.xq.XQInitContext;
import com.sonicsw.xq.XQParameters;
import com.sonicsw.xq.XQServiceContext;
import com.sonicsw.xq.XQServiceEx;
import com.sonicsw.xq.XQServiceException;

public final class SchService implements XQServiceEx {
	private static final String PARAM_CONF_FILE = "confFile";

	private Scheduler scheduler;

	public void destroy() {
	}

	public void init(final XQInitContext ctx) throws XQServiceException {

		try {
			scheduler = StdSchedulerFactory.getDefaultScheduler();

			final XQParameters params = ctx.getParameters();

			final SchConfigDocument doc = SchConfigDocument.Factory
					.parse(params.getParameter(PARAM_CONF_FILE,
							XQConstants.PARAM_XML));

			final SchConfig conf = doc.getSchConfig();

			final Job[] jobs = conf.getJobArray();

			for (int i = 0; i < jobs.length; i++) {
				final JobDetail detail = jobs[i].getJobDetail();

				String jobGrp;

				if (!detail.isSetGrp())
					jobGrp = Scheduler.DEFAULT_GROUP;
				else
					jobGrp = detail.getGrp();

				final org.quartz.JobDetail qtzDetail = new org.quartz.JobDetail(
						detail.getName(), jobGrp, SchJob.class);

				final JobDataMap map = new JobDataMap();

				map.put(SchConstants.PROP_CONNECTION_FACTORY, detail
						.getConnFactory());

				map.put(SchConstants.PROP_DESTINATION, detail.getDestination());

				if (detail.isSetDomain())
					map.put(SchConstants.PROP_DOMAIN, detail.getDomain());

				if (detail.isSetPassword())
					map.put(SchConstants.PROP_PASSWORD, detail.getPassword());

				if (detail.isSetUser())
					map.put(SchConstants.PROP_USER, detail.getUser());

				qtzDetail.setJobDataMap(map);

				final XmlCursor cur = jobs[i].newCursor();

				cur
						.selectPath("declare namespace Sch='http://www.progress.com/codeshare/esbservice/sch/model'; Sch:Cron | Sch:Simple");

				Trigger qtzTrg = null;

				while (cur.toNextSelection()) {
					final com.progress.codeshare.esbservice.sch.model.Trigger trg = (com.progress.codeshare.esbservice.sch.model.Trigger) cur
							.getObject();

					if (trg instanceof CronTrigger) {
						final CronTrigger cronTrg = (CronTrigger) trg;

						String trgGrp;

						if (!cronTrg.isSetGrp())
							trgGrp = Scheduler.DEFAULT_GROUP;
						else
							trgGrp = cronTrg.getGrp();

						final org.quartz.CronTrigger qtzCronTrg = new org.quartz.CronTrigger(
								cronTrg.getName(), trgGrp);

						if (cronTrg.isSetStartTime())
							qtzCronTrg.setStartTime(trg.getStartTime()
									.getTime());

						if (cronTrg.isSetEndTime())
							qtzCronTrg.setEndTime(trg.getEndTime().getTime());

						qtzCronTrg.setCronExpression(cronTrg.getExpr());

						if (cronTrg.isSetTimeZone())
							qtzCronTrg.setTimeZone(TimeZone.getTimeZone(cronTrg
									.getTimeZone()));

						qtzTrg = qtzCronTrg;
					} else if (trg instanceof SimpleTrigger) {
						final SimpleTrigger simpleTrg = (SimpleTrigger) trg;

						String trgGrp;

						if (!simpleTrg.isSetGrp())
							trgGrp = Scheduler.DEFAULT_GROUP;
						else
							trgGrp = simpleTrg.getGrp();

						final org.quartz.SimpleTrigger qtzSimpleTrg = new org.quartz.SimpleTrigger(
								simpleTrg.getName(), trgGrp);

						if (simpleTrg.isSetStartTime())
							qtzSimpleTrg.setStartTime(simpleTrg.getStartTime()
									.getTime());

						if (simpleTrg.isSetEndTime())
							qtzSimpleTrg.setEndTime(simpleTrg.getEndTime()
									.getTime());

						if (simpleTrg.isSetRepeatCount())
							qtzSimpleTrg.setRepeatCount(simpleTrg
									.getRepeatCount());

						if (simpleTrg.isSetRepeatInterval())
							qtzSimpleTrg.setRepeatInterval(simpleTrg
									.getRepeatInterval());

						qtzTrg = qtzSimpleTrg;
					}

					scheduler.scheduleJob(qtzDetail, qtzTrg);
				}

			}

		} catch (final Exception e) {
			throw new XQServiceException(e);
		}

	}

	public void service(final XQServiceContext ctx) throws XQServiceException {

		while (ctx.hasNextIncoming()) {
			final XQEnvelope env = ctx.getNextIncoming();

			final Iterator addressIterator = env.getAddresses();

			if (addressIterator.hasNext())
				ctx.addOutgoing(env);

		}

	}

	public void start() {

		try {
			scheduler.start();
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

	public void stop() {

		try {
			scheduler.shutdown();
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

}