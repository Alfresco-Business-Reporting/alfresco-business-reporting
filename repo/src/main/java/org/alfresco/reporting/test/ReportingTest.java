package org.alfresco.reporting.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.alfresco.reporting.mybatis.impl.ReportingDAOImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.alfresco.reporting.test.TestCycleResults;

public class ReportingTest {
	private static Log logger = LogFactory.getLog(ReportingDAOImpl.class);
	private static final String BASE_URL = "http://localhost:8080/alfresco/service/reporting";
	private static final String USERNAME = "admin";
	private static final String PASSWORD = "admin";
	// web scripts
	private static final String TEST_DB = "test-db";
	private static final String TEST_REPO = "test-repo";
	// Methods
	private static final String COUNT = "count";
	private static final String DROP = "drop";
	private static final String HARVEST = "harvest";
	private static final String CLEAR = "clear";
	private static final String UPDATE = "update";
	private static final String DELETE = "delete";
	// details
	private static final String VERSIONS 	= "versions";
	private static final String ALL 		= "all";
	private static final String LATEST 		= "latest";
	private static final String NONLATEST	= "nonlatest";
	private static final String WORKSPACE 	= "workspace";
	private static final String ARCHIVE 	= "archive";
	//private static  String ALL_TABLES = "forum,calendar,site,link,document,folder" ;
	private static  String ALL_DOCUMENT_TABLES	= "document, calendar,link,datalistitem" ;
	private static  String ALL_FOLDER_TABLES 	= "datalist,folder" ;
	private static  String ALL_PEOPLE_TABLES 	= "person,sitepersons,groups" ;
	private static  String ALL_AUDIT_TABLES 	= "" ;
	private static int TIME_BEFORE_HARVEST = 20000;

	final static  boolean isShort=true;
	
	public static JSONObject getRequest(String script, String method, String table, String details, String noderef) {
		JSONObject json = new JSONObject();
		 try {
			 	String full_url = BASE_URL
			 						+"/"+script+".json"
			 						+"?method=" + method;
			 	if (table != null){
			 		full_url += "&table="+table;
			 	}
			 	
			 	if (details != null){
			 		full_url += "&details=" + details;
			 	}
			 	
			 	if (noderef != null){
			 		full_url += "&noderef=" + noderef;
			 	}
			 	//System.out.println(full_url);
			 	URL url = new URL(full_url);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				
				conn.setRequestMethod("GET");
				//conn.setRequestProperty("Accept", "application/json");
				conn.setRequestProperty("Accept", "text/html");
		 
				String userpass = USERNAME + ":" + PASSWORD;
				String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());

//				String basicAuth = "Basic " + new String(new Base64().encode(userpass.getBytes()));
				conn.setRequestProperty ("Authorization", basicAuth);
				
				if (conn.getResponseCode() != 200) {
					throw new RuntimeException("Failed : HTTP error code : "
							+ conn.getResponseCode());
				}
		 
				BufferedReader br = new BufferedReader(new InputStreamReader(
					(conn.getInputStream())));
				
				String output;
				StringBuffer  totalOutput = new StringBuffer();
				
				while ((output = br.readLine()) != null) {
					totalOutput.append(output);
				}
				//System.out.println("done");
				json = new JSONObject(totalOutput.toString());
		 
				conn.disconnect();
		 
			  } catch (MalformedURLException e) {
		 
				e.printStackTrace();
		 
			  } catch (IOException e) {
		 
				e.printStackTrace();
		 
			  } catch (JSONException e){
				  
				  e.printStackTrace();
				  
			  }
		
