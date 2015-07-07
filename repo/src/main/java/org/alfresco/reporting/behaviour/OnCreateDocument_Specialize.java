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

package org.alfresco.reporting.behaviour;


import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.OnCreateChildAssociationPolicy;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.reporting.ReportingModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.InvalidNodeRefException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class OnCreateDocument_Specialize implements OnCreateChildAssociationPolicy {

	private PolicyComponent policyComponent;
	private NodeService nodeService;
	
	private static Log logger = LogFactory.getLog(OnCreateDocument_Specialize.class);
	
	public void initialise() {
		
		this.policyComponent.bindAssociationBehaviour(
				OnCreateChildAssociationPolicy.QNAME,  
				ReportingModel.TYPE_REPORTING_ROOT,				//ContentModel.TYPE_FOLDER, 
				ContentModel.ASSOC_CONTAINS,
				new JavaBehaviour(this, "onCreateChildAssociation", NotificationFrequency.FIRST_EVENT));
		
		this.policyComponent.bindAssociationBehaviour(
				OnCreateChildAssociationPolicy.QNAME,  
				ReportingModel.TYPE_REPORTING_CONTAINER,		//ContentModel.TYPE_FOLDER, 
				ContentModel.ASSOC_CONTAINS,
				new JavaBehaviour(this, "onCreateChildAssociation", NotificationFrequency.FIRST_EVENT));
		/*
		this.policyComponent.bindClassBehaviour(QName.createQName(
				NamespaceService.ALFRESCO_URI, "onCreateNode"),
				ContentModel.TYPE_CONTENT,
				new JavaBehaviour(this, "onCreateNode",	NotificationFrequency.FIRST_EVENT));
		*/
		
	}
	
	
	/**
	 * if document created as child of ReportingContainer => create ReportTemplate
	 * if document created as child of ReportingRoot => create HarvestDefinition
	 */
	@Override
	public void onCreateChildAssociation(ChildAssociationRef car, boolean arg1) {
		final NodeRef parent = car.getParentRef();
		final NodeRef child = car.getChildRef();
		//public void onCreateNode(final ChildAssociationRef car) {
			
		try{
			// runAs is expensive... Since we swapped to onCreateChildAssoc...
//			 AuthenticationUtil.runAs(new RunAsWork<Void>() {
//				public  Void doWork() throws Exception {
					if (nodeService.exists(child) && nodeService.exists(parent)		// added to prevent nullpointer creating ZIP files
							//&& !nodeService.getNodeStatus(child).isDeleted()		// added to prevent nullpointer creating ZIP files
							&& nodeService.getType(child).equals(ContentModel.TYPE_CONTENT)
							&& !nodeService.hasAspect(child, ContentModel.ASPECT_TEMPORARY)	// added to prevent nullpointer creating ZIP files
							&& !nodeService.getType(child).equals(ContentModel.TYPE_THUMBNAIL)
							&& !nodeService.getType(child).equals(ContentModel.TYPE_FAILED_THUMBNAIL)){
						// if document created as child of ReportingContainer => create ReportTemplate
						if (nodeService.hasAspect(parent, 
												ReportingModel.ASPECT_REPORTING_CONTAINERABLE)  ){
							// its for us! We're in a folder marked Reporting folder. 
							// (Most likely in Data Dictionary) => Specialize!
							nodeService.setType(child, 
												ReportingModel.TYPE_REPORTING_REPORTTEMPLATE);
						}
						
						// if document created as child of ReportingRoot => create HarvestDefinition
						if (nodeService.hasAspect(parent, 
												ReportingModel.ASPECT_REPORTING_REPORTING_ROOTABLE) ){
							// its for us! We're in a folder marked Reporting folder. 
							// (Most likely in Data Dictionary) => Specialize!
							nodeService.setType(child, 
												ReportingModel.TYPE_REPORTING_HARVEST_DEFINITION);
						}
					}
					// runAs is expensive... Since we swapped to onCreateChildAssoc...
//					return null;
//				} // end public doWork
//			}, AuthenticationUtil.getSystemUserName());
			 
		} catch (InvalidNodeRefException inre){
			// silent drop, we don't care, the show must go on to enable zip of folder
			logger.info("The specialization into a REPORTING_REPORT or " +
					 "HARVEST_DFEFINITION failed, throwing a InvalidNodeRefException against document: " + 
					child.toString() + " or against folder: " + parent);
		} catch (Exception e){
			// lets be tolerant. The world remains spinning if this 
			// specialization does not work out well. Can be dome manual too.
			logger.error("The specialization into a REPORTING_REPORT or " +
						 "HARVEST_DFEFINITION failed... Bad luck! (Weird though)");
			logger.error(e.getMessage());
		}

	} // end onCreateNode

	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public void setNodeService(NodeService nodeService)	{
	    this.nodeService = nodeService;
	}

}
