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

 package org.alfresco.reporting;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.reporting.execution.ReportTemplate;
import org.alfresco.reporting.execution.ReportingContainer;
import org.alfresco.reporting.execution.ReportingRoot;
import org.alfresco.reporting.util.resource.HierarchicalResourceLoader;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ReportingHelper {
	private NodeService nodeService;
	private ServiceRegistry serviceRegistry;
	private NamespaceService namespaceService;
	
	private String blacklist = ",";
	private String invalidTableName = "";
	
	private Properties namespacesShortToLong = null;
	private Properties classToColumn;
	private Properties replacementTypes;
	private Properties namespaces;
	private Properties globalProperties;

	private Properties tableNameCache = new Properties();
	private NodeRef reportingRootRef = null;
	
	private HierarchicalResourceLoader hierarchicalResourceLoader;
	private String configLocation;

	private String vendor;
	private static Log logger = LogFactory.getLog(ReportingHelper.class);
	
	
	public Properties getTableNameCache() {
		return tableNameCache;
	}


	public void addTableNameCache(String tablename, String tableNameFixed) {
		this.tableNameCache.setProperty(tablename, tableNameFixed);
	}

	public boolean getResetDoneStatusAtStartup(){
		boolean returnBoolean = true;
		try{
			NodeRef reportingRoot = getReportingRoot();
			logger.info("getResetDoneStatusAtStartup reportingRoot=" + reportingRoot);
			
			returnBoolean = (Boolean)nodeService.getProperty(reportingRoot, ReportingModel.PROP_REPORTING_RESET_DDNE_STATUS_AT_STARTUP);
			logger.info("getResetDoneStatusAtStartup returning: " + returnBoolean);
			//Properties globalProperties = reportingHelper.getGlobalProperties();
			//returnBoolean = "true".equals(
			//	globalProperties.getProperty(Constants.property_resetStatusAtStartupEnabled, "true").toLowerCase());
		} catch (Exception e){
			logger.error("Error getResetDoneStatusAtStartup(): " + e);
		}
		return returnBoolean;
	}

    private NodeRef getReportingRoot(){
    	NodeRef thisRootRef = null;
    	if (false){
    		// none
    	} else {
	    	
	    	ResultSet placeHolderResults= null;
	    	try{

	    		String fullQuery = "TYPE:\"reporting:reportingRoot\"";
	    		SearchParameters sp = new SearchParameters();
	    		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
				sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
				sp.setQuery(fullQuery);
				placeHolderResults = serviceRegistry.getSearchService().query(sp);
				//		Constants.QUERYLANGUAGE, 
				//		"ASPECT:\"reporting:reportingRoot\"");

				// cycle the resultset of containers
				for (ResultSetRow placeHolderRow : placeHolderResults){
					thisRootRef = placeHolderRow.getChildAssocRef().getChildRef();
					
					logger.debug("Found reporting root: " + 
								nodeService.getProperty(thisRootRef, ContentModel.PROP_NAME));
					
					
				} // end for ResultSetRow
	    	} catch (Exception e){
	    		e.printStackTrace();
	    	} finally {
	    		if (placeHolderResults!=null){
	    			placeHolderResults.close();
	    		}
	    	}
    	}
    	return thisRootRef;
    }


	/**
	 * Navigate up in the folder structure a ReportingRoot is found.
	 * 
	 * @param currentNode
	 * @return NodeRef of the first ReportingRoot the currentNode is a child of
	 */
	public NodeRef getReportingRoot(final NodeRef currentNode){
		return getParentByType(currentNode, ReportingModel.TYPE_REPORTING_ROOT);
	}
	
	
	/**
	 * Navigate up in the folder structure a ReportingContainer is found.
	 * 
	 * @param currentNode
	 * @return NodeRef of the first ReportingContainer the currentNode is a child of
	 */
	public NodeRef getReportingContainer(final NodeRef currentNode){
		return getParentByType(currentNode, ReportingModel.TYPE_REPORTING_CONTAINER);
	}
	
	/**
	 * Navigate up in the folder structure until either the object type with the
	 * given QName-type (targetType) is found, or we touch the repositoryRoot.
	 * 
	 * @param currentNode
	 * @param targetType
	 * @return NodeRef of the first parent typed with the given QName
	 */
	private NodeRef getParentByType(final NodeRef currentNode, final QName targetType){
		// consider managing a cache of noderef-to-noderef relations per QName
		
		logger.debug("Enter getParentByType");
		NodeRef returnNode = null;
		  
		if (currentNode!=null){
		
			returnNode = AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
				public NodeRef doWork() throws Exception {
					
					NodeRef rootNode = nodeService.getRootNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
					
					logger.debug("getParentByType: rootNode="+rootNode);
					logger.debug("getParentByType: nodeRef="+currentNode);
					
					NodeRef returnNode = null;
					NodeRef loopRef = currentNode;
					boolean siteTypeFound = false;
					while ( !loopRef.equals(rootNode) && !siteTypeFound){
						//logger.debug("getTypeForNode: voor loopRef="+loopRef);
						loopRef = nodeService.getPrimaryParent(loopRef).getParentRef();
						//logger.debug("getTypeForNode: na   loopRef="+loopRef);
						siteTypeFound = nodeService.getType(loopRef).equals(targetType);
						if (siteTypeFound){
							returnNode = loopRef;
							logger.debug("getParentByType: Found QName node!");
						}
					}
					return returnNode;
				} // end do work
			}, AuthenticationUtil.getSystemUserName());
		 
		} // end if nodeRef!=null
		logger.debug("Exit getParentByType: " + returnNode);
		return returnNode;
    }
	

	/**
	 * Given the selected value in the picklist in the UI, return the JAVA
	 * API string for the particular language, as defined in he SearchService
	 * 
	 * @param objectLanguage UI search language
	 * @return JAVA API name for the language
	 */
	public String getSearchLanguage(String objectLanguage){
		String returnString = SearchService.LANGUAGE_LUCENE;
		if (objectLanguage!= null){
			if ("Full Text Search".equalsIgnoreCase(objectLanguage.trim()))
				returnString = SearchService.LANGUAGE_FTS_ALFRESCO;
			if ("Lucene".equalsIgnoreCase(objectLanguage.trim()))
				returnString = SearchService.LANGUAGE_LUCENE;
			if ("XPath".equalsIgnoreCase(objectLanguage.trim()))
				returnString = SearchService.LANGUAGE_XPATH;
		} // end if objectLanguage != null
		return returnString;
	}
	
	/** 
	 * I cannot get these objects get created with the magic of the node service
	 * on board. Therefore this method will finalize the construction of the object 
	 * by pulling the content out of the Alfresco object, and setting the object props
	 * 
	 * @param reportingRoot
	 */
	public void initializeReportingRoot(ReportingRoot reportingRoot){
		reportingRoot.setGlobalExecutionEnabled( 
				(Boolean)nodeService.getProperty(
						reportingRoot.getNodeRef(), 
						ReportingModel.PROP_REPORTING_GLOBAL_EXECUTION_ENABLED));
		
		reportingRoot.setHarvestEnabled(
				(Boolean)nodeService.getProperty(
						reportingRoot.getNodeRef(), 
						ReportingModel.PROP_REPORTING_HARVEST_ENABLED));
		
		reportingRoot.setRootQueryLanguage( 
				(String)nodeService.getProperty(
						reportingRoot.getNodeRef(), 
						ReportingModel.PROP_REPORTING_ROOT_QUERY_LANGUAGE));
		
		reportingRoot.setOutputExtensionExcel(
				(String)nodeService.getProperty(
						reportingRoot.getNodeRef(), 
						ReportingModel.PROP_REPORTING_OUTPUTEXTENSION_EXCEL));
		
		reportingRoot.setOutputExtensionPdf(
				(String)nodeService.getProperty(
						reportingRoot.getNodeRef(), 
						ReportingModel.PROP_REPORTING_OUTPUTEXTENSION_PDF));
		
		reportingRoot.setOutputExtensionCsv(
				(String)nodeService.getProperty(
						reportingRoot.getNodeRef(), 
						ReportingModel.PROP_REPORTING_OUTPUTEXTENSION_CSV));
		
		reportingRoot.setTargetQueries(
				(String)nodeService.getProperty(
						reportingRoot.getNodeRef(), 
						ReportingModel.PROP_REPORTING_TARGET_QUERIES));
		
		reportingRoot.setName(
				(String)nodeService.getProperty(
						reportingRoot.getNodeRef(), 
						ContentModel.PROP_NAME));
		
	}
	
	public void initializeReportingContainer(ReportingContainer reportingContainer){
		reportingContainer.setExecutionEnabled(
				(Boolean)nodeService.getProperty(
						reportingContainer.getNodeRef(), 
						ReportingModel.PROP_REPORTING_EXECUTION_ENABLED));
		
		
		
		reportingContainer.setExecutionFrequency( 
				(String)nodeService.getProperty(
						reportingContainer.getNodeRef(), 
						ReportingModel.PROP_REPORTING_EXECUTION_FREQUENCY));
		

		
		reportingContainer.setName(
				(String)nodeService.getProperty(
						reportingContainer.getNodeRef(), 
						ContentModel.PROP_NAME));
		
		if (logger.isDebugEnabled()){
			logger.debug("initializeReportingContainer:");
			logger.debug("  Setting execution enabled = " +
						nodeService.getProperty(
								reportingContainer.getNodeRef(), 
								ReportingModel.PROP_REPORTING_EXECUTION_ENABLED));
			logger.debug("  Execution frequency: " + 
						nodeService.getProperty(
								reportingContainer.getNodeRef(), 
								ReportingModel.PROP_REPORTING_EXECUTION_FREQUENCY));
			logger.debug("  Name: "+ 
						nodeService.getProperty(
								reportingContainer.getNodeRef(), 
								ContentModel.PROP_NAME));
		}
	}
	
	public void initializeReport(ReportTemplate report){
		report.setName(
				(String)nodeService.getProperty(
						report.getNodeRef(), 
						ContentModel.PROP_NAME));
		
		report.setOutputFormat(
				(String)nodeService.getProperty(
						report.getNodeRef(), 
						ReportingModel.PROP_REPORTING_REPORTING_FORMAT));
	
		report.setOutputVersioned(
				(Boolean)nodeService.getProperty(
						report.getNodeRef(), 
						ReportingModel.PROP_REPORTING_REPORTING_VERSIONED));
		report.setReportingDocument(nodeService.hasAspect(
						report.getNodeRef(), 
						ReportingModel.ASPECT_REPORTING_REPORTABLE));
		
		report.setTargetPath(
				(String)nodeService.getProperty(
						report.getNodeRef(), 
						ReportingModel.PROP_REPORTING_TARGET_PATH));
				
		report.setSubstitution(
				(String)nodeService.getProperty(
						report.getNodeRef(), 
						ReportingModel.PROP_REPORTING_SUBSTITUTION));
		
		 
		List<AssociationRef> ar = nodeService.getTargetAssocs(
				report.getNodeRef(), 
				 ReportingModel.ASSOC_REPORTING_TARGET_NODE);
		
		if (!ar.isEmpty()){
			report.setTargetNode(ar.get(0).getTargetRef());
		}
	}
	
	/**
	 * returns a Properties object with shortValue = longValue QName
	 * This enables the system to get a short key for a long QName
	 * @return
	 */
	public Properties getNameSpacesShortToLong(){
		if (this.namespacesShortToLong == null){
			this.namespacesShortToLong = new Properties();
			Collection<String> keys = namespaceService.getPrefixes(); 
			for (String shortValue : keys){
				String longValue = namespaceService.getNamespaceURI(shortValue);
				this.namespacesShortToLong.setProperty(shortValue, longValue);
				logger.debug("replaceShortQNameIntoLong: Replacing short value: " + 
							shortValue + 
							" into long value: " + 
							longValue);
			}
		} 
		return this.namespacesShortToLong;
	}
	
	/**
	 * Method to convert a short notation into a long notation to 
	 * be able to construct a QName object from that
	 * 
	 * @param inString in the form cm:name or st:sitePreset
	 * @return a valid QName representing the property (or type, 
	 * or aspect) in the Java World
	 */
	public QName replaceShortQNameIntoLong(String inString){
		Properties namespaces = getNameSpacesShortToLong();
		String namespace = inString.split(":")[0];
		String property  = inString.split(":")[1];
		String longName = namespaces.getProperty(namespace);
		logger.debug("replaceShortQNameIntoLong: Creating long QName: "+longName);
		QName longQName = QName.createQName(longName,property);
		return longQName;
	}
	
	/**
	 * Given the input string, replace all namespaces where possible. 
	 * @param namespace
	 * @return string whith replaced full namespaces into short namespace definitions
	 */
	
	public String replaceNameSpaces(String namespace) {
		// use regular expressions to do a global replace of the full namespace into the short version.
		Properties p = getNameSpaces();
		Enumeration<Object> keys = p.keys(); 
		while (keys.hasMoreElements()){
			String into = (String)keys.nextElement();	
			String from = p.getProperty(into);
			namespace=namespace.replace(from, into);
		}
		namespace=namespace.replace("-","_");
		  
		return namespace;
	}
	
	
	/**
	 * Truncate the table or column name to the max length defined by the respective vendor
	 * @param originalName The name of column/table you like to create
	 * @return the potentially truncated name of the table of the column
	 */
	public String getTableColumnNameTruncated(final String originalName){
		final String vendor = getDatabaseProvider();
		
		String modifiedName=originalName;
		
		if (Constants.VENDOR_ORACLE.equals(vendor)){
			if (originalName.length()>Constants.MAX_COLUMNNAME_LENGTH_ORACLE){
				modifiedName=originalName.substring(0, Constants.MAX_COLUMNNAME_LENGTH_ORACLE);
			}	
		}
		if (Constants.VENDOR_MYSQL.equals(vendor)){
			if (originalName.length()>Constants.MAX_COLUMNNAME_LENGTH_MYSQL){
				modifiedName=originalName.substring(0, Constants.MAX_COLUMNNAME_LENGTH_MYSQL);
			}	
		}
		if (Constants.VENDOR_POSTGRES.equals(vendor)){
			if (originalName.length()>Constants.MAX_COLUMNNAME_LENGTH_POSTGRES){
				modifiedName=originalName.substring(0, Constants.MAX_COLUMNNAME_LENGTH_POSTGRES);
			}	
		}
		return modifiedName;
	}
    /**
     * Mapping of Alfresco property TYPES onto SQL column definitions.
     * the value of "-" means the Alfresco property will NOT be automatically 
     * mapped into the SQL database. The properties file will be read from classpath
     * There are custom calls for Site, Category, Tags
     * 
     * @return Properties object
     * @throws Exception 
     */
     public Properties getClassToColumnType() throws Exception{
		if (classToColumn==null){		
			
			//try {
				ClassLoader cl = this.getClass().getClassLoader();
				
				// get the ibatis resource path from the resource loader. 
				// Tweak this to match our vendor-specific Alfresco type-to-column mapping
				// in order to facilitate differences in DATETIME, TIMESTAMP etc.
				String url = hierarchicalResourceLoader.getResourcePath();
				if (logger.isDebugEnabled()){
					logger.debug("MyBatis resource path: " + url);
				}
				url = url.substring(0, url.lastIndexOf("/")+1);
				url += "reporting-model.properties";
				url = "/alfresco/module/org.alfresco.reporting" + url.split("/org.alfresco.reporting")[1];
				if (logger.isDebugEnabled()){
					logger.debug("Vendor specific mapping path: " + url);
				}
				
				
				InputStream is = cl.getResourceAsStream(url);
				Properties p = new Properties();
				p.load(is);
				classToColumn = p;
				if (logger.isInfoEnabled())
					logger.info("classToColumn Loaded!");
			//} catch (IOException e) {
			//	e.printStackTrace();
			//	throw new Exception(e);
			//}
		}

    	return classToColumn;
    }
     
     /**
      * Reads external properties file. The properties in this file will override 
      * the default mapping of individual Alfresco PROPERTY types into SQL column  
      * definitions ** on a per-property basis**
      * (One often knows a zip code is d:text, but never more than 8 charaters, so 
      * VARCHAR(8) will do). The total length of the row (sum of the column lengths) 
      * can never be more than 65535 bytes. And i guess UTF-8 makes a reservation of 
      * 4 bytes per character
      * 
      * The properties file REPORTING_CUSTOM_PROPERTIES can be named differently using
      * the method setCustomModelProperties(String newFileName)
      * 
      * @return Properties object with as content the key/value pairs from the properties file.
      */
 	 public Properties getReplacementDataType(){ 
 		
 		
 		try {
 			ClassLoader cl = this.getClass().getClassLoader();
 			InputStream is =cl.getResourceAsStream(Constants.REPORTING_CUSTOM_PROPERTIES);
 			Properties p = new Properties();
 			p.load(is);
 			replacementTypes = p;
 		} catch (Exception e) {
 			//e.printStackTrace();
 			replacementTypes = new Properties();
 		}
 		
       return replacementTypes;
 	}
 	 
 	/**
 	 * Get the full list of namespaces and their short form. Cache for future need.
 	 * @return
 	 */
	public Properties getNameSpaces(){
 		if (this.namespaces == null){
 			this.namespaces = new Properties();
 			Collection<String> keys = serviceRegistry.getNamespaceService().getPrefixes(); 
 			for (String key : keys){
 				String value = serviceRegistry.getNamespaceService().getNamespaceURI(key);
 				String into = key + "_";
 				String from = "{" + value + "}";
 				this.namespaces.setProperty(into, from);
 				logger.debug("getNameSpaces: Replacing: " + from + " into: " + into);
 			}
 		} 
 		return this.namespaces;
 	}
 	
	public Properties getGlobalProperties(){
		if (this.globalProperties==null){
			logger.fatal("Whoot! globalProperties object is null!!");
		}
		return this.globalProperties;
	}
	
	private void setBlacklist(String list){
		this.blacklist = list ;
	}
	
	public Properties propertyKeyToLowerCase(Properties p){
		Properties pp = new Properties();
		Iterator<Object> pIterator = p.keySet().iterator();
		String key;
		while (pIterator.hasNext()){
			key = (String)pIterator.next();
			pp.setProperty(
					key.toLowerCase(),
					p.getProperty(key));
		}
		return pp;
	}
	
	public String getBlacklist(){
		String keys = ",";
		if (",".equals(this.blacklist)){
			keys =  getGlobalProperties().getProperty(Constants.property_blockkeys, "-")+",";
			keys += getGlobalProperties().getProperty(Constants.property_blacklist, "")+",";
			keys = keys.replaceAll("-","_");
			keys = keys.replaceAll(":","_");
			setBlacklist(keys);
		} else {
			keys = this.blacklist;
		}
		return keys;
	}
    
	public String getDatabaseProvider(){
		if (this.vendor==null){
			this.vendor = hierarchicalResourceLoader.getDatabaseVendor();
		}
		return this.vendor;
	}

	/**
	 * Filter out most common reserved words as tablename. Next to that, filter on blanks and minus
	 * because it will kill functionality. Need using different quotes for different DB vendors :-(
	 * @param tableName
	 * @return
	 */
	public String getValidTableName(String tableName){
		if ("".equals(invalidTableName)){
			invalidTableName= "," + globalProperties.getProperty(Constants.property_invalid_table_names, "select,from,where,group,order by,order,by,distinct")+",";
		}
		String tableNameFixed="";
		
		// check if fix for tablename already in a cache property
		if (!getTableNameCache().containsKey(tableName)){
			// get rid of blanks, and minus. They are poison because need quotes to fix, 
			// but different quotes depending on DB vendor
			logger.debug("getValidTableName in--tableName=" + tableName);
			String replaceString = globalProperties.getProperty(Constants.property_invalid_table_chars, "- `'");
			tableNameFixed = tableName.toLowerCase().trim();
			for (int i=0;i<replaceString.length();i++){
				String character = String.valueOf(replaceString.charAt(i));
				logger.debug("getValidTableName character=" + character);
				while (tableNameFixed.contains(character)){
					tableNameFixed = 
							tableNameFixed.substring(0,tableNameFixed.indexOf(character)) + "_" +
							tableNameFixed.substring(tableNameFixed.indexOf(character)+1);
				}
			}
			logger.debug("getValidTableName out-tableName=" + tableNameFixed);
				
			//tableName = tableName.toLowerCase().replaceAll("-", "_").replaceAll(" ", "_").trim();
			
			if (invalidTableName.toLowerCase().contains(","+tableNameFixed+",")){
				tableNameFixed = "_" + tableNameFixed;
			}
			
			// Check if length >64, the max for MySQL
			if (tableNameFixed.length()>60){
				tableNameFixed=tableNameFixed.substring(0,60);
			}
			
			tableNameFixed = tableNameFixed.toLowerCase();
			logger.debug("getValidTableName after reserved words=" + tableNameFixed);
			addTableNameCache(tableName, tableNameFixed);
		} else {
			tableNameFixed = getTableNameCache().getProperty(tableName);
			logger.debug("getValidTableName cache hit=" + tableNameFixed);
		}
		
		// make sure the length of the name is within bounrdaries of the database vendor
		tableNameFixed = getTableColumnNameTruncated(tableNameFixed);
		
		return tableNameFixed;
	}
	
	public SimpleDateFormat getSimpleDateFormat(){
		String vendor = getDatabaseProvider();
		String dateformat = Constants.DATE_FORMAT_AUDIT;
		
		if ("mysql".equalsIgnoreCase(vendor)){
			dateformat = Constants.DATE_FORMAT_MYSQL;
		}
		if ("postgresql".equalsIgnoreCase(vendor)){
			dateformat = Constants.DATE_FORMAT_POSTGRESQL;
		}
		if ("oracle".equalsIgnoreCase(vendor)){
			dateformat = Constants.DATE_FORMAT_ORACLE;
		}
		if ("sqlserver".equalsIgnoreCase(vendor)){
			dateformat = Constants.DATE_FORMAT_MSSQL;
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat(dateformat);// "yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getDefault());
		return sdf;
	}
	
	public String getSearchLanguage(NodeRef harvestDefinition){
		String language = SearchService.LANGUAGE_LUCENE;
		if (SearchService.LANGUAGE_LUCENE.equalsIgnoreCase((String)nodeService.getProperty(
				harvestDefinition, 
				ReportingModel.PROP_REPORTING_ROOT_QUERY_LANGUAGE))){
			language = SearchService.LANGUAGE_LUCENE;
		}
		if (SearchService.LANGUAGE_CMIS_ALFRESCO.equalsIgnoreCase((String)nodeService.getProperty(
				harvestDefinition, 
				ReportingModel.PROP_REPORTING_ROOT_QUERY_LANGUAGE))){
			language = SearchService.LANGUAGE_CMIS_ALFRESCO;
		}
		return language;
	}
	/**
	 * General Setter for the Alfresco NodeService
	 * @param nodeService
	 */
	public void setNodeService(NodeService nodeService)	{
	    this.nodeService = nodeService;
	}
	
	public void setNamespaceService(NamespaceService namespaceService) {
		this.namespaceService = namespaceService;
	}
	
	public void setServiceRegistry(ServiceRegistry serviceRegistry){
		this.serviceRegistry = serviceRegistry;
	}
	
	public void setProperties(Properties properties){
		this.globalProperties = properties;
	}
	
	public void setHierarchicalResourceLoader(
			HierarchicalResourceLoader hierarchicalResourceLoader) {
		this.hierarchicalResourceLoader = hierarchicalResourceLoader;
	}
	
	public void setConfigLocation(String configLocation){
		this.configLocation = configLocation;
	}
}
