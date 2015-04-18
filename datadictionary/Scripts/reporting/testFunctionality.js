var phases = ["define objects", "harvest", "define again", "harvest again", "cleanup"]; // 0-based counting!!
var phaseId = 3;
var phase = phases[phaseId];

var testFolder1 = null;
var testFolder1Name = "TestFolder1";
var testDoc1 = null;
var testDoc1Name = "testDoc1";

var testSite = "tech-talk-live";
var reportingRoot = companyhome.childByNamePath("Data Dictionary/Reporting");
var docLib = companyhome.childByNamePath("Sites/" + testSite + "/documentLibrary");
var quote = "\'";




main();

/*********************************************************************************/
/*****                              S E T U P                                *****/
/*********************************************************************************/

function setupTest() {
    reporting.testLog("****** Start test ******");
    //reporting.setLastTimestampAndStatusDone("document", "");
    dropLastRunTable();
    log("dropped last run table");
}

/*********************************************************************************/
/*****                             harvesting related                        *****/
/*********************************************************************************/

function dropLastRunTable() {
    reporting.dropLastTimestampTable();
}

function getHarvestDef(myName) {
    log("Getting harvestDef " + myName);
    var harvest = reportingRoot.childByNamePath(myName);
    log("Returning harvestDef " + harvest.name);
    return harvest;
}

function createHarvestObject(myName, tableQueriesEnabled, auditingEnabled, categoriesEnabled, usersEnabled, auditingList, categoriesList) {
    var harvestDef = reportingRoot.childByNamePath(myName);
    if (!harvestDef) harvestDef = reportingRoot.createNode(myName, "reporting:harvestDefinition");
    harvestDef.properties["reporting:queryTablesEnabled"] = tableQueriesEnabled;
    harvestDef.properties["reporting:auditingExportsEnabled"] = auditingEnabled;
    harvestDef.properties["reporting:categoriesEnabled"] = categoriesEnabled;
    harvestDef.properties["reporting:usersGroupsEnabled"] = usersEnabled;

    harvestDef.properties["reporting:categories"] = categoriesList;
    harvestDef.properties["reporting:auditingQueries"] = auditingList;

    harvestDef.properties["reporting:queryTablesLanguage"] = "Lucene";
    log("created HarvestingDefinition " + harvestDef.nodeRef);
    return harvestDef;
}

function createHarvestObjectDocumentsOnly() {
    var harvestDef = createHarvestObject(testDoc1Name, true, false, false, false, null, null);
    harvestDef.content = "document=TYPE:\"cm:content\" AND NOT TYPE:\"bpm:task\" AND NOT TYPE:\"dl:dataListItem\" AND NOT TYPE:\"ia:calendarEvent\" AND NOT TYPE:\"lnk:link\" AND NOT TYPE:\"cm:dictionaryModel\" AND NOT ASPECT:\"reporting:executionResult\"";
    harvestDef.properties.content.mimetype = "plain/text";
    harvestDef.save();
    return harvestDef;
}

function createHarvestObjectFoldersOnly() {
    var harvestDef = createHarvestObject(testFolder1Name, true, false, false, false, null, null);
    harvestDef.content = "folder=TYPE:\"cm:folder\" AND NOT TYPE:\"st:site\" AND NOT TYPE:\"dl:dataList\" AND NOT TYPE:\"bpm:package\" AND NOT TYPE:\"cm:systemfolder\" AND NOT TYPE:\"fm:forum\"";
    harvestDef.properties.content.mimetype = "plain/text";
    harvestDef.save();
    return harvestDef;
}

function createHarvestObjectAuditOnly(auditList) {
    return createHarvestObject("testAuditExportDefintion", false, true, false, false, auditList, null);
}

function createHarvestObjectCategoriesOnly(categoryList) {
    return createHarvestObject("testCategoryDefinition", false, false, true, false, null, categoryList);
}

function getCategory(categoryName) {
    var cat = null;
    var check = search.luceneSearch("@cm\\:name:\"" + categoryName + "\" AND TYPE:\"category\"");
    if (check.length == 1) {

        cat = check[0];
    }
    return cat;
}

function addCategory(document, categories) {
    document.properties["cm:categories"] = categories;
    return document;
}

/*********************************************************************************/
/*****                           Document related                            *****/
/*********************************************************************************/

