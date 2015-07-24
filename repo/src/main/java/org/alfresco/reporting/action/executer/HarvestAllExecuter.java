package org.alfresco.reporting.action.executer;

import java.util.Iterator;
import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.ReportingModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ActionExecuter to execute all harvestdefinitions within a reporting root
 * 
 * @author Martijn van de Brug
 *
 */
public class HarvestAllExecuter extends ActionExecuterAbstractBase {

	public static final String NAME = "harvest-all-executer";
	private static Log logger = LogFactory.getLog(HarvestAllExecuter.class);

	private SearchService searchService;
	private NodeService nodeService;
	private ActionService actionService;
	private ReportingHelper reportingHelper;

	@Override
	protected void executeImpl(Action action, NodeRef reportingRootRef) {
		logger.info("searching for harvest definitions...");

		// Lucene search for nodes with type reporting:harvestDefinition
		String query = 
				"TYPE:\"" + 
				ReportingModel.REPORTING_PREFIX +
				QName.NAMESPACE_PREFIX+
				ReportingModel.TYPE_REPORTING_HARVEST_DEFINITION.getLocalName()+
				"\"";
		// "TYPE:\"reporting:harvestDefinition\"";

		SearchParameters sp = new SearchParameters();
		StoreRef storeRef = new StoreRef(StoreRef.PROTOCOL_WORKSPACE, "SpacesStore");
		sp.addStore(storeRef);
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
		sp.setQuery(query);

		if (logger.isDebugEnabled())
			logger.debug("Searching for harvest definitions: " + query);

		ResultSet results = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, query);

		if (results.length() > 1) {
			Action harvestAction = actionService.createAction(HarvestingExecuter.NAME);
			Iterator<ResultSetRow> resultsIterator = results.iterator();
			// Iterate over the found nodes
			while (resultsIterator.hasNext()) {
				ResultSetRow node = resultsIterator.next();
				NodeRef harvestDefNodeRef = node.getNodeRef();

				if (logger.isDebugEnabled())
					logger.debug("Harvest definition found: " + harvestDefNodeRef);

				// Check if the found harvest definitions are children of this
				// reportingroot
				if (reportingRootRef.equals(reportingHelper.getReportingRoot(harvestDefNodeRef))) {
					logger.info("Executing harvest definition: " + node.getQName());
					actionService.executeAction(harvestAction, harvestDefNodeRef);
				}
			}
		} else {
			logger.info("No harvest definitions found");
		}

	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> arg0) {
		// TODO Auto-generated method stub
	}

	public SearchService getSearchService() {
		return searchService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
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

	public ReportingHelper getReportingHelper() {
		return reportingHelper;
	}

	public void setReportingHelper(ReportingHelper reportingHelper) {
		this.reportingHelper = reportingHelper;
	}

}
