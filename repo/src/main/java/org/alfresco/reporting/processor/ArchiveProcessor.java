package org.alfresco.reporting.processor;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alfresco.model.ContentModel;
import org.alfresco.reporting.Constants;
import org.alfresco.reporting.ReportLine;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.reporting.mybatis.SelectFromWhere;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Martijn van de Brug
 *
 */
public class ArchiveProcessor {

	/*
	 * ActionService actionService = serviceRegistry.getActionService(); Action
	 * harvestArchiveAction =
	 * actionService.createAction(HarvestArchiveExecuter.NAME);
	 * actionService.executeAction(harvestArchiveAction, harvestDefNodeRef);
	 */

	private ServiceRegistry serviceRegistry;
	private NodeService nodeService;
	private DatabaseHelperBean dbhb;
	private ReportingHelper reportingHelper;
	private NodeRefBasedPropertyProcessor nodeRefProcessor;

	private static Log logger = LogFactory.getLog(ArchiveProcessor.class);

	// Holding <tablename,queue> i.e. <"document",queue_document>
	private HashMap<String, ArrayList<NodeRef>> queues = new HashMap<String, ArrayList<NodeRef>>();
	private ArrayList<String> updatedTables = new ArrayList<String>();

	// TODO: compare Qnames correctly!
	public ArchiveProcessor(ServiceRegistry serviceRegistry, DatabaseHelperBean dbhb, ReportingHelper reportingHelper) {
		this.serviceRegistry = serviceRegistry;
		this.dbhb = dbhb;
		this.reportingHelper = reportingHelper;
		this.nodeService = serviceRegistry.getNodeService();
	}

