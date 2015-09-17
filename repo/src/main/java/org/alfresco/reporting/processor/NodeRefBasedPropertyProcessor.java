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

package org.alfresco.reporting.processor;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.dictionary.DictionaryNamespaceComponent;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.repo.version.Version2Model;
import org.alfresco.reporting.Constants;
import org.alfresco.reporting.ReportLine;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.ReportingModel;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.Path.ChildAssocElement;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.cmr.version.Version;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Sort;

/**
 * This class accepts an Alfresco object (document or folder), strips out all
 * properties and associations, and will return a Map of they key-value pairs.
 * 
 * @author tpeelen
 *
 */
public class NodeRefBasedPropertyProcessor extends PropertyProcessor {

	/**
	 * an object containing the noderef as key, and the parent of the version
	 * series as value
	 */
	// private Properties versionnodes;
	private VersionService versionService;
	private ContentService contentService;

	private String vendor = "";
	private String COLUMN_SIZE = "";

	private static Log logger = LogFactory.getLog(NodeRefBasedPropertyProcessor.class);

	public ContentService getContentService() {
		return contentService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	/**
	 * 
	 * @param versionNodes
	 * @param dbhb
	 * @param reportingHelper
	 * @param serviceRegistry
	 * @throws Exception
	 */
	public NodeRefBasedPropertyProcessor(Properties versionNodes, DatabaseHelperBean dbhb,
			ReportingHelper reportingHelper, ServiceRegistry serviceRegistry) throws Exception {

		setNodeService(serviceRegistry.getNodeService());
		setDictionaryService(serviceRegistry.getDictionaryService());
		setFileFolderService(serviceRegistry.getFileFolderService());
		setContentService(serviceRegistry.getContentService());
		setSearchService(serviceRegistry.getSearchService());

		setReportingHelper(reportingHelper);
		setDbhb(dbhb);
		// this.versionnodes = versionNodes;
		this.versionService = serviceRegistry.getVersionService();
		super.versionService = serviceRegistry.getVersionService();

		setClassToColumnType(reportingHelper.getClassToColumnType());
		setReplacementDataTypes(reportingHelper.getReplacementDataType());
		setGlobalProperties(reportingHelper.getGlobalProperties());
		setNamespaces(reportingHelper.getNameSpaces());
		setBlacklist(reportingHelper.getBlacklist());
		vendor = reportingHelper.getDatabaseProvider();

		if (Constants.VENDOR_ORACLE.equals(vendor)) {
			this.COLUMN_SIZE = Constants.COLUMN_SIZE_ORACLE;
		} else {
			this.COLUMN_SIZE = Constants.COLUMN_SIZE;
		}

		if (logger.isDebugEnabled() && false) {
			logger.debug("##this.dataDictionary       =" + this.dataDictionary);
			logger.debug("##this.replacementDataTypes =" + this.replacementDataTypes);
			// logger.debug("##this.getGlobalProperties() =" +
			// this.getGlobalProperties());
			logger.debug("##this.namespaces           =" + this.namespaces);
			// logger.debug("##this.versionnodes =" + getVersionNodes());
		}
	}

	/**
	 * Gien the current noderef, retrieve the value of the Site name (will be
	 * stored as a column by default)
	 * 
	 * @param currentRef
	 * @return cm:name of the Site, or "" if no site found
	 */
	private String getSiteName(NodeRef currentRef) {
		// logger.debug("Enter getSiteName");
		String siteName = "";

		if (currentRef != null) {

			NodeRef rootNode = getNodeService().getRootNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);

			NodeRef siteRef = null;

			boolean siteTypeFound = getNodeService().getType(currentRef).equals(SiteModel.TYPE_SITE);
			if (siteTypeFound) {
				siteRef = currentRef;
			}

			while (!currentRef.equals(rootNode) && (!siteTypeFound)) {
				// logger.debug("getTypeForNode: voor loopRef="+currentRef);
				currentRef = getNodeService().getPrimaryParent(currentRef).getParentRef();
				// logger.debug("getTypeForNode: na loopRef="+currentRef);
				siteTypeFound = getNodeService().getType(currentRef).equals(SiteModel.TYPE_SITE);
				if (siteTypeFound) {
					siteRef = currentRef;
					// logger.debug("getTypeForNode: Found QName node!");
				}
			}
			if (siteRef != null) {
				siteName = (String) getNodeService().getProperty(siteRef, ContentModel.PROP_NAME);
			}
		} // end if nodeRef!=null

		return siteName;
	}

	/**
	 * @param path
	 * @return display path
	 */
	private String toDisplayPath(Path path) {
		// logger.debug("Enter toDisplayPath");
		StringBuffer displayPath = new StringBuffer();
		if (path.size() == 1) {
			displayPath.append("/");
		} else {
			for (int i = 1; i < path.size(); i++) {
				Path.Element element = path.get(i);
				if (element instanceof ChildAssocElement) {
					ChildAssociationRef assocRef = ((ChildAssocElement) element).getRef();
					NodeRef node = assocRef.getChildRef();
					displayPath.append("/");
					displayPath.append(getNodeService().getProperty(node, ContentModel.PROP_NAME));
				}
			}
		}
		return displayPath.toString();
	}

