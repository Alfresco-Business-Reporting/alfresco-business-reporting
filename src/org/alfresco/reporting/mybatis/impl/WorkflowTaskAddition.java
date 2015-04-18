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

public class WorkflowTaskAddition {
	private String proc_def_id_;
	private String task_def_key_;
	private String delete_reason_;
	private Long duration;
	
	
	public String getProc_def_id_() {
		return proc_def_id_;
	}
	public void setProc_def_id_(String proc_def_id_) {
		this.proc_def_id_ = proc_def_id_;
	}
	public String getTask_def_key_() {
		return task_def_key_;
	}
	public void setTask_def_key_(String task_def_key_) {
		this.task_def_key_ = task_def_key_;
	}
	public String getDelete_reason_() {
		return delete_reason_;
	}
	public void setDelete_reason_(String delete_reason_) {
		this.delete_reason_ = delete_reason_;
	}
	public Long getDuration() {
		return duration;
	}
	public void setDuration(Long duration) {
		this.duration = duration;
	}
	
	
}
