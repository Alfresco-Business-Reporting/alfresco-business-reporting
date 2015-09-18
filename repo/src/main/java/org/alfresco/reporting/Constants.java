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

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class Constants {
	// three methods of using the reporting database
	public final static String INSERT_ONLY = "INSERT_ONLY";
	public final static String SINGLE_INSTANCE = "SINGLE_INSTANCE";
	public final static String UPDATE_VERSIONED = "UPDATE_VERSIONED";
	public final static String QUERYLANGUAGE = "Lucene";
	public final static String SEPARATOR = "~";
	
	public final static String MORE_FREQUENT = "moreFrequent";
	public final static String LESS_FREQUENT = "lessFrequent";
	public final static String ALL = "all";
	
	public final static String FREQUENCY_HOURLY = "hourly";
	public final static String FREQUENCY_DAYLY = "daily";
	public final static String FREQUENCY_WEEKLY = "weekly";
	public final static String FREQUENCY_MONTHLY = "monthly";
	
	public final static String property_jndiName = "reporting.db.jndiName";
	public final static String property_audit_maxResults = "reporting.harvest.audit.maxResults";
	public final static String property_noderef_maxResults = "reporting.harvest.noderef.maxResults";
	public final static String property_blacklist = "reporting.harvest.blacklist"; // the keys that -can- be blocked
	public final static String property_blockkeys = "reporting.harvest.blockkeys"; // the keys that *must* be blocked by definition
	public final static String property_blockNameSpaces = "reporting.harvest.blockNameSpaces"; // the keys that *must* be blocked by definition
	public final static String property_storelist = "reporting.harvest.stores";
	public final static String property_invalid_table_names = "reporting.harvest.invalidTableNames";
	public final static String property_invalid_table_chars = "reporting.harvest.invalidTableChars";
	public final static String property_treshold_child_assocs = "reporting.harvest.treshold.child.assocs"; // if above this treshold, the child nodes will not be harvested
	public final static String property_treshold_soucetarget_assocs = "reporting.harvest.treshold.sourcetarget.assocs"; // if above this treshold, the assoc (source/target) nodes will not be harvested
	public final static String property_harvesting_enabled = "reporting.harvest.enabled";
	public final static String property_execution_enabled = "reporting.harvest.enabled";
//	public final static String property_harvesting_maxBatchCount = "reporting.harvest.maxBatchCount";
//	public final static String property_harvesting_maxBatchSize = "reporting.harvest.maxBatchSize";
//	public final static String property_harvesting_batchTimestampEnabled = "reporting.harvest.batchTimestamp.enabled";
	public final static String property_resetStatusAtStartupEnabled = "reporting.resetStatusAtStartup.enabled";
	
	public final static String PROPERTY_HARVEST_DOCUMENT_TABLES="reporting.harvest.document.tables";
	public final static String PROPERTY_HARVEST_DOCUMENT_TYPE="reporting.harvest.document.type";
	public final static String PROPERTY_HARVEST_FOLDER_TABLES="reporting.harvest.folder.tables";
	public final static String PROPERTY_HARVEST_FOLDER_TYPE="reporting.harvest.folder.type";
	public final static String PROPERTY_HARVEST_ARCHIVE_INDEXED="reporting.harvest.archive.indexed";
	
	public final static String TABLE_PERSON = "person";
	public final static String TABLE_GROUPS = "groups";
	public final static String TABLE_SITEPERSON = "siteperson";
	public final static String TABLE_WOKFLOW_INSTANCE = "workflowinstance";
	public final static String TABLE_WOKFLOW_TASK = "workflowtask";
	
	public final static String COLUMN_ASPECTS= "aspects";
	public final static String COLUMN_ORIG_NODEREF= "orig_noderef";
	public final static String COLUMN_MIMETYPE= "mimetype";
	public final static String COLUMN_NODEREF= "noderef";
	public final static String COLUMN_BJECT_TYPE= "object_type";
	public final static String COLUMN_SITE= "site";
	public final static String COLUMN_PATH= "path";
	public final static String COLUMN_TIMESTAMP = "event_timestamp"; // Audit framework
	public final static String COLUMN_USERNAME = "username"; // Audit framework
	public final static String COLUMN_SYS_NODE_UUID = "sys_node_uuid"; // Audit framework
	public final static String COLUMN_ZONES = "zones";
	public final static String COLUMN_OBJECT_TYPE = "object_type";
	
	public final static String COLUMN_SIZE = "size"; 
	public final static String COLUMN_SIZE_ORACLE = "docsize";
	public final static int HARVESTING_SAFETY_MARGIN=5000; // 5 seconds
	
	public final static int MAX_COLUMNNAME_LENGTH_ORACLE = 30;
	public final static int MAX_COLUMNNAME_LENGTH_POSTGRES = 64;
	public final static int MAX_COLUMNNAME_LENGTH_MYSQL = 64;
	
	public final static String VENDOR_ORACLE="Oracle";
	public final static String VENDOR_MYSQL = "MySQL";
	public final static String VENDOR_POSTGRES = "PostgreSQL";
	public final static String VENDOR_MSSQL = "not implemented";
	
	public final static String KEY_MODIFIED	= "cm_modified";
	public final static String KEY_CREATED 	= "cm_created";
	public final static String KEY_VERSION_LABEL 	= "cm_versionlabel";
	public final static String KEY_ARCHIVED_DATE	= "sys_archiveddate";
	public final static String KEY_NODE_UUID		= "sys_node_uuid";
	public final static String KEY_STORE_PROTOCOL	= "sys_store_protocol";
	
	
	
	
	private ReportingHelper reportingHelper;
	
	//location of configuration files
	/**
	 * Properties file containing mapping of Alfresco types like text, noderef, date
	 * into SQL types.  This file is shipped with the reporting tool (and mandatory). 
	 * 
	 * Sample:
	 * text=VARCHAR(500)
	 * noderef=VARCHAR(100)
	 * date=DATE
	 * datetime=TIMESTAMP
	 */
	public final static String REPORTING_PROPERTIES = "alfresco/module/org.alfresco.reporting/reporting-model.properties";
	
	/**
	 * Properties file containing custom mapping of named properties (short notation) 
	 * into SQL datatypes/column definitions. The rough mapping maps each Alfresc text 
	 * field into say VARCHAR(500). There are some properties that are known to always 
	 * fit in smaller or bigger sizes.
	 * 
	 * Applicable to text based properties only (or properties that map on VARCHAR() columns). 
	 * Purpose: work around the max length of a database row. Expectation MySQL: 64.000 bytes 
	 * devided by 4 (max number of bytes needed to represent any UTF8 character). Using large sets
	 * of custom properties in a single reporting table can cause problems if all default values are used. 
	 * 
	 *  Sample:
	 *  cm_versionLabel=VARCHAR(10)
	 *  child_noderef=VARCHAR(1000)
	 *  
	 */
	public final static String REPORTING_CUSTOM_PROPERTIES = "alfresco/extension/reporting-custom-model.properties";
	
	
	/**
	 * character to be used when concatinating multivalue property values 
	 * into one string-like representation in the reporting database
	 */
	public final static String MULTIVALUE_SEPERATOR = ",";	
	
	/**
	 * Date format to be used in the reporting database so it is actually stored as a Date
	 */
	public static String DATE_FORMAT_AUDIT = "yyyy-MM-dd HH:mm:ss";
	public final static String DATE_FORMAT_MYSQL      = "yyyy-MM-dd HH:mm:ss.SSS";
	public final static String DATE_FORMAT_POSTGRESQL = "yyyy-MM-dd HH:mm:ss.SSS";
	public final static String DATE_FORMAT_ORACLE     = "yyyy-MM-dd HH:mm:ss";
	public final static String DATE_FORMAT_MSSQL = "yyyy-MM-dd HH:mm:ss.SSS";
	
	// contants for managing the last-successful-run table
	
	/**
	 * status if fillReporting-script is running
	 */
	public final static String STATUS_RUNNING = "Running";
	
	/**
	 * status if fillReportingDatabase is idle
	 */
	public final static String STATUS_DONE = "Done";	
	
	/**
	 * table name
	 */
	public final static String TABLE_LASTRUN = "lastsuccessfulrun";
	
	/**
	 * column name, contains timestamp in DATE_FORMAT_DATABASE
	 */
	public final static String COLUMN_LASTRUN = "lastrun"; 
	
	/**
	 * column name, contains status: Running | Done;
	 */
	public final static String COLUMN_STATUS = "status";  
	
	public final static String COLUMN_TABLENAME = "tablename";
	
	
	//TODO remove this method in favour of more intelligent one in ReportingHelper
	public static SimpleDateFormat getAuditDateFormat(){
		//String vendor = reportingHelper.getDatabaseProvider();
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_AUDIT);// "yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getDefault());
		return sdf;
	}


}