	/**
	 * Given the ScriptNode, get all available associations (parent/child &
	 * target/source) Create a column to store the noderef if there are any of
	 * these associations
	 * 
	 * @throws Exception
	 * 
	 */
	private Properties processAssociationDefinitions(Properties definition, NodeRef nodeRef) throws Exception {
		// logger.debug("Enter processAssociationDefinitions");
		// Child References
		try {
			List<ChildAssociationRef> childCars = getNodeService().getChildAssocs(nodeRef);
			if (childCars.size() > 0) {
				String type = getClassToColumnType().getProperty("noderefs", "-");
				if (getReplacementDataType().containsKey("child_noderef")) {
					type = getReplacementDataType().getProperty("child_noderef", "-").trim();
				}
				definition.setProperty("child_noderef", type);
			}
		} catch (Exception e) {
			logger.error("processAssociationDefinitions: child_noderef ERROR! " + e.getMessage());
			e.printStackTrace();

		}

		try {
			// Parent References
			ChildAssociationRef parentCar = getNodeService().getPrimaryParent(nodeRef);
			if (parentCar != null) {
				String type = getClassToColumnType().getProperty("noderef", "-");
				if (getReplacementDataType().containsKey("parent_noderef")) {
					type = getReplacementDataType().getProperty("parent_noderef", "-").trim();
				}
				definition.setProperty("parent_noderef", type);
			}
		} catch (Exception e) {
			logger.error("processAssociationDefinitions: parent_noderef ERROR!");
			e.printStackTrace();

		}

		try {
			Collection<QName> assocTypes = getDictionaryService().getAllAssociations();
			String blockNameSpaces = globalProperties.getProperty(Constants.property_blockNameSpaces, "");
			String[] startValues = blockNameSpaces.split(",");
			for (QName type : assocTypes) {
				String key = "";
				String shortName = replaceNameSpaces(type.toString());
				boolean stop = nodeRef.toString().startsWith("versionStore")
						|| nodeRef.toString().startsWith("archive");

				for (String startValue : startValues) {
					stop = shortName.startsWith(startValue.trim());
					if (stop)
						break;
				}
				// trx,act,blg_,wca,wcm,ver,fm_,emailserver,sys_,cm_member,cm_subcategories,cm_subscribedBy,cm_attachments,
				// cm_preference,cm_translations,cm_replaces,cm_ml,cm_references,cm_failed,cm_references,cm_avatar,rn_,usr_
				/*
				 * if (stop || shortName.startsWith("trx") ||
				 * shortName.startsWith("act") || shortName.startsWith("blg_")
				 * || shortName.startsWith("wca") || shortName.startsWith("wcm")
				 * || shortName.startsWith("ver") || shortName.startsWith("fm_")
				 * || shortName.startsWith("emailserver_") ||
				 * shortName.startsWith("sys_") ||
				 * shortName.startsWith("cm_member") ||
				 * shortName.startsWith("cm_subcategories") ||
				 * shortName.startsWith("cm_subscribedBy") ||
				 * shortName.startsWith("cm_attachments") ||
				 * shortName.startsWith("cm_translations") ||
				 * shortName.startsWith("cm_preference") ||
				 * shortName.startsWith("cm_replaces") ||
				 * shortName.startsWith("cm_ml") ||
				 * shortName.startsWith("cm_failed") ||
				 * shortName.startsWith("cm_references") ||
				 * shortName.startsWith("cm_avatar") ||
				 * shortName.startsWith("rn_") || /*
				 * shortName.startsWith("imap_") || shortName.startsWith("usr_")
				 * ){} else {
				 */
				if (!stop) {
					try {
						// logger.debug("ASSOCIATIONS: processing " + shortName
						// + " for " + shortName);
						List<AssociationRef> targetRefs = getNodeService().getTargetAssocs(nodeRef, type);
						if (targetRefs.size() > 0) {
							// logger.debug("Found a Target association! " +
							// type.toString());
							key = type.toString();
							// logger.debug("Target: key1="+key);
							key = replaceNameSpaces(key);
							// logger.debug("Target: key2="+key);
							if (!getBlacklist().toLowerCase().contains("," + key.toLowerCase() + ",")
									&& !type.equals("-")) {
								// logger.debug("Target: Still in the game!
								// key="+key);
								String sType = getClassToColumnType().getProperty("noderefs", "-");
								if (getReplacementDataType().containsKey(key)) {
									sType = getReplacementDataType().getProperty(key, "-").trim();
								}
								if (logger.isDebugEnabled())
									logger.debug("Target: Setting " + key + "=" + sType);
								definition.setProperty(key, sType);
								// extensionPoint: Include username, or name
								// property of target
							}
						}
					} catch (Exception e) {
						logger.error("processAssociationDefinitions: Target_Association ERROR! key=" + key);
						// e.printStackTrace();
					}

					try {
						List<AssociationRef> sourceRefs = getNodeService().getSourceAssocs(nodeRef, type);
						if (sourceRefs.size() > 0) {
							logger.debug("Found a Source association! " + type.toString());

							key = type.toString();
							key = replaceNameSpaces(key);
							if (!getBlacklist().toLowerCase().contains("," + key.toLowerCase() + ",")
									&& !type.equals("-")) {
								String sType = getClassToColumnType().getProperty("noderefs", "-");
								if (getReplacementDataType().containsKey(key)) {
									sType = getReplacementDataType().getProperty(key, "-").trim();
								}
								definition.setProperty(key, sType);
								// extensionPoint: Include username, or name
								// property of source
							}
						}
					} catch (UnsupportedOperationException uoe) {
						// silent drop. On purpose. Because this is most likely
						// an operation against some
						// version store implementation of the nodeservice... we
						// don't bother...
					} catch (Exception e) {
						logger.warn("processAssociationDefinitions: Source_Association ERROR! key=" + key);
						logger.warn(" Messg: " + e.getMessage());
						logger.warn(" Cause: " + e.getCause());
						logger.warn(" Error: " + e.toString());
						// e.printStackTrace();
					}
				} // end exclude trx_orphan
			} // end for
		} catch (Exception e) {
			logger.warn("processAssociationDefinitions: source-target ERROR!");
			e.printStackTrace();

		}
		return definition;
	}

	// *************** Values *********************

