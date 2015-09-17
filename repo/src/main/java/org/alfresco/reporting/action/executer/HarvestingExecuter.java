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

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.transaction.SystemException;

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.reporting.Constants;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.ReportingModel;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.reporting.processor.AuditingExportProcessor;
import org.alfresco.reporting.processor.CategoryProcessor;
import org.alfresco.reporting.processor.GroupProcessor;
import org.alfresco.reporting.processor.NodeRefBasedPropertyProcessor;
import org.alfresco.reporting.processor.PersonProcessor;
import org.alfresco.reporting.processor.ProcessProcessor;
import org.alfresco.reporting.processor.SitePersonProcessor;
import org.alfresco.reporting.processor.WorkflowTaskPropertyProcessor;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HarvestingExecuter extends ActionExecuterAbstractBase {
	
	// will be used from HarvestingHandlerBean
	public static final String NAME = "harvesting-executer";
	public static final String PARAM_FREQUENCY = "frequency";
	private Properties globalProperties;
	private ServiceRegistry serviceRegistry;
	private SearchService searchService;
	private DatabaseHelperBean dbhb;
	private NodeService nodeService;
	private ReportingHelper reportingHelper;
	
	private static Log logger = LogFactory.getLog(HarvestingExecuter.class);

	
	@Override
	protected void executeImpl(final Action action, final NodeRef harvestDefNodeRef) {
		
		ActionService actionService = serviceRegistry.getActionService();
		Action harvestArchiveAction = actionService.createAction(HarvestArchiveExecuter.NAME);
		actionService.executeAction(harvestArchiveAction, harvestDefNodeRef);

		/*
		 * TARGET_TYPE is one of
		 *   moreFrequent
		 *   lessFrequent
		 *   all	--> process all
		 *   null 	--> process curent NodeRef
		 */
/*		String frequency = (String)action.getParameterValue(PARAM_FREQUENCY);
		
		
		String fullQuery = getHarvestingDefinitionQuery(frequency);
		if (fullQuery!=null){
			if (logger.isDebugEnabled()) 
				logger.debug("executeImpl: frequency=" + frequency);
			SearchParameters sp = new SearchParameters();
			sp.setLanguage(SearchService.LANGUAGE_LUCENE);
			sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
			sp.setQuery(fullQuery);
			ResultSet results = searchService.query(sp);
			if (logger.isDebugEnabled()) 
				logger.debug("executeImpl: Found results: " + results.length());
			Iterator<ResultSetRow> resultsIterator = results.iterator();
			while (resultsIterator.hasNext()){
				NodeRef result = resultsIterator.next().getNodeRef();
				NodeRef parentRef = nodeService.getPrimaryParent(result).getParentRef();
				
				if (ReportingModel.TYPE_REPORTING_HARVEST_DEFINITION.equals(nodeService.getType(result))
						&&	isHarvestingEnabledByProperties()
						&& (null!=nodeService.getProperty(
								parentRef, ReportingModel.PROP_REPORTING_HARVEST_ENABLED))
						&&	(Boolean)nodeService.getProperty(
								parentRef, ReportingModel.PROP_REPORTING_HARVEST_ENABLED)
						 ) {
					processHarvestDefinition(result);
				} // end if
				else {
					// it is another node type -> make it a generic search-all execution
					// check against frequency
					if (logger.isDebugEnabled()){
						logger.debug("No harvesting run");
						logger.debug("Properties: " + isHarvestingEnabledByProperties());
						logger.debug("Object swich: " + nodeService.getProperty(
									 harvestDefNodeRef, ReportingModel.PROP_REPORTING_HARVEST_ENABLED));
					}
				}
			} // end while
		} // end if query!=null
		else {
			if (logger.isDebugEnabled())
				logger.debug("executeImpl frequency=null -> it must be a HarvestingDefintion or ReportingRoot");
			NodeRef parentRef = nodeService.getPrimaryParent(harvestDefNodeRef).getParentRef();
			
			if (ReportingModel.TYPE_REPORTING_HARVEST_DEFINITION.equals(nodeService.getType(harvestDefNodeRef))
					&&	isHarvestingEnabledByProperties()
					&& (null!=nodeService.getProperty(
							parentRef, ReportingModel.PROP_REPORTING_HARVEST_ENABLED))
					&&	(Boolean)nodeService.getProperty(
							parentRef, ReportingModel.PROP_REPORTING_HARVEST_ENABLED)
					 ) {
				if (logger.isDebugEnabled()) 
					logger.debug("executeImpl frequency=null -> it is a HarvestingDefintion");
				processHarvestDefinition(harvestDefNodeRef);
			} // end if
			else {
				if (ReportingModel.TYPE_REPORTING_ROOT.equals(nodeService.getType(harvestDefNodeRef))
						&&	isHarvestingEnabledByProperties()
						 ) {
					if (logger.isDebugEnabled())
						logger.debug("executeImpl frequency=null -> it is a ReportingRoot");
					List<ChildAssociationRef> listRef = nodeService.getChildAssocs(harvestDefNodeRef);
					Iterator<ChildAssociationRef>harvestIteration = listRef.iterator();
					while (harvestIteration.hasNext()){
						NodeRef childRef = harvestIteration.next().getChildRef();
						if (ReportingModel.TYPE_REPORTING_HARVEST_DEFINITION.equals(nodeService.getType(childRef))){
							processHarvestDefinition(childRef);		
						}
					}
					
				} // end if
				else{
					if (logger.isDebugEnabled())
						logger.debug("executeImpl frequency=null -> it is nothing... (To)Do all");
					// it is another node type -> make it a generic search-all execution
					// check against frequency
					if (logger.isDebugEnabled()){
						logger.debug("No harvesting run");
						logger.debug("Properties: " + isHarvestingEnabledByProperties());
						logger.debug("Object swich: " + nodeService.getProperty(
									 harvestDefNodeRef, ReportingModel.PROP_REPORTING_HARVEST_ENABLED));
					}
				} //end else
			}
		} // end else
*/			
	}

	
	private void processHarvestDefinition(NodeRef harvestDefNodeRef){
		
		if ((null!=nodeService.getProperty(harvestDefNodeRef, ReportingModel.PROP_REPORTING_TARGET_QUERIES_ENABLED))
				&& (Boolean)nodeService.getProperty(harvestDefNodeRef, ReportingModel.PROP_REPORTING_TARGET_QUERIES_ENABLED)){
			if (logger.isDebugEnabled())
				logger.debug("Kicking off QueryTables");
			
			processTargetQueries(harvestDefNodeRef);
			
		}
		if ((null!=nodeService.getProperty(harvestDefNodeRef, ReportingModel.PROP_REPORTING_USERGROUPS_ENABLED))
				&& (Boolean)nodeService.getProperty(harvestDefNodeRef, ReportingModel.PROP_REPORTING_USERGROUPS_ENABLED)){
			logger.debug("Kicking off UserGroups");
			
			processUsersAndGroups(harvestDefNodeRef);
			
		}
		if ((null!=nodeService.getProperty(harvestDefNodeRef, ReportingModel.PROP_REPORTING_CATEGORIES_ENABLED))
			&&(Boolean)nodeService.getProperty(harvestDefNodeRef, ReportingModel.PROP_REPORTING_CATEGORIES_ENABLED)){
			logger.debug("Kicking off Categories");
			
			processCategories(harvestDefNodeRef);
		}
		if ((null!=nodeService.getProperty(harvestDefNodeRef, ReportingModel.PROP_REPORTING_AUDIT_ENABLED))
				&& (Boolean)nodeService.getProperty(harvestDefNodeRef, ReportingModel.PROP_REPORTING_AUDIT_ENABLED)){
			logger.debug("Kicking off AuditFramework");
			
			processAuditFramework(harvestDefNodeRef);
		}
		/**
		 * Disabled, because release 1.0.0.0 will not ship with Workflow harvesting enabled...
		 **/
		/*
		if ((Boolean)nodeService.getProperty(
				harvestDefNodeRef, 
				ReportingModel.PROP_REPORTING_WORKFLOW_ENABLED)){
			
			processWorkflows(harvestDefNodeRef);			
			processTasks(harvestDefNodeRef);
	
		}
		*/
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		if (paramList!=null){
			paramList.add(
					new ParameterDefinitionImpl(        // Create a new parameter definition to add to the list
						PARAM_FREQUENCY,					// The name used to identify the parameter
						DataTypeDefinition.TEXT,		// The parameter value type
						false,                           // The parameter is mandatory
						getParamDisplayLabel(PARAM_FREQUENCY)));
		}
		
	}

	// ----------------------------------------------------------------------------
	
	
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService= nodeService;
	}
	
	public void setDatabaseHelperBean(DatabaseHelperBean databaseHelperBean) {
		this.dbhb = databaseHelperBean;
	}
	
	public void setReportingHelper(ReportingHelper reportingHelper){
		this.reportingHelper = reportingHelper;
	}
	
	public void setSearchService(SearchService searchService) {
		this.searchService = searchService; 
	}
