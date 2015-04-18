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

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.reporting.Constants;
import org.alfresco.reporting.ReportLine;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.ReportingModel;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.reporting.script.EntryIdCallback;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.audit.AuditQueryParameters;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.parser.ParseException;

public class AuditingExportProcessor extends PropertyProcessor {

	protected AuditService auditService;
	private String vendor; // Contains the brand of the database 
	private int actualAmount = 0; // Contains the actual nummber of entries processed in this batch
	private long lastFromTime = 0; // Contains the last timestamp processed by the call back class
	
	private static Log logger = LogFactory.getLog(AuditingExportProcessor.class);
			
	public AuditingExportProcessor(	
			DatabaseHelperBean dbhb,
			ReportingHelper reportingHelper,
			ServiceRegistry serviceRegistry) throws Exception{

		this.auditService = serviceRegistry.getAuditService();
		setNodeService(serviceRegistry.getNodeService());
		setDictionaryService(serviceRegistry.getDictionaryService());
		setFileFolderService(serviceRegistry.getFileFolderService());
		setSearchService(serviceRegistry.getSearchService());
		
		setReportingHelper(reportingHelper);
		setDbhb(dbhb);
		
		setClassToColumnType(reportingHelper.getClassToColumnType());
		setReplacementDataTypes( reportingHelper.getReplacementDataType() );
		setGlobalProperties( reportingHelper.getGlobalProperties());
		setNamespaces( reportingHelper.getNameSpaces() );
		setBlacklist(reportingHelper.getBlacklist());
		vendor = reportingHelper.getDatabaseProvider();
		
		if (logger.isDebugEnabled()){
			logger.debug("##this.dataDictionary       =" + this.dataDictionary);
			logger.debug("##this.replacementDataTypes =" + this.replacementDataTypes);
			//logger.debug("##this.getGlobalProperties()     =" + this.getGlobalProperties());
			logger.debug("##this.namespaces           =" + this.namespaces);
		}
	}

	
	@Override
	protected ReportLine processNodeToMap(String identifier, String table,
			ReportLine rl) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	Properties processQueueDefinition(String table) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	void processQueueValues(String table) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void havestNodes(NodeRef harvestDefinition) {
		dbhb.openReportingConnection();
		try{
			String tableNames = (String)getNodeService().getProperty(
									harvestDefinition, 
									ReportingModel.PROP_REPORTING_AUDIT_QUERIES);
			
			if (tableNames!=null){
				String[] tableNamesArray = tableNames.split(",");
				for (String tableName:tableNamesArray){
					String fullTableName = dbhb.fixTableColumnName(tableName);
					if (logger.isDebugEnabled())
						logger.debug("processing table " + fullTableName);
					
					if (!dbhb.tableIsRunning(fullTableName)){
						Date theDate = new Date((new Date()).getTime()-Constants.HARVESTING_SAFETY_MARGIN);  // substract 1 second. Otherwise test will fail
						String nowFormattedDate = reportingHelper.getSimpleDateFormat().format(theDate);
						dbhb.setLastTimestampStatusRunning(fullTableName);
						dbhb.createEmptyTables(fullTableName);
												
						processAuditingExport(tableName.trim(), fullTableName);
						
						dbhb.setLastTimestampAndStatusDone(fullTableName, nowFormattedDate);
					} // end if
				} // end for
			} // end if tableNames != null
		} catch (Exception e) {
			logger.fatal("Exception havestNodes: " + e.getMessage());
			throw new AlfrescoRuntimeException(e.getMessage());
		} finally {
			// make sure we gently close the connection
			dbhb.closeReportingConnection();
		}
	
	}
	
