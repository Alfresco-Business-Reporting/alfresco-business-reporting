package org.alfresco.reporting.test;

public class TestCycleResults {
	int repoListSize;
	int dbIsLatest;
	int dbIsNonLatest;
	int dbWorkspace;
	int dbArchive;
	int dbAll;
	int dbVersions;
	int harvested;
	
	public TestCycleResults(
			int harvested,
			int isLatest,
			int isNonLatest,
			int dbWorkspace,
			int dbArchive,
			int dbAll,
			int dbVersions){
		
		setHarvested(harvested);
		setDbIsLatest(isLatest);
		setDbIsNonLatest(isNonLatest);
		setDbWorkspace(dbWorkspace);
		setDbArchive(dbArchive);
		setDbAll(dbAll);
		setDbVersions(dbVersions);
	}
	
	public void setHarvested(int harvested){
		this.harvested = harvested;
	}
	
	public int getHarvested(){
		return harvested;
	}
	
	public int getDbIsLatest() {
		return dbIsLatest;
	}
	public void setDbIsLatest(int dbIsLatest) {
		this.dbIsLatest = dbIsLatest;
	}
	public int getDbIsNonLatest() {
		return dbIsNonLatest;
	}
	public void setDbIsNonLatest(int dbIsNonLatest) {
		this.dbIsNonLatest = dbIsNonLatest;
	}
	public int getDbWorkspace() {
		return dbWorkspace;
	}
	public void setDbWorkspace(int dbWorkspace) {
		this.dbWorkspace = dbWorkspace;
	}
	public int getDbArchive() {
		return dbArchive;
	}
	public void setDbArchive(int dbArchive) {
		this.dbArchive = dbArchive;
	}
	public int getDbAll() {
		return dbAll;
	}
	public void setDbAll(int dbAll) {
		this.dbAll = dbAll;
	}
	public int getDbVersions() {
		return dbVersions;
	}
	public void setDbVersions(int dbVersions) {
		this.dbVersions = dbVersions;
	}
	
	
	
}
