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

package org.alfresco.reporting.action.executer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.reporting.Constants;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.reporting.processor.ArchiveProcessor;
import org.alfresco.reporting.processor.TypeTableMap;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HarvestArchiveExecuter extends ActionExecuterAbstractBase {

	private ServiceRegistry serviceRegistry;
	private DatabaseHelperBean dbhb;
	private ReportingHelper reportingHelper;
	private Properties globalProps;
	public static final String NAME = "harvestArchiveExecuter";

	private static Log logger = LogFactory.getLog(HarvestArchiveExecuter.class);

	@Override
	protected void executeImpl(final Action action, final NodeRef nodeRef) {

		boolean archiveIndexed = globalProps.getProperty(Constants.PROPERTY_HARVEST_ARCHIVE_INDEXED, "true")
				.equalsIgnoreCase("true");

		//If archived is indexed, it is not necessary to crawl through the archive-store using this action
		if (!archiveIndexed) {

			// type<->table mapping. i.e. <"cm:document", <document,datalistitem>
			TypeTableMap documentMap = new TypeTableMap();
			documentMap.setType(globalProps.getProperty(Constants.PROPERTY_HARVEST_DOCUMENT_TYPE, "content"));
			documentMap.setTables(
					getTableNames(globalProps.getProperty(Constants.PROPERTY_HARVEST_DOCUMENT_TABLES)));

			TypeTableMap folderMap = new TypeTableMap();
			folderMap.setType(globalProps.getProperty(Constants.PROPERTY_HARVEST_FOLDER_TYPE, "folder"));
			folderMap.setTables(
					getTableNames(globalProps.getProperty(Constants.PROPERTY_HARVEST_FOLDER_TABLES, "folder")));

			ArrayList<TypeTableMap> maps = new ArrayList<TypeTableMap>();
			maps.add(documentMap);
			maps.add(folderMap);

			if (logger.isDebugEnabled())
				logger.debug("HarvestArchiveExecuter called");
			ArchiveProcessor archiveProcessor = new ArchiveProcessor(serviceRegistry, dbhb, reportingHelper);
			archiveProcessor.harvestArchive(maps);
		} else {
			logger.error("Archive is indexed, this action is not available. Set "+Constants.PROPERTY_HARVEST_ARCHIVE_INDEXED+" to false to enable this action.");
		}
	}

	private ArrayList<String> getTableNames(String properties) {
		ArrayList<String> tablenames= new ArrayList<String>();
		String[] splittedProps = properties.split(",");
		for (String s : splittedProps){
			logger.debug(s);
			tablenames.add(s);
		}
		return tablenames;
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> arg0) {
		// TODO Auto-generated method stub
	}

	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public DatabaseHelperBean getDatabaseHelperBean() {
		return dbhb;
	}

	public void setDatabaseHelperBean(DatabaseHelperBean dbhb) {
		this.dbhb = dbhb;
	}

	public ReportingHelper getReportingHelper() {
		return reportingHelper;
	}

	public void setReportingHelper(ReportingHelper reportingHelper) {
		this.reportingHelper = reportingHelper;
	}

	public Properties getGlobalProperties() {
		return globalProps;
	}

	public void setGlobalProperties(Properties globalProps) {
		this.globalProps = globalProps;
	}

}