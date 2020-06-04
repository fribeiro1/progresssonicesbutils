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
package com.progress.codeshare.esbservice.db;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Iterator;

import org.apache.xmlbeans.XmlBoolean;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlDate;
import org.apache.xmlbeans.XmlDateTime;
import org.apache.xmlbeans.XmlDecimal;
import org.apache.xmlbeans.XmlInt;
import org.apache.xmlbeans.XmlString;

import com.progress.codeshare.esbservice.db.model.BoolParam;
import com.progress.codeshare.esbservice.db.model.DBRequest;
import com.progress.codeshare.esbservice.db.model.DBRequestDocument;
import com.progress.codeshare.esbservice.db.model.DBResponse;
import com.progress.codeshare.esbservice.db.model.DBResponseDocument;
import com.progress.codeshare.esbservice.db.model.DateField;
import com.progress.codeshare.esbservice.db.model.DateParam;
import com.progress.codeshare.esbservice.db.model.DecField;
import com.progress.codeshare.esbservice.db.model.DecParam;
import com.progress.codeshare.esbservice.db.model.IntField;
import com.progress.codeshare.esbservice.db.model.IntParam;
import com.progress.codeshare.esbservice.db.model.NumField;
import com.progress.codeshare.esbservice.db.model.NumParam;
import com.progress.codeshare.esbservice.db.model.Param;
import com.progress.codeshare.esbservice.db.model.Row;
import com.progress.codeshare.esbservice.db.model.StrField;
import com.progress.codeshare.esbservice.db.model.StrParam;
import com.progress.codeshare.esbservice.db.model.TimestampField;
import com.progress.codeshare.esbservice.db.model.TimestampParam;
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

public class DBService implements XQServiceEx {
	private static String PARAM_DRIVER = "driver";
	private static String PARAM_KEEP_ORIGINAL_PART = "keepOriginalPart";
	private static String PARAM_MESSAGE_PART = "messagePart";
	private static String PARAM_PASSWORD = "password";
	private static String PARAM_URL = "url";
	private static String PARAM_USERNAME = "username";

	private String driver;

	private String password;

	private String url;

	private String username;

	public void destroy() {
	}

	public void init(XQInitContext ctx) {
		XQParameters params = ctx.getParameters();

		driver = params.getParameter(PARAM_DRIVER, XQConstants.PARAM_STRING);

		password = params
				.getParameter(PARAM_PASSWORD, XQConstants.PARAM_STRING);

		url = params.getParameter(PARAM_URL, XQConstants.PARAM_STRING);

		username = params
				.getParameter(PARAM_USERNAME, XQConstants.PARAM_STRING);
	}

