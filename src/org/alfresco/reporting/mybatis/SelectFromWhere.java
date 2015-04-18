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

/**
 * This is a data object for MyBatis Reporting queries.
 * @author tpeelen
 *
 */
public class SelectFromWhere {
	private String select;
	private String from;
	private String where;
	private String orderby;
	private String groupby;
	private String andwhere;
	private String andandwhere;
	private String database;
	
	public SelectFromWhere(String select, String from, String where){
		if (select!=null) setSelect(select);
		if (from!=null) setFrom(from);
		if (where!=null) setWhere(where);
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getSelect() {
		return select;
	}

	public void setSelect(String select) {
		this.select = select;
	}

	// huh why is this? It solves an error in Oracle dialect, but why?
	public void setTablename(String tablename){
		this.from = tablename;
	}
	
	// huh why is this? It solves an error in Oracle dialect, but why?
	public String getTablename(){
		return from;
	}
	
	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from.toLowerCase();
	}

	public String getWhere() {
		return where;
	}

	public void setWhere(String where) {
		this.where = where;
	}
	
	public String getAndwhere() {
		return andwhere;
	}
	
	public void setAndwhere(String andwhere) {
		this.andwhere = andwhere;
	}
	
	public String getAndandwhere() {
		return andandwhere;
	}
	
	public void setAndandwhere(String andandwhere) {
		this.andandwhere = andandwhere;
	}

	public String getOrderby() {
		return orderby;
	}

	public void setOrderby(String orderby) {
		this.orderby = orderby;
	}

	public String getGroupby() {
		return groupby;
	}

	public void setGroupby(String groupby) {
		this.groupby = groupby;
	}
	
	
}