		return json;
	}
	
	public static void makeNoise(String noise){
		System.out.println("**FAILED** " + noise);
	}
	
	public static void assertCompareNoderef(TestCycleResults previousRun, TestCycleResults nextRun, TestCycleResults expectations){
		if (expectations.getHarvested()>-100){
			if (previousRun.getHarvested() != nextRun.getHarvested() - expectations.getHarvested()){
				makeNoise("against harvested");
			}
		}
		if (expectations.getDbIsLatest()>-100){
			if (previousRun.getDbIsLatest() != nextRun.getDbIsLatest() - expectations.getDbIsLatest()){
				makeNoise("against isLatest");
			}
		}
		if (expectations.getDbIsNonLatest()>-100){
			if (previousRun.getDbIsNonLatest() != nextRun.getDbIsNonLatest() - expectations.getDbIsNonLatest()){
				makeNoise("against isNonLatest");
			}
		}
		if (expectations.getDbWorkspace()>-100){
			if (previousRun.getDbWorkspace() != nextRun.getDbWorkspace() - expectations.getDbWorkspace()){
				makeNoise("against isWorkspace");
			}
		}
		if (expectations.getDbArchive()>-100){
			if (previousRun.getDbArchive() != nextRun.getDbArchive() - expectations.getDbArchive()){
				makeNoise("against isArchive");
			}
		}
		if (expectations.getDbAll()>-100){
			if (previousRun.getDbAll() != nextRun.getDbAll() - expectations.getDbAll()){
				makeNoise("against isAll");
			}
		}
		if (expectations.getDbVersions()>-100){
			if (previousRun.getDbVersions() != nextRun.getDbVersions() - expectations.getDbVersions()){
				makeNoise("against getVersion");
			}
		}
		
		//if (nextRun.getDbIsLatest() - nextRun.getDbArchive() != nextRun.getHarvested()){
		//	makeNoise("repo count != isLatest - archived"); 
		//}
		
		if ((nextRun.getDbIsLatest()+nextRun.getDbIsNonLatest()) != nextRun.getDbAll()){
			makeNoise("isLatest + isNonLatest!= all");
		}
		
		if ((nextRun.getDbWorkspace()+nextRun.getDbArchive()) != nextRun.getDbAll()){
			makeNoise("Workspace + Archive != all");
		}
		
	}
	
	private static void testycleNoderefFullRunDocument() throws JSONException{
		//ALL_TABLES = "document";
		String[] tables = ALL_DOCUMENT_TABLES.split(",");
		
		TestCycleResults expectations;
		TestCycleResults nextRun;
		TestCycleResults prevRun;
		
		for (String table: tables){
			table = table.trim();
			
			System.out.println("### Dropping reporting table " + table);
			JSONObject json = getRequest(TEST_DB, CLEAR, table, null, null);
			json = getRequest(TEST_DB, DROP, table, null, null);
			String vendor = json.getString("vendor");
			System.out.println("### Reporting database powered by: " + vendor );
			
			prevRun = testCyclusNoderef(table, null);

			// Create a new object
			System.out.print("Creating new " + table + "... ");
			json = getRequest(TEST_REPO, UPDATE, table, "create", null);
			String noderef = json.getString("noderef");
			System.out.println(noderef);
			
			try {
			    Thread.sleep(TIME_BEFORE_HARVEST);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(
					1, // harvested
					1, // islatest
					0, // is nonlatest
					1, // workspace
					-999, // archive space
					1, // all
					-999); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;
			
			System.out.println("Update title " + table + "... ");
			json = getRequest(TEST_REPO, UPDATE, table, "updateTitle", noderef);
			
			try {
			    Thread.sleep(TIME_BEFORE_HARVEST);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(
					0, // harvested
					0, // islatest
					1, // is nonlatest
					1, // workspace
					0, // archive space
					1, // all
					1); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;
if (!isShort){		
			System.out.println("Update desc " + table + "... ");
			json = getRequest(TEST_REPO, UPDATE, table, "updateDescription", noderef);
			try {
			    Thread.sleep(TIME_BEFORE_HARVEST);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(
					0, // harvested
					0, // islatest
					1, // is nonlatest
					1, // workspace
					0, // archive space
					1, // all
					1); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;
			
			System.out.println("Update name " + table + "... ");
			json = getRequest(TEST_REPO, UPDATE, table, "updateName", noderef);
			try {
			    Thread.sleep(TIME_BEFORE_HARVEST);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(
					0, // harvested
					0, // islatest
					1, // is nonlatest
					1, // workspace
					0, // archive space
					1, // all
					1); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;
			
			System.out.println("Remove row from lastsuccessfulrun: " + table + "... ");
			json = getRequest(TEST_DB, CLEAR, table, null, null);
			
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(
					0, // harvested
					0, // islatest
					0, // is nonlatest
					0, // workspace
					0, // archive space
					0, // all
					0); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;
			
			
			System.out.println("Add tag " + table + "... ");
			json = getRequest(TEST_REPO, UPDATE, table, "addtag", noderef);
			try {
			    Thread.sleep(TIME_BEFORE_HARVEST);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(
					0, // harvested
					0, // islatest
					1, // is nonlatest
					1, // workspace
					0, // archive space
					1, // all
					1); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;
			
			System.out.println("Add tag " + table + "... ");
			json = getRequest(TEST_REPO, UPDATE, table, "addtag", noderef);
			try {
			    Thread.sleep(TIME_BEFORE_HARVEST);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(
					0, // harvested
					0, // islatest
					1, // is nonlatest
					1, // workspace
					0, // archive space
					1, // all
					1); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;
			
			// delete noderef
			System.out.println("Delete name: " + table + " noderef: " + noderef);
			json = getRequest(TEST_REPO, DELETE, table, null, noderef);
			try {
			    Thread.sleep(TIME_BEFORE_HARVEST);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(
					-1, // harvested
					0, // islatest
					1, // is nonlatest
					0, // workspace
					1, // archive space
					1, // all
					1); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;
			
			System.out.println("Remove row from lastsuccessfulrun: " + table + "... ");
			json = getRequest(TEST_DB, CLEAR, table, null, null);
			
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(
					0, // harvested
					0, // islatest
					0, // is nonlatest
					0, // workspace
					0, // archive space
					0, // all
					0); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
}			
		}
		
	}
	

	private static void testycleNoderefFullRunFolder() throws JSONException{
		//ALL_TABLES = "folder";
		String[] tables = ALL_FOLDER_TABLES.split(",");
		
		TestCycleResults expectations;
		TestCycleResults nextRun;
		TestCycleResults prevRun;
		
		for (String table: tables){
			table = table.trim();
			
			System.out.println("### Dropping reporting table " + table);
			JSONObject json = getRequest(TEST_DB, CLEAR, table, null, null);
			json = getRequest(TEST_DB, DROP, table, null, null);
			String vendor = json.getString("vendor");
			System.out.println("### Reporting database powered by: " + vendor );
			
			prevRun = testCyclusNoderef(table, null);

			// Create a new object
			System.out.print("Creating new " + table + "... ");
			json = getRequest(TEST_REPO, UPDATE, table, "create", null);
			String noderef = json.getString("noderef");
			System.out.println(noderef);
			
			try {
			    Thread.sleep(TIME_BEFORE_HARVEST);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			nextRun = testCyclusNoderef(table, noderef);
			if (table.equals("folder")){
				// for folders, is diferent
				expectations = new TestCycleResults( 
						1, // harvested
						1, // islatest
						1, // is nonlatest
						2, // workspace                 // the parent folder AND the actual folder
						0, // archive space
						2, // all
						-999); // versions
			} else {
				// for all but folders
				expectations = new TestCycleResults( 
						1, // harvested
						1, // islatest
						0, // is nonlatest
						1, // workspace                 // the parent folder AND the actual folder
						0, // archive space
						1, // all
						-999); // versions
			}
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;
			
			System.out.println("Update title " + table + "... ");
			json = getRequest(TEST_REPO, UPDATE, table, "updateTitle", noderef);
			
			try {
			    Thread.sleep(TIME_BEFORE_HARVEST);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(
					0, // harvested
					0, // islatest
					1, // is nonlatest
					1, // workspace
					0, // archive space
					1, // all
					-999); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;
if (!isShort){		
			System.out.println("Update desc " + table + "... ");
			json = getRequest(TEST_REPO, UPDATE, table, "updateDescription", noderef);
			try {
			    Thread.sleep(TIME_BEFORE_HARVEST);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(
					0, // harvested
					0, // islatest
					1, // is nonlatest
					1, // workspace
					0, // archive space
					1, // all
					-999); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;
			
			System.out.println("Update name " + table + "... ");
			json = getRequest(TEST_REPO, UPDATE, table, "updateName", noderef);
			try {
			    Thread.sleep(TIME_BEFORE_HARVEST);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(
					0, // harvested
					0, // islatest
					1, // is nonlatest
					1, // workspace
					0, // archive space
					1, // all
					-999); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;

			System.out.println("Remove row from lastsuccessfulrun: " + table + "... ");
			json = getRequest(TEST_DB, CLEAR, table, null, null);
			
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(
					0, // harvested
					0, // islatest
					0, // is nonlatest
					0, // workspace
					0, // archive space
					0, // all
					-999); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;
			
			
			System.out.println("Add tag " + table + "... ");
			json = getRequest(TEST_REPO, UPDATE, table, "addtag", noderef);
			try {
			    Thread.sleep(TIME_BEFORE_HARVEST);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(
					0, // harvested
					0, // islatest
					1, // is nonlatest
					1, // workspace
					0, // archive space
					1, // all
					-999); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;
			
			System.out.println("Add tag " + table + "... ");
			json = getRequest(TEST_REPO, UPDATE, table, "addtag", noderef);
			try {
			    Thread.sleep(TIME_BEFORE_HARVEST);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(   // Why is adding the second tag not resulting in a new line??
					0, // harvested
					0, // islatest
					1, // is nonlatest
					1, // workspace
					0, // archive space
					1, // all
					-999); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;
			
			// delete noderef
			System.out.println("Delete name: " + table + " noderef: " + noderef);
			json = getRequest(TEST_REPO, DELETE, table, null, noderef);
			try {
			    Thread.sleep(TIME_BEFORE_HARVEST);
			} catch(InterruptedException ex) {
			    Thread.currentThread().interrupt();
			}
			nextRun = testCyclusNoderef(table, noderef);
			if (table.equals("folder")){
				expectations = new TestCycleResults(
						-1, // harvested
						0, // islatest
						2, // is nonlatest
						1, // workspace
						1, // archive space
						2, // all
						-999); // versions
			} else {
				expectations = new TestCycleResults(
						-1, // harvested
						0, // islatest
						1, // is nonlatest
						1, // workspace
						1, // archive space
						1, // all
						-999); // versions
			}
			assertCompareNoderef(prevRun, nextRun, expectations);
			prevRun = nextRun;
			
			System.out.println("Remove row from lastsuccessfulrun: " + table + "... ");
			json = getRequest(TEST_DB, CLEAR, table, null, null);
			
			nextRun = testCyclusNoderef(table, noderef);
			expectations = new TestCycleResults(
					0, // harvested
					0, // islatest
					0, // is nonlatest
					0, // workspace
					0, // archive space
					0, // all
					-999); // versions
			assertCompareNoderef(prevRun, nextRun, expectations);
}
		}
		
	}

	public static TestCycleResults testCyclusNoderef(String table, String noderef) throws JSONException{
		int dbVersions=0;
		// plain harvest
		System.out.print("Harvest "+ table );
		int repoListSize = Integer.parseInt(
					getRequest(TEST_REPO, HARVEST, table, null, null).getString("amount"));
		System.out.print("\t repo: " + repoListSize);
		
		int dbIsLatest = Integer.parseInt(
					getRequest(TEST_DB, COUNT, table, LATEST, null).getString("amount"));
		System.out.print("\t latest: " + dbIsLatest);
		
		int dbIsNonLatest = Integer.parseInt(
				getRequest(TEST_DB, COUNT, table, NONLATEST, null).getString("amount"));
		System.out.print("\t nonLatest: " + dbIsNonLatest);
	
		int dbWorkspace = Integer.parseInt( 
					getRequest(TEST_DB, COUNT, table, WORKSPACE, null).getString("amount") );
		System.out.print("\t workspace: " + dbWorkspace);
		
		int dbArchive  = Integer.parseInt(
					getRequest(TEST_DB, COUNT, table, ARCHIVE, null).getString("amount") );
		System.out.print("\t archive: " + dbArchive);
		
		int dbAll = Integer.parseInt(
					getRequest(TEST_DB, COUNT, table, ALL, null).getString("amount") );
		System.out.print("\t all: " + dbAll);
		
		if (noderef!=null){
			dbVersions = Integer.parseInt(
				getRequest(TEST_DB, COUNT, table, VERSIONS, noderef).getString("amount") );
			System.out.println("\t versions: " + dbVersions);
		} else {
			System.out.println("");
		}
		
		TestCycleResults tsr = new TestCycleResults(
				repoListSize,
				dbIsLatest,
				dbIsNonLatest,
				dbWorkspace,
				dbArchive,
				dbAll,
				dbVersions);
		return tsr;
	}
	
		
	public static void main(String[] args) throws JSONException {
		
		JSONObject json = getRequest(TEST_DB, DROP, null, "all", null);
		//testycleNoderefFullRunFolder();
		testycleNoderefFullRunDocument();
		
	}

}
