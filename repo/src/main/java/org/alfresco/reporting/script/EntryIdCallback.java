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

package org.alfresco.reporting.script;

import java.io.Serializable;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.reporting.ReportLine;
import org.alfresco.service.cmr.audit.AuditService.AuditQueryCallback;


public class EntryIdCallback implements AuditQueryCallback
    {
        private final boolean valuesRequired;
        private Long entryId;
        private String LOGIN_AUDIT_APPLICATION = "ReportingLoginAudit";
        private ReportLine rl;
        private Properties replacementTypes;
        private String tableName;
        private Properties cache = new Properties();

        public EntryIdCallback(boolean valuesRequired, ReportLine rl, Properties replacementTypes, String tableName, String feedName)
        {
            this.valuesRequired = valuesRequired;
            this.rl = rl;
            this.replacementTypes = replacementTypes;
            this.tableName = tableName;
            this.LOGIN_AUDIT_APPLICATION = feedName;
        }

        public String getEntryId()
        {
            return entryId == null ? null : entryId.toString();
        }

        public boolean valuesRequired()
        {
            return this.valuesRequired;
        }

        public final boolean handleAuditEntry(Long entryId, String applicationName, String user, long time,
                Map<String, Serializable> values)
        {
            if (applicationName.equals(LOGIN_AUDIT_APPLICATION))
            {
                return handleAuditEntry(entryId, user, time, values);
            }
            return true;
        }

        public boolean handleAuditEntry(Long entryId, String user, long time, Map<String, Serializable> values)
        {
            this.entryId = entryId;
            return true;
        }

        public boolean handleAuditEntryError(Long entryId, String errorMsg, Throwable error)
        {
            throw new AlfrescoRuntimeException("Audit entry " + entryId + ": " + errorMsg, error);
        }

        public Properties getReplacementTypes(){
        	return this.replacementTypes;
        }
        
        public ReportLine getRl(){
        	return this.rl;
        }
        
        
        public String getTableName(){
        	return this.tableName;
        }

        public Properties getCache(){
        	return this.cache;
        }
        
        public void addToCache(String key, String value){
        	this.cache.setProperty(key, value);
        }
    };
