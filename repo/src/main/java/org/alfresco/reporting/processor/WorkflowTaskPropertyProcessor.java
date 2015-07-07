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
import org.alfresco.repo.workflow.WorkflowModel;
import org.alfresco.reporting.Constants;
import org.alfresco.reporting.ReportLine;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.cmr.workflow.WorkflowTask;
import org.alfresco.service.namespace.QName;
//import org.activiti.engine.HistoryService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WorkflowTaskPropertyProcessor extends PropertyProcessor {

	// see http://www.appnovation.com/auditing_workflows
	protected WorkflowService workflowService;
	private static Log logger = LogFactory.getLog(WorkflowTaskPropertyProcessor.class);
	
	
	// Convention: DB_COLUMN_TYPES do not need to be localized to the vendr specific 
	// syntax. Date/DataTime.Boolean MUST be made specific for DB vendors!!
	private final String DELETE_REASON_DB_COLUMN_TYPE = "VARCHAR(50)";
	private final String PROCESS_ID_DB_COLUMN_TYPE = "VARCHAR(20)";
	private final String TASK_DEF_KEY_DB_COLUMN_TYPE = "VARCHAR(100)";
	private final String FILES_ATTACHED_COLUMN_TYPE = "noderefs"; 
	private final String DURATION_COLUMN_TYPE = "int";
	private final String NUMBER_OF_FILES_COLUMN_TYPE = "int";
	private final String MODIFIED_COLUMN_TYPE = "datetime";
	// table name-record for running tasks
	private final String TABLE_NAME_RUNNING   = Constants.TABLE_WOKFLOW_TASK+"_r"; 
	// table name-record for completed tasks
	private final String TABLE_NAME_COMPLETED = Constants.TABLE_WOKFLOW_TASK+"_c"; 
				
	
	public WorkflowTaskPropertyProcessor(
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
		logger.debug("Enter getPropertyValue");
		String returnValue = "";
		if (multiValued && !"category".equals(dtype)){
			@SuppressWarnings("unchecked")
			ArrayList<Object> values = (ArrayList<Object>)s;
			
			if ((values!=null) && (!values.isEmpty()) && (values.size()>0)){
			
				if (dtype.equals("date") || dtype.equals("datetime")){
					
					for (int v=0;v<values.size();v++){
						returnValue += getSimpleDateFormat().format((Date)values.get(v)) + Constants.MULTIVALUE_SEPERATOR;		
					}
				}
				
				if (dtype.equals("id") || dtype.equals("long")){
					for (int v=0;v<values.size();v++){
						returnValue += Long.toString((Long)values.get(v)) + Constants.MULTIVALUE_SEPERATOR;		
					}
				}
				
				if (dtype.equals("int")){
					for (int v=0;v<values.size();v++){
						returnValue += Integer.toString((Integer)values.get(v)) + Constants.MULTIVALUE_SEPERATOR;		
					}
				}
				
				if (dtype.equals("float") || dtype.equals("double") ){
					for (int v=0;v<values.size();v++){
						returnValue += Double.toString((Double)values.get(v)) + Constants.MULTIVALUE_SEPERATOR;		
					}
					}
				
				if (dtype.equals("boolean")){
					for (int v=0;v<values.size();v++){
						returnValue += Boolean.toString((Boolean)values.get(v)) + Constants.MULTIVALUE_SEPERATOR;		
					}
				}
				
				if (dtype.equals("text")){
					for (int v=0;v<values.size();v++){
						returnValue += (String)values.get(v) + Constants.MULTIVALUE_SEPERATOR;		
					}
				}
				
				if (dtype.equals("noderef")){
					for (int v=0;v<values.size();v++){
						returnValue += values.get(v).toString() + Constants.MULTIVALUE_SEPERATOR;		
					}
				}
				
				if (returnValue.equals("")){
					for (int v=0;v<values.size();v++){
						returnValue += (String)values.get(v) + Constants.MULTIVALUE_SEPERATOR;		
					}
				}
			} 
			// end multivalue
		} else {
			if ((s!=null) && !"category".equals(dtype)){
			
				if (dtype.equals("date") || dtype.equals("datetime")){

					Calendar c = Calendar.getInstance();
					c.setTimeInMillis( ((Date)s).getTime() );
					returnValue = getSimpleDateFormat().format((Date)s);
					//returnValue = c.YEAR + "/"+ prefix(c.MONTH+1, 2, "0") + "/"+ prefix(c.DAY_OF_MONTH, 2, "0") + "T" + prefix(c.HOUR_OF_DAY, 2, "0")+":"+prefix(c.MINUTE, 2, "0")+":"+prefix(c.SECOND, 2, "0"); 
				}
				
				if (dtype.equals("id") || dtype.equals("long")){
					returnValue = Long.toString((Long)s);
				}
				
				if (dtype.equals("int")){
					returnValue = Integer.toString((Integer)s);
				}
				
				if (dtype.equals("float") || dtype.equals("double") ){
					returnValue = Double.toString((Double)s);
				}
				
				if (dtype.equals("boolean")){
					returnValue = Boolean.toString((Boolean)s);
				}
				
				if (dtype.equals("text")){
					returnValue = s.toString();
				}
				
				if (dtype.equals("noderef")){
					returnValue = s.toString();
				}
				
				if (returnValue.equals("")){
					returnValue = s.toString();
				}
			}
		} // end single valued
		/*
		if (qname.toString().endsWith("taggable")) {
		logger.error("I am a taggable!");
		List<String> tags = serviceRegistry.getTaggingService().getTags(nodeRef);
		logger.error("Found " + tags.size() + " tags!");
		for (String tag : tags){
		logger.error("processing tag: " + tag);
		if (returnValue.length()>0) returnValue+=",";
		returnValue+=tag;
		}
		} // end taggable
		*/

		// Process categories
		if (dtype.equals("category")){
			logger.debug("Found a category!");
			@SuppressWarnings("unchecked")
			List<NodeRef> categories = (List<NodeRef>) s;
			if (categories != null){
			
				for (NodeRef cat : categories){
					String catName = getNodeService().getProperty(
						cat, ContentModel.PROP_NAME).toString(); 
					
					if (returnValue.length()>0) returnValue+=",";
						returnValue+= catName;
				} // end for
			} // end if categories != null
		} // end category
		
		logger.debug("Exit getPropertyValue, returning: " + returnValue);
		return returnValue; 
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
			logger.debug("Enter processNodeToMap");
			rl.setLine("sys_node_uuid", 
						"VARCHAR(50)", 
						identifier, 
						getReplacementDataType());
			WorkflowTask wft = workflowService.getTaskById(identifier);
			Map<QName, Serializable> map = wft.getProperties();
	
			Iterator<QName> keys = map.keySet().iterator();
			while (keys.hasNext()){
				String key = "";
				String dtype = "";
				try{
					QName qname = keys.next();
					key = qname.toString();
	//			logger.debug("processPropertyValues: voor: KEY="+key);
					key = replaceNameSpaces(key);
					//logger.debug("processPropertyValues: na: KEY="+key);
					
					dtype = dictionaryService.getProperty(qname)
							.getDataType().toString();
					
					//logger.debug("processPropertyValues: voor: DTYPE="+dtype);
					
					dtype = dtype.substring(dtype.indexOf("}")+1, dtype.length()).trim();
					
					//logger.debug("processPropertyValues: na: DTYPE="+dtype);
					
					Object theObject = getClassToColumnType().getProperty(dtype,"-"); 
					String type = theObject.toString();
					//logger.debug("processPropertyValues: na: TYPE="+type);
					
					boolean multiValued = false;
					multiValued = dictionaryService.getProperty(qname).isMultiValued();
					
					logger.debug("processNodeToMap: EVAL: key="+key + ", type="+type+", dtype="+dtype + " multi="+multiValued);
					//logger.debug("processPropertyValues: blacklist="+ blacklist);
				
					if (!blacklist.contains(","+key+",") && !type.equals("-")){
						String value = getPropertyValue(map.get(qname), dtype, multiValued);
						rl.setLine(	key, 
									type, 
									value, 
									getReplacementDataType());
						logger.debug("processNodeToMap: Found " + key + "=" + value);
					}
				
				} catch (Exception e){
					//logger.info("processPropertyValues: " + e.toString());
					logger.info("processNodeToMap: Error in object, property "+key+" not found! (" + dtype +")");
				}

				
				
			} // end while loop through this object's properties
			// add noderefs from the bpm_package
			List<NodeRef> bpm_package = workflowService.getPackageContents(identifier);
			rl.setLine("number_of_files_attached", 
					getClassToColumnType().getProperty(NUMBER_OF_FILES_COLUMN_TYPE), 
						String.valueOf(bpm_package.size()), 
						getReplacementDataType());
			Iterator<NodeRef> bpmi = bpm_package.iterator();
			String bpmString = "";
			while (bpmi.hasNext()){
				NodeRef node = bpmi.next();
				if (bpmString.length()>0)
					bpmString+=",";
				bpmString += node.toString();
			}
			
			rl.setLine("files_attached", 
					getClassToColumnType().getProperty(FILES_ATTACHED_COLUMN_TYPE), 
					bpmString, 
					getReplacementDataType());
			
			//selectMany("get-additional-task-properties", String taskId);
			String taskId = "";
			if (identifier.contains("$")){
				taskId = identifier.substring(identifier.indexOf("$")+1, identifier.length());
			}
			logger.debug("Just before, trying with id=" + taskId);
			@SuppressWarnings("unchecked")
			HashMap<String,Serializable> results = dbhb.getPropertiesForWorkflowTask(taskId);
			
			String proc_ref_id	= (String)results.get("proc_ref_id_");
			if (proc_ref_id!=null){
				rl.setLine("process_id", 
						PROCESS_ID_DB_COLUMN_TYPE, 
						proc_ref_id, 
						getReplacementDataType());
			}

			String task_def_key	= (String)results.get("task_def_key_");
			if (task_def_key!=null){
				rl.setLine("task_def_key", 
						TASK_DEF_KEY_DB_COLUMN_TYPE, 
						task_def_key, 
						getReplacementDataType());
			}

			String delete_reason= (String)results.get("delete_reason_");
			if (delete_reason!=null){
				rl.setLine("delete_reason", 
						DELETE_REASON_DB_COLUMN_TYPE, 
						delete_reason, 
						getReplacementDataType());
			}

			String duration	 = "";
			if (null!=results.get("duration_")){
				duration = String.valueOf(results.get("duration_"));
				rl.setLine("duration", 
						getClassToColumnType().getProperty(DURATION_COLUMN_TYPE), 
						duration, 
						getReplacementDataType());
			}
			
			String now = getSimpleDateFormat().format(new Date());
	    	now = now.replaceAll(" ", "T").trim();
			rl.setLine("cm_modified", 
					getClassToColumnType().getProperty(MODIFIED_COLUMN_TYPE), 
					now, 
					getReplacementDataType());
		} catch (Exception e){
			e.printStackTrace();
		}
		return rl;
		
	}
	

	private void storeTaskProperties(ReportLine rl){
		
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

	protected Properties processTaskNodeDefinition(String identifier, Properties definition) {
		try{
			WorkflowTask wft = workflowService.getTaskById(identifier);
			Map<QName, Serializable> map = wft.getProperties();
	
			Iterator<QName> keys = map.keySet().iterator();
			while (keys.hasNext()){
				String key = "";
				String dtype = "";
				try{
					QName qname = keys.next();
					key = qname.toString();
	//			logger.debug("processPropertyValues: voor: KEY="+key);
					key = replaceNameSpaces(key);
					//logger.debug("processPropertyValues: na: KEY="+key);
					
					dtype = dictionaryService.getProperty(qname)
							.getDataType().toString();
					
					//logger.debug("processPropertyValues: voor: DTYPE="+dtype);
					
					dtype = dtype.substring(dtype.indexOf("}")+1, dtype.length()).trim();
					
					//logger.debug("processPropertyValues: na: DTYPE="+dtype);
					
					Object theObject = getClassToColumnType().getProperty(dtype,"-"); 
					String type = theObject.toString();
					//logger.debug("processPropertyValues: na: TYPE="+type);
					
					boolean multiValued = false;
					multiValued = dictionaryService.getProperty(qname).isMultiValued();
					//definition.setProperty("sys_store_protocol", "noderef");
					
					if (logger.isDebugEnabled())
						logger.debug("processTaskNodeDefinition: EVAL: key="+key + ", type="+type+", dtype="+dtype + " multi="+multiValued);
					//logger.debug("processPropertyValues: blacklist="+ blacklist);
				
					if (!blacklist.contains(","+key+",") && !type.equals("-")){
						definition.setProperty(key, type);
						if (logger.isDebugEnabled())
							logger.debug("processTaskNodeDefinition: Found " + key + "=" + type);
					}
				
				} catch (Exception e){
					//logger.info("processPropertyValues: " + e.toString());
					logger.error("processTaskNodeDefinition: Error in object, property "+key+" not found! (" + dtype +")");
				}
			} // end while loop through this object's properties
		} catch (Exception e){
			e.printStackTrace();
		}
		return definition;
	}
	
	@Override
	public Properties processQueueDefinition(String taskTable){
		taskTable = dbhb.fixTableColumnName(taskTable);
		Properties definitions = new Properties();
		if (logger.isDebugEnabled())
			logger.debug("processQueueDefinition: pocessing " + getQueue().size() + " entries");
		
		Iterator<Object> queueIterator = getQueue().iterator();
		while (queueIterator.hasNext()){
			String taskId = (String)queueIterator.next();
			if (logger.isDebugEnabled())
				logger.debug("processQueueDefinition: pocessing " + taskId );
			definitions = processTaskNodeDefinition(taskId, definitions);
		}
		if (definitions==null){
			definitions = new Properties();
		}
		definitions.setProperty("number_of_files_attached", 
				getClassToColumnType().getProperty(NUMBER_OF_FILES_COLUMN_TYPE));
		definitions.setProperty("files_attached", 
				getClassToColumnType().getProperty(FILES_ATTACHED_COLUMN_TYPE));
		definitions.setProperty("process_id", 
				PROCESS_ID_DB_COLUMN_TYPE);
		definitions.setProperty("task_def_key", 
				TASK_DEF_KEY_DB_COLUMN_TYPE);
		definitions.setProperty("delete_reason", 
				DELETE_REASON_DB_COLUMN_TYPE);
		definitions.setProperty("duration", 
				getClassToColumnType().getProperty(DURATION_COLUMN_TYPE));
		definitions.setProperty("cm_modified", 
				getClassToColumnType().getProperty(MODIFIED_COLUMN_TYPE));
		
		return definitions;
	}
	
	
	@Override
	public void processQueueValues(String table) throws Exception{
		table = dbhb.fixTableColumnName(table);
		logger.debug("processQueueValues: pocessing " + getQueue().size() + " entries");
		ReportLine rl = new ReportLine(table, getSimpleDateFormat(), reportingHelper);
		
		Iterator<Object> queueIterator = getQueue().iterator();
		while (queueIterator.hasNext()){
			String taskId = (String)queueIterator.next();
			if (logger.isDebugEnabled())
				logger.debug("processQueueValues: pocessing " + taskId );
			rl = processNodeToMap(taskId, table, rl);
			storeTaskProperties(rl);
		}
	}
	
	private Date getTimestampTask(String whatDate, String taskId){
		Date returnDate=null;
		try{
			Map<String, Serializable> map = dbhb.getPropertiesForWorkflownstance(taskId);
			
			if ("start_time".equalsIgnoreCase(whatDate)){
				returnDate = (Date)map.get("start_time_");
			}
			if ("end_date".equalsIgnoreCase(whatDate)){
				returnDate = (Date)map.get("end_time_");
			}
		} catch (Exception e){
			logger.error("getTimestampTask: Error getting timestamp " + whatDate);
			logger.error("getTimestampTask:" + e.getMessage());
		}
		logger.debug("getTimestampTask: " + whatDate + " returning" + returnDate);
		return returnDate;
	}
	
	/**
	 * Harvests 
	 *   1) running tasks 
	 *   2) closed tasks after date x
	 */
	public void havestNodes(){
		//check db table act_hi_taskinst
		//
		// running instances (can change before they get completed!):
		// query: select all 
		//        from table 
		//        where delete_reason_=NULL 
		//        (AND where start_time>lastTimestamp) 
		//        ORDER BY start_time_
		//
		// start from the lastsuccesfulrun table ID_
		//   loop till you drop, but is limited to 50 x 1000 tasks (order by start_time)...
		//   if there is still more: set the last ID_ in the lastsuccesfulrun table
		//      - next time continue where we stopped until cycled through
		//   if there is nothing more: clean the ID_ from the lastsuccesfulrun table 
		//      - next time a full scan again to catch up with newly created/modified items
		//
		// completed tasks (will not change anymore) 
		// query: select all
		//        from table
		//        where end_date_ != NULL
		//        (and end_date_ => lastsuccesfulrun timestamp)
		//        order by end_date_
		
		// search items where END_TIME => lastsuccesfulrun timestamp
		//   when there is no more: record the last found last-harvested END_TIME
		
		
		if (allowProcessHarvesting()){
			
			if (logger.isDebugEnabled())
				logger.debug("Harvesting Activiti workflowTasks");	
			
			// Make sure we have a connection
			dbhb.openReportingConnection();
			
			//String formattedDate = null; // dateformat.format( theDate );
			//String batchFormattedDate = reportingHelper.getSimpleDateFormat().format(new Date());
			//String formattedDate = ""; 
			
			int maxItems = getMaxLoopSize();
			long maxLoopCount = getMaxLoopCount();
			long loopCount=0;
			int resultSize=999999;
			boolean canContinueDeleted = false;
//			boolean hasEverRan = false;
			
			List<String> myTaskList;
			Iterator<String> myTaskListIterator;
			String taskId=""; 
			
			
			String formattedDate = dbhb.getLastTimestamp(TABLE_NAME_RUNNING);
			
			while (resultSize>0) {
				
				loopCount++;
				 
				myTaskList = dbhb.getCreatedTasks(formattedDate, maxItems);
				resultSize=myTaskList.size();
				
				if (logger.isDebugEnabled())
					logger.debug("Found " + resultSize + " started workflow tasks...");	
				
				myTaskListIterator = myTaskList.iterator();
				while (myTaskListIterator.hasNext()){
					taskId = myTaskListIterator.next();
					addToQueue("activiti$" + taskId);
				}
				
				if (resultSize==0){
					canContinueDeleted = true;
					// reset/empty the lastSuccesfullRun timestamp
					dbhb.resetLastTimestampTable(TABLE_NAME_RUNNING);
				} else {
					// result.size() > 0
					
					// get the last item from the resultset
					String lastItem = myTaskList.get(myTaskList.size()-1);
					
					//get the start_time_ timestamp
					Date startDate = getTimestampTask("start_time", lastItem);
					
					// set this Date to lastmodified 
					if (logger.isDebugEnabled())
						logger.debug("Setting Batch-based timestamp: " 
								+ getDateAsFormattedString(startDate));
					dbhb.setLastTimestamp(TABLE_NAME_RUNNING , 
										 getDateAsFormattedString(startDate));
				}
			} // end while
			
			// set runStatus = DONE
			// if not hasEverRan setLastModifiedTimestamp
			
			// the same, but then for completed tasks
			formattedDate = dbhb.getLastTimestamp(TABLE_NAME_COMPLETED);
			resultSize=999999;
			loopCount=0;
//			hasEverRan=false;
			
			while ((resultSize>0) && canContinueDeleted && loopCount<maxLoopCount) {
				loopCount++;
				
				myTaskList = dbhb.getDeletedTasks(formattedDate, maxItems);
				resultSize=myTaskList.size();
				
				if (logger.isDebugEnabled())
					logger.debug("Found " + resultSize + " completed workflow tasks...");	
				
				myTaskListIterator = myTaskList.iterator();
				while (myTaskListIterator.hasNext()){
					addToQueue("activiti$" + myTaskListIterator.next());
				}
				
				if (resultSize==0){
					// nothing
				} else {
					// result.size() > 0
					
					// get the last item from the resultset
					String lastItem = myTaskList.get(myTaskList.size()-1);
					
					//get the end_time_ timestamp
					Date startDate = getTimestampTask("end_time", lastItem);
					
					// set this Date to lastmodified 
					if (logger.isDebugEnabled())
						logger.debug("Setting Batch-based timestamp: " 
								+ getDateAsFormattedString(startDate));
					dbhb.setLastTimestamp(TABLE_NAME_COMPLETED , 
										 getDateAsFormattedString(startDate));
					// hasEverRan=true
				}
			}	// end while
			// make sure we gently close the connection
			dbhb.closeReportingConnection();
			
		} // end if activiti enabled=true
		
		
	} // end harvestNodes()


	@Override
	void havestNodes(NodeRef harvestDefinition) {
		// TODO Auto-generated method stub
		
	}
}