	/**
	 * 
	 * @param rl
	 * @param sn
	 * @return
	 * @throws Exception
	 */
	private ReportLine processAssociationValues(ReportLine rl, NodeRef nodeRef) throws Exception {
		// Child References
		try {

			List<ChildAssociationRef> childCars = getNodeService().getChildAssocs(nodeRef);
			long maxChildCount = Math.min(childCars.size(), Integer
					.parseInt(getGlobalProperties().getProperty(Constants.property_treshold_child_assocs, "20")));
			long numberOfChildCars = childCars.size();

			if ((numberOfChildCars > 0) && (numberOfChildCars <= maxChildCount)) {

				String value = "";

				for (ChildAssociationRef car : childCars) {
					if (value.length() > 0)
						value += ",";
					value += car.getChildRef();
				}

				rl.setLine("child_noderef", getClassToColumnType().getProperty("noderefs", "-"), value,
						getReplacementDataType());
			}
		} catch (Exception e) {
			logger.warn("Error in processing processAssociationValues");
			e.printStackTrace();
		}

		// Parent References
		try {
			ChildAssociationRef parentCar = getNodeService().getPrimaryParent(nodeRef);
			if (parentCar != null) {
				String value = parentCar.getParentRef().toString();
				rl.setLine("parent_noderef", getClassToColumnType().getProperty("noderef", "-"), value,
						getReplacementDataType());
			}
		} catch (Exception e) {
			logger.warn("Exception in getting primary Parent noderef: " + e.getMessage());
		}

		// Other associations
		Collection<QName> assocTypes = getDictionaryService().getAllAssociations();
		for (QName type : assocTypes) {
			String shortName = replaceNameSpaces(type.toString());
			// logger.debug("ASSOCIATIONS: processing " + shortName + " for " +
			// sn.getTypeShort());
			if (shortName.startsWith("trx") || shortName.startsWith("act") || shortName.startsWith("wca")) {
				// nothing. Dont like these namespaces, that's all.
			} else {
				try {
					List<AssociationRef> targetRefs = getNodeService().getTargetAssocs(nodeRef, type);

					long maxChildCount = Math.min(targetRefs.size(), Integer.parseInt(
							getGlobalProperties().getProperty(Constants.property_treshold_soucetarget_assocs, "20")));
					long numberOfChildCars = targetRefs.size();

					if ((numberOfChildCars > 0) && (numberOfChildCars <= maxChildCount)) {

						String key = type.toString();
						key = replaceNameSpaces(key);
						if (!getBlacklist().toLowerCase().contains("," + key.toLowerCase() + ",")
								&& !type.equals("-")) {
							if ((targetRefs != null) && targetRefs.size() > 0) {
								String valueRef = "";
								for (AssociationRef ar : targetRefs) {
									if (valueRef.length() > 0)
										valueRef += ",";
									valueRef += ar.getTargetRef().toString();
								}
								rl.setLine(key, getClassToColumnType().getProperty("noderefs", "-"), valueRef,
										getReplacementDataType());
							}
						} // end if blacklist
							// extensionPoint: Include username, or name
							// property of target
					}
				} catch (Exception e) {
				}
				try {
					List<AssociationRef> sourceRefs = getNodeService().getSourceAssocs(nodeRef, type);
					long maxChildCount = Math.min(sourceRefs.size(), Integer.parseInt(
							getGlobalProperties().getProperty(Constants.property_treshold_soucetarget_assocs, "20")));
					long numberOfChildCars = sourceRefs.size();

					if ((numberOfChildCars > 0) && (numberOfChildCars <= maxChildCount)) {

						String key = type.toString();
						key = replaceNameSpaces(key);
						if (!getBlacklist().toLowerCase().contains("," + key.toLowerCase() + ",")
								&& !type.equals("-")) {
							if ((sourceRefs != null) && sourceRefs.size() > 0) {
								String value = "";
								for (AssociationRef ar : sourceRefs) {
									if (value.length() > 0)
										value += ",";
									value += ar.getSourceRef().toString();
								}
								rl.setLine(key, getClassToColumnType().getProperty("noderefs", "-"), value,
										getReplacementDataType());
							}
							// extensionPoint: Include username, or name
							// property of source
						} // end if blacklist
					}

				} catch (Exception e) {
				}
			} // it is not a trx_
		} // end or

		return rl;
	}