/*
	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}
	*/
	public void setGlobalProperties(Properties globalProperties) {
		this.globalProperties = globalProperties;
	}	
	// ----------------------------------------------------------------------------
	
	private boolean isHarvestingEnabledByProperties(){
	   	boolean enabled = true;
	   	try{
	    	enabled = this.globalProperties.getProperty(Constants.property_harvesting_enabled, "true").equalsIgnoreCase("true");
		} catch (Exception e) {
			logger.debug("isExecutionEnabled() returning exception. Thus returning true;");
			logger.debug(e.getMessage());
			enabled = true;
		} 
	   	return enabled;
	}
	   
	
	private void processCategories(NodeRef harvestDefNodeRef){
		try{
			CategoryProcessor catProc = new CategoryProcessor(
								dbhb, 
								reportingHelper, 
								serviceRegistry);
			
			catProc.havestNodes(harvestDefNodeRef);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void processAuditFramework(NodeRef harvestDefinition){
		try {
			AuditingExportProcessor auditingProcessor = new AuditingExportProcessor(
							dbhb, 
							reportingHelper, 
							serviceRegistry);

			auditingProcessor.havestNodes(harvestDefinition);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	private void processWorkflows(NodeRef harvestDefinition){
		
		logger.debug("enter processWorkflows");
		try{
			String tableName = Constants.TABLE_WOKFLOW_INSTANCE;
			logger.debug("Starting WorkflowInstance Harvest");
			if (!dbhb.tableIsRunning(tableName)){
				Date theDate = new Date((new Date()).getTime()-Constants.HARVESTING_SAFETY_MARGIN);  // substract 1 second. Otherwise test will fail
				String nowFormattedDate = reportingHelper.getSimpleDateFormat().format(theDate);
				String formattedDate = dbhb.getLastTimestampStatus(tableName);
				
				dbhb.createEmptyTables(tableName);
				ProcessProcessor pp = new ProcessProcessor(
						serviceRegistry, dbhb, reportingHelper);
				pp.havestNodes();
				Properties defs = pp.processQueueDefinition(tableName);
				
				pp.setTableDefinition(tableName, defs);
				
				logger.debug("Process instances=" + defs);
				pp.processQueueValues(tableName);
				pp = null;
				dbhb.setLastTimestampAndStatusDone(tableName, nowFormattedDate);
			} else {
				logger.fatal("Table "+ tableName + " is already running! (or another table)");
			}
 		} catch (SystemException se){
    		se.printStackTrace();
    	} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		logger.debug("exit processWorkflows");
	}
	
	private void processTasks(NodeRef harvestDefinition)  {
		
		logger.debug("enter processTasks");
		try{
			String tableName = Constants.TABLE_WOKFLOW_TASK;
			logger.debug("Starting WorkflowTask Harvest");
			if (!dbhb.tableIsRunning(tableName)){
				Date theDate = new Date((new Date()).getTime()-Constants.HARVESTING_SAFETY_MARGIN);  // substract 1 second. Otherwise test will fail
				String nowFormattedDate = reportingHelper.getSimpleDateFormat().format(theDate);
				
				dbhb.createEmptyTables(tableName);
				WorkflowTaskPropertyProcessor wfp = new WorkflowTaskPropertyProcessor(
						serviceRegistry, dbhb, reportingHelper);
				wfp.havestNodes();
				Properties defs = wfp.processQueueDefinition(tableName);
				
				wfp.setTableDefinition(tableName, defs);
				
				logger.debug("Task definitions=" + defs);
				wfp.processQueueValues(tableName);
				wfp = null;
				dbhb.setLastTimestampAndStatusDone(tableName, nowFormattedDate);
			} else {
				logger.fatal("Table "+ tableName + " is already running! (or another table)");
			}
 		} catch (SystemException se){
    		se.printStackTrace();
    	} catch (Exception e) {
			
			e.printStackTrace();
		}
		
		logger.debug("exit processTasks");
	}
    
	private void processTargetQueries(NodeRef harvestDefinition){
		try {
			NodeRefBasedPropertyProcessor nodeRefProcessor = new NodeRefBasedPropertyProcessor(
							null, 
							dbhb, 
							reportingHelper, 
							serviceRegistry);

			nodeRefProcessor.havestNodes(harvestDefinition);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void processUsersAndGroups(NodeRef harvestDefinition){
		try {
			String tableName = dbhb.fixTableColumnName(Constants.TABLE_PERSON);
			if (!dbhb.tableIsRunning(tableName)){
				Date theDate = new Date((new Date()).getTime()-Constants.HARVESTING_SAFETY_MARGIN);  // substract 1 second. Otherwise test will fail
				String nowFormattedDate = reportingHelper.getSimpleDateFormat().format(theDate);
				dbhb.setLastTimestampStatusRunning(tableName);
				
				dbhb.dropTables(tableName);
				dbhb.createEmptyTables(tableName);
				
				PersonProcessor personProcessor = new PersonProcessor(
						serviceRegistry,
						reportingHelper,
						dbhb);
				//TODO Split in harvest and processQueue
				personProcessor.processPersons(tableName); 
				dbhb.setLastTimestampAndStatusDone(tableName, nowFormattedDate);
			}else {
				logger.fatal("Table "+ tableName + " is already running! (or another table)");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			String tableName = dbhb.fixTableColumnName(Constants.TABLE_GROUPS);
			if (!dbhb.tableIsRunning(tableName)){
				String nowFormattedDate = reportingHelper.getSimpleDateFormat().format(new Date());
				dbhb.setLastTimestampStatusRunning(tableName);
				
				dbhb.dropTables(tableName);
				dbhb.createEmptyTables(tableName);
				
				GroupProcessor groupProcessor = new GroupProcessor(
						serviceRegistry,
						reportingHelper,
						dbhb);
				//TODO Split in harvest and processQueue 
				groupProcessor.processGroups(tableName);
				dbhb.setLastTimestampAndStatusDone(tableName, nowFormattedDate);
			} else {
				logger.fatal("Table "+ tableName + " is already running! (or another table)");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			String tableName = dbhb.fixTableColumnName(Constants.TABLE_SITEPERSON);
			if (!dbhb.tableIsRunning(tableName)){
				String nowFormattedDate = reportingHelper.getSimpleDateFormat().format(new Date());
				dbhb.setLastTimestampStatusRunning(tableName);
				
				dbhb.dropTables(tableName);
				dbhb.createEmptyTables(tableName);
				
				SitePersonProcessor sitePersonProcessor = new SitePersonProcessor(
						serviceRegistry,
						reportingHelper,
						dbhb);
				//TODO Split in harvest and processQueue 
				sitePersonProcessor.processSitePerson(tableName);
				dbhb.setLastTimestampAndStatusDone(tableName, nowFormattedDate);
			} else {
				logger.fatal("Table "+ tableName + " is already running! (or another table)");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	private String getHarvestingDefinitionQuery(String frequency){
		if (logger.isDebugEnabled()) 
			logger.debug("Enter getHarvestingDefinitionQuery frequency="+frequency);
		String query = "TYPE:\"reporting:harvestDefinition\"";
		/*
		 *   moreFrequent
		 *   lessFrequent
		 *   all	--> process all
		 *   null 	--> process curent NodeRef
		 */
		if ( (frequency==null) || 
			 ("".equals(frequency.trim()))){
			// do nothing, execute current NodeRef
			query=null;
		} else if ("all".equalsIgnoreCase(frequency)){
			// do nothing, the query will find all
		} else {
			query += " AND @reporting\\:harvestFrequency:\"" + frequency +"\"";
		}
		if (logger.isDebugEnabled()) 
			logger.debug("Exit getHarvestingDefinitionQuery query=" + query);
		return query;
	}
	
}