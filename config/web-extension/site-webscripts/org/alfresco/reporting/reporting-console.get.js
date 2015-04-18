/**
 * Admin Console Reporting Console component
 */


var connector = remote.connect("alfresco");
var data = connector.get("/reportingstatus");
var fromServer = eval('(' + data + ')');

model.reportingtables = fromServer["result"];
