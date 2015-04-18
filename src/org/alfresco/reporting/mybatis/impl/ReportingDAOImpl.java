package org.alfresco.reporting.mybatis.impl;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.reporting.ReportingHelper;
import org.alfresco.reporting.mybatis.InsertInto;
import org.alfresco.reporting.mybatis.LastRunDefinition;
import org.alfresco.reporting.mybatis.ReportingColumnDefinition;
import org.alfresco.reporting.mybatis.ReportingDAO;
import org.alfresco.reporting.mybatis.SelectFromWhere;
import org.alfresco.reporting.mybatis.UpdateWhere;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;


public class ReportingDAOImpl implements ReportingDAO {

	private String LASTSCCESSFULRUN = "lastsuccessfulrun";
	private static Log logger = LogFactory.getLog(ReportingDAOImpl.class);
	private SqlSession template = null;
	
	public void init(){
		try{
			org.apache.ibatis.logging.LogFactory.useLog4JLogging();
			template.getConnection().setAutoCommit(true);
		} catch (SQLException sqle){
			throw new AlfrescoRuntimeException(sqle.getMessage());
		}
	}
	// ---------------------------------------------------------------------
	//                        Last Run methods
	// ---------------------------------------------------------------------
	public void openConnection() throws SQLException{
		if (template.getConnection().isClosed()) {
			template.getConnection().notify();
			template.getConnection().getTransactionIsolation();
		}
	}
	
	public void closeConnection() throws SQLException{
		//if (!template.getConnection().isClosed()) {
		//	template.getConnection().close();
		//}
	}
	
	public void createLastTimestampTable(){
		try{
			openConnection();
			template.getConnection().setAutoCommit(true);
			SelectFromWhere sfw = new SelectFromWhere(
						null, LASTSCCESSFULRUN.toLowerCase(), null);
			String url = template.getConnection().getMetaData().getURL();
			String database = url.substring(url.lastIndexOf("/")+1,url.length());
			sfw.setDatabase(database);
			if ((Integer)template.selectOne("table-exists", sfw)==0){
				template.insert("lastrun-create-empty-table");
			}
		} catch (Exception eee){
			logger.fatal("@@@@ createLastTimestampTable Exception!: " + eee.getMessage());
		}
	}

	public void createLastTimestampTableRow(final String tablename){
		if (logger.isDebugEnabled())
			logger.debug("enter createLastTimestampTableRow: " + tablename.toLowerCase());
		final LastRunDefinition lrd = new LastRunDefinition(tablename.toLowerCase(), null, null);

		try{
			template.getConnection().setAutoCommit(true);
			int i = template.insert("lastrun-insertTablename", lrd);
			logger.debug("createLastTimestampTableRow: inserted: lastrun-insertTablename " + i);
		}catch (Exception e){
			try{
				createLastTimestampTable();
				template.insert("lastrun-insertTablename", lrd);
				logger.debug("createLastTimestampTableRow: inserted: lastrun-insertTablename AND created entire table");
			} catch (Exception eee){
				logger.fatal("@@@@ createLastTimestampTableRow Exception!: " + eee.getMessage());
			}
		}
	}

	public void clearLastRunTimestamp(final String tablename){
		if (logger.isDebugEnabled())
			logger.debug("enter clearLastRunTimestamp: " + tablename.toLowerCase());
		LastRunDefinition lrd = new LastRunDefinition(tablename.toLowerCase(), null, "");

		try{
			template.getConnection().setAutoCommit(true);
			int i = template.insert("lastrun-cleanTimestampTablename", lrd);
			logger.debug("cleanTimestampTablename: updated: " + i);
		}catch (Exception e){
			logger.fatal("@@@@ cleanTimestampTablename Exception!: " + e.getMessage());
		}
	}
	
