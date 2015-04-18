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

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.reporting.Constants;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.execution.ReportingContainer;
import org.alfresco.reporting.execution.ReportingRoot;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * ActionRootExecuter gets a frequency label (hourly, daily, weekly, monthly)
 * It searches for TYPE_REPORTING_REPORTING_CONTAINER's that are enabled
 * For each result, get all children. 
 *   Execute a report against each of the children (ReportExecuter)
 *     Store the report in the location defined in the parent PROP_REPORTING_TARGET.
 *       Substitute ${site} into each Site
 *     Parameterize if parameters are defined.
 *      
 * Works only in the structure
 * 
 * A:reporting:reporting
 *   T:reporting:reportingContainer
 *     T:reporting:report
 *     
 * @author tpeelen
 *
 */
public class ReportRootExecutor extends ActionExecuterAbstractBase {

	public static final String PARAM_FREQUENCY = "executionFrequency";
	public static final String NAME = "report-root-executer";
		
	private ActionService actionService;
	private NodeService nodeService;
	private SearchService searchService;
	private ReportingHelper reportingHelper;
	private int startDelayMinutes=0;
	
	private static Log logger = LogFactory.getLog(ReportRootExecutor.class);
	
	/**
	 * This is where the action is.
	 */
	@Override
	protected void executeImpl(Action action, NodeRef someRef) {
		
		if (logger.isDebugEnabled())
			logger.debug("enter executeImpl");
		
		
		//ReportingRoot reportingRoot = new ReportingRoot(someRef);
		//reportingHelper.initializeReportingRoot(reportingRoot);
		
		// get the frequency value (hourly, daily, weekly, monthly) 
		String executionFrequency  = (String)action.getParameterValue(PARAM_FREQUENCY);
		
		//if (executionFrequency==null) executionFrequency = "hourly";
		
		// build a query to find all reportingContainer folders 
		// having the executionFrequency, AND executionEnabled
		String query = "+@reporting\\:executionFrequency:\"" + executionFrequency + "\" " +
					   "+@reporting\\:executionEnabled:true";
		if (logger.isDebugEnabled())
			logger.debug("executeImpl query=" + query);
		
		// build the query and execute. Find all relevant reportingContainers
		
		ResultSet results = searchService.query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, Constants.QUERYLANGUAGE, query);
		
		// cycle the resultset
		for (ResultSetRow resultRow:results){
			NodeRef containerRef = resultRow.getChildAssocRef().getChildRef();
			
			ReportingContainer reportingContainer = new ReportingContainer(containerRef);
			reportingHelper.initializeReportingContainer(reportingContainer);
			
			if (logger.isDebugEnabled())
				logger.debug("Found container: " + reportingContainer.getName()); 
					
			
			// get its related ReportingRoot
			ReportingRoot reportingRoot = 
					new ReportingRoot(
							reportingHelper.getReportingRoot(containerRef));
			reportingHelper.initializeReportingRoot(reportingRoot);
			
			// determine if execution of this container is allowed considering 
			// the parent ReportingRoot
			if (reportingRoot.isGlobalExecutionEnabled()){
			
				// Start a reportingContainerExecuter for the given noderef. 
				// Pass the NodeRef of the ReportingRoot since we know who that is.  
				Action customAction = actionService.createAction(
						ReportContainerExecutor.NAME);
				//action.setParameterValue(
				//		ReportContainerExecutor.REPORTING_CONTAINER_NODEREF, containerRef);
				
				actionService.executeAction(customAction, containerRef);
			} // end if reportingExecturinEnabled()
			else
			{
				logger.warn("Container execution of " 
							+ reportingContainer.getName() 
							+ " veto'd by ReportingRoot " 
							+ reportingRoot.getName());
			}
		} // end for ResultSetRow		
				
		if (logger.isDebugEnabled())
			logger.debug("exit executeImpl");
	}

	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {

		paramList.add(
				new ParameterDefinitionImpl(        // Create a new parameter definition to add to the list
						PARAM_FREQUENCY,            // The name used to identify the parameter
					DataTypeDefinition.TEXT,        // The parameter value type
					false,                           // The parameter is mandatory
					getParamDisplayLabel(PARAM_FREQUENCY)));	

	}
	

	
	public void setNodeService(NodeService nodeService)	{
	    this.nodeService = nodeService;
	}
	
	public void setSearchService(SearchService searchService)	{
	    this.searchService = searchService;
	}
	
	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}
	
	public void setReportingHelper(ReportingHelper reportingHelper) {
		this.reportingHelper = reportingHelper;
	}
	
	public void setStartDelayMinutes(String minutes){
		this.startDelayMinutes=Integer.parseInt(minutes);
	}
}