	@Override
	protected ReportLine processNodeToMap(String identifier, String table, ReportLine rl) {
		table = dbhb.fixTableColumnName(table);
		if (logger.isDebugEnabled())
			logger.debug("processNodeToMap, identifier=" + identifier);

		NodeRef masterRef = null;
		NodeRef nodeRef = new NodeRef(identifier.split(",")[0]);
		if (identifier.contains(",")) {
			masterRef = new NodeRef(identifier.split(",")[1]);
		}

		// NodeRef nodeRef=new NodeRef(identifier);
		if (logger.isDebugEnabled())
			logger.debug("Enter processNodeToMap nodeRef=" + nodeRef);
		try {
			// logger.debug("processNodeToMap: processing values");
			rl = processPropertyValues(rl, nodeRef);
		} catch (Exception e) {
			// logger.error("processprocessNodeToMap: That is weird,
			// processPropertyValues crashed! " + nodeRef);
			e.printStackTrace();
		}

		try {
			rl = processAssociationValues(rl, nodeRef);
		} catch (Exception e) {
			// logger.error("processNodeToMap: That is weird,
			// processAssociationValues crashed! " + nodeRef);
			e.printStackTrace();
		}

		try {
			rl.setLine(Constants.COLUMN_NODEREF, getClassToColumnType().getProperty("noderef"), nodeRef.toString(),
					getReplacementDataType());
		} catch (Exception e) {
			// logger.error("processNodeToMap: That is weird,
			// rl.setLine(noderef) crashed! " + nodeRef);
			e.printStackTrace();
		}

		try {
			String typeString = nodeService.getType(nodeRef).getLocalName(); // .getPrefixedQName(resolver);//
			// apparently for native documents (cm:content) the type == null.
			// Code below should fix
			if (typeString == null) {
				typeString = nodeService.getType(nodeRef).toString();
				typeString = typeString.substring(typeString.lastIndexOf(":"));
			}
			rl.setLine(Constants.COLUMN_OBJECT_TYPE,
					getClassToColumnType().getProperty(Constants.COLUMN_OBJECT_TYPE, ""), typeString,
					getReplacementDataType());
		} catch (Exception e) {
			// it does not have a type (??). Bad luck. Don't crash
			// (versionStore?!)
			logger.debug("EXCPTION1: // it does not have a Type. Bad luck. Don't crash (versionStore?!)");
			e.printStackTrace();

		}

		String aspectString = "";
		try {
			Set<QName> aspectsSet = nodeService.getAspects(nodeRef);
			Iterator<QName> aspectIterator = aspectsSet.iterator();
			while (aspectIterator.hasNext()) {
				QName aspect = aspectIterator.next();
				if (aspectString.length() > 0) {
					aspectString += ",";
				}

				aspectString += aspect.getLocalName(); // getPrefixedQName(resolver);//
														// getLocalName();
			} // end while

			rl.setLine(Constants.COLUMN_ASPECTS, getClassToColumnType().getProperty(Constants.COLUMN_ASPECTS, ""),
					aspectString, getReplacementDataType());
		} catch (Exception e) {
			// it does not have aspects. Bad luck. Don't crash (versionStore?!)
			// logger.debug("EXCPTION1: // it does not have Aspects. Bad luck.
			// Don't crash (versionStore?!)");
			e.printStackTrace();

		}

		Path path;
		String displayPath = "";
		try {
			path = getNodeService().getPath(nodeRef);
			displayPath = toDisplayPath(path);
			rl.setLine(Constants.COLUMN_PATH, getClassToColumnType().getProperty(Constants.COLUMN_PATH), displayPath,
					getReplacementDataType());
		} catch (Exception e) {
			// it does not have a path. Bad luck. Don't crash (versionStore?!)
			// logger.debug("EXCPTION1: // it is not in a site. Bad luck. Don't
			// crash (versionStore?!)");
			e.printStackTrace();
		}

		String site = "";
		try {
			site = getSiteName(nodeRef);
			rl.setLine(Constants.COLUMN_SITE, getClassToColumnType().getProperty(Constants.COLUMN_SITE), site,
					getReplacementDataType());
		} catch (Exception e) {
			// it is not in a site. Bad luck. Don't crash
		}
		QName myType = getNodeService().getType(nodeRef);

		if (getDictionaryService().isSubClass(myType, ContentModel.TYPE_FOLDER)) {
			NodeRef origNodeRef = null;
			try {
				if (nodeRef.toString().startsWith(StoreRef.PROTOCOL_ARCHIVE)) {
					ChildAssociationRef car = (ChildAssociationRef) nodeService.getProperty(nodeRef, QName
							.createQName("http://www.alfresco.org/model/system/1.0", "archivedOriginalParentAssoc"));

					logger.debug("ORIGIN: child:" + car.getChildRef() + " parent: " + car.getParentRef());
					origNodeRef = car.getChildRef();

				}
			} catch (Exception e) {
				logger.fatal("Exception getting orig_noderef" + e.getStackTrace());
			}

			if (origNodeRef != null) {
				// it is an archived thingy
				if (logger.isDebugEnabled())
					logger.debug("Setting Ref from archive to orig_noderef!!!");

				rl.setLine(Constants.COLUMN_ORIG_NODEREF, getClassToColumnType().getProperty("noderef"),
						origNodeRef.toString(), getReplacementDataType());
			} else {
				if (logger.isDebugEnabled())
					logger.debug("Setting currentRef to orig_noderef!!!");

				rl.setLine(Constants.COLUMN_ORIG_NODEREF, getClassToColumnType().getProperty("noderef"),
						nodeRef.toString(), getReplacementDataType());
			}
		}

		if (getDictionaryService().isSubClass(myType, ContentModel.TYPE_CONTENT)
				|| (getDictionaryService().getType(myType)).toString()
						.equalsIgnoreCase(ContentModel.TYPE_CONTENT.toString())) {
			long size = 0;
			String sizeString = "0";
			try {
				size = getFileFolderService().getFileInfo(nodeRef).getContentData().getSize();

				if (size == 0) {
					sizeString = "0";
				} else {
					sizeString = Long.toString(size);
				}
				// get the COLUMN_SIZE prop, being "size" for MySQL, but
				// "docsize" for Oracle
				rl.setLine(COLUMN_SIZE, getClassToColumnType().getProperty(COLUMN_SIZE), sizeString,
						getReplacementDataType());
			} catch (Exception e) {
				logger.info("processNodeToMap: Huh, no size?");
				sizeString = "0";
			}

			boolean versioned = false;

			try {
				versioned = getNodeService().hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE);
				rl.setLine("versioned", getClassToColumnType().getProperty("boolean"), String.valueOf(versioned),
						getReplacementDataType());
			} catch (Exception e) {
				logger.info("processNodeToMap: Huh, no versioned info?");
				e.printStackTrace();
			}

			try {
				String mimetype = getFileFolderService().getFileInfo(nodeRef).getContentData().getMimetype();
				if (mimetype == null)
					mimetype = "NULL";
				rl.setLine(Constants.COLUMN_MIMETYPE, getClassToColumnType().getProperty(Constants.COLUMN_MIMETYPE),
						mimetype, getReplacementDataType());
			} catch (Exception e) {
				logger.info("processNodeToMap: Huh, no mimetype?");
			}

			NodeRef origNodeRef = null;
			try {
				if (nodeRef.toString().startsWith(StoreRef.PROTOCOL_ARCHIVE)) {
					ChildAssociationRef car = (ChildAssociationRef) nodeService.getProperty(nodeRef, QName
							.createQName("http://www.alfresco.org/model/system/1.0", "archivedOriginalParentAssoc"));

					logger.debug("ORIGIN: child:" + car.getChildRef() + " parent: " + car.getParentRef());
					origNodeRef = car.getChildRef();

				}
			} catch (Exception e) {
				logger.warn("Exception getting orig_noderef" + e.getStackTrace());
			}

			try {
				if (nodeRef.toString().startsWith("version")) {
					// replace all archive-space references in the orig_noderef
					// into the workspace one.
					if (logger.isDebugEnabled()) {
						logger.debug("Setting nodeRef to orig_noderef - VERSION!!!");
						logger.debug("Master says: " + masterRef.toString());
					}

					rl.setLine(Constants.COLUMN_ORIG_NODEREF, getClassToColumnType().getProperty("noderef"),
							masterRef.toString(), getReplacementDataType());
				} else {
					if (origNodeRef != null) {
						// it is an archived thingy
						if (logger.isDebugEnabled())
							logger.debug("Setting Ref from archive to orig_noderef!!!");

						rl.setLine(Constants.COLUMN_ORIG_NODEREF, getClassToColumnType().getProperty("noderef"),
								origNodeRef.toString(), getReplacementDataType());
					} else {
						if (logger.isDebugEnabled())
							logger.debug("Setting currentRef to orig_noderef!!!");

						rl.setLine(Constants.COLUMN_ORIG_NODEREF, getClassToColumnType().getProperty("noderef"),
								nodeRef.toString(), getReplacementDataType());
					}

				}
			} catch (Exception e) {
				// don't crash... (versionStore?!)
				// logger.debug("EXCPTION: don't crash... (versionStore?!)");
			}
		} // end if
		else {
			if (logger.isDebugEnabled())
				logger.debug(myType.toString() + " is no content subclass!");
		}
		return rl;
	}

	public Properties processQueueDefinition(String table) {
		table = dbhb.fixTableColumnName(table);
		// logger.debug("Enter processQueueDefinition");
		Properties definition = new Properties(); // set of propname-proptype
		int queuesize = queue.size();
		for (int q = 0; q < queue.size(); q++) {
			// logger.debug("Queue contains: " + queue.get(q).toString());

			// remind, a queue entry can contain versionRef,nodeRef values
			NodeRef nodeRef = new NodeRef(queue.get(q).toString().split(",")[0]);
			try {
				String name = (String) getNodeService().getProperty(nodeRef, ContentModel.PROP_NAME);

				if (logger.isDebugEnabled())
					logger.debug("processQueueDefinition: " + q + "/" + queuesize + ": " + name);
				// Process Properties
				definition = processPropertyDefinitions(definition, nodeRef);
				// logger.debug("processQueueDefinition: Done processing
				// Properties");
				// logger.debug("processQueueDefinition: Returned from
				// processPropertyDefinitions");

				// if it is a versioned noderef, add the original noderef too
				// if ((getVersionNodes()!=null) &&
				// getVersionNodes().containsKey(nodeRef.toString())){
				definition.setProperty(Constants.COLUMN_ORIG_NODEREF,
						getClassToColumnType().getProperty("noderef", "-"));
				// }
			} catch (Exception e) {
				logger.warn("processQueueDefinition: ERROR: versionNodes.containsKey or before " + e.getMessage());
				e.printStackTrace();
			}
			// logger.debug("processQueueDefinition: try/catch survived");

			try {
				// Process 'manual' properties
				definition.setProperty(Constants.COLUMN_ORIG_NODEREF,
						getClassToColumnType().getProperty("noderef", "-"));
				definition.setProperty(Constants.COLUMN_SITE, getClassToColumnType().getProperty("site", "-"));
				definition.setProperty(Constants.COLUMN_PATH, getClassToColumnType().getProperty("path", "-"));
				definition.setProperty(Constants.COLUMN_NODEREF,
						getClassToColumnType().getProperty(Constants.COLUMN_NODEREF, "-"));
				definition.setProperty(Constants.COLUMN_OBJECT_TYPE,
						getClassToColumnType().getProperty(Constants.COLUMN_OBJECT_TYPE, "-"));
				definition.setProperty(Constants.COLUMN_ASPECTS,
						getClassToColumnType().getProperty(Constants.COLUMN_ASPECTS, "-"));
				// logger.debug("processQueueDefinition: custom props
				// survived");
				QName myType = getNodeService().getType(nodeRef);

				if (logger.isDebugEnabled())
					logger.debug("processQueueDefinition: qname=" + myType);

				if (getDictionaryService().isSubClass(myType, ContentModel.TYPE_CONTENT)
						|| (getDictionaryService().getType(myType)).toString()
								.equalsIgnoreCase(ContentModel.TYPE_CONTENT.toString())) {
					if (logger.isDebugEnabled())
						logger.debug("processQueueDefinition: YEAH! We are a subtype of Content! "
								+ ContentModel.TYPE_CONTENT);
					definition.setProperty(Constants.COLUMN_MIMETYPE,
							getClassToColumnType().getProperty(Constants.COLUMN_MIMETYPE, "-"));
					definition.setProperty(COLUMN_SIZE, getClassToColumnType().getProperty(COLUMN_SIZE, "-"));

					// and some stuff default reporting is dependent on
					definition.setProperty("cm_workingCopyLlink", getClassToColumnType().getProperty("noderef", "-"));
					definition.setProperty("cm_lockOwner", getClassToColumnType().getProperty("noderef", "-"));
					definition.setProperty("cm_lockType", getClassToColumnType().getProperty("noderef", "-"));
					definition.setProperty("cm_expiryDate", getClassToColumnType().getProperty("datetime", "-"));
					definition.setProperty("sys_archivedDate", getClassToColumnType().getProperty("datetime", "-"));
					definition.setProperty("sys_archivedBy", getClassToColumnType().getProperty("noderef", "-"));
					definition.setProperty("sys_archivedOriginalOwner",
							getClassToColumnType().getProperty("noderef", "-"));

					definition.setProperty("versioned", getClassToColumnType().getProperty("noderef", "-"));
				} else {
					logger.debug("processQueueDefinition: NOOOOO! We are NOT a subtype of Content!");
				}
				if (getDictionaryService().isSubClass(myType, ContentModel.TYPE_PERSON)) {
					definition.setProperty("enabled", getClassToColumnType().getProperty("boolean", "-"));
				}
				// QName objectType = getNodeService().getType(nodeRef);
				if ((getNodeService().hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE))) {
					// (getDictionaryService().isSubClass(objectType,
					// ContentModel.TYPE_CONTENT))){
					String type = getClassToColumnType().getProperty("text", "-");
					if (getReplacementDataType().containsKey("cm_versionLabel")) {
						type = getReplacementDataType().getProperty("cm_versionLabel", "-").trim();
					}
					definition.setProperty("cm_versionLabel", type);

					type = getClassToColumnType().getProperty("text", "-");
					if (getReplacementDataType().containsKey("cm_versionType")) {
						type = getReplacementDataType().getProperty("cm_versionType", "-").trim();
					}
					definition.setProperty("cm_versionType", type);

				}
			} catch (Exception e) {
				logger.info("unexpeted error in node " + nodeRef.toString());
				logger.info("unexpeted error in node " + e.getMessage());
			}
			// logger.debug("processQueueDefinition: content specific props
			// survived");
			// Process Associations
			try {
				definition = processAssociationDefinitions(definition, nodeRef);
			} catch (Exception e) {
				logger.warn("Error getting assoc definitions" + e.getMessage());
				e.printStackTrace();
			}

		} // end for sn:queue
		// logger.debug("Exit processQueueDefinition");
		return definition;
	}

	public void processQueueValues(String table) throws Exception {
		if (logger.isDebugEnabled())
			logger.debug("Enter processQueueValues table=" + table);

		// Statement stmt = null;
		// Properties tableDesc = dbhb.getTableDescription(stmt, table);

		if (logger.isDebugEnabled())
			logger.debug("************ Found " + queue.size() + " entries in " + table + " **************** " + method);
		ReportLine rl = new ReportLine(table, getSimpleDateFormat(), reportingHelper);

		// Create the processing object once...

		long now_before = (new Date()).getTime();
		int queuesize = queue.size();
		for (int q = 0; q < queue.size(); q++) {
			String identifier = queue.get(q).toString();
			try {
				NodeRef nodeRef = new NodeRef(identifier.split(",")[0]);

				if (logger.isDebugEnabled()) {
					String name = (String) getNodeService().getProperty(nodeRef, ContentModel.PROP_NAME);
					logger.debug("processQueueValues: " + q + "/" + queuesize + ": " + name);
				}

				// run each queue entry through the processing object
				rl = processNodeToMap(identifier, table, rl);

				int numberOfRows = 0;
				if (logger.isDebugEnabled())
					logger.debug("Current method=" + this.method);
				try { // SINGLE_INSTANCE,
						// logger.debug(method + " ##### " + rl.size());
					if ((rl.size() > 0) /*
										 * &&
										 * (rl.getValue("sys_node_uuid").length(
										 * )>5)
										 */) {
						// logger.debug("method="+method+" && row exists?");

						if (this.method.equals(Constants.INSERT_ONLY)) {
							// if (logger.isDebugEnabled()) logger.debug("Going
							// INSERT_ONLY");

							numberOfRows = dbhb.insertIntoTable(rl);
							// logger.debug(numberOfRows+ " rows inserted");
						}

						// -------------------------------------------------------------

						if (this.method.equals(Constants.SINGLE_INSTANCE)) {
							// if (logger.isDebugEnabled()) logger.debug("Going
							// SINGLE_INSTANCE");

							if (dbhb.rowExists(rl)) {
								numberOfRows = dbhb.updateIntoTable(rl);
								// logger.debug(numberOfRows+ " rows updated");
							} else {
								numberOfRows = dbhb.insertIntoTable(rl);
								// logger.debug(numberOfRows+ " rows inserted");

							}

						}

						// -------------------------------------------------------------

						if (this.method.equals(Constants.UPDATE_VERSIONED)) {
							if (logger.isDebugEnabled())
								logger.debug("Going UPDATE_VERSIONED");
							try {
								if (dbhb.rowExists(rl) && ) {
									numberOfRows = dbhb.updateVersionedIntoTable(rl);
									// numberOfRows = dbhb.insertIntoTable(rl);
									if (logger.isDebugEnabled())
										logger.debug(numberOfRows + " rows updated");
								} else {
									if (logger.isDebugEnabled()) {
										logger.debug("No rows exist");
										logger.debug("## Set " + rl.getInsertListOfKeys());
										logger.debug("## Values " + rl.getInsertListOfValues());
									}
									numberOfRows = dbhb.insertIntoTable(rl);
									if (logger.isDebugEnabled())
										logger.debug(numberOfRows + " rows inserted");

								}
							} catch (org.springframework.dao.RecoverableDataAccessException rdae) {
								throw new AlfrescoRuntimeException("processQueueValues1: " + rdae.getMessage());
							} catch (Exception ee) {
								ee.printStackTrace();
								logger.fatal("processQueueValues Exception1: " + ee.getMessage());
							}
						}
					} // end if rl.size>0

				} catch (org.springframework.dao.RecoverableDataAccessException rdae) {
					throw new AlfrescoRuntimeException("processQueueValues2: " + rdae.getMessage());
				} catch (Exception e) {
					logger.fatal("processQueueValues Exception2: " + e.getStackTrace());
					e.printStackTrace();
				} finally {
					rl.reset();
				}
			} catch (Exception e) {
				logger.info("Bad node detected; ignoring... " + identifier);
			}
		} // end for scriptnode in queue

		long now_after = (new Date()).getTime();

		if (logger.isInfoEnabled()) {

			logger.info("processQueueValues summary: " + queuesize + " rows in " + (now_after - now_before) + "ms = "
					+ (now_after - now_before) / queuesize + "ms per row");
		}

		if (logger.isDebugEnabled())
			logger.debug("Exit processQueueValues");
	}

	public void addToQueue(Object nodeRef, Properties versionThingy) {
		addToQueue(nodeRef);
		// now what
	}

	public void addToQueue(final Object nodeRef, final NodeRef masterRef) {

		addToQueue(nodeRef.toString() + "," + masterRef.toString());
		// now what
	}

	public boolean stillContinueHarvesting(long loopcount) {
		return stillContinueHarvesting(loopcount, 1);
	}

	public boolean stillContinueHarvesting(long loopcount, int resultSize) {
		boolean returnBoolean = true;
		if (getMaxLoopCount() != 0) {
			returnBoolean = (loopcount <= getMaxLoopCount());
		}
		try {
			returnBoolean = returnBoolean && (resultSize > 0);
		} catch (Exception e) {
			// leave it, if resultSize==null, return true too
		}
		return returnBoolean;
	}

	/**
	 * Harvests 1) objects 2) closed tasks after date x
	 */
	public void havestNodes(final NodeRef harvestDefinition) {
		/*
		 * for each store:stores for each table:tables while continue search
		 * continue = search.size>0 for each result:resultset if isVersioned
		 * addAllOlderVersionsToQueue(result) addToQueue(result) processQueue()
		 * // end while continue // end for each table // end for each store
		 */
		logger.info("harvest run start...");
		;
		try {
			final ArrayList<StoreRef> storeList = getStoreRefList();

			final Properties queries = getTableQueries(harvestDefinition);
			final String language = reportingHelper.getSearchLanguage(harvestDefinition);

			// Make sure there is a connection
			dbhb.openReportingConnection();

			Enumeration<Object> keys = queries.keys();

			String fullQuery; // is the actual query appending orderby
								// node-dbid, and lastmodifued clause

			// Cycle all Lucene queries
			while (keys.hasMoreElements()) {

				String tableName = (String) keys.nextElement();
				String query = (String) queries.get(tableName);

				tableName = dbhb.fixTableColumnName(tableName);

				if (logger.isDebugEnabled())
					logger.debug("harvest: preparing table =" + tableName);

				// get a clean iterator to cycle all stores
				Iterator<StoreRef> storeIterator = storeList.iterator();
				Date lastModified = null;

				boolean archiveAllowed = false;

				// prevent having two threads doing the same
				if (!dbhb.tableIsRunning(tableName)) {

					String nowFormattedDate = getNowAsFormattedString();

					String timestamp = "";
					// old school all-in-one harvesting
					if (!getBatchTimestampEnabled()) {
						dbhb.setLastTimestampStatusRunning(tableName);
						timestamp = dbhb.getLastTimestamp(tableName);
					} // else, see 5 lines below

					dbhb.createEmptyTables(tableName);
					int maxItems = getMaxLoopSize();

					while (storeIterator.hasNext()) {

						StoreRef storeRef = storeIterator.next();

							// mew insight, limit the number of loops, treat
							// mechanism with more care
							if (getBatchTimestampEnabled()) {
								dbhb.setLastTimestampStatusRunning(
										tableName + "_" + storeRef.getProtocol().substring(0, 1));
								timestamp = dbhb
										.getLastTimestamp(tableName + "_" + storeRef.getProtocol().substring(0, 1));
							}

							if (logger.isDebugEnabled())
								logger.debug("harvest: StoreRef=" + storeRef.getProtocol() + ", archiveAllowed="
										+ archiveAllowed);

							// (re)set some essential process markers.
							// These are local to the run-per-storeRef
							long startDbId = 0; // the original database id of
												// the noderef
							long loopcount = 0; // count the number of
												// harvesting loops. Should be
												// <=2 for initial harvesting
												// agaist factory repo
							boolean letsContinue = true;

							// break if we process the archive before the
							// workspace is done...
							if (storeRef.getProtocol().equals(StoreRef.PROTOCOL_ARCHIVE) && !archiveAllowed) {
								letsContinue = false;
							}

							if (logger.isDebugEnabled())
								logger.debug("harvest: before while, letsContinue=" + letsContinue);

							while (letsContinue) {
								loopcount++;

								// hasEverRun is needed to determine if an
								// update of lastModifiedTimestamp has occured
								// ever in a batch, or never.
								boolean hasEverRun = false;
								if (logger.isInfoEnabled()) {
									logger.info("++ Loop number: " + loopcount + ", tablename: " + tableName
											+ ", archive: " + archiveAllowed);
								}
								if (getBatchTimestampEnabled()) { // default =
																	// true
									nowFormattedDate = getNowAsFormattedString();
								}

								fullQuery = query + queryClauseTimestamp(language, timestamp, storeRef.getProtocol())
										+ queryClauseOrderBy(language, startDbId, storeRef.getProtocol());

								if (logger.isDebugEnabled()) {
									logger.debug("harvest: StoreProtocol = " + storeRef.getProtocol() + ", archive: "
											+ archiveAllowed + "\nfullQuery = " + fullQuery);
								}

								SearchParameters sp = new SearchParameters();
								sp.setLanguage(language);
								sp.addStore(storeRef);
								sp.setQuery(fullQuery);
								// sp.addSort(getOrderBy(language), true);
								if (maxItems > 0) {
									sp.setMaxItems(maxItems);
								}
								if (logger.isDebugEnabled()) {
									logger.debug("Searchparameter query: " + sp.getQuery());
								}
								ResultSet results = getSearchService().query(sp);

								if (logger.isDebugEnabled())
									logger.debug("harvest: prepare flipping: archiveAllowed=" + archiveAllowed
											+ ", length=" + results.length());

								// allow harvesting the archive if the workspace
								// has been done!
								// workspace is done if there are no more search
								// results
								if (results.length() == 0 && !archiveAllowed) {
									if (logger.isDebugEnabled())
										logger.debug("harvest: flipping to archiveAllowed=true");
									archiveAllowed = true;
								}

								letsContinue = stillContinueHarvesting(loopcount, results.length());

								logger.debug("harvest: loopcount= " + loopcount + "\n" + "harvest: resultsize   = "
										+ results.length() + "\n" + "harvest: letsContinue = " + letsContinue + "\n"
										+ "harvest: archiveAllow = " + archiveAllowed + "\n"
										+ "harvest: tablename    = " + tableName);
								SimpleDateFormat sdf = getSimpleDateFormat();

								if (letsContinue) {

									Iterator<ResultSetRow> resultsIterator = results.iterator();
									while (resultsIterator.hasNext()) {
										try { // be tolerant for non-existing
												// nodes... happens to hurt
												// leaving status=Running
											NodeRef result = resultsIterator.next().getNodeRef();
											logger.debug("harvest noderef " + result);
											if (!storeRef.getProtocol().equalsIgnoreCase("archive")) {

												if (getNodeService().hasAspect(result, ContentModel.ASPECT_VERSIONABLE)
														// versionService.isVersioned(result)
														&& versionService.getVersionHistory(result).getAllVersions()
																.size() > 1) {
													VersionHistory vh = versionService.getVersionHistory(result);
													Iterator<Version> vhi = vh.getAllVersions().iterator();
													String latestVersionLabel = (String) nodeService.getProperty(
															vh.getHeadVersion().getVersionedNodeRef(),
															ContentModel.PROP_VERSION_LABEL);
													// Date latestDate =
													// (Date)nodeService.getProperty(result,
													// ContentModel.PROP_MODIFIED);
													while (vhi.hasNext()) {
														Version version = vhi.next();
														String currentVersionLabel = version.getVersionLabel();
														// Date versionDate =
														// version.getFrozenModifiedDate();
														// logger.debug("comparing:
														// " +
														// currentVersionLabel +
														// "/" +
														// latestVersionLabel
														// );//+ " and " +
														// sdf.format(versionDate)
														// +"/"+
														// sdf.format(latestDate));
														if (!currentVersionLabel.equals(latestVersionLabel)) {
															if (logger.isInfoEnabled())
																logger.info(
																		"harvest: Adding Version " + currentVersionLabel
																				+ " " + version.getFrozenStateNodeRef()
																				+ " - " + result.toString()); // version.getVersionedNodeRef());
															addToQueue(version.getFrozenStateNodeRef(), result);
														} else {
															if (logger.isDebugEnabled())
																logger.info("Ignoring version " + currentVersionLabel);
														} // end ifelse

													} // end while
												} // id if
													// hasAspect(versionable)

											} // end exclude Archive

											// all versions should be post-fixed
											// with their head version workspace
											// ref
											if (!result.toString().startsWith("version")) {
												if (logger.isDebugEnabled())
													logger.debug("harvest: " + " adding NodeRef " + result);
												addToQueue(result);
											}
										} catch (Exception e) {
											// ignore, we need to buffer for
											// non-existing nodes...
											logger.info("NodeRef appears broken: " + e.getMessage());
											logger.info("   " + e.getStackTrace());
										}
									} // end resultsIterator

									try {
										// process the current queue
										Properties props = processQueueDefinition(tableName);
										if (logger.isDebugEnabled())
											logger.debug("harvest: queueDef done, setting tableDefinition");

										setTableDefinition(tableName, props);
										if (logger.isDebugEnabled())
											logger.debug("harvest: tableDef done. Processing queue Values");

										processQueueValues(tableName);

										// prep the queue for the next run
										resetQueue();

										if (results.length() > 0) {
											// prepare the dbid for the next run
											startDbId = Long.parseLong(String.valueOf(getNodeService().getProperty(
													results.getNodeRef(results.length() - 1),
													ContentModel.PROP_NODE_DBID)));

											lastModified = (Date) getNodeService().getProperty(
													results.getNodeRef(results.length() - 1),
													ContentModel.PROP_MODIFIED);
											if (logger.isDebugEnabled()) {
												logger.debug("harvest: StoreProtocol = " + storeRef.getProtocol());
												logger.debug("harvest: New start DBID=" + startDbId);
												logger.debug("harvest: New lastModified=" + lastModified);
											}
										}
									} catch (Exception e) {
										logger.info("harvest: something wrong with the noderef, skipping");
									}

									if ((results.length() > 0) && getBatchTimestampEnabled()
											&& (lastModified != null)) {
										if (logger.isDebugEnabled())
											logger.debug("Setting Batch-based timestamp: "
													+ getDateAsFormattedString(lastModified));
										dbhb.setLastTimestamp(tableName + "_" + storeRef.getProtocol().substring(0, 1),
												getDateAsFormattedString(lastModified));
										hasEverRun = true;
									}

								} // end if(letsContinue)
								if ((!letsContinue) && (results.length() == 0)) {
									// register lastruntimestamp anyway
									if (hasEverRun) {
										dbhb.setAllStatusesDoneForTable();
									} else {
										dbhb.setLastTimestampAndStatusDone(
												tableName + "_" + storeRef.getProtocol().substring(0, 1),
												nowFormattedDate);
									}
								}
								letsContinue = stillContinueHarvesting(loopcount, results.length());
							} // end while letsContinue

					} // end storeProtocol

					if (getBatchTimestampEnabled()) {
						// dbhb.setLastTimestamp(tableName,
						// getDateAsFormattedString(lastModified));
						if (lastModified != null) {
							if (logger.isDebugEnabled())
								logger.debug("Setting Batch-based status to done");
							dbhb.setAllStatusesDoneForTable();
						}
					} else {
						if (logger.isDebugEnabled())
							logger.debug("Setting end-last-run-based timestamp");
						dbhb.setLastTimestampAndStatusDone(tableName, nowFormattedDate);
					}
					// startDbId=0;

				} // end if tableIsRunning
				else {
					if (logger.isDebugEnabled())
						logger.debug("harvest: table is running; leaving...");
				}
			} // end while keys

		} catch (Exception e) {
			// e.printStackTrace();
			logger.info("Fatality: " + e);
		} finally {
			// make sure we gently close the connection
			dbhb.closeReportingConnection();
		}
		logger.info("harvest run done...");
	}

	private ArrayList<StoreRef> getStoreRefList() {
		String[] stores = reportingHelper.getGlobalProperties().getProperty(Constants.property_storelist, "")
				.split(",");
		ArrayList<StoreRef> storeRefArray = new ArrayList<StoreRef>();

		for (String store : stores) {
			logger.debug("Adding store: " + store);
			StoreRef s = new StoreRef(store);
			storeRefArray.add(s);
		}
		return storeRefArray;
	}

	// Helper methods to build the queue and process the queue
	public String queryClauseTimestamp(final String language, final String timestamp, String protocol) {
		String dateQuery = " ";

		final String myTimestamp = timestamp.replaceAll(" ", "T");
		if (SearchService.LANGUAGE_LUCENE.equalsIgnoreCase(language)) {
			if (protocol.equalsIgnoreCase(StoreRef.PROTOCOL_WORKSPACE)) {
				dateQuery += "AND @cm\\:modified:[" + myTimestamp + " TO NOW]";
			}
			if (protocol.equalsIgnoreCase(StoreRef.PROTOCOL_ARCHIVE)) {
				dateQuery += "AND @sys\\:archivedDate:[" + myTimestamp + " TO NOW]";
			}
		}
		if (SearchService.LANGUAGE_CMIS_ALFRESCO.equalsIgnoreCase(language)) {
			if (protocol.equalsIgnoreCase(StoreRef.PROTOCOL_WORKSPACE)) {
				// dateQuery += "AND @cm\\:modified:[" + myTimestamp + " TO
				// NOW]";
				// TODO
			}
			if (protocol.equalsIgnoreCase(StoreRef.PROTOCOL_ARCHIVE)) {
				// dateQuery += "AND @sys\\:archivedDate:[" + myTimestamp + " TO
				// NOW]";
				// TODO
			}
		}
		return dateQuery;
	}

	public String queryClauseOrderBy(final String language, final long dbid, final String protocol) {
		String orderBy = " ";
		if (SearchService.LANGUAGE_LUCENE.equalsIgnoreCase(language)) {
			if ((dbid > 0)
			// && (protocol.equalsIgnoreCase("workspace"))
			) {
				orderBy += "AND @sys\\:node-dbid:[" + (dbid + 1) + " TO MAX]";
			}
		}

		if (SearchService.LANGUAGE_CMIS_ALFRESCO.equalsIgnoreCase(language)) {
			// TODO
		}
		return orderBy;
	}

	public String getOrderBy(final String language) {
		String orderBy = " ";
		if (SearchService.LANGUAGE_LUCENE.equalsIgnoreCase(language)) {
			orderBy += ContentModel.PROP_NODE_DBID.toPrefixString();// "@sys\\:node-dbid";
			// orderBy += "sys:node-dbid";
			orderBy = QueryParser.escape(orderBy);

		}
		if (SearchService.LANGUAGE_CMIS_ALFRESCO.equalsIgnoreCase(language)) {
			// TODO
		}
		return orderBy;
	}

	// public String queryClauseGetStartAtDbid(String language, long startDbId){
	// return "";
	// }

	private Properties getTableQueries(final NodeRef nodeRef) {
		Properties p = new Properties();
		if (logger.isDebugEnabled()) {
			logger.debug("getTableQueries, nodeRef=" + nodeRef);
		}
		try {
			ContentService cs = getContentService();
			ContentReader contentReader = cs.getReader(nodeRef, ContentModel.PROP_CONTENT);
			p.load(contentReader.getContentInputStream());
		} catch (ContentIOException e) {
			logger.error(e.getMessage());
			// e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}

		return p;
	}

}
