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

package org.alfresco.reporting.constraints;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.alfresco.repo.dictionary.constraint.ListOfValuesConstraint;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExecutionFrequencyListConstraint extends ListOfValuesConstraint {

	private static Log logger = LogFactory.getLog(ExecutionFrequencyListConstraint.class);

	private static List<String> listOfFrequencies=null;
	
	private Properties globalProperties;
	
	final String PROPERTY_PREFIX="reporting.execution.frequency.";
	
	
	
	public ExecutionFrequencyListConstraint() {
		super();
		
		logger.debug("exit ExecutionFrequencyListConstraint");
	}
	
	
	/* read from alfresco-global.properties all properties. Filter against
	 * reporting.execution.frequency.  use the latter part for the naming
	 * In order of Global Properties
	 */
	/*
	public void initialize(){
		super.initialize();
		if (logger.isDebugEnabled())
				logger.debug("enter initialize");
		List<String> myListOfValues = getDummyList();
		//setListOfFrequencies(myListOfValues);
		super.setAllowedValues(myListOfValues);
		if (logger.isDebugEnabled())
				logger.debug("exit initialize" );
		
	}
	*/
	
	private List<String> getDummyList(){
		List<String> allowedValues = new ArrayList<String>();
		allowedValues.add("1");
		allowedValues.add("2");
		return allowedValues;
	}
	
	private List<String> getList(){
		if (logger.isDebugEnabled())
			logger.debug("enter getList");
		List<String> myListOfValues = new ArrayList<String>();
		
			String key="";
			if (getProperties()==null){
				logger.debug("Someone killed the Properties!!");
			}
			Enumeration keys = getProperties().keys();
		
			if (logger.isDebugEnabled())
				logger.debug("#keys="+ getProperties().size());
			while (keys.hasMoreElements()){
				key = keys.nextElement().toString();
				try{
					if  (key.startsWith(PROPERTY_PREFIX)){
						String value = key.substring(PROPERTY_PREFIX.length(), key.length());
						if (logger.isDebugEnabled())
								logger.debug("Found value: " + value);
						myListOfValues.add(value);
					} // end if
				} catch (Exception e) {
					// be tolerant. We are looking for an unknown null-pointer??
				}
			} // end while
			if (myListOfValues.isEmpty()){
				myListOfValues.add("-unexpectedly empty-");
			}
		
		if (logger.isDebugEnabled())
				logger.debug("exit getList" );
		return myListOfValues;
	
	}
	
	
	@Override
	public List<String> getAllowedValues() {
		if (logger.isDebugEnabled())
			logger.debug("enter getAllowedValues");
		List<String> allowedValues = new ArrayList<String>();
		
		allowedValues = getDummyList();
		super.setAllowedValues(allowedValues);
		
		logger.debug("getAllowedValues: before super: " + allowedValues.size());
		
		if (logger.isDebugEnabled())
				logger.debug("getAllowedValues returning: " + allowedValues.size());
		
		return allowedValues;
	}
		
	/*
	protected List<String> getListOfFrequencies(){
		if (logger.isDebugEnabled())
			logger.debug("getListOfFrequencies" );
		return listOfFrequencies;
	}
	
	private void setListOfFrequencies(List<String> list){
		logger.debug("setListOfFrequencies: Setting "  + list.size() + " values");
		listOfFrequencies = list;
	}
	*/
	
	public void setProperties(Properties properties){
    	this.globalProperties = properties;
    }
	
	private Properties getProperties(){
		return this.globalProperties;
	}

}
