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


package org.alfresco.reporting.action.evaluator;

import javax.faces.context.FacesContext;

import org.alfresco.reporting.ReportingModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ExecuteAllReportsEvaluator extends BaseActionEvaluator {

	private static final long serialVersionUID = -290962952423019602L;
	private static Log logger = LogFactory.getLog(ExecuteAllReportsEvaluator.class);
	
	public boolean evaluate(Node node) {
		boolean found = false; // true if node has the desired Aspect
		logger.debug("start evaluate");
		ServiceRegistry serviceRegistry = 
				Repository.getServiceRegistry(FacesContext.getCurrentInstance());
		
		if (node.hasAspect(ReportingModel.ASPECT_REPORTING_REPORTING_ROOTABLE) &&
				serviceRegistry.getAuthorityService().hasAdminAuthority()){
			found=true;
		}
		return found;
	}
	
}
