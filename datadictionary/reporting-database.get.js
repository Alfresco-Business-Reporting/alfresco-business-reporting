var method = args["method"]; // { drop | count | clear | value }
var table = args["table"];
var details = args["details"]; // { latest | nonlatest | workspace | arhive | versions }
var noderef = args["noderef"];
var allTables="calendar,datalistitem,datalist,document,folder,forum,groups,lastsuccessfulrun,link,person,site,sitepersons";

var message = "";
var vendor = reporting.getDatabaseVendor();
model.method=method;
model.table=table;
model.details=details;
model.vendor=vendor;

// ****************************************
// method == drop {table=tablename || details=all}

if (method=="drop"){
  if ((table!=null) && (!details)){
    reporting.dropTables( table );
    message = "done dropping table " + table;
  }
  if ((!table) && (details=="all")){
    reporting.dropTables(allTables);
    message = "done dropping tables " + allTables;
  }
}

// ****************************
// method == count
if ((method=="count") && (table!=null) && (details=="latest")){

  var select ="count(*)";
  var from = table;
  var where = "isLatest=1";
  if (vendor=="Oracle"){
  	var where = "isLatest=1";
  }
  message = reporting.selectFromWhere( select, from, where );
}

if ((method=="count") && (table!=null) && (details=="nonlatest")){

  var select ="count(*)";
  var from = table;
  var where = "isLatest=0";
  if (vendor=="Oracle"){
  	var where = "isLatest=0";
  }
  message = reporting.selectFromWhere( select, from, where );
}

if ((method=="count") && (table!=null) && (details=="all")){

  var select ="count(*)";
  var from = table;
  var where = "noderef IS NOT NULL";
  message = reporting.selectFromWhere( select, from, where );
}

if ((method=="count") && (table!=null) && (details=="workspace")){

  var select ="count(*)";
  var from = table;
  var where = "sys_store_protocol='workspace'";
  message = reporting.selectFromWhere( select, from, where );
}

if ((method=="count") && (table!=null) && (details=="archive")){

  var select ="count(*)";
  var from = table;
  var where = "sys_store_protocol!='workspace'";
  message = reporting.selectFromWhere( select, from, where );
}

if ((method=="count") && (table!=null) && (details=="versions")){

  var select ="count(*)";
  var from = table;
  var where = " orig_noderef like '" + noderef + "'";
  message = reporting.selectFromWhere( select, from, where );
}

// ****************************
// method == lastsuccessfulrun

if (method=="clear"){
  if (table!=null){
    reporting.clearLastTimestampTable( table );
    message = "done removing row " + table + " from lastsuccessfulrun";
  }
  if ((!table) && (details=="all")){
    var tables = allTables.split(",");
    for each (table in tables){
      reporting.clearLastTimestampTable(table);
    } // end for
    message = "done removing rows " + allTables + " from lastsuccessfulrun";
  }
}

if ((method=="value") && (table!=null) && (details=null) && noderef!=nul){
  var select ="count(*)";
  var from = table;
  var where = " orig_noderef like '" + noderef + "' AND isLatest=1 AND " + details;
  message = reporting.selectFromWhere( select, from, where );

}
model.response=message;
