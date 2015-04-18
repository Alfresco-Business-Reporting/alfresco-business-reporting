var method = args["method"];
var table = args["table"];
var details = args["details"];
var noderef = args["noderef"];
//var allTables="calendar,datalistitem,datalist,document,folder,forum,groups,lastsuccessfulrun,link,person,site,sitepersons";

var message = "-1";
var BASEPATH = "sites/swsdp"

var testHarvesting="harvestingTempQuery";


var testSite = "swdp1";
var reportingRoot = companyhome.childByNamePath("Data Dictionary/Reporting");

var queries = new Object();
queries["folder"]="folder=TYPE:\"cm:folder\" AND NOT TYPE:\"st:site\" AND NOT TYPE:\"dl:dataList\" AND NOT TYPE:\"bpm:package\" AND NOT TYPE:\"cm:systemfolder\" AND NOT TYPE:\"fm:forum\" AND NOT TYPE:\"trx:transferGroup\" AND NOT TYPE:\"reporting:reportingRoot\" AND NOT TYPE:\"reporting:reportingContainer\" AND NOT TYPE:\"reporting:harvestDefinition\""; 
queries["document"]="document=TYPE:\"cm:content\" AND NOT TYPE:\"bpm:task\" AND NOT TYPE:\"dl:dataListItem\" AND NOT TYPE:\"ia:calendarEvent\" AND NOT TYPE:\"lnk:link\" AND NOT TYPE:\"cm:dictionaryModel\" AND NOT ASPECT:\"reporting:executionResult\" AND NOT TYPE:\"reporting:reportTemplate\"";
//                           TYPE:"cm:content" AND NOT TYPE:"bpm:task" AND NOT TYPE:"dl:dataListItem"         AND NOT TYPE:"ia:calendarEvent" AND NOT TYPE:"lnk:link" AND NOT TYPE:"cm:dictionaryModel" AND NOT ASPECT:"reporting:executionResult"
queries["calendar"]="calendar=TYPE:\"ia:calendarEvent\"";
queries["forum"]="forum=TYPE:\"fm:forum\"";
queries["link"]="link=TYPE:\"lnk:link\"";
queries["site"]="ite=TYPE:\"st:site\"";
queries["datalist"]="datalist=TYPE:\"dl:dataList\"";
queries["datalistitem"]="datalistitem=TYPE:\"dl:dataListItem\"";
// topic
logger.log(queries[table]);

function createHarvestDef(query, table, queryTablesEnabled, auditingEnabled,
		categoriesEnabled, userGroupsEnabled, categories, auditingQueries){
  var harvestDef = reportingRoot.createNode(testHarvesting, "reporting:harvestDefinition");
  harvestDef.properties["reporting:queryTablesEnabled"] = queryTablesEnabled;
  harvestDef.properties["reporting:auditingExportsEnabled"] = auditingEnabled;
  harvestDef.properties["reporting:categoriesEnabled"] = categoriesEnabled;
  harvestDef.properties["reporting:usersGroupsEnabled"] = userGroupsEnabled;

  harvestDef.properties["reporting:categories"] = null;
  harvestDef.properties["reporting:auditingQueries"] = auditingQueries;
  harvestDef.properties["reporting:queryTablesLanguage"] = "Lucene";
  var content = query;
  harvestDef.content = content;
  harvestDef.save();

  return harvestDef;
}

function createHarvestDefAndHarvest(query, table, queryTablesEnabled, auditingEnabled,
		categoriesEnabled, userGroupsEnabled, categories, auditingQueries){

  var harvestDef = createHarvestDef(query, table, queryTablesEnabled, auditingEnabled,
		categoriesEnabled, userGroupsEnabled, categories, auditingQueries);

  var harvest = actions.create("harvesting-executer");
  harvest.execute(harvestDef);
  message = executeSearchAndCount(table);
  harvestDef.remove();
}

function executeSearchAndCount(tablename){
  var table=queries[tablename];
  if (table){
    var query = table.substring(table.indexOf("=")+1, table.length);
    return reporting.countSearchResutls(query);
  } else {
    return 0;
  }
}

function getInsertFolder(folderExtension){
  logger.log(BASEPATH+folderExtension);
  return companyhome.childByNamePath(BASEPATH+folderExtension);
}


