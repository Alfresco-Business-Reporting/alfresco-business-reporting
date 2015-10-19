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

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.alfresco.reporting.ReportLine;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SitePersonProcessor extends PropertyProcessor {

	private SiteService siteService;
	
	private static Log logger = LogFactory.getLog(SitePersonProcessor.class);
			
	public SiteService getSiteService() {
		return siteService;
	}

	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	} 

	public SitePersonProcessor(	
			ServiceRegistry serviceRegistry,
			ReportingHelper reportingHelper,
			DatabaseHelperBean dbhb) throws Exception{

		setSiteService(serviceRegistry.getSiteService());

		setReportingHelper(reportingHelper);
		setDbhb(dbhb);
		

		setClassToColumnType(reportingHelper.getClassToColumnType());
		setReplacementDataTypes( reportingHelper.getReplacementDataType() );
		setGlobalProperties( reportingHelper.getGlobalProperties());
		setNamespaces( reportingHelper.getNameSpaces() );
		setBlacklist(reportingHelper.getBlacklist());

		if (logger.isDebugEnabled()){
			logger.debug("##this.dataDictionary       =" + this.dataDictionary);
			logger.debug("##this.replacementDataTypes =" + this.replacementDataTypes);
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
	void havestNodes(NodeRef harvestDefinition) {
		// TODO Auto-generated method stub

	}

    public void processSitePerson(String tableName) throws Exception {
    	
    	logger.debug("enter processSitePerson");
    	
    	// Make sure we have a connection
    	dbhb.openReportingConnection();
    	
    	try{
    		tableName = reportingHelper.getValidTableName(tableName);
	    	ReportLine rl = new ReportLine(tableName, getSimpleDateFormat(), reportingHelper);
	    	
	    	
			Properties classToColumnType = getClassToColumnType();
			Properties replacementTypes = getReplacementDataType();
			Properties definition = new Properties();
			
			// first make sure our table has the right set of columns
			//logger.debug("processSitePerson: Prepping table columns");
			//definition.setProperty("sys_store_protocol", classToColumnType.getProperty("sys_store_protocol","-"));
			definition.setProperty("siteName", classToColumnType.getProperty("name","-"));
			definition.setProperty("siteRole", classToColumnType.getProperty("name","-"));
			definition.setProperty("siteRoleGroup", classToColumnType.getProperty("name","-"));
			definition.setProperty("userName", classToColumnType.getProperty("name","-"));
			//setTable(tableName);
	    	setTableDefinition(tableName, definition);
			//logger.debug("processSitePerson: Done prepping table columns");
			
			List<String> roleList = siteService.getSiteRoles();
	
	    	List<SiteInfo> siteInfoList = siteService.listSites(null, null);
	    	for (SiteInfo siteInfo : siteInfoList){
	    		for (String role : roleList){
	    			try{
		    			//logger.debug("processSitePerson: getting role " + role +" from site " + siteInfo.getShortName());
		    			String roleGroup = siteService.getSiteRoleGroup(siteInfo.getShortName(), role);
		    			Map<String, String> someMap = siteService.listMembers(siteInfo.getShortName(), null, role, 0, true);
		    			Set<String> keys = someMap.keySet();
		    			for (String userName : keys){
		    				if (logger.isDebugEnabled())
		    					logger.debug("processSitePerson: " +
		        					siteInfo.getShortName() + " | " +
		        					roleGroup + " | " + 
		        					userName);
		    				
		    				rl.reset();
		    				//logger.debug("processSitePerson: Before processProperties");
							try{
								rl.setLine("siteName", classToColumnType.getProperty("name"), siteInfo.getShortName(), replacementTypes);
								rl.setLine("siteRole", classToColumnType.getProperty("name"), role, replacementTypes);
								rl.setLine("siteRoleGroup", classToColumnType.getProperty("name"), roleGroup, replacementTypes);
								rl.setLine("userName", classToColumnType.getProperty("name"), userName, replacementTypes);
							} catch (Exception e){
								//logger.error("processSitePerson: siteName; That is weird");
								e.printStackTrace();
							}
								
							@SuppressWarnings("unused")
							int numberOfRows=0;
							numberOfRows = dbhb.insertIntoTable(rl);
		    			} // end for key in keySet
	    			} catch (Exception e){
	        			e.printStackTrace();
	        			logger.fatal(e.getMessage());
	        		}		
	    		} // end for role in roleList
			
	    	} // end for siteInfo in siteInfoList
	    	
	    } catch (Exception e) {
			logger.fatal("Exception selectFromWhere: " + e.getMessage());
			throw new Exception(e);
		}
    	finally {
			// make sure we gently close the connection
			dbhb.closeReportingConnection();
		}
    	if (logger.isDebugEnabled())
    		logger.debug("Exit processSitePerson");
    }
}
