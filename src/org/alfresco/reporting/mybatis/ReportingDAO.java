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

package org.alfresco.reporting.mybatis;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public interface ReportingDAO {
	public void init();
	
	public void openConnection() throws SQLException;
	
	public void closeConnection() throws SQLException;
	
	
	public void pushPreliminaryFunctions();
	
	public boolean lastRunTableIsRunning();
	 
	public void updateLastSuccessfulRunDateForTable(String tableName, String status, String timestamp);
	
	public void updateLastSuccessfulRunStatusForTable(String tableName, String status);
	 
	public String getLastSuccessfulRunDateForTable(String tablename);
	
	public String getLastSuccessfulRunStatusForTable(String tablename);
	
	public int getNumberOfRowsForTable(String tablename);
	
	public void createLastTimestampTable();
	
	public void createLastTimestampTableRow(String tablename);

	public String selectLastRunForTable(String tablename);
	
	public void setAllStatusesDoneForTable();
	
	public  List<String> getShowTables();
	
	public Properties getDescTable(String tablename);
	
	public void dropTable(String tablename);
	
	public void clearLastRunTimestamp(String tablename);
	
	public void createEmtpyTable(String tablename);
	
	
	/**
	 * creates an Index against any given column in any given table
	 */
	public void createCustomIndex(String tablename, String columnname, String indexname);
	
	
	public void extendTableDefinition(ReportingColumnDefinition rcd);
	
	public void updateLastSuccessfulBatchForTable(String tableName, String timestamp);
	
	public boolean reportingRowExists(SelectFromWhere sfw);
	
	public boolean reportingRowEqualsModifiedDate(SelectFromWhere sfw);
	
	public boolean reportingRowVersionedEqualsModifiedDate(SelectFromWhere sfw);
	
	public int reportingUpdateIntoTable(UpdateWhere updateWhere);
	
	public int reportingUpdateVersionedIntoTable(UpdateWhere updateWhere);
	
	public int reportingInsertIntoTable(InsertInto insertInto);
	
	public String reportingSelectFromWhere(String select, String tablename, String where);
	
	public List<Map<String,Object>> reportingSelectStoreProtocolPerTable(String tablename);
	
	public String reportingSelectStatusFromLastsuccessfulrun(String tablename);
	 
	public List<Map<String,Object>> reportingSelectIsLatestPerTable(String tablename);
}
