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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.reporting.Constants;
import org.alfresco.reporting.ReportLine;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.ReportingModel;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

abstract class PropertyProcessor {

	protected ServiceRegistry serviceRegistry;
	private SearchService searchService;
	private AuthorityService authorityService;
	private AuthenticationService authenticationService;
	protected VersionService versionService;
	protected NodeService nodeService;
	protected DictionaryService dictionaryService;
	protected FileFolderService fileFolderService;
	protected DatabaseHelperBean dbhb;
	
	protected ReportingHelper reportingHelper;
	
	protected String method = Constants.UPDATE_VERSIONED;
	private static Log logger = LogFactory.getLog(PropertyProcessor.class);
	
	protected List<Object> queue = new ArrayList<Object>();
	
	private Map<String,Object> cacheMap = new HashMap<String,Object>();

	private static final String ROOT_KEY = "reportingRootKey";
	private static final String MAX_BATCH_COUNT = "maxBatchCount";
	private static final String MAX_BATCH_SIZE = "maxBatchSize";
	/**
	 * Allow a max number of (non-fatal) errors, then terminate
	 * NOT IN USE YET
	 */
	protected int maxErrorsPerRun = 100; 
	/** 
	 * the default mapping of Alfresco types against SQL types
	 */
	Properties dataDictionary;
	
	/**
	 * an obect containing name based property exceptions to the default mapping 
	 * of Alfresco property types against SQL types
	 */
	Properties replacementDataTypes;
	
	/**
	 * Alfrsco's list of global-properties
	 */
	Properties globalProperties;
	
	Properties namespaces;

	
	/**
	 * The list of property names not to include in the reporting database
	 */
	String blacklist;

	
	
	// ****************************************************************
	
	public void setQueue(final List<Object> theQueue){
		this.queue = theQueue;
	}
	
	public void resetQueue(){
		this.queue = new ArrayList<Object>();
	}
	