	/** This method should return only if ALL entries up until now() are processed. 
	 *  This can mean multiple cycles are needed (limit to 50.000 or any other reasonable amount)
	 *  **!! NOT how it works right now!!**
	 */
    public void processAuditingExport(String auditFeed, String tableName){
    	if (logger.isDebugEnabled())
    		logger.debug("enter processAuditingExport " + auditFeed);
    	
    	// http://code.google.com/a/apache-extras.org/p/alfresco-opencmis-extension/source/browse/trunk/src/main/java/org/alfresco/cmis/client/authentication/OAuthCMISAuthenticationProvider.java?r=19
    	if ((auditFeed!=null) && (!"".equals(auditFeed.trim()))){
	 
    		// getLastTimestamp returns actual timestamp, or timestamp in 1970
    		String timestamp = dbhb.getLastTimestamp(tableName);
    		
    		if ((timestamp!=null) && !"".equals(timestamp)){
    			timestamp = timestamp.replaceAll("T", " ").trim();
    		}
	    	
	    	SimpleDateFormat format = Constants.getAuditDateFormat(); 
	    	
	    	Date startDate=new Date();
			try {
				startDate = format.parse(timestamp);
			} catch (java.text.ParseException e1) {
				e1.printStackTrace();
			}
	    	Long fromTime = startDate.getTime() +1; // +1 otherwise you always get the last one double
	    	
	    	ReportLine rl = new ReportLine( tableName, 
	    									getSimpleDateFormat(), 
	    									reportingHelper);
	    	Properties replacementTypes = reportingHelper.getReplacementDataType();
	    	boolean letsContinue = true;
	    	
	        try
	        {
	        	if (logger.isDebugEnabled())
	        		logger.debug("processAuditingExport: Prepping table columns");
	        	// if specified, respect the max number of results from the audit framework.
	    		// if not, get as much as you can!
	    		int maxAmount = Math.min( Integer.parseInt(globalProperties.getProperty(Constants.property_audit_maxResults, "50000")),
	    				 				  Integer.MAX_VALUE);
	    		
	        	Properties definition = new Properties();
	        	definition.setProperty("sys_store_protocol", getClassToColumnType().getProperty("sys_store_protocol","-"));
	    		definition.setProperty(Constants.COLUMN_TIMESTAMP, reportingHelper.getClassToColumnType().getProperty("datetime","-"));
	    		definition.setProperty(Constants.COLUMN_USERNAME, reportingHelper.getClassToColumnType().getProperty("name","-"));
	    		//definition.setProperty("success", getClassToColumnType().getProperty("boolean","-"));
	    		setTableDefinition(tableName, definition);
				
				 
	        	EntryIdCallback changeLogCollectingCallback = 
	        			new EntryIdCallback(true, rl, replacementTypes, tableName, auditFeed)
	            {
	        		private String validateColumnName(String tablename){
	        			if (logger.isDebugEnabled())
	        				logger.debug("enter validateColumnName: " + tablename);
	        			String origTablename = tablename;
	        			if (getCache().containsKey(tablename)){
	        				//logger.debug("Cache hit! returning: " + getCache().getProperty(tablename));
	        				return getCache().getProperty(tablename);
	        			}
	        			
	        			String replaceChars = "/-:;'.,;";
	        			int index=10;
	        			try{
		        			for (int i=0;i<replaceChars.length();i++){
		        				while (tablename.indexOf(replaceChars.charAt(i))>-1){
			        				index = tablename.indexOf(replaceChars.charAt(i));
			        				//logger.debug("Processing char=" + replaceChars.charAt(i) + " at index="+index + " | " + tablename);
			        				
			        				// the first
			        				if (index==0){
			        					tablename=tablename.substring(1, tablename.length());
			        				} else {
			        					// the last
				        				if (index==tablename.length()-1){
				        					tablename=tablename.substring(0, tablename.length()-2);
				        				} else {
				        					if ((index<(tablename.length()-1)) && (index>-1)){
				        						// otherwise in between
				        						tablename = tablename.substring(0,index) + "_" +
					            						tablename.substring(index+1, tablename.length());
				        					} else {
				        						//logger.fatal("That's weird: index=" + index + " and length="+ tablename.length()+ " " + tablename);
				        					}
				        				}
			        				} // end if/else index==0
		        				} // end while
		        				
		        			}
		        			if (Constants.VENDOR_ORACLE.equalsIgnoreCase(vendor)){
		        				if (tablename.length()>30)
		        					tablename=tablename.substring(0, 30);
		        			} // end if Oracle
		        			// update the cache with our newly calculated replacement string
		        			if (!getCache().containsKey(tablename)){
		        				this.addToCache(origTablename, tablename);
		        			}
		        		} catch (Exception e){
		        			logger.fatal("That's weird: index=" + index + " and length="+ tablename.length()+ " " + tablename);
		        			e.getMessage();
		        			
		        		}
	        			
	        			if (logger.isDebugEnabled())
	        				logger.debug("exit validateColumnName: " + tablename.toLowerCase());
	        			return tablename.toLowerCase();
	        		}
	        		
	                @Override
	                public boolean handleAuditEntry(Long entryId, String user, long time, Map<String, Serializable> values)
	                {
	                	// get the datemask in order to convert Date to String and back. 
	                	// Remind, the 'T' is missing, and a blank instead. Replace this later 
	                	// on in execution (see replaceAll(" ","T");!!)
	                	SimpleDateFormat format = Constants.getAuditDateFormat(); 
	                	Date theDate = new Date(time);
	                	lastFromTime = time;
	                    //process the values into the reporting DB;
	                	//logger.debug("id="+ entryId+" user="+ user+" time="+ time+" values"+ values);
	                	Set<String> theValues = null;
	                	if (values!=null){
		                	theValues = values.keySet(); 
		                	Properties definition = new Properties();
		    				for (String value : theValues){
		    					try{
			    		    		definition.setProperty(	
			    		    				validateColumnName(value), 
			    		    				reportingHelper.getClassToColumnType().getProperty(
			    		    				"noderef",
			    		    				"-")
			    		    		);
			    		    		setTableDefinition(getTableName(), definition);
		    					} catch (Exception e){
		    						logger.fatal("handleAuditEntry: UNABLE to process property from Values Map object");
		    					}
		    						
		    				} // end for
	                	} // end if values !=null
	    				try{
	    					getRl().reset();
	    					Properties replacementTypes = reportingHelper.getReplacementDataType();
	    					Properties classToColumnType = reportingHelper.getClassToColumnType();
	    					getRl().setLine(Constants.COLUMN_SYS_NODE_UUID, 
	    							reportingHelper.getClassToColumnType().getProperty("noderef"), 
	    							entryId.toString(), 
	    							replacementTypes);
	    					getRl().setLine(Constants.COLUMN_TIMESTAMP, 
	    							classToColumnType.getProperty("datetime"), 
   									format.format(theDate).replaceAll(" ", "T"), 
   									replacementTypes);
	    					getRl().setLine(Constants.COLUMN_USERNAME, 
   									classToColumnType.getProperty("name"), 
   									user, 
   									replacementTypes);
	    					if (values!=null){
		    					for (String value : theValues){
		    						logger.debug("Setting value=" + value);
		       						getRl().setLine(validateColumnName(value), 
			       						classToColumnType.getProperty("noderef"), 
			       						String.valueOf(values.get(value)), 
			       						replacementTypes);
		        				} // end for value:theValues from Map
	    					} // end if
	    				} catch (Exception e){
	    					logger.fatal("Setting values in ResultLine object failed...");
	    					e.printStackTrace();
	    				}
	    				
	    				@SuppressWarnings("unused")
						int numberOfRows=0;
	    				try {
							if (dbhb.rowExists(getRl())){
								numberOfRows = dbhb.updateIntoTable(getRl());
								//logger.debug(numberOfRows+ " rows updated");
							} else {
								numberOfRows = dbhb.insertIntoTable(getRl());
								//logger.debug(numberOfRows+ " rows inserted");
								
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}
	    				catch (Exception e) {
								e.printStackTrace();
						}
	    				actualAmount++;
	                    return super.handleAuditEntry(entryId, user, time, values);
	                    //return true;
	                } // end handleAuditEntry
	            }; //end newEntryIdCallback
	            

	            // Contiue to loop until te number of results is less than the max number
	            // of hits Then we're done...
				while (letsContinue){
					this.actualAmount=0;
		            //logger.debug("Before auditQuery, fromtime=" + fromTime + " | Timestamp=" + timestamp);
		            AuditQueryParameters params = new AuditQueryParameters();
		            params.setApplicationName(auditFeed);
		            params.setForward(true);
		            params.setFromTime(fromTime);
		            
		            auditService.auditQuery(changeLogCollectingCallback, params, maxAmount);
		            fromTime=this.lastFromTime;
		            if (logger.isDebugEnabled())
		            	logger.debug("After auditQuery actual="+actualAmount + " max=" + maxAmount);
		            
		            letsContinue = (actualAmount==maxAmount);
				} // end while
					
	        }
	        catch (ParseException e)
	        {
	        	e.printStackTrace();
	            throw new RuntimeException(e);
	        } catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				rl.reset();
			}
    	} // end if auditFeed !="" --> Prevent action if auditLog=enabled but no value specified
    	if (logger.isDebugEnabled())
    		logger.debug("exit processAuditingExport");
    }


}