	/*
	 * harvestArchive(){ sortQueue(){ splits the main queue in smaller subqueues
	 * processQueue(){ processes queue for tables in mapping
	 * processNodesForTable() procces queue for single table } }
	 * 
	 * 
	 */
	/**
	 * @param maps
	 *            containing the TypeTableMap objects that specify the types and
	 *            corresponding tables
	 */
	public void harvestArchive(ArrayList<TypeTableMap> maps) {
		try {
			nodeRefProcessor = new NodeRefBasedPropertyProcessor(null, dbhb, reportingHelper, serviceRegistry);
			
			String timestamp = nodeRefProcessor.getDateAsFormattedString(new Date() );
			
			if (logger.isDebugEnabled())
				logger.debug("Start harvesting archive-store");
			NodeRef archiveStoreRoot = nodeService.getRootNode(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
			List<ChildAssociationRef> childAssocList = nodeService.getChildAssocs(archiveStoreRoot);

			ArrayList<NodeRef> archivedNodes = new ArrayList<NodeRef>();
			// Get all nodes in archive store
			for (ChildAssociationRef childAssocRef : childAssocList) {
				NodeRef archivedNodeRef = childAssocRef.getChildRef();
				if (logger.isDebugEnabled())
					logger.debug("Adding to archivedNodes: " + archivedNodeRef);
				archivedNodes.add(archivedNodeRef);
			}
			
			queryAgainstAllTables(archivedNodes);
			// process type/tables/nodes for every TypeTableMap
			for (TypeTableMap map : maps) {
				archivedNodes = sortQueue(map, archivedNodes);
			}
			// Check if left-over nodes exist in ANY of the tables
			if (!archivedNodes.isEmpty()) {
				archivedNodes = queryAgainstAllTables(archivedNodes);
			}
			// Log untracable nodes
			if (!archivedNodes.isEmpty()) {
				logger.error("Nodes found in Archive-SpacesStore, but not in reporting database (Business Reporting will ignore these nodes, they remain in the trashcan):");
				for (NodeRef nodeRef : archivedNodes) {
					logger.error(nodeRef.toString() + " " + nodeService.getProperty(nodeRef, ContentModel.PROP_NAME)
							+ " Type: " + nodeService.getType(nodeRef));
				}
			}
			setLastSuccesForTables(updatedTables, timestamp);
		} catch (Exception e) {
			logger.error(e);
		}
	}

	/**
	 * @param queueType
	 *            type used as argument to sort the nodes into the correct queue
	 * @return archivedNodes the archivedNodes minus the sorted nodes
	 */
	public ArrayList<NodeRef> sortQueue(TypeTableMap mapping, ArrayList<NodeRef> archivedNodes) {
		String type = mapping.getType();
		ArrayList<NodeRef> leftOverArchivedNodes = new ArrayList<NodeRef>(archivedNodes);
		ArrayList<NodeRef> currentQueue = new ArrayList<NodeRef>();
		for (NodeRef nodeRef : archivedNodes) {
			if (nodeService.getType(nodeRef).toPrefixString().equals(type)) {
				if (logger.isDebugEnabled()) {
					logger.debug(
							nodeService.getProperty(nodeRef, ContentModel.PROP_NAME) + " added to " + type + " queue");
				}
				currentQueue.add(nodeRef);
				leftOverArchivedNodes.remove(nodeRef);
			}
		}
		processQueue(currentQueue, mapping);
		return leftOverArchivedNodes;
	}

	/**
	 * @param queue
	 *            queue to process
	 * @param mapping
	 *            mapping containing the type and corresponding tables in order
	 *            Calls processNodesForTable() for tablename in mapping until
	 *            queue size == 0
	 */
	public void processQueue(ArrayList<NodeRef> queue, TypeTableMap mapping) {
		String type = mapping.getType();
		// Call processNodesForTable() for every
		for (String tablename : mapping.getTables()) {
			if (logger.isDebugEnabled())
				logger.debug("Checking for type: " + type + " in table: " + tablename);
			queue = processNodesForTable(tablename, queue);
			if (queue.isEmpty())
				break;
		}
	}

	/**
	 * @param tablename
	 * @param queue
	 * @return nodesNotInTable the nodes that where not in this table
	 */
	public ArrayList<NodeRef> processNodesForTable(String tablename, ArrayList<NodeRef> queue) {
		ArrayList<NodeRef> nodesNotInTable = new ArrayList<NodeRef>(queue);
		// boolean to keep track wether this table was updated or not
		if (!dbhb.tableIsRunning(tablename + "_a")) {
			dbhb.setLastTimestampStatusRunning(tablename+"_a");
			boolean tableUpdated = false;
			for (NodeRef nodeRef : queue) {
				ReportLine rl = new ReportLine(tablename, nodeRefProcessor.getSimpleDateFormat(), reportingHelper);
				try {
					nodeRefProcessor.processNodeToMap(nodeRef.toString(), tablename, rl);
					if (dbhb.rowExists(rl)) {
						tableUpdated = true;
						if (dbhb.archivedDateIsEmpty(rl)) {
							dbhb.updateVersionedIntoTable(rl);
							if (logger.isDebugEnabled())
								logger.debug("ArchivedDate was null, new row added for "
										+ nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
						} else {
							dbhb.updateIntoTable(rl);
							if (logger.isDebugEnabled())
								logger.debug("ArchivedDate was NOT null, previous record was updatet: "
										+ nodeService.getProperty(nodeRef, ContentModel.PROP_NAME));
						}
					} else {
						nodesNotInTable.remove(nodeRef);
					}
				} catch (Exception e) {
					logger.error(e);
				}
			}
			dbhb.resetLastTimestampTable(tablename+"_a");
			if (tableUpdated) {
				updatedTables.add(tablename);
			}
		}else{
			logger.error("Table is running, did not execute harvest!! "+tablename+"_a");
		}
		return nodesNotInTable;
	}

	public ArrayList<NodeRef> queryAgainstAllTables(ArrayList<NodeRef> queue) {
		if (logger.isDebugEnabled()) logger.debug("Query left-over nodes against all tables...");
		Map<String, String> tables = dbhb.getShowTables();
		for (Map.Entry<String, String> entry : tables.entrySet()) {
			queue = processNodesForTable(entry.getKey(), queue);
		}
		return queue;
	}
	
	public void setLastSuccesForTables(ArrayList<String> tables, String timestamp){
		for(String tablename : tables){
			dbhb.setLastTimestamp(tablename+"_a", timestamp);
			if (logger.isDebugEnabled()) logger.debug("Lastrun set for: "+tablename);
		}
	}

}
