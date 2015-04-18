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

import org.alfresco.service.cmr.repository.NodeRef;

public class ReportingContainer {
	private NodeRef nodeRef;
	private boolean executionEnabled;
	private String executionFrequency;
	private String name;
	
//	private static Log logger = LogFactory.getLog(ReportingContainer.class);
	
	public ReportingContainer(NodeRef reportingContainerRef){
		this.nodeRef = reportingContainerRef;		

	}
	
	public void setName(String name){
		this.name=name;
	}
	
	public String getName(){
		return name;
	}

	/**
	 * @return the nodeRef
	 */
	public NodeRef getNodeRef() {
		return nodeRef;
	}

	
	//public void setTargetNode(NodeRef target) {
	//	targetNode=target;
	//}
	
	/**
	 * @return the targetNode
	 */
	//public NodeRef getTargetNode() {
	//	return targetNode;
	//}

	public void setExecutionEnabled(boolean execEnabled) {
		executionEnabled = execEnabled;
	}
	/**
	 * @return the executionEnabled
	 */
	public boolean isExecutionEnabled() {
		return executionEnabled;
	}

	public void setExecutionFrequency(String frequency) {
		executionFrequency = frequency;
	}
	
	/**
	 * @return the executionFrequency
	 */
	public String getExecutionFrequency() {
		return executionFrequency;
	}

	/**
	 * @return the targetPath
	 */
	//public String getTargetPath() {
	//	return targetPath;
	//}

/*
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
*/
	
	//public void setSubstitution(String substitution) {
	//	this.substitution= substitution ;
	//}

	
	/**
	 * @return the substitution
	 */
	/*
	public Properties getSubstitution() {
		Properties keyValues = new Properties();
		if ((substitution!=null) || (substitution.length()==0)){
			String[] singleKeyValue = substitution.split(",");
			String key;
			for (String line : singleKeyValue){
				try{
					key = line.split("=")[0];
					keyValues.setProperty(key, line.split("=")[1]);
				} catch (Exception e) {
					logger.fatal("HELP! Processing parameter " + line + " failed!");
				}
			}
		}
		return keyValues;
	}
	*/

}