function manipulateDocuments() {
    log("**** Starting Manipulate Documents ****");
    // create a testfolder
    testFolder1 = docLib.childByNamePath(testFolder1Name);
    if (!testFolder1) testFolder1 = docLib.createFolder(testFolder1Name);

    testFolder1.properties["cm:title"] = testFolder1Name + " title";
    testFolder1.properties["cm:description"] = testFolder1Name + " description";
    testFolder1.save();
    log("got folder: " + testFolder1.name + " - " + testFolder1.nodeRef);

    // create a testdocument
    testDoc1 = testFolder1.childByNamePath(testDoc1Name + ".txt");
    if (!testDoc1) testDoc1 = testFolder1.createFile(testDoc1Name + ".txt");
    testDoc1.addAspect("cm:versionable");
    log("got document: " + testDoc1.name + " - " + testDoc1.nodeRef);

    testDoc1.properties["cm:title"] = testDoc1Name + " title";
    testDoc1.properties["cm:description"] = testDoc1Name + " description";
    testDoc1.content = testDoc1Name + " content";

    testDoc1.addTag("Tag1");
    testDoc1.addTag("Tag2");

    catList = [getCategory("Nigeria"), getCategory("Belgium")];
    testDoc1 = addCategory(testDoc1, catList);
    testDoc1.save();
    log("**** Finalizing Manipulate Documents ****");
}

function manipulateDocumentsAgain() {
    log("**** Starting Manipulate Documents Again ****");
    // get the testfolder
    testFolder1 = docLib.childByNamePath(testFolder1Name);
    log("got folder: " + testFolder1.name + " - " + testFolder1.nodeRef);

    // get the testdocument
    testDoc1 = testFolder1.childByNamePath(testDoc1Name + ".txt");
    log("got document: " + testDoc1.name + " - " + testDoc1.nodeRef);

    testDoc1.properties["cm:title"] = testDoc1Name + " title2";
    testDoc1.properties["cm:description"] = testDoc1Name + " description2";
    testDoc1.content = testDoc1Name + " content2";

    testDoc1.addTag("Tag3");
    testDoc1.addTag("Tag4");

    var catList = [getCategory("Netherlands"),getCategory("Belgium")];
    testDoc1 = addCategory(testDoc1, catList);
    testDoc1.save();
    log("**** Finalizing Manipulate Documents Again ****");
}

function manipulateFoldersAgain() {
    log("**** Starting Manipulate Folders Again ****");
    // get the testfolder
    testFolder1 = docLib.childByNamePath(testFolder1Name);

    testFolder1.properties["cm:title"] = testFolder1Name + " title2";
    testFolder1.properties["cm:description"] = testFolder1Name + " description2";
    
  	testFolder1.save();
    testFolder.addTag("FolderTag");
  
    log("**** Finalizing Manipulate Folders Again ****");
}