	public void service(XQServiceContext ctx) throws XQServiceException {
		Connection dbConn = null;

		try {
			XQMessageFactory factory = ctx.getMessageFactory();

			XQParameters params = ctx.getParameters();

			boolean keepOriginalPart = params.getBooleanParameter(
					PARAM_KEEP_ORIGINAL_PART, XQConstants.PARAM_STRING);

			int messagePart = params.getIntParameter(PARAM_MESSAGE_PART,
					XQConstants.PARAM_STRING);

			Class.forName(driver);

			dbConn = DriverManager.getConnection(url, username, password);

			while (ctx.hasNextIncoming()) {
				XQEnvelope env = ctx.getNextIncoming();

				XQMessage origMsg = env.getMessage();

				XQMessage newMsg = factory.createMessage();

				for (int i = 0; i < origMsg.getPartCount(); i++) {

					/* Decide whether to process the part or not */
					if ((messagePart == i)
							|| (messagePart == XQConstants.ALL_PARTS)) {
						PreparedStatement dbStmt = null;

						ResultSet dbRs = null;

						try {
							XQPart origPart = origMsg.getPart(i);

							DBRequestDocument reqDoc = DBRequestDocument.Factory
									.parse((String) origPart.getContent());

							DBRequest req = reqDoc.getDBRequest();

							dbStmt = dbConn.prepareStatement(req.getExpr());

							XmlCursor xmlCur = req.newCursor();

							xmlCur
									.selectPath("declare namespace DB='http://www.progress.com/codeshare/esbservice/db/model'; DB:Bool | DB:Date | DB:Dec | DB:Int | DB:Num | DB:Str | DB:Timestamp");

							while (xmlCur.toNextSelection()) {
								Param param = (Param) xmlCur.getObject();

								int id = param.getId();

								if (param instanceof BoolParam) {

									if (!param.isNil()) {
										XmlBoolean value = XmlBoolean.Factory
												.newValue(xmlCur.getTextValue());

										dbStmt.setBoolean(id, value
												.getBooleanValue());
									} else {
										dbStmt.setNull(id, Types.BOOLEAN);
									}

								} else if (param instanceof DateParam) {

									if (!param.isNil()) {
										XmlDate value = XmlDate.Factory
												.newValue(xmlCur.getTextValue());

										dbStmt.setDate(id, new Date(value
												.getDateValue().getTime()));
									} else {
										dbStmt.setNull(id, Types.DATE);
									}

								} else if (param instanceof DecParam) {

									if (!param.isNil()) {
										XmlDecimal value = XmlDecimal.Factory
												.newValue(xmlCur.getTextValue());

										dbStmt.setBigDecimal(id, value
												.getBigDecimalValue());
									} else {
										dbStmt.setNull(id, Types.DECIMAL);
									}

								} else if (param instanceof IntParam) {

									if (!param.isNil()) {
										XmlInt value = XmlInt.Factory
												.newValue(xmlCur.getTextValue());

										dbStmt.setInt(id, value.getIntValue());
									} else {
										dbStmt.setNull(id, Types.INTEGER);
									}

								} else if (param instanceof NumParam) {

									if (!param.isNil()) {
										XmlDecimal value = XmlDecimal.Factory
												.newValue(xmlCur.getTextValue());

										dbStmt.setBigDecimal(id, value
												.getBigDecimalValue());
									} else {
										dbStmt.setNull(id, Types.NUMERIC);
									}

								} else if (param instanceof StrParam) {

									if (!param.isNil()) {
										XmlString value = XmlString.Factory
												.newValue(xmlCur.getTextValue());

										dbStmt.setString(id, value
												.getStringValue());
									} else {
										dbStmt.setNull(id, Types.VARCHAR);
									}

								} else if (param instanceof TimestampParam) {

									if (!param.isNil()) {
										XmlDateTime value = XmlDateTime.Factory
												.newValue(xmlCur.getTextValue());

										dbStmt.setTimestamp(id, new Timestamp(
												value.getCalendarValue()
														.getTimeInMillis()));
									} else {
										dbStmt.setNull(id, Types.TIMESTAMP);
									}

								}

							}

							boolean hasMoreResults = dbStmt.execute();

							int dbCnt = dbStmt.getUpdateCount();

							/*
							 * Copy all headers of the original message to the
							 * new message
							 */
							Iterator headerIterator = origMsg
									.getHeaderNames();

							while (headerIterator.hasNext()) {
								String name = (String) headerIterator
										.next();

								newMsg.setHeaderValue(name, origMsg
										.getHeaderValue(name));
							}

							/* Decide whether to keep the original part or not */
							if (keepOriginalPart) {
								origPart.setContentId("original_part_" + i);

								newMsg.addPart(origPart);
							}

							XQPart newPart = newMsg.createPart();

							newPart.setContentId("Result-" + i);

							DBResponseDocument resDoc = DBResponseDocument.Factory
									.newInstance();

							DBResponse res = resDoc.addNewDBResponse();

							while ((hasMoreResults) || (dbCnt != -1)) {

								if (hasMoreResults) {
									dbRs = dbStmt.getResultSet();

									com.progress.codeshare.esbservice.db.model.ResultSet rs = res
											.addNewResultSet();

									ResultSetMetaData dbMetaData = dbRs
											.getMetaData();

									while (dbRs.next()) {
										Row row = rs.addNewRow();

										for (int j = 1; j <= dbMetaData
												.getColumnCount(); j++) {
											int type = dbMetaData
													.getColumnType(j);

											String name = dbMetaData
													.getColumnName(j);

											if (Types.DATE == type) {
												DateField field = row
														.addNewDate();

												Date dbValue = dbRs
														.getDate(j);

												if (dbValue != null) {
													XmlDate value = XmlDate.Factory
															.newInstance();

													value.setDateValue(dbValue);

													field.set(value);
												} else {
													field.setNil();
												}

												field.setName(name);
											} else if (Types.DECIMAL == type) {
												DecField field = row
														.addNewDec();

												BigDecimal dbValue = dbRs
														.getBigDecimal(j);

												if (dbValue != null) {
													XmlDecimal value = XmlDecimal.Factory
															.newInstance();

													value
															.setBigDecimalValue(dbValue);

													field.set(value);
												} else {
													field.setNil();
												}

												field.setName(name);
											} else if (Types.INTEGER == type) {
												IntField field = row
														.addNewInt();

												XmlInt value = XmlInt.Factory
														.newInstance();

												value.setIntValue(dbRs
														.getInt(j));

												field.set(value);

												field.setName(name);
											} else if (Types.NUMERIC == type) {
												NumField field = row
														.addNewNum();

												BigDecimal dbValue = dbRs
														.getBigDecimal(j);

												if (dbValue != null) {
													XmlDecimal value = XmlDecimal.Factory
															.newInstance();

													value
															.setBigDecimalValue(dbValue);

													field.set(value);
												} else {
													field.setNil();
												}

												field.setName(name);
											} else if (Types.TIMESTAMP == type) {
												TimestampField field = row
														.addNewTimestamp();

												Timestamp dbValue = dbRs
														.getTimestamp(j);

												if (dbValue != null) {
													XmlDateTime value = XmlDateTime.Factory
															.newInstance();

													value.setDateValue(dbValue);

													field.set(value);
												} else {
													field.setNil();
												}

												field.setName(name);
											} else {
												StrField field = row
														.addNewStr();

												String dbValue = dbRs
														.getString(j);

												if (dbValue != null) {
													XmlString value = XmlString.Factory
															.newInstance();

													value
															.setStringValue(dbValue);

													field.set(value);
												} else {
													field.setNil();
												}

												field.setName(name);
											}

										}

									}

									dbRs.close();
								}

								if (dbCnt != -1) {
									XmlInt resCnt = res
											.addNewUpdateCount();

									resCnt.setIntValue(dbCnt);
								}

								hasMoreResults = dbStmt.getMoreResults();

								dbCnt = dbStmt.getUpdateCount();
							}

							resDoc.setDBResponse(res);

							newPart.setContent(resDoc.toString(),
									XQConstants.CONTENT_TYPE_XML);

							newMsg.addPart(newPart);
						} catch (SQLException e) {
							e.printStackTrace();
						} finally {

							if (dbRs != null) {

								try {
									dbRs.close();
								} catch (SQLException e) {
									e.printStackTrace();
								} finally {

									if (dbStmt != null) {

										try {
											dbStmt.close();
										} catch (SQLException e) {
											e.printStackTrace();
										}

									}

								}

							}

						}

					}

				}

				env.setMessage(newMsg);

				Iterator addressIterator = env.getAddresses();

				if (addressIterator.hasNext())
					ctx.addOutgoing(env);

			}

		} catch (Exception e) {
			throw new XQServiceException(e);
		} finally {

			if (dbConn != null) {

				try {
					dbConn.close();
				} catch (SQLException e) {
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
