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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.reporting.Constants;
import org.alfresco.reporting.ReportLine;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.ReportingModel;
import org.alfresco.reporting.Utils;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PersonProcessor extends PropertyProcessor {

	
	private static Log logger = LogFactory.getLog(PersonProcessor.class);
			
	public PersonProcessor (
			ServiceRegistry serviceRegistry,
			ReportingHelper reportingHelper,
			DatabaseHelperBean dbhb) throws Exception{
		
		setSearchService(serviceRegistry.getSearchService());
		setAuthorityService(serviceRegistry.getAuthorityService());
		setAuthenticationService(serviceRegistry.getAuthenticationService());
		setNodeService(serviceRegistry.getNodeService());
		setDictionaryService(serviceRegistry.getDictionaryService());
		setFileFolderService(serviceRegistry.getFileFolderService());
		
		setReportingHelper(reportingHelper);
		setDbhb(dbhb);
		
		setClassToColumnType(reportingHelper.getClassToColumnType());
		setReplacementDataTypes( reportingHelper.getReplacementDataType() );
		setGlobalProperties( reportingHelper.getGlobalProperties());
		setNamespaces( reportingHelper.getNameSpaces() );
		setBlacklist(reportingHelper.getBlacklist());
		
	}
	
	@Override
	protected ReportLine processNodeToMap(String identifier, String table,
			ReportLine rl) {
		table = dbhb.fixTableColumnName(table);
		try{
			NodeRef userRef = new NodeRef(identifier);
			
			try{ 
				// first process all 'ordinary' properties
				rl = processPropertyValues(
						rl, 
						userRef, 
						",cm_homeFolder,cm_homeFolderProvider"+getBlacklist());
			} catch (Exception e){
				//logger.error("processUpdate: That is weird, rl.setLine(noderef) crashed! " + rsr.getNodeRef());
				e.printStackTrace();
			}
			
			//logger.debug("processPerson: Before noderef" );
			try{
				// manually add the noderef. Pretty important
				rl.setLine("noderef", getClassToColumnType().getProperty("noderef"), identifier, getReplacementDataType());
			} catch (Exception e){
				logger.error("processPerson: That is weird, rl.setLine(noderef) crashed! " + userRef);
				e.printStackTrace();
			}

			
			String username 		  = (String)getNodeService().getProperty(userRef, ContentModel.PROP_USERNAME);
			String account_expires 	  = null;
			String account_expirydate = null;
			String account_locked 	  = null;
			String enabled 			  = null;

			username 		   = (String)getNodeService().getProperty(userRef, ContentModel.PROP_USERNAME);
			account_expires    = (String)getNodeService().getProperty(userRef, ContentModel.PROP_ACCOUNT_EXPIRES);
			account_expirydate = (String)getNodeService().getProperty(userRef, ContentModel.PROP_ACCOUNT_EXPIRY_DATE);
			account_locked 	   = (String)getNodeService().getProperty(userRef, ContentModel.PROP_ACCOUNT_LOCKED);
			Set<String> zones  = getAuthorityService().getAuthorityZones(username); 
			if (getAuthenticationService().getAuthenticationEnabled(username)){
				enabled="true";
			} else {
				enabled="false";
			}
			
			//logger.debug("processPerson: Setting user " + username + " is enabled="+ enabled);
			rl.setLine("account_enabled", getClassToColumnType().getProperty("boolean"), enabled.toString(), getReplacementDataType());
			rl.setLine("account_expires", getClassToColumnType().getProperty("boolean"), account_expires, getReplacementDataType());
			rl.setLine("account_expirydate", getClassToColumnType().getProperty("datetime"), account_expirydate, getReplacementDataType());
			rl.setLine("account_locked", 
						getClassToColumnType().getProperty("boolean"), 
						account_locked, 
						getReplacementDataType());
			rl.setLine( Constants.COLUMN_ZONES, 
						getClassToColumnType().getProperty("zones"), 
						Utils.setToString(zones), 
						getReplacementDataType());
			rl.setLine("validFrom", getClassToColumnType().getProperty("datetime"), getSimpleDateFormat().format(new Date()), getReplacementDataType());
		} catch (Exception e){
			logger.fatal("processPerson: That is weird, rl.setLine(noderef) crashed! " + identifier);
			e.printStackTrace();
		}	

		return rl;
	}

	@Override
	Properties processQueueDefinition(String table) {
		// TODO Auto-generated method stub
		table = dbhb.fixTableColumnName(table);
		return null;
	}

	@Override
	void processQueueValues(String table) throws Exception {
		// TODO Auto-generated method stub
		table = dbhb.fixTableColumnName(table);

	}

	@Override
	void havestNodes(NodeRef harvestDefinition) {
		// TODO Auto-generated method stub

	}

	
    public void processPersons(String tableName) throws Exception{
    	logger.debug("Enter processPerson");
    	
    	// make sure we have a connection
		dbhb.openReportingConnection();
		
    	try{
	    	tableName = dbhb.fixTableColumnName(tableName);
	    	
	    	dbhb.createEmptyTables(tableName);
	    	ReportLine rl = new ReportLine( tableName, 
	    									getSimpleDateFormat(), 
	    									reportingHelper);
	    	Statement stmt = null;
	    	Properties definition = new Properties(); // set of propname-proptype
	    	
	    	
    		long highestDbId=0;
			boolean continueSearchCycle=true;
			String query = "+TYPE:\"cm:person\"";
			//setTable(tableName);
			ResultSet rs = null;
			
			while (continueSearchCycle){
				//continueSearchCycle=false;
				try { // make sure to have a finally to close the result set)
					if (logger.isDebugEnabled())
						logger.debug("processPerson: classToColumnType="+getClassToColumnType());
					SearchParameters sp = new SearchParameters();
					String fullQuery = query + " +@sys\\:node-dbid:["+ highestDbId+" TO MAX]";  
					if (logger.isDebugEnabled())
						logger.debug("processPerson: query="+fullQuery);
					sp.setLanguage(SearchService.LANGUAGE_LUCENE);
					sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
					//sp.addSort("@" + ReportingModel.PROP_SYSTEM_NODE_DBID.toString(), true);
					sp.addSort("@{http://www.alfresco.org/model/system/1.0}node-dbid", true);
					sp.setQuery(fullQuery);
					if (logger.isDebugEnabled())
						logger.debug("processPerson: Before searchService" );
					rs = getSearchService().query(sp);
					if (logger.isDebugEnabled())
						logger.debug("processPerson: Found results=" + rs.length());
					if (rs.length()==0){
						continueSearchCycle=false;
						if (logger.isDebugEnabled())
							logger.debug("processPerson: Break fired!");
						break; // we're done, no more search results
					}
					if (continueSearchCycle){
						
						Iterator<ResultSetRow> rsi = rs.iterator();
						while (rsi.hasNext()){ 
							ResultSetRow rsr = rsi.next();
							definition = processPropertyDefinitions(
									definition, 
									rsr.getNodeRef(), 
									",cm_homeFolder,cm_homeFolderProvider"+getBlacklist());
							//definition.setProperty("sys_store_protocol", getClassToColumnType().getProperty("sys_store_protocol","-"));
				    		definition.setProperty("noderef", getClassToColumnType().getProperty("noderef","-"));
				    		definition.setProperty("account_enabled", getClassToColumnType().getProperty("boolean","-"));
				    		definition.setProperty("account_expires", getClassToColumnType().getProperty("boolean","-"));
				    		definition.setProperty("account_expirydate", getClassToColumnType().getProperty("datetime","-"));
				    		definition.setProperty("account_locked", getClassToColumnType().getProperty("boolean","-"));
				    		definition.setProperty(Constants.COLUMN_ZONES, getClassToColumnType().getProperty("zones","-"));
				    		
				    		if (logger.isDebugEnabled())
				    			logger.debug("Processing person with dbid="+getNodeService().getProperty(rsr.getNodeRef(),ReportingModel.PROP_SYSTEM_NODE_DBID));
				    		
				    		highestDbId=(Long) getNodeService().getProperty(rsr.getNodeRef(),ReportingModel.PROP_SYSTEM_NODE_DBID)+1;
				    		
				    		if (logger.isDebugEnabled())
				    			logger.debug("## Table def = " + definition);
						}
						
						if (logger.isDebugEnabled())
							logger.debug("processPerson: Before setTableDefinition size=" + definition.size());
						setTableDefinition(tableName, definition);
						
						rsi = rs.iterator();
						while (rsi.hasNext()){
							ResultSetRow rsr = rsi.next();
							rl.reset();
							
							
							//logger.debug("processPerson: Before enabled" );
							rl = processNodeToMap(rsr.getNodeRef().toString(), tableName, rl);
						
							
							@SuppressWarnings("unused")
							int numberOfRows=0;
							if (dbhb.rowExists(rl)){
								numberOfRows = dbhb.updateIntoTable(rl);
								//logger.debug(numberOfRows+ " rows updated");
							} else {
								numberOfRows = dbhb.insertIntoTable(rl);
								//logger.debug(numberOfRows+ " rows inserted");
								
							} // end if/else
						} // end while
					} // end if !continueSearchCycle
				} catch (Exception e){
					e.printStackTrace();
				} finally {
					if (rs!=null){
						rs.close();
					}
				}
				
			} // end while continueSearchCycle
			
    	} catch (Exception e) {
			logger.fatal("Exception processPersson: " + e.getMessage());
			throw new Exception(e);
		}	finally {
			// make sure we gently close the connection
			dbhb.closeReportingConnection();
		}
	    	
    	if (logger.isDebugEnabled())
    		logger.debug("Exit processPerson");
    }
    
	/**
	 * 
	 * @param definition
	 * @param nodeRef	Current nodeRef to put all related and relevant property 
	 * values into the reporting database
	 * @param defBacklist the Blacklist String
	 * @return
	 */
	
	public Properties processPropertyDefinitions(Properties definition, NodeRef nodeRef, String defBacklist){
		if (logger.isDebugEnabled()){
			logger.debug("enter processPropertyDefinitions def  : " + definition);
			logger.debug("enter processPropertyDefinitions node : " + nodeRef);
			logger.debug("enter processPropertyDefinitions black: " + defBacklist);
		}
		
		try{
			Map<QName, Serializable> map = getNodeService().getProperties(nodeRef);
			Iterator<QName> keys = map.keySet().iterator();
			Properties classToColumnType = getClassToColumnType();
			Properties replacementDataType = getReplacementDataType();
			while (keys.hasNext()){
				String key="";
				String type="";
				try{
					QName qname = keys.next();
					//Serializable s = map.get(qname);
					if (qname!=null){
						key = qname.toString();
						key = replaceNameSpaces(key);
						//logger.debug("processPropertyDefinitions: Processing key " + key);
						if (!key.startsWith("{urn:schemas_microsoft_com:}") && !definition.containsKey(key) ){
							type="";
							if (replacementDataType.containsKey(key)){
								type = replacementDataType.getProperty(key, "-").trim();
							} else {
								type="-";
								try{
									type=getDictionaryService().getProperty(qname)
										.getDataType().toString().trim();
									type = type.substring(type.indexOf("}")+1, type.length());
									type = classToColumnType.getProperty(type,"-");
								} catch (NullPointerException npe){
									// ignore. cm_source and a few others have issues in their datatype??
									//logger.fatal("Silent drop of NullPointerException against " + key);
								}
								// if the key is not in the BlackList, add it to the prop object that 
								// will update the table definition
							}
							if ((type!=null) && !type.equals("-") && !type.equals("") && (key!=null) && 
									(!key.equals("")) && (!defBacklist.contains(","+key+","))){
								definition.setProperty(key, type);
								//if (logger.isDebugEnabled())
								//	logger.debug("processPropertyDefinitions: Adding column "+ key + "=" + type);
							} else {
								//if (logger.isDebugEnabled())
								//	logger.debug("Ignoring column "+ key + "=" + type);
							}
						} // end if containsKey
					} //end if key!=null
				} catch (Exception e) {
					logger.error("processPropertyDefinitions: Property not found! Property below...");
					logger.error("processPropertyDefinitions: type=" + type + ", key="+ key);
					e.printStackTrace();
				}
			} // end while
		} catch (Exception e){
			e.printStackTrace();
		}
		//logger.debug("Exit processPropertyDefinitions");
		return definition;
	}
	


	/**
	 * 
	 * @param rl
	 * @param sn
	 * @param blacklist
	 * @return
	 */
	// also used in Persons!!
	public ReportLine processPropertyValues(
						ReportLine rl, 
						NodeRef nodeRef, 
						String blacklist){
		
		Map<QName, Serializable> map = 
						getNodeService().getProperties(nodeRef);
		
		Iterator<QName> keys = map.keySet().iterator();
		while (keys.hasNext()){
			String key = "";
			String dtype = "";
			try{
				QName qname = keys.next();
				key = qname.toString();
				if (logger.isDebugEnabled())
					logger.debug("processPropertyValues: voor: KEY="+key);
				if (!key.startsWith("{urn:schemas_microsoft_com:}")) {
					key = replaceNameSpaces(key);
					if (logger.isDebugEnabled())
						logger.debug("processPropertyValues: na: KEY="+key);
					
					dtype = getDictionaryService().getProperty(qname)
							.getDataType().toString();
					
					if (logger.isDebugEnabled())
						logger.debug("processPropertyValues: voor: DTYPE="+dtype);
					
					dtype = dtype.substring(dtype.indexOf("}")+1, dtype.length()).trim();
					
					if (logger.isDebugEnabled())
						logger.debug("processPropertyValues: na: DTYPE="+dtype);
					
					Object theObject = getClassToColumnType().getProperty(dtype,"-"); 
					String type = theObject.toString();
					if (logger.isDebugEnabled())
						logger.debug("processPropertyValues: na: TYPE="+type);
					
					boolean multiValued = false;
					multiValued = getDictionaryService().getProperty(qname).isMultiValued();
//try{					
//logger.debug("processPropertyValues EVAL: key="+key + ", type="+type+", dtype="+dtype+", value=" + getPropertyValue(nodeRef, qname, dtype, multiValued));
//} catch (Exception e){}
					//logger.debug("processPropertyValues: blacklist="+ blacklist);
				
					if (!blacklist.toLowerCase().contains(","+key.toLowerCase()+",") && !type.equals("-")){
						String value = "";
						try{
							value = getPropertyValue(nodeRef, qname, dtype, multiValued);
							if (value!=null) {
								rl.setLine( key, 
										type, 
										value, 
										getReplacementDataType());
							}
						} catch (Exception e){
							logger.error("Error setting ReportLine " + key + "=" + value);
							logger.error(e.getMessage());
						}
					}
				} // end exclude Microsoft shizzle. It is created when doing WebDAV
			} catch (Exception e){
				//logger.info("processPropertyValues: " + e.toString());
				logger.error("processPropertyValues: Error in object, property "+key+" not found! (" + dtype +")");
			}
		} // end while loop through this object's properties
		return rl;
	} // end processPropertyValues
	
}
