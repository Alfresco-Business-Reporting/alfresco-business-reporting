package org.alfresco.reporting.action.executer;

import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.reporting.Constants;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Action to execute ReportRootExecuter with all frequencies.
 * 	[hourly, daily, weekly, monthly]
 * 
 * @author Martijn van de Brug
 *
 */
public class AllFrequenciesRootExecuter extends ActionExecuterAbstractBase {
	
	public static final String NAME = "all-frequencies-report-root-executer";
	private static Log logger = LogFactory.getLog(AllFrequenciesRootExecuter.class);
	
	private ActionService actionService;

	@Override
	protected void executeImpl(Action action, NodeRef reportRootRef) {
		Action reportRootAction = actionService.createAction(ReportRootExecuter.NAME);
		
		if (logger.isDebugEnabled())
			logger.debug("Executing reporting root with frequency Daily");
		
		//Daily
		reportRootAction.setParameterValue(
				ReportRootExecuter.PARAM_FREQUENCY, Constants.FREQUENCY_DAYLY);
		actionService.executeAction(reportRootAction, reportRootRef);
		
		if (logger.isDebugEnabled())
			logger.debug("Executing reporting root with frequency Hourly");
		
		//Hourly
		reportRootAction.setParameterValue(
				ReportRootExecuter.PARAM_FREQUENCY, Constants.FREQUENCY_HOURLY);
		actionService.executeAction(reportRootAction, reportRootRef);
		
		if (logger.isDebugEnabled())
			logger.debug("Executing reporting root with frequency Weekly");
		
		//Weekly
		reportRootAction.setParameterValue(
				ReportRootExecuter.PARAM_FREQUENCY, Constants.FREQUENCY_WEEKLY);
		actionService.executeAction(reportRootAction, reportRootRef);
		
		if (logger.isDebugEnabled())
			logger.debug("Executing reporting root with frequency Monthly");
		
		//Monthly
		reportRootAction.setParameterValue(
				ReportRootExecuter.PARAM_FREQUENCY, Constants.FREQUENCY_MONTHLY);
		actionService.executeAction(reportRootAction, reportRootRef);
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> arg0) {
		// TODO Auto-generated method stub
	}
	
	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

}
