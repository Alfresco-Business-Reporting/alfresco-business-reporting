package org.alfresco.reporting.processor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alfresco.model.ContentModel;
import org.alfresco.reporting.Constants;
import org.alfresco.reporting.ReportLine;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowInstance;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProcessProcessor extends PropertyProcessor {

	private String START_DATE_TYPE = "datetime";
	private String END_DATE_TYPE = "datetime";
	private String DUE_DATE_TYPE = "datetime";
	private String PROCESS_DEFINITION_TYPE = "noderef";
	private String DESCRIPTION_TYPE = "text";
	private String PRIORITY_TYPE = "long";
	private String IS_ACTIVE_TYPE = "boolean";
	
	private String START_DATE_KEY = "start_date";
	private String END_DATE_KEY = "end_date";
	private String DUE_DATE_KEY = "due_date";
	private String PROCESS_DEFINITION_KEY = "process_definition";
	private String DESCRITION_KEY = "description";
	private String PRIORITY_KEY = "priority";
	private String IS_ACTIVE_KEY = "is_active";
	
	private WorkflowService workflowService;
	private static Log logger = LogFactory.getLog(ProcessProcessor.class);

	public ProcessProcessor(
			ServiceRegistry serviceRegistry,
			DatabaseHelperBean dbhb,
			ReportingHelper reportingHelper) throws Exception{

		this.workflowService 	= serviceRegistry.getWorkflowService(); 
		
		setNodeService(serviceRegistry.getNodeService());
		setDictionaryService(serviceRegistry.getDictionaryService());
		setFileFolderService(serviceRegistry.getFileFolderService());
		//this.historyService = serviceRegistry.getHistoryServ
		
		setReportingHelper(reportingHelper);
		setDbhb(dbhb);
		
		setClassToColumnType(reportingHelper.getClassToColumnType());
		setReplacementDataTypes( reportingHelper.getReplacementDataType() );
		setGlobalProperties( reportingHelper.getGlobalProperties());
		setNamespaces( reportingHelper.getNameSpaces() );
		setBlacklist(reportingHelper.getBlacklist());
		
	}
	
	/**
	 * 
	 * @param s
	 * @param dtype
	 * @param multiValued
	 * @return
	 */
	private String getPropertyValue(Serializable s,
			final String dtype,
			final boolean multiValued){
		// not needed
		return ""; 
	}


	
	private ReportLine processToReportingLine(ReportLine rl, String key, String type, String value){
		if (!blacklist.contains(","+key+",") && !type.equals("-")){
			rl.setLine(	key, 
						getClassToColumnType().getProperty(type, "-"), 
						value, 
						getReplacementDataType());
			logger.debug("processNodeToMap: Found " 
						+ key + "=" 
						+ value + " (" 
						+ getClassToColumnType().getProperty(type, "-") 
						+ ")");
		}
		return rl;
	}
	
	
	/**
	 * Process all relevant node properties and assocs. Get all propertes in a ReportLine
	 * object
	 * 
	 */
	@Override
	protected ReportLine processNodeToMap(String identifier, String table, ReportLine rl) {
		table = dbhb.fixTableColumnName(table);
		try{
			String key;
			String type;
			String value;
			
			logger.debug("Enter processNodeToMap");
			rl.setLine("sys_node_uuid", 
						"VARCHAR(50)", 
						identifier, 
						getReplacementDataType());
			
			WorkflowInstance wfi = workflowService.getWorkflowById(identifier);
			
			
			String processDefinition = wfi.getDefinition().getId(); //String
			String description = wfi.getDescription(); //String
			Date dueDate = wfi.getDueDate(); // date
			Date endDate = wfi.getEndDate(); // date
			//NodeRef identifier wfi.getInitiator()identifier; // noderef
			int  priority = wfi.getPriority(); // String
			Date startDate = wfi.getStartDate(); // date

			rl = processToReportingLine(rl,
					START_DATE_KEY,
					START_DATE_TYPE,
					getSimpleDateFormat().format(startDate));
					
			rl = processToReportingLine(rl,
					END_DATE_KEY,
					END_DATE_TYPE,
					getSimpleDateFormat().format(endDate));

			rl = processToReportingLine(rl,
					DUE_DATE_KEY,
					DUE_DATE_TYPE,
					getSimpleDateFormat().format(dueDate));

			rl = processToReportingLine(rl,
					PROCESS_DEFINITION_KEY,
					PROCESS_DEFINITION_TYPE,
					getSimpleDateFormat().format(processDefinition));

			rl = processToReportingLine(rl,
					DESCRITION_KEY,
					DESCRIPTION_TYPE,
					getSimpleDateFormat().format(description));

			rl = processToReportingLine(rl,
					PRIORITY_KEY,
					PRIORITY_TYPE,
					String.valueOf(priority));
			
			rl = processToReportingLine(rl,
					IS_ACTIVE_KEY,
					IS_ACTIVE_TYPE,
					String.valueOf(priority));
			/*
			 * nice idea, just not really doable
			rl = processToReportingLine(rl,
					PROC_DEF_KEY,
					PROC_DEF_DBTYPE,
					String.valueOf(priority));
			*/
			
		} catch (Exception e){
			e.printStackTrace();
		}
		return rl;
		
	}
	

	private void storeProcessProperties(ReportLine rl){
		
		@SuppressWarnings("unused")
		int numberOfRows;
		logger.debug("Current method=" + this.method);
		try{ //SINGLE_INSTANCE, 
			//logger.debug(method + " ##### " + rl.size());
			if ( (rl.size()>0) /* && (rl.getValue("sys_node_uuid").length()>5)*/){
				//logger.debug("method="+method+" && row exists?");
				
				if (this.method.equals(Constants.INSERT_ONLY) ){
					//if (logger.isDebugEnabled()) logger.debug("Going INSERT_ONLY");
					
					numberOfRows = dbhb.insertIntoTable(rl);
					//logger.debug(numberOfRows+ " rows inserted");
				}
				
				// -------------------------------------------------------------
				
				if (this.method.equals(Constants.SINGLE_INSTANCE) ) {
					//if (logger.isDebugEnabled()) logger.debug("Going SINGLE_INSTANCE");
					
					if (dbhb.rowExists(rl)){
						numberOfRows = dbhb.updateIntoTable(rl);
						//logger.debug(numberOfRows+ " rows updated");
					} else {
						numberOfRows = dbhb.insertIntoTable(rl);
						//logger.debug(numberOfRows+ " rows inserted");
						
					}
					
				}
				
				// -------------------------------------------------------------
				
				if (this.method.equals(Constants.UPDATE_VERSIONED)) {
					if (logger.isDebugEnabled()) logger.debug("Going UPDATE_VERSIONED");
					if (dbhb.rowExists(rl)){
    					numberOfRows = dbhb.updateVersionedIntoTable(rl);
    					//logger.debug(numberOfRows+ " rows updated");
					} else {
						numberOfRows = dbhb.insertIntoTable(rl);
						//logger.debug(numberOfRows+ " rows inserted");
						
					}
				}
			} // end if rl.size>0

		} catch (Exception e){
			logger.fatal(e);
			e.printStackTrace();
		} finally {
			rl.reset();
		}
	}

	
	@Override
	public Properties processQueueDefinition(String taskTable){
		Properties definitions = new Properties();
		
		definitions.setProperty(START_DATE_KEY, 
				getClassToColumnType().getProperty(START_DATE_TYPE,"-")); 
		definitions.setProperty(END_DATE_KEY, 
				getClassToColumnType().getProperty(END_DATE_TYPE,"-"));
		definitions.setProperty(DUE_DATE_KEY, 
				getClassToColumnType().getProperty(DUE_DATE_TYPE,"-"));
		definitions.setProperty(DESCRITION_KEY, 
			getClassToColumnType().getProperty(DESCRIPTION_TYPE,"-"));
		definitions.setProperty(PRIORITY_KEY, 
				getClassToColumnType().getProperty(PRIORITY_TYPE,"-"));
		definitions.setProperty(PROCESS_DEFINITION_KEY, 
				getClassToColumnType().getProperty(PROCESS_DEFINITION_TYPE,"-"));
		//definitions.setProperty("sys_store_protocol", 
		//		getClassToColumnType().getProperty("sys_store_protocol","-"));
		
		return definitions;
	}

	
	
	@Override
	public void processQueueValues(String table) throws Exception{
		table = dbhb.fixTableColumnName(table);
		logger.debug("processQueueValues: pocessing " + getQueue().size() + " entries");
		ReportLine rl = new ReportLine( table, 
										getSimpleDateFormat(), 
										reportingHelper);
		
		Iterator<Object> queueIterator = getQueue().iterator();
		while (queueIterator.hasNext()){
			String taskId = (String)queueIterator.next();
			if (logger.isDebugEnabled())
				logger.debug("processQueueValues: pocessing " + taskId );
			rl = processNodeToMap(taskId, table, rl);
			storeProcessProperties(rl);
		}
	}
	
	/**
	 * Harvests 
	 *   1) running tasks 
	 *   2) closed tasks after date x
	 */
	public void havestNodes(){
		//check db table act_hi_taskinst
		
		// Make sure we have a connection
		dbhb.openReportingConnection();
		
		if ( allowProcessHarvesting() ){
			
			if (logger.isDebugEnabled())
				logger.debug("Harvesting Activiti workflowTasks");	
			
			// start temp solution for test
//kan weg			Calendar cal = Calendar.getInstance();
			
			//TODO make this flex!!
//kan weg			cal.set(2013, 03, 26, 11, 34, 40);
//kan weg			Date theDate = cal.getTime(); // get back to a Date object

			String nowFormattedDate = reportingHelper.getSimpleDateFormat().format(new Date());
			String formattedDate = dbhb.getLastTimestamp(Constants.TABLE_WOKFLOW_INSTANCE);
			
			int maxItems = getMaxLoopSize();
			long maxLoopCount = getMaxLoopCount();
			long loopCount=0;
			int resultSize=999999;
			boolean canContinueCompleted = false;
			
			List<String> myProcessList;
			Iterator<String> myProcessListIterator;
			
			// end temp solution for test
			while (resultSize>0) {
				loopCount++;
				
				if (getBatchTimestampEnabled()) { // default = true
					nowFormattedDate = reportingHelper.getSimpleDateFormat().format(new Date());
				}
				// - Remind that this method will return anything. You are probably
				//   limited by heap-size or Inceger.MAX-VALUE, whatever comes first
				//   Replace by providing some kind of limit, and cycle until no more results
				//   equal to noderef search
				// - Remind that a DB query is executed instead of the API call, in order 
				//   to get the delta-only, not -all- workflow instances.
				myProcessList = dbhb.getCreatedProcesses(formattedDate, maxItems);
				if (logger.isDebugEnabled())
					logger.debug("Found " + myProcessList.size() + " started workflow instancess...");	
				
				myProcessListIterator = myProcessList.iterator();
				while (myProcessListIterator.hasNext()){
					addToQueue("activiti$" + myProcessListIterator.next());
				}
				
				if (resultSize==0)
					canContinueCompleted = true;
			}
			
			resultSize=999999;
			loopCount=0;
			
			while ((resultSize>0) && canContinueCompleted && loopCount<maxLoopCount) {
				loopCount++;
				// - Remind that this method will return anything. You are probably
				//   limited by heap-size or Inceger.MAX-VALUE, whatever comes first
				//   Replace by providing some kind of limit, and cycle until no more results
				//   equal to noderef search
				// - Remind that a DB query is executed instead of the API call, in order 
				//   to get the delta-only, not -all- workflow instances.
				myProcessList = dbhb.getCompletedProcesses(formattedDate, maxItems);
				if (logger.isDebugEnabled())
					logger.debug("Found " + myProcessList.size() + " deleted workflow instancess...");	
				
				myProcessListIterator = myProcessList.iterator();
				while (myProcessListIterator.hasNext()){
					addToQueue("activiti$" + myProcessListIterator.next());
				}
				if (logger.isDebugEnabled())
					logger.debug("Found total of " + getQueue().size() + " workflow instancess...");
			}
		}
		
		// make sure we gently close the connection
		dbhb.closeReportingConnection();
		
		
	}


	@Override
	void havestNodes(NodeRef harvestDefinition) {
		// TODO Auto-generated method stub

	}

}