	public int getNumberOfRowsForTable(final String tablename){
		if (logger.isDebugEnabled())
			logger.debug("enter getNumberOfRowsForTable: " + tablename.toLowerCase());
		int returnInt =  0;
		try{
			SelectFromWhere sfw = new SelectFromWhere(
						null, tablename.toLowerCase(), null);
			returnInt = (Integer)template.selectOne("show-table-count", sfw);
			sfw=null;
		} catch (Exception eee){
			logger.fatal("@@@@ getNumberOfRowsForTable Exception!: " + eee.getMessage());
		}
		if (logger.isDebugEnabled())
			logger.debug("exit getNumberOfRowsForTable: " + returnInt);
		return returnInt;
	}
	
	public String selectLastRunForTable(final String tablename){
		if (logger.isDebugEnabled())
			logger.debug("enter selectLastRunForTable: " + tablename.toLowerCase());
		String returnString = "";
		try{
			returnString = (String)template.selectOne("lastrun-selectLastRunForTable", tablename.toLowerCase());
		} catch (Exception eee){
			logger.fatal("@@@@ selectLastRunForTable Exception!: " + eee.getMessage());
		}
		return returnString;
	}
	
	public boolean lastRunTableIsRunning(){
		if (logger.isDebugEnabled())
			logger.debug("enter lastRunTableIsRunning" );
		boolean returnBoolean = true;
		try{
			Object hummy = template.selectOne("lastrun-table-is-running");
			logger.debug("lastRunTableIsRunning: mybatis returning: " + hummy);
			returnBoolean = (0 != (Integer)hummy);
			//returnBoolean = false;
		} catch (Exception e) {
			returnBoolean = false;
			try{
				createLastTimestampTable();
			} catch (Exception eee){
				logger.fatal("@@@@ lastRunTableIsRunning Exception!: " + eee.getMessage());
			}
		}
		if (logger.isDebugEnabled()) 
			logger.debug("lastRunTableIsRunning returning: " + returnBoolean);
		return returnBoolean;
	}
	//****************************************
	// here something goes completely wrong!!
	//****************************************
	
	/**
	 * Update the lastRun timestamp && set status='done'. 
	 * If this modified !=1 rows, insert the new row AND update the row
	 */
	public void updateLastSuccessfulRunDateForTable(final String tableName, final String status, final String timestamp){
		final LastRunDefinition lrd = new LastRunDefinition(tableName.toLowerCase(), status, timestamp);
		Integer returnInt = 0;
		if (logger.isDebugEnabled()) 
			logger.debug("updateLastSuccessfulRunDateForTable enter; table="+lrd.getTablename() + ", time: " + lrd.getLastrun() + ", status: " + lrd.getStatus());
		try{
			template.getConnection().setAutoCommit(true);
			returnInt = template.update("lastrun-updateLastSuccessfulRunDateForTable", lrd);
			logger.debug("updateLastSuccessfulRunDateForTable ### updated rows: " + returnInt);
			
			if (!(returnInt==1)){ 
				throw new AlfrescoRuntimeException("There were no rows updated, try an Insert statement instead!");
			}
			logger.debug("updateLastSuccessfulRunDateForTable updated the timestamp for " + lrd.getTablename() + " into " + lrd.getLastrun());
			
		} catch (Exception e){
			try{
				createLastTimestampTableRow(lrd.getTablename());
				returnInt = (Integer)template.update("lastrun-updateLastSuccessfulRunDateForTable", lrd);
				logger.debug("updateLastSuccessfulRunDateForTable created the row AND updated the timestamp for " + lrd.getTablename() + " into " + lrd.getLastrun());
			} catch (Exception eee){
				logger.fatal("@@@@ updateLastSuccessfulRunDateForTable Exception!: " + eee.getMessage());
			} 
		}
		//return returnInt;
	}
	
