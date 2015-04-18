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

import java.util.Date;
import java.util.Properties;
import java.util.Set;

import org.alfresco.reporting.Constants;
import org.alfresco.reporting.ReportLine;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.Utils;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GroupProcessor extends PropertyProcessor {

	
	private static Log logger = LogFactory.getLog(GroupProcessor.class);
	
	public GroupProcessor(
			ServiceRegistry serviceRegistry,
			ReportingHelper reportingHelper,
			DatabaseHelperBean dbhb) throws Exception{
		
		setAuthorityService(serviceRegistry.getAuthorityService());
		
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
		return null;
	}

	@Override
	Properties processQueueDefinition(String table) {
		return null;
	}

	@Override
	void processQueueValues(String table) throws Exception {

	}

	@Override
	void havestNodes(NodeRef harvestDefinition) {
		// TODO Auto-generated method stub

	}

    public void processGroups(String tableName){
    	logger.debug("enter processGroups");
    	ReportLine rl = new ReportLine( tableName, 
    									getSimpleDateFormat(), 
    									reportingHelper);
    	
    	// Make sure there is a connection
    	dbhb.openReportingConnection();
    	
    	try{
    		// first make sure our table has the right set of columns
    		//logger.debug("processGroups: Prepping table columns");
    		Properties definition = new Properties();
    		Properties classToColumnType = getClassToColumnType();
    		definition.setProperty("sys_store_protocol", classToColumnType.getProperty("sys_store_protocol","-"));
    		definition.setProperty("groupName", classToColumnType.getProperty("name","-"));
    		definition.setProperty("groupDisplayName", classToColumnType.getProperty("name","-"));
    		definition.setProperty("userName", classToColumnType.getProperty("name","-"));
    		definition.setProperty(Constants.COLUMN_ZONES, classToColumnType.getProperty("zones","-")); // zone is a reserved word in Postgresql
    		setTableDefinition(tableName, definition);
    	
    		Set<String> groupNames = getAuthorityService().getAllAuthorities(AuthorityType.GROUP);
	    	for (String groupName : groupNames){
	    		String groupDisplayName=getAuthorityService().getAuthorityDisplayName(groupName);
	    		Set<String> zones = getAuthorityService().getAuthorityZones(groupName);
	    		
	    		// TODO Use recursive group membership as a configurable property
	    		Set<String> userNames = getAuthorityService().getContainedAuthorities(AuthorityType.USER, groupName, false);
	    		for (String userName : userNames){
	    			//logger.debug("Processing: " + groupDisplayName + " and user " + userDisplayName);
	    			
	    			// store groupname, groupDisplayName, userName
	    			rl.reset();
					
		    		//logger.debug("processUpdate: Before processProperties");
					try{
						rl.setLine("groupName", 
									classToColumnType.getProperty("name"), 
									groupName, 
									getReplacementDataType());
						rl.setLine("groupDisplayName", 
									classToColumnType.getProperty("name"), 
									groupDisplayName, 
									getReplacementDataType());
						rl.setLine("userName", 
									classToColumnType.getProperty("name"), 
									userName, 
									getReplacementDataType());
						rl.setLine(Constants.COLUMN_ZONES, 
									classToColumnType.getProperty("zones"), 
									Utils.setToString(zones), 
									getReplacementDataType());
						rl.setLine("cm_created", 
									getClassToColumnType().getProperty("validFrom"), 
									getSimpleDateFormat().format(new Date()), 
									getReplacementDataType());
					} catch (Exception e){
						logger.error("processUpdate: That is weird");
						e.printStackTrace();
					}
					
					@SuppressWarnings("unused")
					int numberOfRows=0;
					numberOfRows = dbhb.insertIntoTable(rl);
	    		} // end for user:userNames
	    	} // end for group:groupNames
    	} catch (Exception e) {
			logger.fatal("processGroups - terrible error:");
			e.printStackTrace();
		} finally {
			// make sure we gently close the connection
			dbhb.closeReportingConnection();
			rl.reset();
		}
    	logger.debug("Exit processGroups");	
    	
    }
}
