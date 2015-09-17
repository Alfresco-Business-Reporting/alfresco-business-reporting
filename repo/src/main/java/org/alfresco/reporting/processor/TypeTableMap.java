package org.alfresco.reporting.processor;

import java.util.ArrayList;

/**
 * @author Martijn van de Brug
 * Mapping tablenames to a type
 */
public class TypeTableMap {
	
	private String type;
	private ArrayList<String> tables = new ArrayList<String>();
	
	public String getType() {
		return type;
	}
	
	public void setType(String type) {
		this.type = type;
	}
	
	public void addTable(String tablename){
		tables.add(tablename);
	}
	
	public void removeTable(String tablename){
		tables.remove(tablename);
	}
	
	public ArrayList<String> getTables() {
		return tables;
	}
	
	public void setTables(ArrayList<String> tables) {
		this.tables = tables;
	}
	

}
