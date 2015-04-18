<import resource="classpath:alfresco/module/org.alfresco.reporting/scripts/getAllTableNames.js">
// This script is part of the Alfresco Business Reporting project.

reporting.dropTables(getAllTableNames());
reporting.resetLastTimestampTable();