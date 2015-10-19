package org.alfresco.reporting;

import java.util.Set;

public class Utils {

	/**
	 * returns the toString display of a set, but without the [] characters
	 * 
	 * @param inputSet set of Strings
	 * @return String inputSet.toString() without [ and ]
	 */
	public static String setToString(Set inputSet){
		String returnString ="";
		if (inputSet != null ){
			returnString = inputSet.toString();
			if (returnString.startsWith("[")){
				returnString = returnString.substring(1, returnString.length());
			} // end startsWith [
			if (returnString.endsWith("]")){
				returnString = returnString.substring(0,returnString.length()-1);
			} // end endsWith ]
		} // end if inputSet!=null
		return  returnString;
	}
	
	
}
