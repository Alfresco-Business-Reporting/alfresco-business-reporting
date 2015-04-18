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

public class UpdateWhere {

	private String tablename;
	private String updateClause;
	private String whereClause;
	
	public UpdateWhere(String table, String update, String where){
		if (table  != null) setTablename(table);
		if (update != null) setUpdateClause(update);
		if (where  != null) setWhereClause(where);
	}
	
	public String getTablename() {
		return tablename;
	}
	public void setTablename(String tablename) {
		this.tablename = tablename.toLowerCase();
	}
	public String getUpdateClause() {
		return updateClause;
	}
	public void setUpdateClause(String updateClause) {
		this.updateClause = updateClause;
	}
	public String getWhereClause() {
		return whereClause;
	}
	public void setWhereClause(String whereClause) {
		this.whereClause = whereClause;
	}
	
	
}
