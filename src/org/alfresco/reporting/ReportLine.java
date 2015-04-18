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
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

import org.alfresco.repo.rule.ReorderRules;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ReportLine {

	private Properties types = new Properties();
	private Properties values = new Properties();
	private String table = ""; 
	private String vendor;
	private ReportingHelper reportingHelper;
	
	
	
	private final SimpleDateFormat sdf; // = Constants.getSimpleDateFormat();
	private static Log logger = LogFactory.getLog(ReportLine.class);
	
	public ReportLine(	final String table, 
						final SimpleDateFormat sdf, 
						final ReportingHelper reportingHelper){
		
		this.vendor =reportingHelper.getDatabaseProvider();
		this.reportingHelper=reportingHelper;
		
		String myTable = table.trim();
		// An Oracle column can be varchar(30) at max....
		if (Constants.VENDOR_ORACLE.equalsIgnoreCase(vendor)){
			if (myTable.length()>30)
				myTable=myTable.substring(0, 30);
		}
		
		setTable(table);
		this.sdf = sdf;
		//logger.debug("$$$ VENDOR=" + this.vendor);
	}
	
	public void reset(){
		types  = new Properties();
		values = new Properties();
	}
	
	public void setLine(	final String key, 
							final String sqltype, String value, 
							final Properties replacementTypes){
		
		// An Oracle column can be varchar(30) at max....
//		if (logger.isDebugEnabled()
//			logger.debug("setLine: entry: key=" + key + ", type="+ sqltype + ", value=" + value);
		
		if ((value==null) 
				|| value.equalsIgnoreCase("null")
				|| value.equalsIgnoreCase("~~null~~")
				|| key.equals("isLatest")){
			logger.debug("setLine *************** NULL ****************");
			return; // do not process null values, they potentially fail
			//value="~~NULL~~";
		}
		if ((key!=null) && (sqltype!=null)){
			String myKey = key; // .toLowerCase();
			String mySqltype = sqltype.trim();
			value= value.trim();
			
			// truncate the name if needed depending on DB provider
			myKey = reportingHelper.getTableColumnNameTruncated(myKey);
			
			// if Oracle, replace key "size" by key "docsize"
			// size is a reserved word in Oracle...
			if (Constants.VENDOR_ORACLE.equals(vendor)){
	
				if (myKey.equalsIgnoreCase(Constants.COLUMN_SIZE)){
					myKey=Constants.COLUMN_SIZE_ORACLE.toLowerCase();
				}
			}
			
//			if (logger.isDebugEnabled()			
//				logger.debug("setLine: stuff not null type="+ mySqltype + ", value=" + value);
			
			// validate if the current definition is overridden...
			if (replacementTypes.containsKey(myKey)){
				mySqltype = replacementTypes.getProperty(myKey, "-").trim();
			}
			
//			if (logger.isDebugEnabled() 
//				logger.debug("setLine: after replacement, sqltype="+mySqltype);
			
			if (mySqltype.toUpperCase().startsWith("VARCHAR")){
				try{
					if (logger.isDebugEnabled()){
//						logger.debug("setLine: Inside VARCHAR " + mySqltype  + ", " + mySqltype.indexOf("("));
//						logger.debug("setLine: Inside VARCHAR " + mySqltype  + ", " + (mySqltype.length()-1));
					}
					int length = Integer.parseInt(mySqltype.substring(mySqltype.indexOf("(")+1,mySqltype.length()-1));
					if (value.length()>length){
						value = value.substring(0,length-2);
					} else {
						//logger.debug("setLine: length is fine...");
					}
				} catch (Exception e){
					e.printStackTrace();
					logger.fatal("Error in processing VARCHAR!!");
					logger.fatal(e.getMessage());
				}
			}
			types.setProperty(myKey, mySqltype);
			values.setProperty(myKey, value);
		}
//		if (logger.isDebugEnabled()
//			logger.debug("setLine: exit: Type=" + sqltype + ", value=" + value);
	}
	
	public void setTable(String table){
		this.table = table.toLowerCase().replaceAll("-", "_").replaceAll(" ", "_");
	}
	
	public String getType(final String key){
		
		String type=key;
		//type = types.getProperty(key);
		if (type.equalsIgnoreCase(Constants.COLUMN_SIZE)
				&& vendor.equalsIgnoreCase(Constants.VENDOR_ORACLE)){
			type = Constants.COLUMN_SIZE_ORACLE;
		}
		return types.getProperty(type);
		
		//return types.getProperty(key);
	}
	
	public boolean hasValue(String key){
		String returnString = values.getProperty(key);
		return (returnString != null);
	}
	
	public String getValue(final String key){
		String returnString;
		// insert a hack to return Archived date... 
		// ...if modified date is asked for and item appears archived
		if (key.equalsIgnoreCase(Constants.KEY_MODIFIED) 
				&& (values.getProperty(Constants.KEY_ARCHIVED_DATE)!=null)){
			returnString = values.getProperty(Constants.KEY_ARCHIVED_DATE);
		} else {
			returnString = values.getProperty(key);
		}
		
		if (returnString!=null){
			returnString = returnString.replaceAll("'", "_").replaceAll("\"","_");
		}
		return returnString;
	}
	
	public int size(){
		return types.size();
	}
	
	@SuppressWarnings("rawtypes")
	public Enumeration getKeys(){
		return types.keys();
	}
	
	public String getTable(){
		return table;
	}
	
	/**
	 * generate "a=1, b=2, c=3"
	 * @return String of set
	 */
	public String getUpdateSet(){
		String returnString = "";
		String ignoreKey=Constants.KEY_NODE_UUID; // comma seperated list of values. sys:node-uuid is key that is used to check if the row already exists
		@SuppressWarnings("unchecked")
		Enumeration<String> keys = getKeys();
		while (keys.hasMoreElements()){
			String key = (String)keys.nextElement();
			if (ignoreKey.indexOf(key)<0){
				if (returnString!=""){returnString+=", ";}
				String value = getValue(key);
				String type  = getType(key);
				if (Constants.VENDOR_ORACLE.equalsIgnoreCase(vendor)){
					returnString += key.toLowerCase() + "="+ formatValue(type, value);
				} else {
					returnString += key + "="+ formatValue(type, value);
				}
			}
		}
		if (logger.isDebugEnabled())
			logger.debug("SQL-update:"+returnString);
		return returnString;
	}
	
	private String formatValue(String type, String value){
		String returnString = "''";
		boolean wasBoolean=false;
		
		if ("~~NULL~~".equals(value) || "-".equals(value)){
			returnString="NULL";
		} else {
			logger.debug("formatValue: type=" + type + " -was- " + value + " & vendor=" + vendor );
			if (Constants.VENDOR_ORACLE.equalsIgnoreCase(vendor) && "NUMBER(1)".equalsIgnoreCase(type)){
				if ("true".equalsIgnoreCase(value)){
					returnString="1"; // TRUE
				} else {
					returnString="0"; // FALSE
				}
				return returnString;
				//wasBoolean=true;
			}
			if (Constants.VENDOR_POSTGRES.equalsIgnoreCase(vendor) && "SMALLINT".equalsIgnoreCase(type)){
				if ("true".equalsIgnoreCase(value)){
					returnString="1"; // TRUE
				} else {
					returnString="0"; // FALSE
				}
				return returnString;
				//wasBoolean=true;
			}
			if (Constants.VENDOR_MYSQL.equalsIgnoreCase(vendor) && "TINYINT".equalsIgnoreCase(type)){
				if ("true".equalsIgnoreCase(value)){
					returnString="1"; // TRUE
				} else {
					returnString="0"; // FALSE
				}
				return returnString;
				//wasBoolean=true;
			}
			
			String compareType="";
			// the sql type can contain a ( in the name, killing the logic. So, first strip it
			if (type.indexOf("(")>0){
				compareType = type.substring(0, type.indexOf("("));
			} else {
				compareType = type;
			}
//			if (logger.isDebugEnabled())
//				logger.debug("formatValue: type=" + compareType + " -was- " + type );
			if ((",BIGINT,BOOLEAN,NUMBER(,INTEGER,DOUBLE PRECISION,BINARY_DOUBLE,BINARY_FLOAT,PLS_INTEGER,LONG".indexOf(compareType.toUpperCase())>-1)
					&& !wasBoolean){
				return value;
				
			} else {
				if (	   "DATETIME".equalsIgnoreCase(type) 
						|| "DATE".equalsIgnoreCase(type) 
						|| "TIMESTAMP".equalsIgnoreCase(type)){
					//logger.debug("POSTGRES: We're in Else");
					// If we are talking Oracle, we dont have an ISO date, 
					// but a number of Seconds TIMESTAMP. Therefore, 
					// transform the ISO date to TIMESTAMP
					if (Constants.VENDOR_ORACLE.equalsIgnoreCase(vendor)){
						value = value.replaceAll("T", " ");
						return "TO_DATE('" + value + "','YYYY-MM-DD HH24:MI:SS')";
					} else {

						try{
							Date myDate  = new Date(Long.parseLong(value));
							returnString =  "'" + sdf.format(myDate).replace(" ","T")+"'";
						} catch (Exception e) {
							returnString =  "'"+value+"'";	
						}
						
					}
				} else {
					if (!wasBoolean){
						// if booleans already trapped, don't go here 
						logger.debug("It is a String/VARCHAR");
						return  "'"+value+"'";
					} // end if !wasBoolean
				}
			}
		} // end else value==null
		
		if (!wasBoolean && !type.contains("VARCHAR") && "".equals(value)){
			returnString="NULL";
		}
		return returnString;
	}
	

	public String getInsertListOfKeys(){
		String returnString = "";
		String key = "";
		@SuppressWarnings("unchecked")
		Enumeration<String> keys = getKeys();
		while (keys.hasMoreElements()){
			if (returnString!=""){returnString+=", ";}
			key = keys.nextElement();
			if (Constants.VENDOR_ORACLE.equalsIgnoreCase(vendor)){
				returnString += key.toLowerCase();
			} else {
				returnString += key;
			}
		}
		if (logger.isDebugEnabled())
			logger.debug("SQL-keys :"+returnString);
		return returnString;
	}
	
	public String getInsertListOfValues(){
		String returnString = "";
		@SuppressWarnings("rawtypes")
		Enumeration keys = getKeys();
		while (keys.hasMoreElements()){
			String key = (String)keys.nextElement();
			
			if (returnString!=""){returnString+=", ";}
			String value = getValue(key);
			String type  = getType(key);
			returnString += formatValue(type, value);
		}
		if (logger.isDebugEnabled())
			logger.debug("SQL-values:"+returnString);
		return returnString;
	}
}