// ****************************
// method == havest
if ((method.toLowerCase()=="harvest") && (table!=null)){

  if (table.toLowerCase()=="calendar"){
    createHarvestDefAndHarvest(queries["calendar"], "calendar", true, false, false, false, null, null);
  }
  if (table.toLowerCase()=="document"){
    createHarvestDefAndHarvest(queries["document"], "document", true, false, false, false, null, null);
  }
  if (table.toLowerCase()=="folder"){
    createHarvestDefAndHarvest(queries["folder"], "folder", true, false, false, false, null, null);
  }
  if (table.toLowerCase()=="forum"){
    createHarvestDefAndHarvest(queries["forum"], "forum", true, false, false, false, null, null);
  }
  if (table.toLowerCase()=="site"){
    createHarvestDefAndHarvest(queries["site"], "site", true, false, false, false, null, null);
  }
  if (table.toLowerCase()=="link"){
    createHarvestDefAndHarvest(queries["link"], "link", true, false, false, false, null, null);
  }
  if (table.toLowerCase()=="datalist"){
    createHarvestDefAndHarvest(queries["datalist"], "datalist", true, false, false, false, null, null);
  }
  if (table.toLowerCase()=="datalistitem"){
    createHarvestDefAndHarvest(queries["datalistitem"], "datalistitem", true, false, false, false, null, null);
  }
/*
  if (table.toLowerCase()=="person"){
    createHarvestDefAndHarvest(null, "person", false, false, false, true, null, null);
  }

  if (table.toLowerCase()=="auditingQueries"){
    createHarvestDefAndHarvest(null, "ReportingLoginAudit", false, false, true, false, null, "ReportingLoginAudit");
  }
*/
  message = executeSearchAndCount(table);
}

// ********************v********
// method == count
if ((method.toLowerCase()=="count") && (table!=null)){

  if (table.toLowerCase()=="calendar"){
    message = executeSearchAndCount("calendar");
  }
  if (table.toLowerCase()=="document"){
    message = executeSearchAndCount("document");
  }
  if (table.toLowerCase()=="folder"){
    message = executeSearchAndCount("folder");
  }
  if (table.toLowerCase()=="forum"){
    message = executeSearchAndCount("forum");
  }
  if (table.toLowerCase()=="site"){
    message = executeSearchAndCount("site");
  }
  if (table.toLowerCase()=="link"){
    message = executeSearchAndCount("link");
  }
  if (table.toLowerCase()=="datalistitem"){
    message = executeSearchAndCount("datalistitem");
  }
// TODO Groups, Persons, SitePersons

}

function addTag(){
      var thingy = search.findNode(noderef);
      thingy.addTag( (new Date()).getTime() );
      thingy.save();
}

function updateName(){
  var thingy = search.findNode(noderef);
  var newName = "New-name-" + (new Date()).getTime();
  if (thingy.isContainer){
    thingy.properties["cm:name"] = newName;
  } else {
    thingy.properties["cm:name"] = newName + ".txt";
  }
  thingy.save();
}

function updateTitle(){

  var thingy = search.findNode(noderef);
  logger.log("NAME " + thingy.name);
  thingy.properties["cm:title"] = "New title";
  thingy.save();
}

function updateDescription(){

  var thingy = search.findNode(noderef);
  thingy.properties["cm:description"] = "New description";
  thingy.save();
}