	/**
	 * WHY IS IT NOT PERSISTED??!!
	 * >> It is, but there is a difference between the timestamp of WorkspaceStore 
	 *    and ArchiveStore... That is why. The mechnism works lke a charm, actually
	 */
	public void updateLastSuccessfulBatchForTable(final String tableName, final String timestamp){
		final LastRunDefinition lrd = new LastRunDefinition(tableName.toLowerCase(), null, timestamp);
		Integer returnInt = 0;
		if (logger.isDebugEnabled()) 
			logger.debug("updateLastSuccessfulBatchForTable enter; table="+lrd.getTablename() + ", time: " + lrd.getLastrun() + ", status: " + lrd.getStatus());
		try{
			template.getConnection().setAutoCommit(true);
			returnInt = template.update("lastrun-updateLastSuccessfulBatchDateForTable", lrd);
			logger.debug("updateLastSuccessfulBatchForTable ### updated rows: " + returnInt);
			
			if (!(returnInt==1)){ 
				throw new AlfrescoRuntimeException("There were no rows updated, try an Insert statement instead!");
			}
			logger.debug("updateLastSuccessfulBatchForTable updated the timestamp for " + lrd.getTablename() + " into " + lrd.getLastrun());
			
		} catch (Exception e){
			try{
				createLastTimestampTableRow(lrd.getTablename()); // does it set the status too? Does it need to?
				returnInt = (Integer)template.update("lastrun-updateLastSuccessfulRunDateForTable", lrd);
				logger.debug("updateLastSuccessfulBatchForTable created the row AND updated the timestamp for " + lrd.getTablename() + " into " + lrd.getLastrun());
			} catch (Exception eee){
				logger.fatal("@@@@ updateLastSuccessfulBatchForTable Exception!: " + eee.getMessage());
			}
		}
		//return returnInt;
	}
	
	public void updateLastSuccessfulRunStatusForTable(final String tableName, final String status){
		final LastRunDefinition lrd = new LastRunDefinition(tableName.toLowerCase(), status, null);
		Integer returnInt = 0;
		if (logger.isDebugEnabled()) 
			logger.debug("updateLastSuccessfulRunStatusForTable enter");
		try{
			template.getConnection().setAutoCommit(true);
			returnInt = (Integer)template.update("lastrun-updateLastSuccessfulRunStatusForTable", lrd);
			logger.debug("updateLastSuccessfulRunStatusForTable ### number of updated rows: " + returnInt);
			if (returnInt==0) 
				throw new AlfrescoRuntimeException("There were no rows updated, try an Insert statement instead!");
			logger.debug("updateLastSuccessfulRunStatusForTable updated "+lrd.getTablename()+" the status to " + lrd.getStatus());
			
		} catch (Exception e){
			try{
				createLastTimestampTableRow(lrd.getTablename());
				returnInt = (Integer)template.update("lastrun-updateLastSuccessfulRunStatusForTable", lrd);
				logger.debug("updateLastSuccessfulRunStatusForTable created the row, AND updated "+lrd.getTablename()+" the status to " + lrd.getStatus());
			} catch (Exception eee){
				logger.fatal("@@@@ updateLastSuccessfulRunStatusForTable Exception!: " + eee.getMessage());
			}
			
		}

		//return returnInt;
	}
	
	/**
	 * get lastSuccessfulRun timestamp for a given tablename
	 * if none returned, create a new line in the table for this tablename
	 */
	public String getLastSuccessfulRunDateForTable(final String tablename){
		final String table = tablename.toLowerCase();
		if (logger.isDebugEnabled()) 
			logger.debug("getLastSuccessfulRunDateForTable enter for table " + table);
		LastRunDefinition lrd = new LastRunDefinition(table, null, null);
		String theDate = "";
		try{
			
			theDate = (String)template.selectOne("lastrun-getLastSuccessfulRunDateForTable", lrd);
			if (theDate==null) {
				theDate = "";
				//throw new AlfrescoRuntimeException("There were no rows updated, try an Insert statement instead!");
			}
			logger.debug("getLastSuccessfulRunDateForTable got the lastRunDate: " + theDate);
			
		} catch (Exception e){
			try{
				createLastTimestampTableRow(lrd.getTablename());
				theDate = "";
				logger.debug("getLastSuccessfulRunDateForTable added the TableRow, ADN got empty string");
			} catch (Exception eee){
				logger.fatal("@@@@ getLastSuccessfulRunDateForTable Exception!: " + eee.getMessage());
			}
		}
		lrd=null;
		return theDate;
	}
	
