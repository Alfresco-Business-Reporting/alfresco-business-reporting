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
	
	private List<NodeRef> archivedNodes = new ArrayList<NodeRef>();
	// Holding <tablename,queue> i.e. <"document",queue_document>
	private HashMap<String,ArrayList<NodeRef>> queues = new HashMap<String,ArrayList<NodeRef>>();
	
	//TODO: compare Qnames correctly!
	private String QUEUE_TYPE_DOCUMENT="content";
	private String QUEUE_TYPE_FOLDER="folder";
	
	public ArchiveProcessor(ServiceRegistry serviceRegistry, DatabaseHelperBean dbhb, ReportingHelper reportingHelper){
		this.serviceRegistry = serviceRegistry;
		this.dbhb = dbhb;
		this.reportingHelper = reportingHelper;
		this.nodeService = serviceRegistry.getNodeService();
	}
	
	public void harvestArchive() {
		
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

			//Get all nodes in archive store
			for(ChildAssociationRef childAssocRef : childAssocList){
				NodeRef archivedNodeRef = childAssocRef.getChildRef();
				if (logger.isDebugEnabled())logger.debug("Adding to archivedNodes: "+archivedNodeRef);
				archivedNodes.add(archivedNodeRef);
			}
			sortQueues("document", QUEUE_TYPE_DOCUMENT);
			sortQueues("folder", QUEUE_TYPE_FOLDER);
			//processQueues();
			processQueuesInNodeProcessor();
		} catch (Exception e) {
			logger.error(e);
		}
		
	}
	
	//Sort the nodes into the correct queues according to the give type
	private void sortQueues(String tablename, String type){
		if (logger.isDebugEnabled())logger.debug("Sorting nodes of type "+type+" for table "+tablename);
		ArrayList<NodeRef> currentQueue;
		//check if queues exist, else create new
		if(queues.containsKey(tablename)){
			currentQueue = queues.get(tablename);
		}else{
			currentQueue = new ArrayList<NodeRef>();
		}
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
		queues.put(tablename, currentQueue);
	}
	private void processQueuesInNodeProcessor(){
		for (HashMap.Entry<String, ArrayList<NodeRef>> entry : queues.entrySet()) {
		    String tablename = entry.getKey();
		    ArrayList<NodeRef> queue = entry.getValue();
		    ReportLine rl = new ReportLine(tablename, nodeRefProcessor.getSimpleDateFormat(), reportingHelper);
		    for(NodeRef nodeRef : queue){
		    	nodeRefProcessor.addToQueue(nodeRef);
		    }
		    try {
				nodeRefProcessor.processQueueValues(tablename);
			} catch (Exception e) {
				logger.error(e);
			}
		}
		
	}
	
	//process sorted queues (i.e. document queue for table document)
	private void processQueues(){
		for (HashMap.Entry<String, ArrayList<NodeRef>> entry : queues.entrySet()) {
		    String tablename = entry.getKey();
		    ArrayList<NodeRef> queue = entry.getValue();
		    ReportLine rl = new ReportLine(tablename, nodeRefProcessor.getSimpleDateFormat(), reportingHelper);
		    for(NodeRef nodeRef : queue){
		    	String uuid = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_NODE_UUID);
		    	if (logger.isDebugEnabled()) logger.debug("Checking if "+nodeService.getProperty(nodeRef, ContentModel.PROP_NAME)+" exists in table "+tablename+" uuid: "+uuid);
		    	try {
					if(dbhb.nodeUUIDExists(tablename, uuid)){
						nodeRefProcessor.processNodeToMap(nodeRef.toString(), tablename, rl);
						logger.info("lines updated into table: "+dbhb.updateIntoTable(rl));
					}
				} catch (Exception e) {
					logger.error(e);
				}
		    	
		    }
		}
		
	}
	

}
