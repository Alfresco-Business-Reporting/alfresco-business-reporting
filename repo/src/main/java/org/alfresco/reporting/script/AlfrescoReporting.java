/**
 * Copyright (C) 2011 - 2013 Alfresco Business Reporting project
 *
 * This file is part of the Alfresco Business Reporting project.
 *
 * Licensed under the GNU LGPL, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.gnu.org/licenses/lgpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.alfresco.reporting.script;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.TimeZone;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.reporting.Constants;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.ReportingModel;
import org.alfresco.reporting.action.executer.HarvestingExecuter;
import org.alfresco.reporting.action.executer.ReportRootExecutor;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.reporting.mybatis.SelectFromWhere;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//import com.google.gdata.data.finance.CostBasis;

public class AlfrescoReporting extends BaseScopableProcessorExtension {
	private static Log logger = LogFactory.getLog(AlfrescoReporting.class);

	private ServiceRegistry serviceRegistry;
	private SearchService searchService;
	private NodeService nodeService = null;
	private AuthorityService authorityService = null;
	private AuthenticationService authenticationService = null;
	private AuditService auditService = null;
	private SiteService siteService = null;

	private ReportingHelper reportingHelper;
	private DatabaseHelperBean dbhb = null;

	private NodeRef reportingRootRef = null;

	// private String blacklist=",";
	// private List<Object> queue = new ArrayList<Object>();
	// private Properties versionNodes = new Properties();

	// private String multivalue_seperator = Constants.MULTIVALUE_SEPERATOR;

	// -----------------------------------------------------------------------
	/**
	 * the obvious getters and setters from bean definition
	 */

	// TODO
	private SimpleDateFormat getSimpleDateFormat() {
		return new SimpleDateFormat();
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setAuditService(AuditService auditService) {
		this.auditService = auditService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}

	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	public void setDatabaseHelperBean(DatabaseHelperBean databaseHelperBean) {
		this.dbhb = databaseHelperBean;
	}

	public void setReportingHelper(ReportingHelper reportingHelper) {
		this.reportingHelper = reportingHelper;
	}

	// -----------------------------------------------------------------------

	// -----------------------------------------------------------------------

	/**
	 * Can be called from script. Redefines SQL column types for the Alfresco properties existing in the properties file. (Sum of all columns <65535 bytes,
	 * UTF-8 takes up to 4 bytes for a char, so each d:text into VARCHAR(500) is too eager... This properties file will overide individual PROPERTIES, not
	 * Alfresco TYPES
	 * 
	 * @param newFileName
	 *            a properties file that contains custom SQL column defintions for a given Alfresco property
	 */

	public String getStoreList() {
		return reportingHelper.getGlobalProperties().getProperty(Constants.property_storelist, "");
	}

	/**
	 * Adds this string to all multi value properties. Default = comma (',').
	 * 
	 * @param inString
	 *            seperator, can me multi-character. At least 1 character.
	 */
	/*
	 * public void setMultiValueSeperator(String inString){ if ((inString!=null) && (inString.length()>0)){ multivalue_seperator = inString+" "; } }
	 */

	public boolean isExecutionEnabled() {
		boolean executionEnabled = true;

		try {
			// executionEnabled = globalProperties.getProperty("reporting.execution.enabled", "true").equalsIgnoreCase("true");
			logger.debug("isExecutionEnabled: " + nodeService.getProperty(getReportingRoot(), ReportingModel.PROP_REPORTING_GLOBAL_EXECUTION_ENABLED));
			executionEnabled = (Boolean) nodeService.getProperty(getReportingRoot(), ReportingModel.PROP_REPORTING_GLOBAL_EXECUTION_ENABLED);
		} catch (Exception e) {
			logger.debug("isExecutionEnabled() returning exception. Thus returning true;");
			// logger.debug(e);
			executionEnabled = true;
		}
		return executionEnabled;
	}

	public boolean isHarvestEnabled() {
		boolean harvestEnabled = true;
		try {
			// harvestEnabled = globalProperties.getProperty("reporting.harvest.enabled", "true").equalsIgnoreCase("true");
			logger.debug("isHarvestEnabled: " + nodeService.getProperty(getReportingRoot(), ReportingModel.PROP_REPORTING_HARVEST_ENABLED));
			harvestEnabled = (Boolean) nodeService.getProperty(getReportingRoot(), ReportingModel.PROP_REPORTING_HARVEST_ENABLED);
		} catch (Exception e) {
			logger.debug("isHarvestEnabled() returning exception. Thus returning true;");
			// logger.debug(e);
			harvestEnabled = true;
		}
		return harvestEnabled;
	}

	public int countSearchResutls(String query) {
		SearchParameters sp = new SearchParameters();
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		sp.setQuery(query);
		ResultSet placeHolderResults = searchService.query(sp);
		return placeHolderResults.length();

	}

	/**
	 * Get the Folder that contains the reporting:reporting aspect. This contains the booleans harvestEnabled and globalExecutionEnabled
	 * 
	 * @return NodeRef of the folder Duplicate in NodeRefProcessor!!
	 */

	private NodeRef getReportingRoot() {

		if (this.reportingRootRef != null) {
			return this.reportingRootRef;
		} else {
			NodeRef thisRootRef = null;
			ResultSet placeHolderResults = null;
			try {

				String fullQuery = "TYPE:\"reporting:reportingRoot\"";
				SearchParameters sp = new SearchParameters();
				sp.setLanguage(SearchService.LANGUAGE_LUCENE);
				sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
				sp.setQuery(fullQuery);
				placeHolderResults = searchService.query(sp);
				// Constants.QUERYLANGUAGE,
				// "ASPECT:\"reporting:reportingRoot\"");

				// cycle the resultset of containers
				for (ResultSetRow placeHolderRow : placeHolderResults) {
					thisRootRef = placeHolderRow.getChildAssocRef().getChildRef();

					logger.debug("Found reporting root: " + nodeService.getProperty(thisRootRef, ContentModel.PROP_NAME));

				} // end for ResultSetRow
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (placeHolderResults != null) {
					placeHolderResults.close();
				}
			}
			this.reportingRootRef = thisRootRef;
			return thisRootRef;
		}
	}

	public ScriptNode getReportingRootNode() {
		return new ScriptNode(getReportingRoot(), serviceRegistry);
	}

	public void testLog(String logString) {
		logger.debug("## " + logString);
	}

	public void testLogFailed(String logString) {
		logger.fatal("## !! " + logString);
	}

	public String selectFromWhere(String select, String table, String where) {
		try {
			logger.debug("enter selectFromWhere: select=" + select + " from=" + table + " where=" + where);
			SelectFromWhere sfw = new SelectFromWhere(select, table, where);
			return dbhb.selectFromWhere(select, table, where);
		} catch (Exception e) {
			logger.fatal("Exception selectFromWhere: select=" + select + " from=" + table + " where=" + where);
			logger.fatal(e.getMessage());
			throw new AlfrescoRuntimeException(e.getMessage());
		}
	}

	public void resetLastTimestampTable(String tablename) {
		dbhb.resetLastTimestampTable(tablename);
	}

	public void clearLastTimestampTable(String tablename) {
		dbhb.clearLastTimestampTable(tablename);
	}

	/**
	 * get the current status of the reporting tool
	 * 
	 * @return
	 */
	public String getLastTimestampStatus(String tablename) {
		return dbhb.getLastTimestampStatus(tablename);
	}

	/**
	 * get the timestamp of the last succesful run. should be invoked from Script only
	 * 
	 * @return
	 */

	public String getLastTimestamp(String tablename) {
		return dbhb.getLastTimestamp(tablename);
	}

	public void setLastTimestampAndStatusDone(String tablename, String timestamp) {
		dbhb.setLastTimestampAndStatusDone(tablename, timestamp);
	}

	/**
	 * Set the string value of timestamp in the table 'lastrun' If the table does not yet exist, the table will be created.
	 * 
	 * @param timestamp
	 * @throws SQLException
	 */
	public void setLastTimestampStatusRunning(String tablename) {
		dbhb.setLastTimestampStatusRunning(tablename);
	}

	public void createLastTimestampTable(String tablename) {
		dbhb.createLastTimestampTableRow(tablename);
	}

	public void dropLastTimestampTable() {
		dbhb.dropLastTimestampTable();
	}

	public Map<String, String> getShowTables() {
		return dbhb.getShowTables();
	}

	private void harvest(final String frequency) {
		ActionService actionService = serviceRegistry.getActionService();
		Action harvest = actionService.createAction(HarvestingExecuter.NAME);
		harvest.setExecuteAsynchronously(true);
		harvest.setParameterValue(HarvestingExecuter.PARAM_FREQUENCY, frequency);
		actionService.executeAction(harvest, getReportingRoot());
	}

	/**
	 * Have a handler to invoke harvesting async from the javscript UI - MORE FREQUENT
	 * 
	 */
	public void harvestMoreFrequent() {
		harvest(Constants.MORE_FREQUENT);
	};

	/**
	 * have a handler to invoke harvesting async frm the javscript UI - LESS FREQUENT
	 * 
	 */
	public void harvestLessFrequent() {
		harvest(Constants.LESS_FREQUENT);
	}

	/**
	 * Have a handler to invoke harvesting async from the javscript UI - MORE FREQUENT
	 * 
	 */
	public void harvestAll() {
		harvest(Constants.ALL);
	};

	private void executeReport(String frequency) {
		ActionService actionService = serviceRegistry.getActionService();
		Action execute = actionService.createAction(ReportRootExecutor.NAME);
		execute.setExecuteAsynchronously(true);
		execute.setParameterValue(HarvestingExecuter.PARAM_FREQUENCY, frequency);
		actionService.executeAction(execute, getReportingRoot());
	}

	public void executeHourly() {
		executeReport(Constants.FREQUENCY_HOURLY);
	}

	public void executeDaily() {
		executeReport(Constants.FREQUENCY_DAYLY);
	}

	public void executeWeekly() {
		executeReport(Constants.FREQUENCY_WEEKLY);
	}

	public void executeMonthly() {
		executeReport(Constants.FREQUENCY_MONTHLY);
	}

	public void executeAll() {
		executeHourly();
		executeDaily();
		executeWeekly();
		executeMonthly();
	}

	/**
	 * add indexes to the uuid columns, as well as uuid+isLatest, for the given table
	 */
	public void addIndexesToTable(String tablename) {
		dbhb.createIndexesForTable(tablename);
	}

	/**
	 * add indexes to the uuid columns, as well as uuid+isLatest, for all tables
	 */
	public void addindexesToTables() {
		dbhb.createIndexForTables();
	}

	/**
	 * add an index against any column in the reporting database in any table to enhance report generation performance
	 * 
	 * @param tablename
	 * @param columnname
	 */
	public void createCustomIndexForTable(final String tablename, final String columnname) {
		dbhb.createCustomIndexForTable(tablename, columnname);
	}

	public AlfrescoReporting() {
		logger.info("Starting AlfrescoReporting module (Constructor)");
		int numberOfHours = TimeZone.getDefault().getRawOffset() / 3600000;
		String sign = "+";
		if (numberOfHours < 0)
			sign = "-";
		logger.info("Using timezone (for right hour in timestamp): " + TimeZone.getDefault().getDisplayName() + " (" + sign + numberOfHours + ")");

	}

	// ----------------------------------------------------------------------------
	// actual script methods
	// ----------------------------------------------------------------------------

	/**
	 * dropTables drops a list of tables if they exist
	 * 
	 * @param tables
	 *            a comma separated list of table names
	 */
	public void dropTables(String tablesToDrop) {
		logger.debug("Starting dropTables: " + tablesToDrop);
		try {
			dbhb.dropTables(tablesToDrop);
		} catch (Exception e) {
			logger.fatal("Exception dropTables: " + e.getMessage());
			throw new AlfrescoRuntimeException(e.getMessage());
		}
	}

	/**
	 * gets a list of all tables, and drops each one of them
	 */
	public void dropAllTables() {
		dbhb.dropAllTables();
	}

	/**
	 * sets the status of all tables to "Done"
	 */
	public void setAllStatusesDoneForTable() {
		dbhb.setAllStatusesDoneForTable();
	}

	public String getDatabaseVendor() {
		return reportingHelper.getDatabaseProvider();
	}

	/**
	 * logAllPropertyTypes shows all property types that are currently registered in the Alfresco repository Required: enable debug logging of this class!
	 */
	public void logAllPropertyTypes() {
		if (logger.isDebugEnabled()) {
			Collection<QName> dts = serviceRegistry.getDictionaryService().getAllDataTypes();
			Iterator<QName> myIterator = dts.iterator();
			while (myIterator.hasNext()) {
				QName q = myIterator.next();
				String returnType = q.toString();
				returnType = returnType.substring(returnType.indexOf("}") + 1, returnType.length());
				logger.debug(returnType);
			} // end while
		}
	}

	/**
	 * Log all reporting related key/values from alfresco-global.properties Required: enable debug logging of this class!
	 */
	public void logAllGlobalProperties() {
		if (logger.isDebugEnabled()) {
			Enumeration<Object> keys = reportingHelper.getGlobalProperties().keys();
			while (keys.hasMoreElements()) {
				String key = (String) keys.nextElement();
				if (key.contains("reporting.")) {
					logger.debug(key + "=" + reportingHelper.getGlobalProperties().getProperty(key));
				}
			}
		}
	}

}