	public String getLastSuccessfulRunStatusForTable(final String tablename){
		final String table = tablename.toLowerCase();
		if (logger.isDebugEnabled()) 
			logger.debug("getLastSuccessfulRunStateForTable enter for table " + table);
		final LastRunDefinition lrd = new LastRunDefinition(table, null, null);
		String status = "";
		try{
			
			status = (String)template.selectOne("lastrun-getLastSuccessfulRunStatusForTable", lrd);
			if (status==null) {
				status = "";
				//throw new AlfrescoRuntimeException("There were no rows updated, try an Insert statement instead!");
			}
			logger.debug("getLastSuccessfulRunStateForTable got the lastRunStatus: " + status);
			
		} catch (Exception e){
			try{
				createLastTimestampTableRow(lrd.getTablename());
				status = "";
				logger.debug("getLastSuccessfulRunStateForTable added the TableRow, ADN got empty string");
			} catch (Exception eee){
				logger.fatal("@@@@ getLastSuccessfulRunStateForTable Exception!: " + eee.getMessage());
			}
		}
		return status;
	}
	
	public void setAllStatusesDoneForTable(){
				
		try{
			logger.debug("setAllStatusesDoneForTable enter");
			int returnInt = (Integer)template.update("lastrun-updateLastSuccessfulRunStatusesDoneForTable");
			
		} catch (Exception e){
			logger.fatal("@@@@ setAllStatusesDoneForTable Exception! : " + e.getMessage());
		}
	}
	// ---------------------------------------------------------------------
	//                        Reporting methods
	// ---------------------------------------------------------------------
		
	public void pushPreliminaryFunctions(){
		template.insert("push-preliminary-functions");
	}
	
	
	public void createEmtpyTable(final String tablename){
		final String from = tablename.toLowerCase();
		if (logger.isDebugEnabled())
			logger.debug("enter createEmtpyTable: " + from);
		final String sequencename = from + "_seq"; 		// required for Oracle 
		final String triggername = from + "_trigger";	// required for Oracle
		SelectFromWhere sfw = new SelectFromWhere(
				sequencename, from, triggername);
		
		try{
			openConnection();
			//pushPreliminaryFunctions();
			final String url = template.getConnection().getMetaData().getURL();
			final String database = url.substring(url.lastIndexOf("/")+1,url.length());
			sfw.setDatabase(database);
			template.getConnection().setAutoCommit(true);
			if (logger.isDebugEnabled())
				logger.debug("enter createEmtpyTable: checking if table already exists...");
			if ((Integer)template.selectOne("table-exists", sfw)==0){
				template.insert("reporting-create-empty-table", sfw);
				createCustomIndex(from.trim(), "sys_node_uuid", from.trim()+"_uuid_idx"); 
				createCustomIndex(from.trim(), "sys_node_uuid,isLatest", from.trim()+"_uuid_islatest_idx");
			}
			logger.debug("exit createEmtpyTable " + sfw.getFrom());
		} catch (Exception e){
			logger.fatal("select table : " + sfw.getTablename());
			logger.fatal("select from  : " + sfw.getFrom());
			logger.fatal("select where : " + sfw.getWhere());
			logger.debug("## " + e.getMessage());
			e.printStackTrace();
		}
	}

	
	/**
	 * creates an Index against any given column in any given table
	 */
	public void createCustomIndex(final String tablename, final String column, final String indexname){
		final String from = tablename.toLowerCase();
		final String select = column.toLowerCase();
		final String where = indexname.toLowerCase(); // index name
		if (logger.isDebugEnabled())
			logger.debug("enter createCustomIndex. table: " + from + " column: " + select + " indexname: " + where);
		SelectFromWhere sfw = new SelectFromWhere(column, from, where);
		try{
			openConnection();
			template.insert("reporting-create-index", sfw);
			logger.debug("exit createIndex");
		} catch (Exception e){
			logger.fatal("table : " + sfw.getTablename());
			logger.fatal("from  : " + sfw.getFrom());
			logger.fatal("where : " + sfw.getWhere());
			
			logger.debug("## " + e.getMessage());
			e.printStackTrace();
		} 
		
	}
	
