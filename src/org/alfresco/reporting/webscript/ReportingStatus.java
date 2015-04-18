/**
 * Copyright (C) 2011 - 2014 Alfresco Business Reporting project
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
 **/

package org.alfresco.reporting.webscript;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import org.alfresco.reporting.Constants;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class ReportingStatus extends AbstractWebScript {

	private DatabaseHelperBean dbhb = null;
	private static Log logger = LogFactory.getLog(ReportingStatus.class);
	
	
	
	private String postFix(String base, final int size, final String filler){
		while (base.length()<size){
			base += filler;
		}
		return base;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void execute(WebScriptRequest arg0, WebScriptResponse pResponse)
			throws IOException {
		
		
		try {
			final Map<String, String> tables = dbhb.getShowTablesDetails2();
			
			final Iterator<String> keys = new TreeSet<String> ( tables.keySet() ).iterator();
			final JSONObject mainObject = new JSONObject();
			final JSONArray mainArray = new JSONArray();

			while (keys.hasNext()){
				String key = (String)keys.next();
				
				if (!Constants.TABLE_LASTRUN.equalsIgnoreCase(key) 
							&& key!=null 
							&& !"".equals(key)){
					key = key.trim();
					JSONObject rowObject = new JSONObject();
					rowObject.put("table", postFix(key, 5," "));

					logger.debug("Getting key=" + key);
					String keyList = tables.get(key);
					logger.debug("Getting values="+ keyList);
					try{
						String[] t = keyList.split(",");
						int tSize = t.length; 
						if (tSize>0) rowObject.put("last_run_w", (String)t[0]);
						if (tSize>1) rowObject.put("last_run_a", (String)t[1]);
						if (tSize>2) rowObject.put("status", (String)t[2]);
						if (tSize>3) rowObject.put("number_of_rows", (String)t[3]);
						
						
						/*
						if (tSize>0) rowObject.put("last_run", (String)t[0]);
						if (tSize>1) rowObject.put("status", (String)t[1]);
						if (tSize>2) rowObject.put("number_of_rows", (String)t[2]);
						if (tSize>3) rowObject.put("number_of_latest", (String)t[3]);
						if (tSize>4) rowObject.put("number_of_non_latest", (String)t[4]);
						if (tSize>5) rowObject.put("number_in_workspace", (String)t[5]);
						if (tSize>6) rowObject.put("number_in_archivespace", (String)t[6]);
						*/
					} catch (Exception e) {
						logger.fatal(e.getMessage());
					}
					mainArray.add(rowObject);
				} // end if exclude table lastsuccessfulrun
			} // end while
			mainObject.put("result", mainArray);
			pResponse.getWriter().write(mainObject.toJSONString());
		} catch (IOException e) {
			e.printStackTrace();
		} 


	}
	
	
	public void setDatabaseHelperBean(DatabaseHelperBean databaseHelperBean) {
		this.dbhb = databaseHelperBean;
	}
	

}