if (method.toLowerCase()=="update"){

  // #################### Document ################### 
  if (table.toLowerCase()=="document"){

    if (details.toLowerCase()=="create"){
      var parentFolder = getInsertFolder("/documentLibrary");
      var filename = (new Date()).getTime() + ".txt";
      var type = "cm:content";
      var props = new Array();
      props["cm:title"]="This is the document title";
      props["cm:description"]="This is the document description " + filename;
      var thisNode = parentFolder.createNode(filename, type, props);
      thisNode.content = "This is the content " + filename;
      thisNode.addAspect("cm:versionable");
      noderef = thisNode.nodeRef.toString();
    }


    if (details.toLowerCase()=="addtag"){
	addTag()
    }

    if (details.toLowerCase()=="updatename") {
      updateName();
    }

    if (details.toLowerCase()=="updatetitle") {
      updateTitle();
    }

    if (details.toLowerCase()=="updatedescription") {
      updateDescription();
    }

  } // end document

  // #################### DataListItem ################### 
  if (table.toLowerCase()=="datalistitem"){

    if (details.toLowerCase()=="create"){
      var parentFolder = getInsertFolder("/dataLists");
      var filename = (new Date()).getTime() + ".txt";
      var type = "dl:dataListItem";
      var props = new Array();
      props["cm:title"]="This is the datalistitem title";
      props["cm:description"]="This is the datalistitem description " + filename;
      var thisNode = parentFolder.createNode(filename, type, props);
      noderef = thisNode.nodeRef.toString();
    }


    if (details.toLowerCase()=="addtag"){
	addTag()
    }

    if (details.toLowerCase()=="updatename") {
      updateName();
    }

    if (details.toLowerCase()=="updatetitle") {
      updateTitle();
    }

    if (details.toLowerCase()=="updatedescription") {
      updateDescription();
    }

  } // end datalistitem

  // #################### DataList ################### 
  if (table.toLowerCase()=="datalist"){

    if (details.toLowerCase()=="create"){
      var parentFolder = getInsertFolder("/dataLists");
      var filename = (new Date()).getTime();
      var type = "dl:dataList";
      var props = new Array();
      props["cm:title"]="This is the datalistitem title";
      props["cm:description"]="This is the datalistitem description " + filename;
      var thisNode = parentFolder.createNode(filename, type, props);
      noderef = thisNode.nodeRef.toString();
    }


    if (details.toLowerCase()=="addtag"){
	addTag()
    }

    if (details.toLowerCase()=="updatename") {
	// do nothing!
    }

    if (details.toLowerCase()=="updatetitle") {
      updateTitle();
    }

    if (details.toLowerCase()=="updatedescription") {
      updateDescription();
    }

  } // end datalist

  // #################### Folder ################### 
  if (table.toLowerCase()=="folder") {

    if (details.toLowerCase()=="create"){
      var parentFolder = getInsertFolder("/documentLibrary");
      var filename = (new Date()).getTime();
      var type = "cm:folder";
      var props = new Array();
      props["cm:title"]="This is the folder title";
      props["cm:description"]="This is the folder description " + filename;
      var thisNode = parentFolder.createNode(filename, type, props);
      noderef = thisNode.nodeRef.toString();
    }

    if (details.toLowerCase()=="addtag"){
	addTag()
    }

    if (details.toLowerCase()=="updatename") {
      updateName();
    }

    if (details.toLowerCase()=="updatetitle") {
      updateTitle();
    }

    if (details.toLowerCase()=="updatedescription") {
      updateDescription();
    }
  } // end folder

  // #################### Calendar ################### 
  if (table.toLowerCase()=="calendar") {

    if (details.toLowerCase()=="create"){
      var parentFolder = getInsertFolder("/calendar");
      var filename = (new Date()).getTime()+".ics";
      var type = "ia:calendarEvent";
      var props = new Array();
      props["cm:title"]="This is the calendar title";
      props["cm:description"]="This is the calendar description " + filename;
      var thisNode = parentFolder.createNode(filename, type, props);
      noderef = thisNode.nodeRef.toString();
    }

    if (details.toLowerCase()=="addtag"){
	addTag()
    }

    if (details.toLowerCase()=="updatename") {
      updateName();
    }

    if (details.toLowerCase()=="updatetitle") {
      updateTitle();
    }

    if (details.toLowerCase()=="updatedescription") {
      updateDescription();
    }
  } // end calendar

  // #################### Forum ################### 
  if (table.toLowerCase()=="forum") {

    if (details.toLowerCase()=="create"){
      var parentFolder = getInsertFolder("/blog");
      var filename = (new Date()).getTime()+"-forum";
      var type = "fm:forums"; // sub of folder
      var props = new Array();
      props["cm:title"]="This is the forum title";
      props["cm:description"]="This is the forum description " + filename;
      var thisNode = parentFolder.createNode(filename, type, props);
      noderef = thisNode.nodeRef.toString();
    }

    if (details.toLowerCase()=="addtag"){
	addTag()
    }

    if (details.toLowerCase()=="updatename") {
      updateName();
    }

    if (details.toLowerCase()=="updatetitle") {
      updateTitle();
    }

    if (details.toLowerCase()=="updatedescription") {
      updateDescription();
    }
  } // end forum

  // #################### Link ################### 
  if (table.toLowerCase()=="link") {

    if (details.toLowerCase()=="create"){
      var parentFolder = getInsertFolder("/links");
      var filename = (new Date()).getTime()+"-link";
      var type = "lnk:link";
      var props = new Array();
      props["cm:title"]="This is the link title";
      props["cm:description"]="This is the link description " + filename;
      var thisNode = parentFolder.createNode(filename, type, props);
      noderef = thisNode.nodeRef.toString();
    }

    if (details.toLowerCase()=="addtag"){
	addTag()
    }

    if (details.toLowerCase()=="updatename") {
      updateName();
    }

    if (details.toLowerCase()=="updatetitle") {
      updateTitle();
    }

    if (details.toLowerCase()=="updatedescription") {
      updateDescription();
    }
  } // end link



} // end method=update

if (method.toLowerCase()=="delete"){
  var thingy = search.findNode(noderef);
  thingy.remove();
}

model.vendor = reporting.getDatabaseVendor();
model.table=table;
model.method=method;
model.details=details;
model.noderef=noderef;
model.response=message;
