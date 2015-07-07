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

package org.alfresco.reporting.web.bean;

//import java.util.HashMap;
//import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.reporting.ReportingModel;
import org.alfresco.reporting.action.executer.HarvestingExecuter;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.BrowseBean;
import org.alfresco.web.bean.repository.Repository;
import org.alfresco.web.ui.common.component.UIActionLink;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.UnexpectedRollbackException;

public class HarvestHandlerBean {
	private static final String PARAM_ID = "id";

	private static final String PARAM_FREQUENCY = "frequency";

	private static final String PARAM_LABEL_ID = "label-id";

	private static final String PARAM_ACTION_LOCATION = "actionLocation";

	//private static Log logger = LogFactory.getLog(HarvestHandlerBean.class);

	private BrowseBean browseBean;

	private NodeService nodeService;

	private ActionService actionService;

	public void execute(ActionEvent event) {

		UIActionLink link = (UIActionLink) event.getComponent();
		Map<String, String> params = link.getParameterMap();

		String id = params.get(PARAM_ID);

		if (id == null || id.equals(""))
			throw new AlfrescoRuntimeException(
					"Required parameter 'id' is null or empty");

		String frequency = params.get(PARAM_FREQUENCY);

		String labelId = params.get(PARAM_LABEL_ID);

		if (labelId == null || labelId.equals(""))
			throw new AlfrescoRuntimeException(
					"Required parameter 'label-id' is null or empty");

		FacesContext context = FacesContext.getCurrentInstance();

		NodeRef actionedUponNodeRef = new NodeRef(Repository.getStoreRef(), id);
		String filename = (String)nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_NAME);
		
		String replyString = "Done harvesting " + filename;
		
		if (ReportingModel.TYPE_REPORTING_ROOT.equals(nodeService.getType(actionedUponNodeRef))){
			replyString += " (ReportingRoot)";
		}
		if (ReportingModel.TYPE_REPORTING_HARVEST_DEFINITION.equals(nodeService.getType(actionedUponNodeRef))){
			replyString += " (" + 
							nodeService.getProperty(
										actionedUponNodeRef, 
										ReportingModel.PROP_REPORTING_HARVEST_FREQUENCY) + 
							")";
		}
		
		NodeRef parent = nodeService.getPrimaryParent(actionedUponNodeRef)
				.getParentRef();

		if (nodeService.exists(actionedUponNodeRef)) {

			
			Action action = actionService
					.createAction(HarvestingExecuter.NAME);
			if (frequency!=null){
				action.setParameterValue(
						HarvestingExecuter.PARAM_FREQUENCY,
						frequency);
			} // end if frequency!=null
			try{
				actionService.executeAction(action, actionedUponNodeRef);
			} catch (UnexpectedRollbackException uerb){
				// silent drop. Usually it is because of.... dunno but no good reason
			}
			
			
			
			BrowseBean browseBean = (BrowseBean) FacesHelper.getManagedBean(
					context, "BrowseBean");
			String actionLocation = params.get(PARAM_ACTION_LOCATION);

			if (actionLocation.equals("document-details") == true) {
				browseBean.getDocument().reset();
				UIComponent comp = context.getViewRoot().findComponent(
						"dialog:dialog-body:document-props");
				comp.getChildren().clear();
			} else if (actionLocation.equals("folder-details") == true) {
				if (browseBean.getActionSpace() != null)
					browseBean.getActionSpace().reset();
				UIComponent comp = context.getViewRoot().findComponent(
						"dialog:dialog-body:space-props");
				if (comp != null && comp.getChildren() != null)
					comp.getChildren().clear();
			} else if (actionLocation.equals("folder-browse") == true) {
				if (nodeService.exists(parent))
					browseBean.clickSpace(parent);
			}
		} // end if node exists

		FacesMessage fm = new FacesMessage(FacesMessage.SEVERITY_INFO, replyString, replyString);
		context.addMessage(replyString, fm);

	}



	public BrowseBean getBrowseBean() {
		return browseBean;
	}

	public void setBrowseBean(BrowseBean browseBean) {
		this.browseBean = browseBean;
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public ActionService getActionService() {
		return actionService;
	}

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

}
