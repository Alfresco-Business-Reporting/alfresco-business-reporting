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

package org.alfresco.reporting.execution;

import java.util.Properties;

import org.alfresco.model.ContentModel;
import org.alfresco.reporting.ReportingModel;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ReportTemplate {
	private NodeRef nodeRef;
	private String  outputFormat;
	private boolean outputVersioned;
	private boolean reportingDocument;
	private NodeRef targetNode;
	private String targetPath;
	private String substitution;
	private String  name;
	
	private static Log logger = LogFactory.getLog(ReportTemplate.class);
	
	/**
	 * The one and only constructor to create this class
	 * @param reportRef
	 */
	public ReportTemplate(NodeRef reportRef){
		this.nodeRef = reportRef;
			
		//this.outputFormat = (String)nodeService.getProperty(reportRef, ReportingModel.PROP_REPORTING_REPORTING_FORMAT);
		//this.outputVersioned = (Boolean)nodeService.getProperty(reportRef, ReportingModel.PROP_REPORTING_REPORTING_VERSIONED);
	}
	
	public void setName(String name){
		this.name=name;
	}
	
	public String getName(){
		return name;
	}
	
	
	public void setReportingDocument(boolean reportingDoc){
		this.reportingDocument=reportingDoc;
	}
	
	public boolean isReportingDocument(){
		return reportingDocument;
	}
	
	public void setTargetNode(NodeRef target) {
		targetNode=target;
	}
	
	/**
	 * @return the targetNode 
	 */
	public NodeRef getTargetNode() {
		return targetNode;
	}
	
	/**
	 * @return the targetPath
	 */
	public String getTargetPath() {
		return targetPath;
	}


	public void setTargetPath(String rawTargetPath){
		try{
			//returnPath =  (String)nodeService.getProperty(docRef, ReportingModel.PROP_REPORTING_TARGET_PATH);
			while (rawTargetPath.indexOf("\\")>-1){
				rawTargetPath = rawTargetPath.substring(0,rawTargetPath.indexOf("\\")) 
						+ "/" 
						+ rawTargetPath.substring(rawTargetPath.indexOf("\\")+1); 
		} // end while
		} catch (Exception e){
			// ignore, be tolerant
		}
		targetPath = rawTargetPath;
	}

	
	public void setSubstitution(String substitution) {
		this.substitution= substitution ;
	}

	
	/**
	 * @return the substitution
	 */
	public Properties getSubstitution() {
		Properties keyValues = new Properties();
		if ((substitution!=null) && (substitution.length()>0)){
			String[] singleKeyValue = substitution.split(",");
			String key;
			for (String line : singleKeyValue){
				logger.debug("getSubstitution: Processing "+ line);
				if (line.contains("=")){
					try{
						key = line.split("=")[0];
						keyValues.setProperty(key, line.split("=")[1]);
					} catch (Exception e) {
						logger.error("HELP! Processing parameter " + line + " failed!");
					}
				} // end if line contains ("=")
			}
		}
		return keyValues;
	}

	
	/**
	 * @return the nodeRef
	 */
	public NodeRef getNodeRef() {
		return nodeRef;
	}
	
	/**
	 * 
	 * @param outputFormat
	 */
	public void setOutputFormat(String outputFormat) {
		this.outputFormat=outputFormat;
	}
	
	/**
	 * @return the outputFormat
	 */
	public String getOutputFormat() {
		return outputFormat;
	}
	
	/**
	 * 
	 * @param versioned
	 */
	public void setOutputVersioned(boolean versioned) {
		this.outputVersioned=versioned;
	}
	
	/**
	 * @return the outputVersioned
	 */
	public boolean isOutputVersioned() {
		return outputVersioned;
	}
		
}
