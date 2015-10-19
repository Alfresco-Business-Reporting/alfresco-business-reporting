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

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public interface WorkflowDAO {
	public  List<String> getCreatedTasks(String fromDate, int maxItems);
	
	public  List<String> getDeletedTasks(String fromDate, int maxItems);
	
	public HashMap<String,Serializable> getPropertiesForWorkflowTask(String taskId);
	
	public  List<String> getCreatedProcesses(String fromDate, int maxItems);
	
	public  List<String> getCompletedProcesses(String fromDate, int maxItems);
	
	public HashMap<String,Serializable> getPropertiesForWorkflowInstance(String processId);
	
}