	/** get info fo hthe admin dashlet
	 *  returns a plain list ot tables
	 */
	public List<String> getShowTables(){
		if (logger.isDebugEnabled())
			logger.debug("enter getShowTables");
		@SuppressWarnings("unchecked")
		final List<String> results = (List<String>)template.selectList("show-tables");
		if (logger.isDebugEnabled()){
			for (int i=0;i<results.size();i++){
				logger.debug(" +"+ results.get(i));
			}
			logger.debug("exit getShowTables");
		}
		return results;
	}

	/**
	 * Get the column names and column types of a given table.
	 * The column_name is important. Make sure you NAME the columns 
	 * COLUMN_NAME and COLUMN_TYPE, otherwise the logic will fail... 
	 * I don't think it will actually be used by anyone...
	 *  
	 * REMIND: This does not use the SqlMap, but plain old SQL-in-Java!
	 */
	public Properties getDescTable(final String tablename){
		final String from = tablename.toLowerCase();
		Properties props = new Properties();
		SelectFromWhere sfw = new SelectFromWhere(
				null, from, null);
		
		try{
			final String url = template.getConnection().getMetaData().getURL();
			final String database = url.substring(url.lastIndexOf("/")+1,url.length());
			if (logger.isDebugEnabled())
				logger.debug("$$$ getDescTable: Database appears to be: " + database);
			sfw.setDatabase(database);
			
			if (logger.isDebugEnabled())
				logger.debug("getDescTable: before selectList");
			@SuppressWarnings("unchecked")
			final List<Map<String,String>> myList = template.selectList("describe-table", sfw);
			if (logger.isDebugEnabled())
				logger.debug("getDescTable after selectList");			
			String key = "";
			
			final Iterator<Map<String,String>> listIterator = myList.iterator();
			while (listIterator.hasNext()){
				Map<String,String> map = (Map<String,String>)listIterator.next();
				if (logger.isDebugEnabled())
					logger.debug("Map=" + map);
				Iterator<String> mapIterator = map.keySet().iterator();
				if (logger.isDebugEnabled())
					logger.debug("getDescTable: Map Iterator constructed");
				String theKey="";
				String theValue = "";
				while (mapIterator.hasNext()){
					key = mapIterator.next();
					if (logger.isDebugEnabled())
						logger.debug("Key="+key);
					if ("COLUMN_NAME".equalsIgnoreCase(key) ){
						// store the key in lower-case, we happen to run into trouble if the key is processed in any-case.
						theKey = map.get(key).toLowerCase();
					}
					if ("COLUMN_TYPE".equalsIgnoreCase(key) ){
						theValue = map.get(key);
					}
				}
				if (logger.isDebugEnabled()){
					logger.debug("getDescTable: processed key  =" + theKey);
					logger.debug("getDescTable: processed value=" + theValue);
				}
				props.setProperty(theKey, theValue);
				if (logger.isDebugEnabled())
					logger.debug("getDescTable: done processing key=value" );

			}
			
			
			/*
			String sql = "DESC " + tablename +";";
			ResultSet rs = template.getConnection()
									.createStatement().executeQuery(sql);

		    while(rs.next()){
		         //Retrieve by column number
		         props.setProperty(rs.getString(1),rs.getObject(2).toString());
		         logger.debug("Found column: "+rs.getString(1) + " - Type: " + rs.getObject(2).toString());
		    }
		    */
		} catch (Exception eee){
			logger.fatal("@@@@ getDescTable Exception!: " + eee.getMessage());
		}
		if (logger.isDebugEnabled()) 
			logger.debug("exit getDescTable returning " + props);
		return props;
	}
	