	public SearchService getSearchService() {
		return searchService;
	}


	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}


	public AuthorityService getAuthorityService() {
		return authorityService;
	}


	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}


	public AuthenticationService getAuthenticationService() {
		return authenticationService;
	}


	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}


	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public DictionaryService getDictionaryService() {
		return dictionaryService;
	}

	public void setDictionaryService(DictionaryService dictionaryService) {
		this.dictionaryService = dictionaryService;
	}

	public FileFolderService getFileFolderService() {
		return fileFolderService;
	}

	public void setFileFolderService(FileFolderService fileFolderService) {
		this.fileFolderService = fileFolderService;
	}

	public DatabaseHelperBean getDbhb() {
		return dbhb;
	}

	public void setDbhb(DatabaseHelperBean dbhb) {
		this.dbhb = dbhb;
	}

	public ReportingHelper getReportingHelper() {
		return reportingHelper;
	}

	public void setReportingHelper(ReportingHelper reportingHelper) {
		this.reportingHelper = reportingHelper;
	}

	protected String getNowAsFormattedString(){
		Date theDate = new Date((new Date()).getTime()-Constants.HARVESTING_SAFETY_MARGIN);  // substract 1 second. Otherwise test will fail
		String nowFormattedDate = reportingHelper.getSimpleDateFormat().format( theDate );
		return nowFormattedDate;
	}
	
	protected String getDateAsFormattedString(Date lastModified){
		String nowFormattedDate = reportingHelper.getSimpleDateFormat().format( lastModified );
		return nowFormattedDate;
	}
	
	protected boolean allowProcessHarvesting(){
		boolean returnBoolean = getGlobalProperties()
					.getProperty("system.workflow.engine.activiti.enabled", "true")
					.toLowerCase()
					.equals("true");
		if (returnBoolean){
			// @ToDo AND workflow enabled @ harvestinDefinition
		} else {
			logger.info("activity workflow is disabled at system level, no havesting!");
		}
		
		return returnBoolean;
	}
	
	public NodeRef getReportingRoot(){
		NodeRef returnRef;
	
		if (logger.isDebugEnabled())
			logger.debug("getReportingRoot: enter...");
		if (cacheMap.containsKey(ROOT_KEY)){
			returnRef = (NodeRef)cacheMap.get(ROOT_KEY);
    		if (logger.isDebugEnabled())
    			logger.debug("getReportingRoot: returning from cache: "+ returnRef);
    		return returnRef;
    	} else {
	    	NodeRef thisRootRef = null;
	    	ResultSet placeHolderResults= null;
	    	try{

	    		String fullQuery = "TYPE:\"reporting:reportingRoot\"";
	    		SearchParameters sp = new SearchParameters();
	    		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
				sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
				sp.setQuery(fullQuery);
				
				if (logger.isDebugEnabled())
					logger.debug("getReportingRoot: starting query...");
				
				placeHolderResults = searchService.query(sp);
				
				if (logger.isDebugEnabled())
					logger.debug("getReportingRoot: result size=" + placeHolderResults.length());
				// cycle the resultset of containers
				for (ResultSetRow placeHolderRow : placeHolderResults){
					thisRootRef = placeHolderRow.getChildAssocRef().getChildRef();
					
					if (logger.isDebugEnabled())
						logger.debug("Found reporting root: " + 
								nodeService.getProperty(thisRootRef, ContentModel.PROP_NAME));
					
					
				} // end for ResultSetRow
	    	} catch (Exception e){
	    		logger.error("getReportingRoot: " + e.getMessage());
	    		//e.printStackTrace();
	    	} finally {
	    		if (placeHolderResults!=null){
	    			placeHolderResults.close();
	    		}
	    	}
	    	cacheMap.put(ROOT_KEY, thisRootRef);
	    	
	    	if (logger.isDebugEnabled())
    			logger.debug("getReportingRoot: returning: "+ thisRootRef);
	    	return thisRootRef;
    	}
    }
	
	public boolean getBatchTimestampEnabled(){
		boolean returnBoolean = true;
		
		try{
			NodeRef reportingRoot = getReportingRoot();
			if (logger.isDebugEnabled())
				logger.debug("getBatchTimestampEnabled: ReportingRoot="+reportingRoot);
			
			if (logger.isDebugEnabled()){
				logger.debug("getBatchTimestampEnabled: boolean=" + nodeService.getProperty(reportingRoot, ReportingModel.PROP_REPORTING_BATCH_TIMESTAMP_ENABLED));
				logger.debug(nodeService.getProperties(reportingRoot));
			}
			returnBoolean = (Boolean)nodeService.getProperty(reportingRoot, ReportingModel.PROP_REPORTING_BATCH_TIMESTAMP_ENABLED);
		} catch (Exception e){
			logger.fatal("getBatchTimestampEnabled: FATAL: " + e.getMessage());
		}
		if (logger.isDebugEnabled())
			logger.debug("getBatchTimestampEnabled: Returning="+returnBoolean);
		/*
		if (batchTimestampEnabled==0){
			returnBoolean=false;
		}
		if (batchTimestampEnabled==5){
			Properties globalProperties = reportingHelper.getGlobalProperties();
			returnBoolean = "true".equals(
				globalProperties.getProperty(Constants.property_harvesting_batchTimestampEnabled, "true").toLowerCase());
			if (returnBoolean){
				batchTimestampEnabled=1;
			} else {
				batchTimestampEnabled=0;
			}
		} 
		*/
		return returnBoolean;
	}
	
	public long getMaxLoopCount(){
		long returnLong=50;
		try{
			NodeRef reportingRoot = getReportingRoot();
			if (logger.isDebugEnabled())
				logger.debug("getMaxLoopCount: ReportingRoot="+reportingRoot);
			returnLong = (Integer) nodeService.getProperty(reportingRoot, ReportingModel.PROP_REPORTING_MAX_BATCH_COUNT);
		} catch (Exception e){
			logger.fatal("getMaxLoopCount: " + e.getMessage());
		}
/*
		
		if (maxLoopCount==987654){
			Properties globalProperties = reportingHelper.getGlobalProperties();
			returnLong = Long.parseLong(
					globalProperties.getProperty(Constants.property_harvesting_maxBatchCount, "0"));
		} else {
			returnLong = maxLoopCount; 
		}
*/
		return returnLong;
	}


	public int getMaxLoopSize(){
		int returnInt = 1000;
		try{
			NodeRef reportingRoot = getReportingRoot();
			if (logger.isDebugEnabled())
				logger.debug("getMaxLoopSize: ReportingRoot="+reportingRoot);
			returnInt = (Integer) nodeService.getProperty(reportingRoot, ReportingModel.PROP_REPORTING_MAX_BATCH_SIZE);
		} catch (Exception e){
			logger.fatal("getMaxLoopSize: " + e.getMessage());
		}

		return returnInt;
	}
	
	/**
	 * Allow a max number of (non-fatal) errors, then terminate
	 * NOT IN USE YET
	 * @param theMaxErrorsPerRun
	 */
	public void setMaxErrorsPerRun(int theMaxErrorsPerRun){
		try{
			Math.abs(theMaxErrorsPerRun );
			this.maxErrorsPerRun = theMaxErrorsPerRun;
		} catch (Exception e){
			// it is not a number or null <-- ignore and use the default
		}
			
	}
	
	/**
	 * Allow a max number of (non-fatal) errors, then terminate
	 * NOT IN USE YET
	 */
	public int getMaxErrorsPerRun(){
		return this.maxErrorsPerRun;
	}
	

	public Properties getGlobalProperties() {
		return globalProperties;
	}

	public void setGlobalProperties(final Properties globalProperties) {
		this.globalProperties = globalProperties;
	}

	public Properties getNamespaces() {
		return this.namespaces;
	}

	public void setNamespaces(final Properties namespaces) {
		this.namespaces = namespaces;
	}

	public Properties getReplacementDataTypes() {
		return replacementDataTypes;
	}

	public void setBlacklist(final String blacklist) {
		this.blacklist = blacklist;
	}

	public List<Object> getQueue(){
		return this.queue;
	}
	
	public Properties getClassToColumnType(){
		return this.dataDictionary;
	}
	
	void setClassToColumnType(Properties dataDictionary){
		this.dataDictionary = dataDictionary;
	}
	
	public Properties getReplacementDataType(){
		return this.replacementDataTypes;
	}

	public String getBlacklist(){
		return this.blacklist;
	}

	public void setReplacementDataTypes(Properties p){
		this.replacementDataTypes = p;;
	}


	
	@SuppressWarnings("unused")
	private void createEmtpyQueue(final String table){
		this.queue = new ArrayList<Object>();
	}
	
	public void addToQueue(final Object queueItem){
		this.queue.add(queueItem);
	}
	 
	public SimpleDateFormat getSimpleDateFormat(){
		return reportingHelper.getSimpleDateFormat();
	}
	/**
	 * Given the input string, replace all namespaces where possible. 
	 * @param namespace
	 * @return string whith replaced full namespaces into short namespace definitions
	 */
	 public String replaceNameSpaces(final String namespace) {
		// use regular expressions to do a global replace of the full namespace into the short version.
		Properties p = getNamespaces();
		String returnSpace=namespace;
		//logger.debug("namespaces:" + p);
		Enumeration<Object> keys = p.keys(); 
		while (keys.hasMoreElements()){
			String into = (String)keys.nextElement();	
			String from = p.getProperty(into);
			returnSpace=returnSpace.replace(from, into);
		}
		returnSpace=returnSpace.replace("-","_");
		  
		return returnSpace;
	}
	 
	 public static void propertyLogger(final String description, final Properties p){
		 if (logger.isDebugEnabled())
			 logger.debug("PropertyLogger: " + description);
		 Enumeration<Object> keys = p.keys();
		 while (keys.hasMoreElements()){
				String key = (String)keys.nextElement();
				String value = p.getProperty(key,"-");
				if (logger.isDebugEnabled())
					logger.debug("  entry: "+ key + "=" + value);
		 }		 
	 }

		/**
		 * Validate if the unique sum of properties exists in the table definition.
		 * Update the table definition if columns are not yet defined
		 * @param props unique set of columns and their type
		 * @throws Exception 
		 */
	public void setTableDefinition(final String tableName, Properties props) throws Exception{
		if (logger.isDebugEnabled())
			logger.debug("Enter setTableDefinition tableName="+tableName + " with props=" + props);
		// get the existing table definition
		
		try{
			//props = reportingHelper.propertyKeyToLowerCase(props);	
			
			Properties tableDesc = dbhb.getTableDescription(tableName);
			
			if (logger.isDebugEnabled()){
				propertyLogger("## Object properties", props);
				propertyLogger("## Table description", tableDesc);
			}
			
			// check if our properties are defined or not
			Enumeration<Object> keys = props.keys();
			while (keys.hasMoreElements()){
				String key = (String)keys.nextElement();
				String type = props.getProperty(key,"-");
				// compare the key in lower-case. tableDesc contains lower case, 
				// key should too... Otherwise we happen to run into not-created issues against columns
				if (logger.isDebugEnabled())
					logger.debug("## COMPARE: key="+key);
				if (tableDesc.containsKey(key /*.toLowerCase() */) ||
						tableDesc.containsKey(key.toUpperCase()) || // Oracle sucks, Oracle sucks
						tableDesc.containsKey(key.toLowerCase()) ){ // Oracle sucks, Oracle sucks
					// column exists... do nothing
					if (logger.isDebugEnabled())
						logger.debug("DEFINITION Column " + key + " already exists.");
				} else {
					// column does NOT exist, accordingly.
					// if type is known, alter/extend the table 
					if (!"-".equals(type) && !"".equals(type) ){
						if (logger.isDebugEnabled())
							logger.debug("DEFINITION Adding column: " + key +"=" + type);
						dbhb.extendTable(tableName, key, type);
					} else {
						if (logger.isDebugEnabled())
							logger.debug("DEFINITION Column " + key + " is empty. Type=" + type);
					}
				}
			} // end while
		} catch (Exception e) {
			logger.fatal("Exception setTableDefinition: " + e.getMessage());
			throw new Exception(e);
		}
		logger.debug("Exit setTableDefinition");
	} // end setTableDefinition
	
	// *************** Definitions *********************
	/**
	 * 
	 * @param definition
	 * @param nodeRef	Current nodeRef to put all related and relevant property 
	 * values into the reporting database
	 * @param defBacklist the Blacklist String
	 * @return
	 */ 
	public Properties processPropertyDefinitions(final Properties definition, final NodeRef nodeRef){
		if (logger.isDebugEnabled())
			logger.debug("enter processPropertyDefinitions #props=" + definition.size() + " and nodeRef " + nodeRef);
		try{
			Map<QName, Serializable> map = nodeService.getProperties(nodeRef);
			if (logger.isDebugEnabled())
				logger.debug("processPropertyDefinitions: Size of map="+map.size());
			Iterator<QName> keys = map.keySet().iterator();
			while (keys.hasNext()){
				String key="";
				String type="";
				try{
					QName qname = keys.next();
					//Serializable s = map.get(qname);
					if (qname!=null){
						key = qname.toString();
						key = replaceNameSpaces(key);
						
						if (logger.isDebugEnabled())
							logger.debug("processPropertyDefinitions: Processing key " + key);
						
						if (!key.startsWith("{urn:schemas_microsoft_com:}") && !definition.containsKey(key) ){
							type="";
							if (getReplacementDataType().containsKey(key)){
								type = getReplacementDataType().getProperty(key, "-").trim();
							} else {
								type="-";
								try{
									type=dictionaryService.getProperty(qname)
										.getDataType().toString().trim();
									type = type.substring(type.indexOf("}")+1, type.length());
									type = getClassToColumnType().getProperty(type,"-");
								} catch (NullPointerException npe){
									// ignore. cm_source and a few others have issues in their datatype??
									logger.info("Silent drop of NullPointerException against " + key);
								}
								// if the key is not in the BlackList, add it to the prop object that 
								// will update the table definition
							}
							if ((type!=null) && !type.equals("-") && !type.equals("") && (key!=null) && 
									(!key.equals("")) && (!getBlacklist().toLowerCase().contains(","+key.toLowerCase()+","))){
								definition.setProperty(key, type);
								if (logger.isDebugEnabled())
									logger.debug("processPropertyDefinitions: Adding column "+ key + "=" + type);
							} else {
								if (logger.isDebugEnabled())
									logger.debug("Ignoring column "+ key + "=" + type);
							}
						} // end if containsKey
					} //end if key!=null
				} catch (Exception e) {
					logger.error("processPropertyDefinitions: Property not found! Property below...");
					logger.error("processPropertyDefinitions: type=" + type + ", key="+ key);
					e.printStackTrace();
				}
				if (logger.isDebugEnabled())
					logger.debug("processPropertyDefinitions: end while");
			} // end while
		} catch (Exception e){
			e.printStackTrace();
			logger.error("processPropertyDefinitions: Finally an EXCEPTION " + e.getMessage());
		}
		//logger.debug("Exit processPropertyDefinitions");
		return definition;
	}
	

	
	
	/**
	 * 
	 * @param s
	 * @param dtype
	 * @param multiValued
	 * @return
	 */
	@SuppressWarnings("unused")
	private String getPropertyValue(
			Serializable s,
			final String dtype,
			final boolean multiValued){
		if (logger.isDebugEnabled())
			logger.debug("Enter getPropertyValue (3 params)");
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
			if (logger.isDebugEnabled())
				logger.debug("Found a category!");
			@SuppressWarnings("unchecked")
			List<NodeRef> categories = (List<NodeRef>) s;
			if (categories != null){
			
				for (NodeRef cat : categories){
					String catName = nodeService.getProperty(
						cat, ContentModel.PROP_NAME).toString(); 
					
					if (returnValue.length()>0) returnValue+=",";
						returnValue+= catName;
				} // end for
			} // end if categories != null
		} // end category
		
		if (logger.isDebugEnabled())
			logger.debug("Exit getPropertyValue, returning: " + returnValue);
		return returnValue; 
	}
	
	private String getCategoryDisplayPath(NodeRef category){
		String returnString = (String)getNodeService().getProperty(category, ContentModel.PROP_NAME);
		NodeRef parent = getNodeService().getPrimaryParent(category).getParentRef();
		NodeRef parentsParent=getNodeService().getPrimaryParent(parent).getParentRef();
		int counter=0;
		while (!ContentModel.TYPE_CATEGORYROOT.equals(getNodeService().getType(parentsParent)) && (counter<20)){
			returnString = (String)getNodeService().getProperty(parent, ContentModel.PROP_NAME)
							+ "/" + returnString;
			parent = getNodeService().getPrimaryParent(parent).getParentRef();
			parentsParent=getNodeService().getPrimaryParent(parent).getParentRef();
			counter++;
		}
		return returnString;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public String getPropertyValue(final NodeRef nodeRef, 
									final QName qname, 
									final String dtype, 
									final boolean multiValued){
		if (logger.isDebugEnabled())
			logger.debug("Enter getPropertyValue (4 params), qname="+qname+", noderef="+nodeRef+", dtype="+dtype);
		String returnValue = ""; // how could this have been null... there are too many += constructs that will fail...
		Serializable s = getNodeService().getProperty(nodeRef, qname);
		if (logger.isDebugEnabled())
			logger.debug("getPropertyType Serialized="+s);
		
		// Tjarda: Check of s!=null wel valide is! Bij Tags en Categories
		if (multiValued && !"category".equals(dtype)){
			
			ArrayList<Object> values = new ArrayList();
			
			
			values = (ArrayList)getNodeService().getProperty(nodeRef, qname);
			
			if ((values!=null) && (!values.isEmpty()) && (values.size()>0)){
				
				if (dtype.equals("date") || dtype.equals("datetime")){
					SimpleDateFormat dateformat = getSimpleDateFormat();
//					Calendar c = Calendar.getInstance();
					
					for (int v=0;v<values.size();v++){
						returnValue += dateformat.format((Date)values.get(v)) + Constants.MULTIVALUE_SEPERATOR;		
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
					SimpleDateFormat dateformat = getSimpleDateFormat(); 
					Calendar c = Calendar.getInstance();
					c.setTimeInMillis( ((Date)s).getTime() );
					returnValue = dateformat.format((Date)s);
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
				
				// why this one below?? Without it it doesn't work, but that is a bad excuse...
				if (returnValue.equals("")){
					returnValue = String.valueOf(s);
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
		
		if (dtype.equals("category")){
			if (logger.isDebugEnabled())
				logger.debug("I am a category!");
			List<NodeRef> categories = (List<NodeRef>) nodeService.getProperty(nodeRef, qname);
			if (categories != null){
				
				for (NodeRef cat : categories){
					String catName = nodeService.getProperty(
							cat, ContentModel.PROP_NAME).toString(); 
					catName = getCategoryDisplayPath(cat);
					if (returnValue.length()>0) returnValue+=",";
					returnValue+= catName;
				} // end for
			} // end if categories != null
		} // end category
	
		if (logger.isDebugEnabled())
			logger.debug("Exit getPropertyValue, returning: " + returnValue);
		return returnValue; 
	}
	
	/**
	 * 
	 * @param rl
	 * @param sn
	 * @param blacklist
	 * @return
	 */
	public ReportLine processPropertyValues(ReportLine rl, NodeRef nodeRef){
		Map<QName, Serializable> map = 
						nodeService.getProperties(nodeRef);
		
//		if (nodeRef.toString().startsWith("version")) {
//			Map<String, Serializable> versionMap = versionService.getCurrentVersion(nodeRef).getVersionProperties();
//			logger.fatal("VersionMap: " + versionMap.keySet());
//			logger.fatal("NodeRefMap: " + map.keySet());
//		}
		if (logger.isDebugEnabled())
			logger.debug("processPropertyValues enter " + nodeRef);
		
		if (dictionaryService.isSubClass(
						nodeService.getType(nodeRef), 
						ContentModel.TYPE_CONTENT)){
			try {
				rl.setLine("cm_workingcopylink", getClassToColumnType().getProperty("noderef",""), null, getReplacementDataType());
				rl.setLine("cm_lockOwner", getClassToColumnType().getProperty("noderef",""), null, getReplacementDataType());
				rl.setLine("cm_lockType", getClassToColumnType().getProperty("noderef",""), null, getReplacementDataType());
				rl.setLine("cm_expiryDate", getClassToColumnType().getProperty("datetime",""), null, getReplacementDataType());
			} catch (Exception e) {
				logger.error("processPropertyValues Exception " + e.getMessage());
				e.printStackTrace();
				//throw new Exception(e)l
			}
		} // end pre-set valued for these props, since they need to be cleared for cases the checkout has been undone
		
		Iterator<QName> keys = map.keySet().iterator();
		while (keys.hasNext()){
			String key = "";
			String dtype = "";
			try{
				QName qname = keys.next();
				key = qname.toString();
//			logger.debug("processPropertyValues: voor: KEY="+key);
				if (!key.startsWith("{urn:schemas_microsoft_com:}")) {
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
					
					//logger.debug("processPropertyValues EVAL: key="+key + ", type="+type+", dtype="+dtype+", value=" + getPropertyValue(nodeRef, qname, dtype, multiValued));
					//logger.debug("processPropertyValues: blacklist="+ blacklist);
				
					if (!blacklist.toLowerCase().contains(","+key.toLowerCase()+",") && !type.equals("-")){
						String value = getPropertyValue(nodeRef, qname, dtype, multiValued);
						rl.setLine(key, type, value, getReplacementDataType());
					}
				} // end exclude Microsoft shizzle. It is created when doing WebDAV
			} catch (Exception e){
				//logger.info("processPropertyValues: " + e.toString());
				logger.debug("processPropertyValues: Error in object, property "+key+" not found! (" + dtype +")");
				// usually this is not bad....
			}
		} // end while loop through this object's properties
		return rl;
	}
	
	
	/**
	 * Process a single node to a ReportLine object  
	 * @param identifier
	 * @param table
	 * @return
	 */
	abstract protected ReportLine processNodeToMap(String identifier, String table, ReportLine rl);
	
	abstract Properties processQueueDefinition(String table);
	
	abstract void processQueueValues(String table) throws Exception;
	
	abstract void havestNodes(NodeRef harvestDefinition);
}
