package org.alfresco.reporting.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
	
	/*		ActionService actionService = serviceRegistry.getActionService();
	Action harvestArchiveAction = actionService.createAction(HarvestArchiveExecuter.NAME);
	actionService.executeAction(harvestArchiveAction, harvestDefNodeRef);*/

	private ServiceRegistry serviceRegistry;
	private NodeService nodeService;
	private DatabaseHelperBean dbhb;
	private ReportingHelper reportingHelper;
	private NodeRefBasedPropertyProcessor nodeRefProcessor;
	
	private static Log logger = LogFactory.getLog(ArchiveProcessor.class);
	
	// Holding <tablename,queue> i.e. <"document",queue_document>
	private HashMap<String,ArrayList<NodeRef>> queues = new HashMap<String,ArrayList<NodeRef>>();
	
	//TODO: compare Qnames correctly!
	// type<->table mapping. i.e. <"cm:document", <document, datalistitem>
	private TypeTableMap documentMap = new TypeTableMap();
	private TypeTableMap folderMap = new TypeTableMap();
	
	private String TYPE_DOCUMENT="content";
	private String TYPE_FOLDER="folder";
	
	public ArchiveProcessor(ServiceRegistry serviceRegistry, DatabaseHelperBean dbhb, ReportingHelper reportingHelper){
		this.serviceRegistry = serviceRegistry;
		this.dbhb = dbhb;
		this.reportingHelper = reportingHelper;
		this.nodeService = serviceRegistry.getNodeService();
	}
	
	/*
	 * harvestArchive(){
	 * 		sortQueue(){						splits the main queue in smaller subqueues
	 * 			processQueue(){					processes queue for tables in mapping
	 * 				processNodesForTable()		procces queue for single table
	 * 		}
	 * }
	 * 
	 * 
	 */
	
	public void harvestArchive() {
		
		//Create TYPE->tablename mapping (for testing purposes), should be defined in properties or harvestdefinition
		documentMap.setType(TYPE_DOCUMENT);
		documentMap.addTable("document");
		//documentMap.addTable("datalistitem");
		folderMap.setType(TYPE_FOLDER);
		folderMap.addTable("folder");
		
		try {
			//nodeRefProcessor to reuse methods
			nodeRefProcessor = new NodeRefBasedPropertyProcessor(
					null, 
					dbhb, 
					reportingHelper, 
					serviceRegistry);
			
			if (logger.isDebugEnabled())logger.debug("Start harvesting archive-store");
			NodeRef archiveStoreRoot = nodeService.getRootNode(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE);
			List<ChildAssociationRef> childAssocList = nodeService.getChildAssocs(archiveStoreRoot);

			ArrayList<NodeRef> archivedNodes = new ArrayList<NodeRef>();
			//Get all nodes in archive store
			for(ChildAssociationRef childAssocRef : childAssocList){
				NodeRef archivedNodeRef = childAssocRef.getChildRef();
				if (logger.isDebugEnabled())logger.debug("Adding to archivedNodes: "+archivedNodeRef);
				archivedNodes.add(archivedNodeRef);
			}
			archivedNodes = sortQueue(documentMap, archivedNodes);
			if(archivedNodes != null){
				archivedNodes = sortQueue(folderMap, archivedNodes);
			}
		} catch (Exception e) {
			logger.error(e);
		}
	}
	
	
	/**
	 * @param queueType type used as argument to sort the nodes into the correct queue
	 * @return archivedNodes the archivedNodes minus the sorted nodes
	 */
	public ArrayList<NodeRef> sortQueue(TypeTableMap mapping, ArrayList<NodeRef> archivedNodes){
		String type = mapping.getType();
		ArrayList<NodeRef> leftOverArchivedNodes = new ArrayList<NodeRef>(archivedNodes);
		ArrayList<NodeRef> currentQueue = new ArrayList<NodeRef>();
		for(NodeRef nodeRef : archivedNodes){
			if(nodeService.getType(nodeRef).toPrefixString().equals(type)){
				if (logger.isDebugEnabled()){
					logger.debug(nodeService.getProperty(nodeRef, ContentModel.PROP_NAME)+" added to "+type+" queue");
				}
				currentQueue.add(nodeRef);
				leftOverArchivedNodes.remove(nodeRef);
			}
		}
		processQueue(currentQueue, mapping);
		return leftOverArchivedNodes;
/*
		
		
		
		if (logger.isDebugEnabled())logger.debug("Sorting nodes of type "+type+" for table "+tablename);
		ArrayList<NodeRef> currentQueue;
		//check if queues exist, else create new
		if(queues.containsKey(tablename)){
			currentQueue = queues.get(tablename);
		}else{
			currentQueue = new ArrayList<NodeRef>();
		}
		//nodes that where not 
		ArrayList<NodeRef> leftOverArchivedNodes = new ArrayList<NodeRef>(archivedNodes);
		for(NodeRef nodeRef : archivedNodes){
			if(nodeService.getType(nodeRef).toPrefixString().equals(type)){
				if (logger.isDebugEnabled()){
					logger.debug("adding "+nodeService.getProperty(nodeRef, ContentModel.PROP_NAME)+" to "+tablename+" queue.");
				}
				currentQueue.add(nodeRef);
				leftOverArchivedNodes.remove(nodeRef);
			}
		}
		archivedNodes = leftOverArchivedNodes;
		queues.put(tablename, currentQueue);*/
	}
	

	/**
	 * @param queue queue to process
	 * @param mapping mapping containing the type and corresponding tables in order
	 * Calls processNodesForTable() for tablename in mapping until queue size == 0
	 */
	public void processQueue(ArrayList<NodeRef> queue, TypeTableMap mapping){
		String type = mapping.getType();
		//Call processNodesForTable() for every
		for(String tablename : mapping.getTables()){
			if (logger.isDebugEnabled()) logger.debug("Checking for type: "+type+" in table: "+tablename);
			queue = processNodesForTable(tablename, queue);
			if(queue.isEmpty()) break;
		}
/*		//process all sortedqueues
		for (HashMap.Entry<String, ArrayList<NodeRef>> entry : queues.entrySet()) {
		    String tablename = entry.getKey();
		    ArrayList<NodeRef> queue = entry.getValue();
		    ReportLine rl = new ReportLine(tablename, nodeRefProcessor.getSimpleDateFormat(), reportingHelper);
		    //process nodes in sortedQueue
		    for(NodeRef nodeRef : queue){
		    	String uuid = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NODE_UUID);
		    	if (logger.isDebugEnabled()) logger.debug("Checking if "+nodeService.getProperty(nodeRef, ContentModel.PROP_NAME)+" exists in table "+tablename+" uuid: "+uuid);
		    	try {
		    		nodeRefProcessor.processNodeToMap(nodeRef.toString(), tablename, rl);
					if(dbhb.rowExists(rl)){
						logger.info("lines updated into table: "+dbhb.updateIntoTable(rl));
					}
				} catch (Exception e) {
					logger.error(e);
				}
		    	
		    }
		}*/
		
	}
	
	
	/**
	 * @param tablename
	 * @param queue
	 * @return nodesNotInTable the nodes that where not in this table
	 */
	public ArrayList<NodeRef> processNodesForTable(String tablename, ArrayList<NodeRef> queue){
		ArrayList<NodeRef> nodesNotInTable = new ArrayList<NodeRef>(queue);
		for(NodeRef nodeRef : queue){
			ReportLine rl = new ReportLine(tablename, nodeRefProcessor.getSimpleDateFormat(), reportingHelper);
			try {
				nodeRefProcessor.processNodeToMap(nodeRef.toString(), tablename, rl);
				if(dbhb.rowExists(rl)){
					logger.info("lines updated into table: "+dbhb.updateIntoTable(rl));
				}else{
					nodesNotInTable.remove(nodeRef);
				}
			} catch (Exception e) {
				logger.error(e);
			}
		}
		return nodesNotInTable;
	}
	

}