function measureDocuments(harvestDef) {
    log("**** Starting Measuring Documents ****");
    testFolder1 = docLib.childByNamePath(testFolder1Name);
    log("got folder: " + testFolder1.name + " - " + testFolder1.nodeRef);
    testDoc1 = testFolder1.childByNamePath(testDoc1Name + ".txt");
    log("got document: " + testDoc1.name + " - " + testDoc1.nodeRef);

    var harvest = actions.create("harvesting-executer");
    harvest.execute(harvestDef);

    test("count(*)", "document", "orig_noderef=" + quote + testDoc1.nodeRef + quote, "3");
    test("cm_taggable", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", "tag1,tag2");
    test("cm_title", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", testDoc1Name + " title");
    test("size", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", "16");
    test("site", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", testSite);
    test("cm_description", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", testDoc1Name + " description");
    test("validUntil", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", null);
    test("path", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", "/Company Home/Sites/tech-talk-live/documentLibrary/TestFolder1/testDoc1.txt");
    test("versioned", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", "1");
    test("cm_categories", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", "Regions/AFRICA/Western Africa/Nigeria,Regions/EUROPE/Western Europe/Belgium");

    log("**** Finalizing Measuring Documents ****");
}


function measureDocumentsAgain(harvestDef) {
    log("**** Starting Measuring Documents Again ****");
    testFolder1 = docLib.childByNamePath(testFolder1Name);
    log("got folder: " + testFolder1.name + " - " + testFolder1.nodeRef);
    testDoc1 = testFolder1.childByNamePath(testDoc1Name + ".txt");
    log("got document: " + testDoc1.name + " - " + testDoc1.nodeRef);

    var harvest = actions.create("harvesting-executer");
    harvest.execute(harvestDef);

    test("count(*)", "document", "orig_noderef=" + quote + testDoc1.nodeRef + quote, "4");
    test("cm_taggable", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", "tag1,tag2,tag3,tag4");
    test("cm_title", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", testDoc1Name + " title2");
    test("size", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", "17");
    test("site", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", testSite);
    test("cm_description", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", testDoc1Name + " description2");
    test("validUntil", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", null);
    test("path", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", "/Company Home/Sites/tech-talk-live/documentLibrary/TestFolder1/testDoc1.txt");
    test("versioned", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", "1");
    test("cm_categories", "document", "noderef=" + quote + testDoc1.nodeRef + quote + " AND isLatest=true", "Regions/EUROPE/Western Europe/Netherlands,Regions/EUROPE/Western Europe/Belgium");

    reporting.setLastTimestampAndStatusDone("document", "");
    harvest.execute(harvestDef);
    log("**** Harvesting Again ****");

    test("count(*)", "document", "orig_noderef=" + quote + testDoc1.nodeRef + quote, "5");
    log("**** Finalizing Measuring Documents Again ****");
}

function measureFoldersAgain(harvestDef) {
    log("**** Finalizing Measuring Folders Again ****");

    testFolder1 = docLib.childByNamePath(testFolder1Name);
    log("got folder: " + testFolder1.name + " - " + testFolder1.nodeRef);

    reporting.setLastTimestampAndStatusDone("folder", "");
    test("count(*)", "folder", "orig_noderef=" + quote + testFolder1.nodeRef + quote, "1");
    var harvest = actions.create("harvesting-executer");
    harvest.execute(harvestDef);
    test("count(*)", "folder", "orig_noderef=" + quote + testFolder1.nodeRef + quote, "1");

    log("**** Finalizing Measuring Folders Again ****");
}

function restoreStateDocuments() {
    log("**** Starting Restore Documents ****");
    testFolder1 = docLib.childByNamePath(testFolder1Name);
    testDoc1 = testFolder1.childByNamePath(testDoc1Name + ".txt");
    if (testDoc1) testDoc1.remove();
    if (testFolder1) testFolder1.remove();
    getHarvestDef(testDoc1Name).remove();
    log("**** Finalizing Removing Documents ****");
}

function sleep(delay) {
    log("Waiting " + delay + " ms");
    var start = new Date().getTime();
    while (new Date().getTime() < start + delay);
}

function log(inString) {
    reporting.testLog(inString);
    logger.log(inString);
}

function fail(inString) {
    reporting.testLogFailed(inString);
    logger.log(inString);
}

/*********************************************************************************/
/*****                                M A I N                                *****/
/*********************************************************************************/


function test(select, from, where, origString) {
    //log("Where=" + where);
    var inString = reporting.selectFromWhere(select, from, where);
    if (inString != origString) {
        fail("ERROR: " + select + " from " + from + " value '" + inString + "' !='" + origString + "'");
    } else {
        log("success: " + select + " from " + from + " value '" + inString + "' =='" + origString + "'");
    }
}

function main() {
    var harvestDocumentsDef = null;
    var harvestFoldersDef = null;
    if (phase == "define objects") {
        setupTest();

        // process Document manipulations
        harvestDocumentsDef = createHarvestObjectDocumentsOnly();
        harvestFoldersDef = createHarvestObjectFoldersOnly();
        manipulateDocuments();
    }


    if (phase == "harvest") {
        // measure Document manipulations
        harvestDocumentsDef = getHarvestDef(testDoc1Name);
        measureDocuments(harvestDocumentsDef);
    }

    if (phase == "define again") {
        // process Document manipulations
        manipulateDocumentsAgain();
		manipulateFoldersAgain();
    }

    if (phase == "harvest again") {
        // measure Document manipulations
        harvestDocumentsDef = getHarvestDef(testDoc1Name);
        measureDocumentsAgain(harvestDocumentsDef);

        harvestFoldersDef = getHarvestDef(testFolder1Name);
        measureFoldersAgain(harvestFoldersDef);
    }

    if (phase == "cleanup") {
        // clean up like urshi would have cleaned up
        restoreStateDocuments();
        if (harvestDocumentsDef) harvestDocumentsDef.remove();
        log("stuff removed.");
    }
}