	/**
	 * Execute ALTER TABLE against given table, with given column name, and given type
	 */
	public void extendTableDefinition(final ReportingColumnDefinition rcd){
		if (logger.isDebugEnabled()) 
			logger.debug("enter extendTableDefinition");
		rcd.setTablename(rcd.getTablename().toLowerCase());
		try{
			template.getConnection().setAutoCommit(true);
			template.update("reporting-extendTableDefinition", rcd);
		} catch (Exception eee){
			logger.fatal("insert into       : " + rcd.getTablename());
			logger.fatal("insert columnname : " + rcd.getColumnname());
			logger.fatal("insert columntype : " + rcd.getColumntype());
			logger.fatal("@@@@ extendTableDefinition Exception!: " + eee.getMessage());
		}
		if (logger.isDebugEnabled()) 
			logger.debug("enter extendTableDefinition");
	}
	
	
	/**
	 * Returns true if the Row with the given tablename exists. 
	 */
	public boolean reportingRowExists(final SelectFromWhere sfw){
		sfw.setFrom(sfw.getFrom().toLowerCase());
		int i = (Integer) template.selectOne("reporting-row-exists", sfw);
		return i>0;
	}
	
	public boolean reportingRowEqualsModifiedDate(final SelectFromWhere sfw){
		sfw.setFrom(sfw.getFrom().toLowerCase());
		int i = (Integer) template.selectOne("reporting-row-equals-modified-date", sfw);
		return i>0;
	}
	
	public boolean reportingRowVersionedEqualsModifiedDate(final SelectFromWhere sfw){
		sfw.setFrom(sfw.getFrom().toLowerCase());
		int i = (Integer) template.selectOne("reporting-row-versioned-equals-modified-date", sfw);
		return i>0;
	}
	
	public int reportingInsertIntoTable(final InsertInto insertInto){
		insertInto.setTablename( insertInto.getTablename().toLowerCase() );
		if (logger.isDebugEnabled()){
			logger.debug("insert into : " + insertInto.getTablename());
			logger.debug("insert keys : " + insertInto.getKeys());
			logger.debug("insert value: " + insertInto.getValues());
		}
		int i=0;
		try{
			template.getConnection().setAutoCommit(true);
			i = template.insert("reporting-insert-into-table", insertInto);
		} catch (Exception eee){
			logger.fatal("insert into : " + insertInto.getTablename());
			logger.fatal("insert keys : " + insertInto.getKeys());
			logger.fatal("insert value: " + insertInto.getValues());
			logger.fatal("@@@@ reportingInsertIntoTable Exception!: " + eee.getMessage());
		}
		return i;
	}
	
	public int reportingUpdateIntoTable(final UpdateWhere updateWhere){
		int i=0;
		try{
			updateWhere.setTablename(updateWhere.getTablename().toLowerCase());
			if (logger.isDebugEnabled()){
				logger.debug("update into   : " + updateWhere.getTablename());
				logger.debug("update update : " + updateWhere.getUpdateClause());
				logger.debug("update where  : " + updateWhere.getWhereClause());
			}
			
			template.getConnection().setAutoCommit(true);
			i = template.update("reporting-update-into-table", updateWhere);
			
		
		} catch (Exception e){
			logger.fatal("update table  : " + updateWhere.getTablename());
			logger.fatal("update update : " + updateWhere.getUpdateClause());
			logger.fatal("update where  : " + updateWhere.getWhereClause());
			logger.fatal("@@@@ reportingUpdateIntoTable Exception!: " + e.getMessage());
		
		}
		return i;
	}
	
