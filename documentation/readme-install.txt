Add to alfresco-global.properties
===================================
reporting.db.name=alfrescoreporting
reporting.db.username=alfresco
reporting.db.password=alfresco
reporting.db.host=localhost
reporting.db.port=3306
reporting.db.driver=org.gjt.mm.mysql.Driver
reporting.cron.filldatabase=0 0/5 * * * ?
reporting.cron.generatereports=0 0/30 * * * ?
reporting.enabled=true

reporting.db.url=jdbc:mysql://${reporting.db.host}:${reporting.db.port}/${reporting.db.name}
