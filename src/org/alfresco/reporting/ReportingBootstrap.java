package org.alfresco.reporting;

import org.alfresco.reporting.db.DatabaseHelperBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ReportingBootstrap {

	private DatabaseHelperBean dbhb = null;
	
	private static Log logger = LogFactory.getLog(ReportingBootstrap.class);
	
	
	public void setDatabaseHelperBean(DatabaseHelperBean databaseHelperBean) {
		this.dbhb = databaseHelperBean;
	}
	
	public void init(){
		
		try{
			if (logger.isInfoEnabled())
					logger.info("Reset all statusses to DONE - starting");
			dbhb.setAllStatusesDoneForTable();
			
		} catch (Exception e){
			logger.error(e.getMessage());
		}
	}
}