	public int reportingUpdateVersionedIntoTable(UpdateWhere updateWhere){
		updateWhere.setTablename(updateWhere.getTablename().toLowerCase());
		if (logger.isDebugEnabled()){
			logger.debug(" update table  : " + updateWhere.getTablename());
			logger.debug(" update update : " + updateWhere.getUpdateClause());
			logger.debug(" update where  : " + updateWhere.getWhereClause());
		}
		int i=0;
		try{
			template.getConnection().setAutoCommit(true);
			i = template.update("reporting-update-versioned-into-table-reset-islatest", updateWhere);
		} catch (Exception eee){
			logger.fatal("update table  : " + updateWhere.getTablename());
			logger.fatal("update update : " + updateWhere.getUpdateClause());
			logger.fatal("update where  : " + updateWhere.getWhereClause());
			logger.fatal("@@@@ reportingUpdateVersionedIntoTable Exception!: " + eee.getMessage());
		}
		return i;
	}
	
	@SuppressWarnings("unchecked")
	public List<Map<String,Object>> reportingSelectIsLatestPerTable(final String tablename){
		String from = tablename.toLowerCase();
		SelectFromWhere sfw = new SelectFromWhere(null, from, null);
		List<Map<String,Object>> myTable = template.selectList("reporting-select-islatest-per-table", sfw);
		sfw=null;
		return myTable;
	}
	
	@SuppressWarnings("unchecked")
	public String reportingSelectFromWhere(final String select, final String tablename, final String where){
		String from = tablename.toLowerCase();
		SelectFromWhere sfw = new SelectFromWhere(select, from, where);
		try{
			final String url = template.getConnection().getMetaData().getURL();
			final String database = url.substring(url.lastIndexOf("/")+1,url.length());
			sfw.setDatabase(database);
			int tableCols = (Integer)template.selectOne("table-exists", sfw);
			if ((Integer)template.selectOne("table-exists", sfw)>0){
				return (String)template.selectOne("reporting-select-from-where", sfw);
			} else
			{
				
				return null;
			}
		} catch (SQLException e){
			logger.fatal("select table : " + sfw.getTablename());
			logger.fatal("select from  : " + sfw.getFrom());
			logger.fatal("select where : " + sfw.getWhere());
			logger.fatal("@@@@ reportingSelectFromWhere Exception!: " + e.getMessage());
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	public String reportingSelectStatusFromLastsuccessfulrun(final String tablename){
		final String from = tablename.toLowerCase();
		String status=null;
		
		SelectFromWhere sfw = new SelectFromWhere(null, from, null);
		status = (String)template.selectOne("lastrun-select-status-for-table", sfw);
		sfw=null;
		return status;
	}
	
	@SuppressWarnings("unchecked") 
	public List<Map<String,Object>> reportingSelectStoreProtocolPerTable(final String tablename){
		final String from  = tablename.toLowerCase();
		final SelectFromWhere sfw = new SelectFromWhere(null, from, null);
		List<Map<String,Object>> myTable = template.selectList("reporting-select-store-protocol-per-table", sfw);
		return myTable;
	}
	
	public void dropTable(final String tablename){
		try{
			final String from = tablename.toLowerCase();
			template.getConnection().setAutoCommit(true);
			final String sequencename = tablename + "_seq"; 		// required for Oracle 
			final String triggername = tablename + "_trigger";		// required for Oracle
			SelectFromWhere sfw = new SelectFromWhere(sequencename, from, triggername);
			final String url = template.getConnection().getMetaData().getURL();
			final String database = url.substring(url.lastIndexOf("/")+1,url.length());
			sfw.setDatabase(database);
			int numberOfColumns = (Integer)template.selectOne("table-exists", sfw);
//			logger.debug("dropTable: found columns: "+numberOfColumns);
			if (numberOfColumns>0){
//				logger.debug("dropTable: table " + sfw.getFrom() + " exists");
				template.delete("dropTable", sfw);
//				logger.debug("dropTable: drop done");
			} else {
//				logger.debug("dropTable: table " + sfw.getFrom() + " does not exist");
			}
			sfw=null;
		} catch (Exception eee){
			logger.fatal("Exception dropping table " + tablename);
			logger.fatal("@@@@ dropTable Exception!: " + eee.getMessage());
		}
	}
	
	// ------------------------------------------------
	
	
	public void setReportingTemplate(SqlSessionTemplate template){
		this.template = template;
	}
	
	
}
