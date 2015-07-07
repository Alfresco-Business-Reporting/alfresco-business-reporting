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

package org.alfresco.reporting.mybatis.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import org.alfresco.reporting.mybatis.WorkflowDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;

public class WorkflowDAOImpl implements WorkflowDAO{
	
	private SqlSession sessionTemplate = null;
	
	private static Log logger = LogFactory.getLog(WorkflowDAOImpl.class);

	public void setWorkflowTemplate(SqlSessionTemplate template){
		this.sessionTemplate = template;
	}

	@SuppressWarnings("unchecked")
	@Override
	public  List<String> getDeletedTasks(final String fromDate, int maxItems){
		List<String> results=null;
		
		if (null==fromDate){
			logger.debug("getDeletedTasks - no date");
			results = (List<String>)sessionTemplate.selectList("get-all-completed-tasks");
		} else {
			logger.debug("getDeletedTasks - with date " + fromDate);
			results = (List<String>)sessionTemplate.selectList("get-completed-tasks-since", fromDate);
		}
		return results;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<String> getCreatedTasks(final String fromDate, int maxItems) {
		List<String> results;
	
		if (null==fromDate){
			logger.debug("getCreatedTasks - no date");
			results = (List<String>)sessionTemplate.selectList("get-all-created-tasks");
		} else {
			logger.debug("getCreatedTasks - with date " + fromDate);
			results = (List<String>)sessionTemplate.selectList("get-created-tasks-since", fromDate);
		}
		return results;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public HashMap<String,Serializable> getPropertiesForWorkflowTask(final String taskId){
		return (HashMap<String,Serializable>)sessionTemplate.selectOne("get-additional-task-properties", taskId);
	}

	@SuppressWarnings("unchecked")
	@Override
	public  List<String> getCompletedProcesses(final String fromDate, int maxItems){
		List<String> results=null;
		
		if (null==fromDate){
			logger.debug("getCompletedProcesses - no date");
			results = (List<String>)sessionTemplate.selectList("get-all-completed-processes");
		} else {
			logger.debug("getCompletedProcesses - with date " + fromDate);
			results = (List<String>)sessionTemplate.selectList("get-completed-processes-since", fromDate);
		}
		return results;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<String> getCreatedProcesses(final String fromDate, int maxItems) {
		List<String> results;
	
		if (null==fromDate){
			logger.debug("getCreatedProcesses - no date");
			results = (List<String>)sessionTemplate.selectList("get-all-created-processes");
		} else {
			logger.debug("getCreatedProcesses - with date " + fromDate);
			results = (List<String>)sessionTemplate.selectList("get-created-processes-since", fromDate);
		}
		return results;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public HashMap<String,Serializable> getPropertiesForWorkflowInstance(final String processId){
		return (HashMap<String,Serializable>)sessionTemplate.selectOne("get-additional-process-properties", processId);
	}

}
