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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;

import org.alfresco.model.ContentModel;
import org.alfresco.reporting.Constants;
import org.alfresco.reporting.ReportLine;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.ReportingModel;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.CategoryService;
import org.alfresco.service.cmr.search.CategoryService.Depth;
import org.alfresco.service.cmr.search.CategoryService.Mode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CategoryProcessor extends PropertyProcessor {
	protected CategoryService categoryService;
	
	private static Log logger = LogFactory.getLog(CategoryProcessor.class);
	
	public CategoryProcessor(	
			DatabaseHelperBean dbhb,
			ReportingHelper reportingHelper,
			ServiceRegistry serviceRegistry) throws Exception{
		
		setNodeService(serviceRegistry.getNodeService());
		setDictionaryService(serviceRegistry.getDictionaryService());
		setFileFolderService(serviceRegistry.getFileFolderService());
		setSearchService(serviceRegistry.getSearchService());
		categoryService = serviceRegistry.getCategoryService();
		
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
			//logger.debug("##this.getGlobalProperties()     =" + this.getGlobalProperties());
			logger.debug("##this.namespaces           =" + this.namespaces);
			//logger.debug("##this.versionnodes         =" + getVersionNodes());
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
	public void havestNodes(NodeRef harvestDefinition)  { 
		if (logger.isDebugEnabled()) logger.debug("enter Categories");
		
		// Make sure there is a connection 
		dbhb.openReportingConnection();
		
		try{
			Collection<ChildAssociationRef>	car = categoryService.getRootCategories(
						StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, 
						ContentModel.ASPECT_GEN_CLASSIFIABLE);
			
//TODO Right now it captures ALL categories. Not just the configured ones. See IBFD Project?
//		and it does not add the sub categories by the way...
			ArrayList<NodeRef> carObject = (ArrayList<NodeRef>)getNodeService().getProperty(
															harvestDefinition, 
															ReportingModel.PROP_REPORTING_CATEGORIES);
			if ((carObject!=null) && (carObject.size()>0)){
				Iterator<NodeRef> cari = carObject.iterator();
				
		logger.debug("havestNodes: ## Categories=" + car.size());
				//Iterator<ChildAssociationRef> cari = car.iterator();
				while (cari.hasNext()){
					NodeRef category = cari.next();//.getChildRef();
					String catName = (String)getNodeService().getProperty(category, ContentModel.PROP_NAME);
					String tableName = dbhb.fixTableColumnName(catName);
					
					if (logger.isDebugEnabled())
						logger.debug("havestNodes: categoryName=" + catName + " tableName=" + tableName);
					
					processCategoriesAsPath(tableName, category, catName, "value");
						
				} // end while
			}
		} catch (Exception e){
			e.printStackTrace();
			logger.fatal("Exception in harvestNodes() " + e.getMessage());
		} finally {
			// make sure we gently close the connection
			dbhb.closeReportingConnection();
		}
		if (logger.isDebugEnabled()) 
			logger.debug("exit Categories");
	}
	
    private void storeRegionPath(ReportLine rl, NodeRef nodeRef, String regionPath, String columnName, String labelValue){
    	//logger.debug("storeRegionPath: " + table + " | " + regionPath);

    	try{
    		Properties replacementTypes = reportingHelper.getReplacementDataType();
    		String uuid = nodeRef.toString();
    		rl.setLine("sys_node_uuid", 
    					reportingHelper.getClassToColumnType().getProperty("noderef"), 
    					uuid.split("SpacesStore/")[1], 
    					replacementTypes);
    		rl.setLine( columnName, 
    					reportingHelper.getClassToColumnType().getProperty("path"), 
    					regionPath, 
    					replacementTypes);
    		rl.setLine("label", 
    					reportingHelper.getClassToColumnType().getProperty("label"), 
    					labelValue, 
    					replacementTypes);
    		
    		@SuppressWarnings("unused")
    		int numberOfRows = dbhb.insertIntoTable(rl);
			//logger.debug("storeRegionPath: " + numberOfRows+ " rows inserted");
	    } catch (Exception e){
			e.printStackTrace();
			logger.fatal(e.getMessage());
		} 
    	
    }
    
    
    public void processCategoriesAsPath(String tableName, final NodeRef rootCatRef, String categoryName, String columnName) throws Exception{
    	if (logger.isDebugEnabled()) 
    		logger.debug("Enter processCategoriesAsPath, rootName="+rootCatRef);
    	//logger.debug("Currently supporting 3 levels deep structures only!!");
    	if (rootCatRef!=null){
 
    		//String rootCatString = (String)getNodeService().getProperty(rootCatRef, ContentModel.PROP_NAME);
    		columnName = dbhb.fixTableColumnName(columnName); // fix for blanks and minus-signs
	    	
    		ReportLine rl = new ReportLine( tableName, 
    										getSimpleDateFormat(), 
    										reportingHelper);
	    	Properties definition = new Properties(); // set of propname-proptype
	    	//definition.setProperty("sys_store_protocol", getClassToColumnType().getProperty("sys_store_protocol","-"));
			definition.setProperty(columnName, reportingHelper.getClassToColumnType().getProperty("path","-"));
			definition.setProperty("label", reportingHelper.getClassToColumnType().getProperty("label","-"));
			
			try{
				setTableDefinition(tableName, definition);
				
    	    	Collection<ChildAssociationRef> rcrs = categoryService
    	    												.getChildren(rootCatRef, 
    	    														Mode.SUB_CATEGORIES, 
    	    														Depth.IMMEDIATE);
    			String labelValue="";
    			// process werelddeel
	    		String regionPath = (String)getNodeService()
	    									.getProperty(rootCatRef, 
	    										ContentModel.PROP_NAME);

	    		if ( //regionPath.equalsIgnoreCase(categoryName)  
	    				!dbhb.tableIsRunning(tableName)){
	    			Date theDate = new Date((new Date()).getTime()-Constants.HARVESTING_SAFETY_MARGIN);  // substract 1 second. Otherwise test will fail
					String nowFormattedDate = reportingHelper.getSimpleDateFormat().format(theDate);
					dbhb.setLastTimestampStatusRunning(tableName);
					
					dbhb.dropTables(tableName);
					dbhb.createEmptyTables(tableName);
					setTableDefinition(tableName, definition);
					
    	    		storeRegionPath(rl, rootCatRef, regionPath, columnName, regionPath);
    	    		rl.reset();
    	    		
    	    		// get Country refs
    	    		Collection<ChildAssociationRef> ccrs = categoryService
    	    													.getChildren(rootCatRef, 
    	    															Mode.SUB_CATEGORIES, 
    	    															Depth.IMMEDIATE);
    	    		if (ccrs.size()>0){
    	    	    	for (ChildAssociationRef countryChildRef:ccrs){
    	    	    		NodeRef countryRef = countryChildRef.getChildRef();
    	    	    		labelValue = (String)getNodeService()
    	    	    								.getProperty(countryRef, 
    	    	    									ContentModel.PROP_NAME);
    	    	    		String countryPath = regionPath + "/" + labelValue;
    	    	    		storeRegionPath(rl, countryRef, countryPath, columnName, labelValue);
    	    	    		rl.reset();

    	    	    		// get countryDiv refs
    	    	    		Collection<ChildAssociationRef> cdcrs = categoryService
    	    	    								.getChildren(countryRef, 
    	    	    											Mode.SUB_CATEGORIES, 
    	    	    											Depth.IMMEDIATE);
    	    	    		if (cdcrs.size()>0){
    	    	    	    	for (ChildAssociationRef countryDivChildRef:cdcrs){
    	    	    	    		NodeRef countryDivRef = countryDivChildRef.getChildRef();
    	    	    	    		labelValue = (String)getNodeService().getProperty(
    	    	    	    									countryDivRef, 
    	    	    	    									ContentModel.PROP_NAME);
    	    	    	    		String countryDivPath = countryPath + "/" + labelValue;
    	    	    	    		storeRegionPath(rl, countryDivRef, countryDivPath, columnName, labelValue);
    	    	    	    		rl.reset();
    	    	    			}
    	    	    		} // end if cdcrs
    	    			} // end for
    	    		} // end if ccrs
    	    		
    	    		dbhb.setLastTimestampAndStatusDone(tableName, nowFormattedDate);
    	    		
	    		} // end if regionPath.equalsIgnoreCase(rootCatString)
			} catch (Exception e) {
				e.printStackTrace();
			}
    	} // end if rootName !=null && rootName!=""
    	if (logger.isDebugEnabled()) 
    		logger.debug("Exit processCategoriesAsPath");
    }

}
