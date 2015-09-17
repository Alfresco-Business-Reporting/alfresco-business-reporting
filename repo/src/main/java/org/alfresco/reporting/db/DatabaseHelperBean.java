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

package org.alfresco.reporting.db;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.reporting.Constants;
import org.alfresco.reporting.ReportLine;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.ReportingModel;
import org.alfresco.reporting.mybatis.InsertInto;
import org.alfresco.reporting.mybatis.ReportingColumnDefinition;
import org.alfresco.reporting.mybatis.ReportingDAO;
import org.alfresco.reporting.mybatis.SelectFromWhere;
import org.alfresco.reporting.mybatis.UpdateWhere;
import org.alfresco.reporting.mybatis.WorkflowDAO;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class DatabaseHelperBean {

	private static Log logger = LogFactory.getLog(DatabaseHelperBean.class);
	private Properties globalProperties;
	private ReportingDAO reportingDAO;
	private WorkflowDAO workflowDAO;
	private ReportingHelper reportingHelper;
	private final String LASTSUCCESSFULRUN = "lastsuccessfulrun";
	
	
	public String fixTableColumnName(String inName){
		return reportingHelper.getValidTableName(inName);
	}
	
	
	private ReportingHelper getReportingHelper() {
		return reportingHelper;
	}


	public void setReportingHelper(ReportingHelper reportingHelper) {
		this.reportingHelper = reportingHelper;
	}


	public void setWorkflowDAOImpl (WorkflowDAO workflowDAO){
		this.workflowDAO = workflowDAO;
	}
	
	public void setReportingDAOImpl (ReportingDAO reportingDAO){
		this.reportingDAO = reportingDAO;
	}
	
    public void setProperties(Properties properties){
    	this.globalProperties = properties;
    }
    
    public void logAllProperties(){
 
    	Enumeration<Object> keys = globalProperties.keys();
   		while (keys.hasMoreElements()){
   			String key = (String)keys.nextElement();
   			if (key.indexOf("reporting.")>-1){
   				if (logger.isDebugEnabled()) 
   					logger.debug(key+"="+globalProperties.getProperty(key));
	   			
   			}
   		}

    }
    
	public Map<String, String> getShowTables(){
		Map<String, String> sm = new HashMap<String, String>();
		
		List<String> tables = reportingDAO.getShowTables();
		logger.debug("Found show tables results: " + tables.size());
		for (int t=0;t<tables.size(); t++){
			String tablename = tables.get(t);
			logger.debug("Processing table " + tablename);
			int amount = reportingDAO.getNumberOfRowsForTable(tablename);
			logger.debug("Returned " + amount);
			sm.put(tablename, String.valueOf(amount));
		}
		return sm;
	}
	
	

	public Map<String, String> getShowTablesDetails2(){
		Map<String, String> sm = new HashMap<String, String>();

		List<String> tableList = reportingDAO.getShowTables();
		Iterator<String> tableIterator = tableList.iterator();
		while (tableIterator.hasNext()){
			
			String tablename = tableIterator.next().trim();
			if ((tablename!=null) && !LASTSUCCESSFULRUN.equalsIgnoreCase(tablename)){
				
				String timestamp_w = getLastTimestamp_w(tablename);
				String timestamp_a = getLastTimestamp_a(tablename);
				
				String status = reportingDAO.reportingSelectStatusFromLastsuccessfulrun( tablename );
				String status_w = reportingDAO.reportingSelectStatusFromLastsuccessfulrun( tablename+"_w" );
				String status_a = reportingDAO.reportingSelectStatusFromLastsuccessfulrun( tablename+"_a" );
				
				if (!"Done".equalsIgnoreCase(status)){
					if ("Running".equals(status_w)) status="Running";
					if ("Running".equals(status_a)) status="Running";
					if ("Done".equals(status_w) && "Done".equals(status_a)) status = "Done";
				}
				
				String totaal = String.valueOf(reportingDAO.getNumberOfRowsForTable(tablename));
				logger.debug("Rows for table " + tablename + ": " + totaal);
				
				sm.put(tablename, timestamp_w + "," 
								+ timestamp_a + "," 
								+ status + ","
								+ totaal + ",");
				
			} // end if !LOASTSUCESFULRUN
		} // end while
		return sm;
	}
	
	/**
	 * This method provides the input for the Admin Dashlet. 
	 * (All but the lastSuccessfulRun timestamp....)
	 * @return
	 */
	public Map<String, String> getShowTablesDetails(){
		Map<String, String> sm = new HashMap<String, String>();

		List<String> tableList = reportingDAO.getShowTables();
		Iterator<String> tableIterator = tableList.iterator();
		
		// for each table in the list of tables
		while (tableIterator.hasNext()){
			
			String tablename = tableIterator.next();
			
			
			if ((tablename!=null) && !LASTSUCCESSFULRUN.equalsIgnoreCase(tablename)){
				tablename = tablename.trim();
				
				String status = reportingDAO.reportingSelectStatusFromLastsuccessfulrun( tablename );
				
				String timestamp = getLastTimestamp(tablename);
				//String timestamp_a = getLastTimestamp_a(tablename);
				
				logger.debug("Current Status for "+tablename+"=" + status);
				
				if (logger.isDebugEnabled())
					logger.debug("Processing table >" + tablename + "<");
				
				String totaal = "";
		    	String isLatest = "0";
		    	String isNonLatest = "0";
		    	String isWorkSpace = "0";
		    	String isArchive = "0";
		    	
				totaal = String.valueOf(reportingDAO.getNumberOfRowsForTable(tablename));
				logger.debug("Rows for table " + tablename + ": " + totaal);
				
				if (!Constants.VENDOR_ORACLE.equalsIgnoreCase(reportingHelper.getDatabaseProvider())){
					
					if (!"0".equals(totaal)){ // an exception will be thrown if there are no rows...
						logger.debug("Lets prep for reportingSelectIsLatestPerTable"); 
						List<Map<String,Object>> rowList = reportingDAO.reportingSelectIsLatestPerTable(tablename);
						logger.debug("Done reportingSelectIsLatestPerTable: " +rowList.size());
						
						Iterator<Map<String,Object>> rowIterator = rowList.iterator();
						//for each row in the resultlist
						while (rowIterator.hasNext()){
							Map<String,Object> columnMap = rowIterator.next();
							Iterator<String> columnIterator =  columnMap.keySet().iterator();
							boolean isLatestKey=false;
							boolean isNonLatestKey=false;
							boolean isLatestSet=false;
							boolean isNonLatestSet=false;
							long value=0;
							
							// iterate over the column. Each column appears in the actual case, and upperCase
							while (columnIterator.hasNext()){
								String key = columnIterator.next();
								logger.debug("isLatest key=" + key + " value=" + columnMap.get(key));
								if ( "islatest".equalsIgnoreCase(key) 
										&& columnMap.get(key).toString().equals("1") ){
									isLatestKey=true;
									//isLatest = Long.toString(map1.get(key));
						    	}
								if ( "islatest".equalsIgnoreCase(key) 
										&& columnMap.get(key).toString().equals("0") ){
						    		isNonLatestKey=true;
									//isNonLatest = Long.toString(map1.get(key));
								}
								if ( key.toLowerCase().startsWith("count")){
									//value = (Long)columnMap.get(key); 
									if (isLatestKey && !isLatestSet){ 
										isLatest = String.valueOf(columnMap.get(key));
										logger.debug("Setting isLatest="+ columnMap.get(key));
										isLatestKey=false;
										isLatestSet=true; // set the value only once
									}
									if (isNonLatestKey && !isNonLatestSet){ 
										isNonLatest = String.valueOf(columnMap.get(key));
										logger.debug("Setting isNonLatest="+ columnMap.get(key));
										isNonLatestKey=false;
										isNonLatestSet=true; // set the value only once
									}
								} // end if (COUNT(*))
							} // end while (keyIterator1.hasNext())
							
						} // end while list1Iterator.hasNext()
						rowIterator=null;
						rowList=null;
					
						logger.debug("isLatest=" + isLatest + " isNonLatest=" + isNonLatest);
						
					
						try{ // can be dangerous if applied to table without spacestore details like groups
							rowList = reportingDAO.reportingSelectStoreProtocolPerTable(tablename);
							logger.debug("Done reportingSelectStoreProtocolPerTable: " +rowList.size());
						
							rowIterator = rowList.iterator();
							while (rowIterator.hasNext()){
								try{
									Map<String,Object> columnMap = rowIterator.next();
									Iterator<String> columnIterator =  columnMap.keySet().iterator();
									boolean isArchiveKey=false;
									boolean isWorkspaceKey=false;
									boolean isArchiveSet=false;
									boolean isWorkspaceSet=false;
									long value=0;
									while (columnIterator.hasNext()){
										String keyString = columnIterator.next();
										logger.debug("SpaceStore key=" + keyString + " value=" + columnMap.get(keyString));
										if ( Constants.KEY_STORE_PROTOCOL.equalsIgnoreCase(keyString) 
													&& ("archive".equalsIgnoreCase(columnMap.get(keyString).toString())) ){
											isArchiveKey=true;
											//isLatest = Long.toString(map1.get(key));
								    	}
										if ( Constants.KEY_STORE_PROTOCOL.equalsIgnoreCase(keyString) 
												&& ("workspace".equalsIgnoreCase(columnMap.get(keyString).toString())) ){
											isWorkspaceKey=true;
											//isNonLatest = Long.toString(map1.get(key));
										}
										if ( keyString.toLowerCase().startsWith("count") ){
											//value = (Long)columnMap.get(keyString);
											if (isArchiveKey && !isArchiveSet){ 
												isArchive = String.valueOf(columnMap.get(keyString));
												logger.debug("Setting archive="+ columnMap.get(keyString));
												isArchiveKey=false;
												isArchiveSet=true; // set value only once
											}
											if (isWorkspaceKey && !isWorkspaceSet){ 
												isWorkSpace = String.valueOf(columnMap.get(keyString));
												logger.debug("Setting worksace="+ columnMap.get(keyString));
												isWorkspaceKey=false;
												isWorkspaceSet=true; // set value only once
											}
											
										} // end if COUNT(*)
								
									} // end while keyIterator2.hasNext()
									logger.debug("setting isWorkspace=" + isWorkSpace + " isArchive=" + isArchive);
								} catch (Exception e) {
									// table without workspace info
								}
								
								
							} // end while list2Iterator.hasNext()
							rowIterator=null;
							rowList=null;
						} catch (Exception e) {
							// drop it
							logger.info("Trying to process spacestore column:  " + e.toString());
						} // end try/catch
						
					} // end if (!"0".equals(totaal))
				} // end if not Oracle	
				logger.debug("Through with a single loop");
				//int tableLength = tablename.length()+5;
				sm.put(tablename, timestamp + "," 
								+ status + ","
								+ totaal + ","
								+ isLatest + ","
								+ isNonLatest + ","
								+ isWorkSpace + ","
								+ isArchive);
				
			} // end exclude lastsuccessfulrun table
		} // end while (tableIterator.hasNext())
		
		return sm;
	}
	

	public void init(){
		if (logger.isInfoEnabled()) logger.info("Starting init - " + reportingHelper.getDatabaseProvider());
		
		
		try{
			Map<String, String> p = getShowTables();
			SortedSet<String> ss = new TreeSet<String> ( p.keySet() );
			Iterator<String> keys = ss.iterator();
			if (ss.size()==0){
				logger.info("  No reporting tables to display...");
			} else {
				while (keys.hasNext()){
					String key = (String)keys.next();
					logger.info("  " + key + " (" + p.get(key) + ")");
				}
			} // end if ss.size()			
		} catch (Exception e){
			logger.warn("Reporting table information could not be retrieved!!");
			logger.fatal("Exception was: " + e.getMessage());
		}	

	}
	
	// ------------------------------------------------------------------
	//       methods for retrieving workflow props from Alfresco
	// ------------------------------------------------------------------
	
	public List<String> getCreatedTasks(String fromDate, int maxItems){
		return workflowDAO.getCreatedTasks(fromDate, maxItems);
	}
	
	public List<String> getDeletedTasks(String fromDate, int maxItems){
		return workflowDAO.getDeletedTasks(fromDate, maxItems);
	}
	
	public HashMap<String, Serializable> getPropertiesForWorkflowTask(String id){
		return workflowDAO.getPropertiesForWorkflowTask(id);
	}
	
	// ------------------------------------------------------------------
	//       methods for retrieving process instances props from Alfresco
	// ------------------------------------------------------------------
	
	public List<String> getCreatedProcesses(String fromDate, int maxItems){
		return workflowDAO.getCreatedProcesses(fromDate, maxItems);
	}
	
	public List<String> getCompletedProcesses(String fromDate, int maxItems){
		return workflowDAO.getCompletedProcesses(fromDate, maxItems);
	}
	
	public HashMap<String, Serializable> getPropertiesForWorkflownstance(String id){
		return workflowDAO.getPropertiesForWorkflowTask(id);
	}
    
	// ------------------------------------------------------------------
	
	public boolean isEnabled(){
    	boolean returnBoolean = !globalProperties.getProperty("reporting.enabled", "true").equalsIgnoreCase("false");
    	return returnBoolean;
    }
    
	// ------------------------------------------------------------------
	//                     generic SQL statements
	// ------------------------------------------------------------------
	

	public void extendTable(String table, String column, String type) throws Exception{
		if (logger.isDebugEnabled()) 
			logger.debug("enter extendTable colum="+ column + " type=" + type);
		try{
			table  = fixTableColumnName(table);
			column = fixTableColumnName(column);
			ReportingColumnDefinition rcd = new ReportingColumnDefinition(table, column, type);
			if (logger.isDebugEnabled()) 
				logger.debug("extendTable: prep starting reportinggDAO");
			reportingDAO.extendTableDefinition(rcd);
			
		} catch (Exception e) {
			// nothing. Probably, column exists
			logger.fatal("@@@@ Exception: extendTable: " + table + " | " + column + " | " + type);
			logger.fatal("@@@@ "+e.getMessage());
			System.out.println(e);
			throw new Exception(e);
		}
		if (logger.isDebugEnabled()) 
			logger.debug("exit extendTable");
	}

	
	public String selectFromWhere(String select, String from, String where) throws Exception{
		String returnString="";
		try{
			returnString = reportingDAO.reportingSelectFromWhere(select, from.toLowerCase(), where);
		} catch (Exception e) {
			logger.fatal("Exception selectFromWhere: select=" + select + " from=" + from + " where=" + where); 
			logger.fatal(e.getMessage());
			throw new Exception(e);
		}
		return returnString;
	}
	
	public boolean rowExists( ReportLine rl ) throws Exception{
		boolean returnValue = false;
		try{
			SelectFromWhere sfw = new SelectFromWhere(
					null,
					rl.getTable().toLowerCase(),
					rl.getValue(Constants.KEY_NODE_UUID));
			
			returnValue=reportingDAO.reportingRowExists(sfw);
			if (logger.isDebugEnabled()) 
				logger.debug("rowExists returning "  + returnValue);
		} catch (Exception e) {
			logger.fatal("Exception rowExists: " + e.getMessage());
			throw new Exception(e);
		}	
		return returnValue;
	}
	
	/*
	 * Returns true if the given UUID & isLatest=true has an empty 
	 * archivedDate.This is to determine if the object has been archived, 
	 * since the archive action does not modify the modified timestamp.
	 */
	public boolean archivedDateIsEmpty( ReportLine rl ) throws Exception{
		boolean returnValue = false;
		try{
			SelectFromWhere sfw = new SelectFromWhere(
					null,
					rl.getTable().toLowerCase(),
					rl.getValue(Constants.KEY_NODE_UUID));
			
			// this method will fail if there is no archived item in the database,
			// because the column will not exist...
			returnValue=reportingDAO.reportingArchivedDateIsEmpty(sfw);
			if (logger.isDebugEnabled()) 
				logger.debug("archivedDateIsEmpty returning "  + returnValue);
		} catch (Exception e) {
			logger.fatal("Exception archivedDateIsEmpty: " + e.getMessage());
			throw new Exception(e);
		}	
		return returnValue;
	}
		
	private boolean rowEqualsModifiedDate(ReportLine rl, String lastModified) throws Exception{
		// if this query exists, there already is a valid entry. Workaround for issue that Lucene search does not return time, just date
		boolean returnValue = false;
		logger.debug("rowEqualsModifiedDate: lastModified=" 
					+ lastModified + " vs. " + rl.getValue(Constants.KEY_MODIFIED));
		try{
			SelectFromWhere sfw = new SelectFromWhere(
					null,
					rl.getTable(),
					rl.getValue(Constants.KEY_NODE_UUID));
			sfw.setAndwhere(lastModified);
			
			if (rl.hasValue(Constants.KEY_VERSION_LABEL)
					//&& (!"archive".equals(rl.getValue("sys_store_protocol"))) // exclude archived nodes from version check 
				){
				sfw.setAndandwhere(rl.getValue(Constants.KEY_VERSION_LABEL).toLowerCase());
				returnValue = reportingDAO.reportingRowVersionedEqualsModifiedDate(sfw);
	
				if (logger.isDebugEnabled()) 
					logger.debug("rowEqualsModifiedDate: Versioned! VersionLabel="
								+ rl.getValue(Constants.KEY_VERSION_LABEL) 
								+ " returns: "
								+ returnValue);
			} else {
				returnValue = reportingDAO.reportingRowEqualsModifiedDate(sfw);
				if (logger.isDebugEnabled()) 
					logger.debug("rowEqualsModifiedDate: returns: "+returnValue);
			}
		} catch (Exception e) {
			logger.fatal("Exception rowEqualsModifiedDate: " + e.getMessage());
			throw new Exception(e);
		} 
		logger.debug("exit rowEqualsModifiedDate: " + returnValue);
		return returnValue;
		
	}
	
	//@SuppressWarnings("deprecation")
	public int updateVersionedIntoTable(ReportLine rl) throws Exception{
		logger.debug("enter updateVersionedIntoTable modified=" 
				+ rl.getValue(Constants.KEY_MODIFIED) + " vs. archived="
				+ rl.getValue(Constants.KEY_ARCHIVED_DATE));
		try{
			SimpleDateFormat sdf = reportingHelper.getSimpleDateFormat();
			
			if (//rl.getValue("cm_modified")!=null) &&
					!rowEqualsModifiedDate(rl, rl.getValue(Constants.KEY_MODIFIED) )){
				try{
					//sdf.parse(rl.getValue("cm_modified")); // read the cm_modified, and return a decent MySQL date format
					logger.debug("updateVersionedIntoTable table=" + rl.getTable());
					logger.debug("updateVersionedIntoTable vaidUntil="+ rl.getValue(Constants.KEY_MODIFIED)); // sdf.parse
					logger.debug("updateVersionedIntoTable sys_ode_uuid=" + rl.getValue(Constants.KEY_NODE_UUID));

					String modified = "'"+rl.getValue(Constants.KEY_MODIFIED)+"'";
					if (Constants.VENDOR_ORACLE.equalsIgnoreCase(reportingHelper.getDatabaseProvider())){
						modified = "TO_DATE("+modified+",'yyyy-MM-dd HH24:MI:SS')";
					}
					
					UpdateWhere updateWhere = new UpdateWhere(
							rl.getTable(), // next line: sdf.parse
							"validUntil=" + modified + "" , // isLatest=0 added in template
							"sys_node_uuid LIKE '" + rl.getValue(Constants.KEY_NODE_UUID)+"'"); // AND (isLatest=1) added in template
					reportingDAO.reportingUpdateVersionedIntoTable(updateWhere);
					logger.debug("exit updateVersionedIntoTable");
					return insertIntoTable(rl);
					
				} catch (Exception e){

					logger.fatal("Exception updateVersionedIntoTable1: " + e.getMessage());
					throw new Exception(e);

				}
			} else {
				logger.debug("exit updateVersionedIntoTable 0");
				return 0;
				//return insertIntoTable(rl);
			}
		} catch (Exception e) {
			logger.fatal("Exception updateVersionedIntoTable2: " + e.getMessage());
			throw new Exception(e);
		}
		//return 0;
	}
	
	/**
	 * @param rl
	 * @return int number of affected rows
	 * @throws Exception
	 * updates an archived node in table. Does not check for modified like updateVersionedIntoTable, because archived nodes use archivedDate
	 */
	public int updateArchivedIntoTable(ReportLine rl) throws Exception{
		logger.debug("enter updateVersionedIntoTable modified=" 
				+ rl.getValue(Constants.KEY_MODIFIED) + " vs. archived="
				+ rl.getValue(Constants.KEY_ARCHIVED_DATE));
		try{
			SimpleDateFormat sdf = reportingHelper.getSimpleDateFormat();
				try{
					//sdf.parse(rl.getValue("cm_modified")); // read the cm_modified, and return a decent MySQL date format
					logger.debug("updateVersionedIntoTable table=" + rl.getTable());
					logger.debug("updateVersionedIntoTable vaidUntil="+ rl.getValue(Constants.KEY_MODIFIED)); // sdf.parse
					logger.debug("updateVersionedIntoTable sys_ode_uuid=" + rl.getValue(Constants.KEY_NODE_UUID));

					String modified = "'"+rl.getValue(Constants.KEY_MODIFIED)+"'";
					if (Constants.VENDOR_ORACLE.equalsIgnoreCase(reportingHelper.getDatabaseProvider())){
						modified = "TO_DATE("+modified+",'yyyy-MM-dd HH24:MI:SS')";
					}
					
					UpdateWhere updateWhere = new UpdateWhere(
							rl.getTable(), // next line: sdf.parse
							"validUntil=" + modified + "" , // isLatest=0 added in template
							"sys_node_uuid LIKE '" + rl.getValue(Constants.KEY_NODE_UUID)+"'"); // AND (isLatest=1) added in template
					reportingDAO.reportingUpdateVersionedIntoTable(updateWhere);
					logger.debug("exit updateVersionedIntoTable");
					return insertIntoTable(rl);
					
				} catch (Exception e){

					logger.fatal("Exception updateVersionedIntoTable1: " + e.getMessage());
					throw new Exception(e);

				}
		} catch (Exception e) {
			logger.fatal("Exception updateVersionedIntoTable2: " + e.getMessage());
			throw new Exception(e);
		}
		//return 0;
	
	}
	
	
	public int updateIntoTable(ReportLine rl) throws Exception{
		logger.debug("enter updateIntoTable");
		int myInt = 0;
		try{
			UpdateWhere updateWhere = new UpdateWhere(
					rl.getTable(),
					rl.getUpdateSet(),
					"sys_node_uuid LIKE '" + rl.getValue(Constants.KEY_NODE_UUID)+"'");
			myInt =  reportingDAO.reportingUpdateIntoTable(updateWhere);
			logger.debug("exit updateIntoTable " + myInt);
		} catch (Exception e) {
			logger.fatal("Exception updateIntoTable: " + e.getMessage());
			throw new Exception(e);
		}	
		return myInt;
	}
	
	public int insertIntoTable(ReportLine rl) throws Exception{
		logger.debug("enter insertIntoTable");
		int myInt = 0;
		try{
			logger.debug("### sys_store_protocol="+rl.getValue(Constants.KEY_STORE_PROTOCOL));
			Properties classToColumn = getReportingHelper().getClassToColumnType();
			Properties replacementTypes = getReportingHelper().getReplacementDataType(); 
			if ("archive".equals(rl.getValue(Constants.KEY_STORE_PROTOCOL))){
				// validFrom = cm_created, validUntil=sys_archivedDate, isLatest=false
				String created  = rl.getValue(Constants.KEY_CREATED);
				String archived = rl.getValue(Constants.KEY_ARCHIVED_DATE);
				
				rl.setLine("isLatest", classToColumn.getProperty("boolean","-"), "false", replacementTypes);
				rl.setLine("validFrom", classToColumn.getProperty("datetime","-"), created, replacementTypes);
				rl.setLine("validUntil", classToColumn.getProperty("datetime","-"), archived, replacementTypes);
			} else {
	
				if ((rl.getValue("noderef")!=null) // exclude the groups and other objects that have null vaue for noderef
						&&  rl.getValue("noderef").toString().startsWith("version")){
					String validFrom = rl.getValue(Constants.KEY_CREATED);
					String validUntil = rl.getValue(Constants.KEY_MODIFIED);
					rl.setLine("isLatest", classToColumn.getProperty("boolean","-"), "false", replacementTypes);
					rl.setLine("validFrom", classToColumn.getProperty("datetime","-"), validFrom, replacementTypes);
					rl.setLine("validUntil", classToColumn.getProperty("datetime","-"), validUntil, replacementTypes);
				} else {
	
					String modified = rl.getValue(Constants.KEY_MODIFIED);
					rl.setLine("validFrom", classToColumn.getProperty("datetime","-"), modified, replacementTypes);
					//rl.setLine("validUntil", classToColumn.getProperty("datetime","-"), null, new Properties());
	
				}
				
			}
	
			if (logger.isDebugEnabled()){
				logger.debug("insertIntoTable table=" + rl.getTable());
				logger.debug("insertIntoTable keys=" + rl.getInsertListOfKeys());
				logger.debug("insertIntoTable values=" + rl.getInsertListOfValues());
			}
			
			InsertInto insertInto = new InsertInto(
					rl.getTable(),
					rl.getInsertListOfKeys(),
					rl.getInsertListOfValues());
			
			myInt = reportingDAO.reportingInsertIntoTable(insertInto);
		} catch (Exception e) {
			logger.fatal("Exception insertIntoTable: " + e.getMessage());
			throw new Exception(e);
		}
		logger.debug("exit insertIntoTable " + myInt);
		return myInt;
		
	}

	
	
    /**
     * dropTables drops a list of tables if they exist
     * 
     * @param tables a comma separated list of table names
     */
    public void dropTables(String tablesToDrop) throws Exception{
    	logger.debug("Enter dropTables: "+tablesToDrop);
    	try{
	    	for (String table : tablesToDrop.toLowerCase().split(",")){
	    		table = fixTableColumnName(table.trim());
	    		logger.debug("dropTables: before: "+ table);
	    		reportingDAO.dropTable(table);
	    		logger.debug("dropTables: after: "+table);
			}
    	} catch (Exception e) {
			logger.fatal("Exception dropTables: " + e.getMessage());
			throw new Exception(e);
		}	
    	logger.debug("Exit dropTables: "+tablesToDrop);
    }
    
    
    /**
     * gets the list of all reporting tables, and drops each one of them
     */
    public void dropAllTables(){
    	logger.debug("Starting dropAllTables" );
		try {
			
			List<String> tables = reportingDAO.getShowTables();
			for (String tablename : tables){
				dropTables(tablename);
			}
			
		} catch (Exception e) {
			logger.fatal("Exception dropAllTables: " + e.getMessage());
			throw new AlfrescoRuntimeException(e.getMessage());
		}
		
    }
    
    /** 
     * createEmptyTables: creates emtpy tables for the known Alfresco types
     *                    only if the table does not exist yet
     * The one and only column created is sys_node_uuid
     * 
     * @param tables a comma separated list of table names
     */
    public void createEmptyTables(String tablesToCreate) throws Exception{
    	logger.debug("Starting createEmptyTables: " + tablesToCreate);
    	try{
	    	for (String table : tablesToCreate.toLowerCase().split(",")){
	    		table=fixTableColumnName(table);
				if (logger.isDebugEnabled())
					logger.debug("createEmptyTables: now table " + table.trim());
				reportingDAO.createEmtpyTable(table.trim());
;			}
	    } catch (Exception e) {
			logger.fatal("Exception createEmptyTables: " + e.getMessage());
			throw new Exception(e);
		}	
    }

    /**
     * create the default indexes for a reporting table (sys_node_uuid AND sys_node_uuid+isLatest)
     * @param tablenames
     */
	public void createIndexesForTable(String tablenames) {
    	logger.debug("Starting createIndexForTable: " + tablenames);
    	try{
	    	for (String table : tablenames.toLowerCase().split(",")){
	    		table=fixTableColumnName(table);
	    		if (!LASTSUCCESSFULRUN.equals(table)){
				if (logger.isDebugEnabled())
					logger.debug("createIndexForTable: now table " + table.trim());
						reportingDAO.createCustomIndex(table.trim(), "sys_node_uuid", table.trim()+"_uuid_idx"); 
						reportingDAO.createCustomIndex(table.trim(), "sys_node_uuid,isLatest", table.trim()+"_uuid_islatest_idx");
				}
			}
	    } catch (Exception e) {
			logger.fatal("Exception createIndexForTable: " + e.getMessage());
			throw new AlfrescoRuntimeException(e.getMessage());
		}	
    }

	public void createIndexForTables() {
		logger.debug("Starting createIndexForTables" );
		try {
			
			List<String> tables = reportingDAO.getShowTables();
			for (String tablename : tables){
				createIndexesForTable(tablename);
			}
			
		} catch (Exception e) {
			logger.fatal("Exception createIndexForTables: " + e.getMessage());
			throw new AlfrescoRuntimeException(e.getMessage());
		}
		
	}
		
	/**
	 * 
	 * @param tablename
	 * @param columnname can be a concatenation (comma separated list) of values
	 */
	public void createCustomIndexForTable(final String tablename, final String columnname){
		final String table  = fixTableColumnName(tablename);
		final String index = fixTableColumnName(table+"_"+columnname+"_index");
		if (!LASTSUCCESSFULRUN.equals(table))
		if (logger.isDebugEnabled())
			logger.debug("createIndexForTable: now table " + table.trim() + 
						", column: " + columnname + 
						", indexname: " + index);
		reportingDAO.createCustomIndex(table.trim(), columnname, index);
	}
	
	
	public Properties getTableDescription(String table) throws Exception{
		Properties props = new Properties();
		try{
			if (logger.isDebugEnabled())
				logger.debug("Starting getTableDescription");
			table = fixTableColumnName(table);
			props = reportingDAO.getDescTable(table);
		} catch (Exception e) {
			logger.fatal("Exception getTableDescription: " + e.getMessage());
			throw new Exception(e);
		}	
		return props;
	}
	
	
	// --------------------------------------------------------------------------
	//                LastSuccesfulRun table management
	// --------------------------------------------------------------------------
	
	public void setAllStatusesDoneForTable(){
		reportingDAO.setAllStatusesDoneForTable();
	}
	
	public void resetLastTimestampTable(String tableName){
		tableName = fixTableColumnName(tableName).toLowerCase();
		logger.debug("enter resetLastTimestampTable table="+tableName);
		reportingDAO.updateLastSuccessfulRunStatusForTable(tableName, Constants.STATUS_DONE);
    }
	
	public void clearLastTimestampTable(String tableName){
		tableName = fixTableColumnName(tableName).toLowerCase();
		logger.debug("enter clearLastTimestampTable table="+tableName);
		reportingDAO.clearLastRunTimestamp(tableName);
    }
		
 	
	//used outside this bean
    public String getLastTimestampStatus(String tablename){
    	tablename = fixTableColumnName(tablename);
    	logger.debug("enter setLastTimestampStatus table="+tablename);
    	//String returnString = reportingDAO.getLastSuccessfulRunDForTable(tablename);
		String returnString = reportingDAO.getLastSuccessfulRunDateForTable(tablename);
		logger.debug("exit getLastTimestampStatus returning: " + returnString);
    	return returnString;
    }


    public String getLastTimestamp(String tableName){
    	tableName = fixTableColumnName(tableName).toLowerCase();
    	logger.debug("enter getLastTimestamp table="+tableName);
    	String returnString =reportingDAO.getLastSuccessfulRunDateForTable(tableName);
    	logger.debug("getLastTimestamp (" + tableName + ") returns " + returnString);

    	if ((returnString==null) ||(returnString.trim().equals(""))){
			// if there is no date available, make it somewhere early 1970, so we 
			// include as much as possible in the reporting DB 
	    	SimpleDateFormat format = reportingHelper.getSimpleDateFormat();//  Constants.getAuditDateFormat();
	    	Date myDate = new Date(1);
	    	returnString = format.format(myDate);
	    	returnString = returnString.replaceAll(" ", "T").trim();
		}
		logger.debug("exit getLastTimestamp returning " + returnString);
    	return returnString;
    }
    
    public String getLastTimestamp_w(String tableName){
    	boolean checkOldTable=false;
    	SimpleDateFormat format = reportingHelper.getSimpleDateFormat();//  Constants.getAuditDateFormat();
    	tableName = fixTableColumnName(tableName).toLowerCase();
    	String tableName_w = tableName+"_w";
    	logger.debug("enter getLastTimestamp_w table="+tableName_w);
    	String returnString = reportingDAO.getLastSuccessfulRunDateForTable(tableName_w);
    	logger.debug("getLastTimestamp_w (" + tableName + ") returns " + returnString);
    	try{
    		if ( returnString.trim().length()<10){
    			checkOldTable=true;
    			throw (new Exception("date is not set"));
    		}
    	} catch (Exception e){
	    	Date myDate_w = new Date(1);
	    	returnString = format.format(myDate_w);
	    	returnString = returnString.replaceAll(" ", "T").trim();
    	}
    	
    	try{
	    	if ( checkOldTable){
	    		// get the original without the _w
	    		returnString = getLastTimestamp(tableName);
	    		if ((returnString==null) ||(returnString.trim().equals(""))){
					// if there is no date available, make it somewhere early 1970, so we 
					// include as much as possible in the reporting DB 
			    	
			    	Date myDate = new Date(1);
			    	returnString = format.format(myDate);
			    	returnString = returnString.replaceAll(" ", "T").trim();
	    		}
	    	}
		} catch (Exception e){
			// nothing
			logger.info("getLastTimestamp_w got a non-fatal error, most likely a null-pointer string: " + e.getMessage());
		}
		logger.debug("exit getLastTimestamp_w returning " + returnString);
    	return returnString;
    }

    public String getLastTimestamp_a(String tableName){
    	boolean checkOldTable=false;
    	SimpleDateFormat format = reportingHelper.getSimpleDateFormat();//  Constants.getAuditDateFormat();
    	tableName = fixTableColumnName(tableName).toLowerCase();
    	String tableName_a = tableName+"_a";
    	logger.debug("enter getLastTimestamp_a table="+tableName_a);
    	//String returnString = getLastTimestamp(tableName_a); 
    	String returnString = reportingDAO.getLastSuccessfulRunDateForTable(tableName_a);
    	
    	logger.debug("getLastTimestamp_a (" + tableName + ") returns " + returnString);
    	
    	try{
    		if ( returnString.trim().length()<10){
    			checkOldTable=true;
    			throw (new Exception("date is not set"));
    		}
    	} catch (Exception e){
	    	Date myDate_w = new Date(1);
	    	returnString = format.format(myDate_w);
	    	returnString = returnString.replaceAll(" ", "T").trim();
    	}

    	
    	try{
    		if ( checkOldTable ){
	    		// get the originl withoout the _a
	    		returnString = getLastTimestamp(tableName);
	    		
	    		if ((returnString==null) ||(returnString.trim().equals(""))){
					// if there is no date available, make it somewhere early 1970, so we 
					// include as much as possible in the reporting DB 
			    	Date myDate = new Date(1);
			    	returnString = format.format(myDate);
			    	returnString = returnString.replaceAll(" ", "T").trim();
	    		}
			}
    	} catch (Exception e){
			// nothing
			logger.info("getLastTimestamp_a got a non-fatal error, most likely a null-pointer string: " + e.getMessage());
		}
		logger.debug("exit getLastTimestamp_a returning " + returnString);
    	return returnString;
    }

    

    public boolean tableIsRunning(String tableName){
    	boolean returnBoolean = reportingDAO.lastRunTableIsRunning();
		logger.debug("exit tableIsRunning (" + tableName + ") returning " + returnBoolean);
		// IT IS A BAD THING TO RUN A HARVESTING JOB IF ANY OTHER HARVESTING JOB IS RUNNING
    	return returnBoolean;
    }

    public void setLastTimestampAndStatusDone(String tableName, String timestamp){
    	tableName = fixTableColumnName(tableName).toLowerCase();
    	logger.debug("enter setLastTimestamp for table " + tableName + " timestamp="+timestamp);
    	reportingDAO.updateLastSuccessfulRunDateForTable(tableName, Constants.STATUS_DONE, timestamp);
    }

    public void setLastTimestamp(String tableName, String timestamp){
    	tableName = fixTableColumnName(tableName).toLowerCase();
    	logger.debug("enter setLastTimestamp for table " + tableName + " timestamp="+timestamp);
    	reportingDAO.updateLastSuccessfulBatchForTable(tableName, timestamp);
    } 
    
    public void setLastTimestampStatusRunning(String tableName) {
    	tableName = fixTableColumnName(tableName);
    	logger.debug("enter setLastTimestampStatusRunning: for table="+tableName);
    	reportingDAO.updateLastSuccessfulRunStatusForTable(tableName, Constants.STATUS_RUNNING);
    	logger.debug("exit setLastTimestampStatusRunning");
    }
    
    
    public void createLastTimestampTableRow(String tableName){
    	tableName = fixTableColumnName(tableName).toLowerCase();
    	logger.debug("enter createLastTimestampTableRow table=" + tableName);
    	reportingDAO.createLastTimestampTableRow(tableName);
		logger.debug("exit createLastTimestampTableRow");
    }

	public void dropLastTimestampTable(){
		logger.debug("enter dropLastTimestampTable table=lastsuccessfulrun");
		reportingDAO.dropTable(LASTSUCCESSFULRUN);
	}

	public void openReportingConnection(){
		/*
		try {
			reportingDAO.openConnection();
		} catch (SQLException e) {
			logger.error("failed to open an SQL connection...");
			logger.error(e.getMessage());
		}
		*/
	}
	
	public void closeReportingConnection(){
		/*
		try {
			reportingDAO.closeConnection();
		} catch (SQLException e) {
			logger.error("failed to close an SQL connection...");
			logger.error(e.getMessage());
		}
		*/
	}
}