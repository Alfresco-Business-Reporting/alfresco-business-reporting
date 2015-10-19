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

package org.alfresco.reporting.action.executer;

import java.util.List;
import java.util.Properties;

import javax.transaction.SystemException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.reporting.Constants;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.reporting.execution.JasperReporting;
import org.alfresco.reporting.execution.PentahoReporting;
import org.alfresco.reporting.execution.Reportable;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ReportExecuter extends ActionExecuterAbstractBase {
	
	public static final String NAME = "report-executer";
	public static final String PARAM_FREQUENCY = "frequency";
	public static final String TARGET_DOCUMENT = "targetDocument";
	public static final String OUTPUT_TYPE = "outputType";
	public static final String SEPARATOR = "seperator";
	public static final String PARAM_1 = "param1";
	public static final String PARAM_2 = "param2";
	public static final String PARAM_3 = "param3";
	public static final String PARAM_4 = "param4";
	private Properties globalProperties;
	private ServiceRegistry serviceRegistry;
	private DatabaseHelperBean dbhb = null;
	private String jndiName; // needed to remember the JNDI name
	
	private static Log logger = LogFactory.getLog(ReportExecuter.class);

	
	@Override
	protected void executeImpl(final Action action, final NodeRef reportDefNodeRef) {
		try{
			NodeRef outputNodeRef = (NodeRef)action.getParameterValue(TARGET_DOCUMENT);
			String outputType  = (String)action.getParameterValue(OUTPUT_TYPE);
			
			processReport(reportDefNodeRef, outputNodeRef, outputType, action);
		} catch (Exception e) {
			// prevent report execution to crash if a single report fails
			String reportName = serviceRegistry.getNodeService().getProperty(
					reportDefNodeRef, 
					ContentModel.PROP_NAME).toString();
			logger.fatal("Report execution failed! ReportDef=" +reportName+ " Noderef=" + reportDefNodeRef);
			e.printStackTrace();
			
		}
	}

	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(
				new ParameterDefinitionImpl(        // Create a new parameter definition to add to the list
					TARGET_DOCUMENT,                // The name used to identify the parameter
					DataTypeDefinition.NODE_REF,    // The parameter value type
					true,                           // The parameter is mandatory
					getParamDisplayLabel(TARGET_DOCUMENT)));	
		paramList.add(
				new ParameterDefinitionImpl(        // Create a new parameter definition to add to the list
					OUTPUT_TYPE,                    // The name used to identify the parameter
					DataTypeDefinition.TEXT,        // The parameter value type
					true,                           // The parameter is mandatory
					getParamDisplayLabel(OUTPUT_TYPE)));	
		paramList.add(
				new ParameterDefinitionImpl(                // Create a new parameter definition to add to the list
					SEPARATOR,                              // The name used to identify the parameter
					DataTypeDefinition.TEXT,               // The parameter value type
					false,                                   // Indicates whether the parameter is mandatory
					getParamDisplayLabel(SEPARATOR)));	
		paramList.add(
				new ParameterDefinitionImpl(                // Create a new parameter definition to add to the list
					PARAM_1,                              // The name used to identify the parameter
					DataTypeDefinition.TEXT,               // The parameter value type
					false,                                   // Indicates whether the parameter is mandatory
					getParamDisplayLabel(PARAM_1)));	
		paramList.add(
				new ParameterDefinitionImpl(                // Create a new parameter definition to add to the list
					PARAM_2,                              // The name used to identify the parameter
					DataTypeDefinition.TEXT,               // The parameter value type
					false,                                   // Indicates whether the parameter is mandatory
					getParamDisplayLabel(PARAM_2)));
		paramList.add(
				new ParameterDefinitionImpl(                // Create a new parameter definition to add to the list
					PARAM_3,                              // The name used to identify the parameter
					DataTypeDefinition.TEXT,               // The parameter value type
					false,                                   // Indicates whether the parameter is mandatory
					getParamDisplayLabel(PARAM_3)));	
		paramList.add(
				new ParameterDefinitionImpl(                // Create a new parameter definition to add to the list
					PARAM_4,                              // The name used to identify the parameter
					DataTypeDefinition.TEXT,               // The parameter value type
					false,                                   // Indicates whether the parameter is mandatory
					getParamDisplayLabel(PARAM_4)));	
		
	}

	// ----------------------------------------------------------------------------
	
	public void setProperties(Properties properties){
		this.globalProperties = properties;
		this.jndiName = properties.getProperty(Constants.property_jndiName);
	}
	
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
	
	public void setDatabaseHelperBean(DatabaseHelperBean databaseHelperBean) {
		this.dbhb = databaseHelperBean;
	}
	
	// ----------------------------------------------------------------------------
	
	private boolean isExecutionEnabled(){
	   	boolean enabled = true;
	   	try{
	    		enabled = this.globalProperties.getProperty("reporting.execution.enabled", "true").equalsIgnoreCase("true");
		} catch (Exception e) {
			logger.debug("isExecutionEnabled() returning exception. Thus returning true;");
			logger.debug(e);
			enabled = true;
		} 
	   	return enabled;
	}
	   
    /**
     * processReport executes a JasperReports/iReport Report
     * @param jrxmlNodeRef the noderef containing the report definition
     * @param outputNodeRef the noderef containing the resulting report (pdf, doc...)
     * @param outputType the type of report to generate [pdf, html, doc, xls]
     */
    public void processReport(NodeRef inputNodeRef, NodeRef outputNodeRef, String outputType, Action action){
    	logger.debug("starting ProcessReport generating a " + outputType);
    	try{
    		jndiName=globalProperties.getProperty("reporting.db.jndiName","");
	    	if (isExecutionEnabled()){
		    	String name = serviceRegistry.getNodeService().getProperty(inputNodeRef, ContentModel.PROP_NAME).toString();
		    	Reportable reportable = null;
		    	if (name.toLowerCase().endsWith(".jrxml") || name.toLowerCase().endsWith(".jasper")){
		    		logger.debug("It is a Jasper thingy!");
		    		reportable = new JasperReporting();
		       	}
		    	if (name.endsWith(PentahoReporting.EXTENSION)){
		    		logger.debug("It is a Pentaho thingy!");
		    		reportable = new PentahoReporting();
		    	}
		    	if (reportable!= null){
		    		reportable.setGlobalProperties(globalProperties);
		    		reportable.setDatabaseHelper(dbhb);
		    		//reportable.setUsername(dbhb.getUsername());
		    		//reportable.setPassword(dbhb.getPassword());
		    		//reportable.setDriver(dbhb.getJdbcDriver());
		    		//reportable.setUrl(dbhb.getDatabase()); 
		    		//reportable.setConnection(dbhb.getConnection()); // NEVER set this connection. We need no conn!
					reportable.setServiceRegistry(serviceRegistry);
					reportable.setReportDefinition(inputNodeRef);
					reportable.setOutputFormat(outputType);
					reportable.setResultObject(outputNodeRef);
					reportable.setMimetype(outputType);
					
					String separator   = (String)action.getParameterValue(SEPARATOR);
	
					if ((separator!=null) && !separator.equals("")){
						String param1   = (String)action.getParameterValue(PARAM_1);
						String param2   = (String)action.getParameterValue(PARAM_2);
						String param3   = (String)action.getParameterValue(PARAM_3);
						String param4   = (String)action.getParameterValue(PARAM_4);
						String key="";
						String value="";
						if ((param1!=null) && !param1.equals("")){
							if (param1.indexOf(separator)>1){
								key = param1.split(separator)[0];
								value = param1.split(separator)[1];
								reportable.setParameter(key,value);
								logger.debug("1Setting: " +key + "=" + value);
							}
						}
						if ((param2!=null) && !param2.equals("")){
							if (param2.indexOf(separator)>1){
								key = param2.split(separator)[0];
								value = param2.split(separator)[1];
								reportable.setParameter(key,value);
								logger.debug("2Setting: " +key + "=" + value);
							}
						}
						if ((param3!=null) && !param3.equals("")){
							if (param3.indexOf(separator)>1){
								key = param3.split(separator)[0];
								value = param3.split(separator)[1];
								reportable.setParameter(key,value);
								logger.debug("3Setting: " +key + "=" + value);
							}
						}
						if ((param4!=null) && !param4.equals("")){
							if (param4.indexOf(separator)>1){
								key = param4.split(separator)[0];
								value = param4.split(separator)[1];
								reportable.setParameter(key,value);
								logger.debug("4Setting: " +key + "=" + value);
							}
						}
					}
					
					logger.debug("Lets go processReport!");
					reportable.processReport();
		    	} else {
		    		logger.error(name + " is not a valid report definition");
		    	}
	    	} else {
	    		logger.warn("Alfresco Business Reporting is NOT enabled...");
	    	}
    	} catch (SystemException se){
    		se.printStackTrace();
    	}
    }
}
