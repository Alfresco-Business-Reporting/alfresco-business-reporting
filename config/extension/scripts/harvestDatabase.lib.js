/**
 * Copyright (C) 2011 - 2013 Alfresco Business Reporting project
 * 
 * This file is part of the Alfresco Business Reporting project.
 * 
 * Licensed under the GNU LGPL, Version 3.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 * 
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var timestampsAreDateOnly = false; // the Alfresco default. Change settings if
									// Lucene stores full time too.

//------------------------------------------------------------------------

function main(harvestNode, frequency) {
	logger.log("Welcome in main!");
	try{
		if (reporting.isHarvestEnabled()) {
			
			var harvestDefinitions;
			
			logger.log("Checking moreFrequent");
			if ((frequency!=null) && (frequency.toLowerCase()=="morefrequent")) {
				harvestDefinitions = search
					.luceneSearch("+TYPE:\"reporting:harvestDefinition\" +@reporting\\:harvestFrequency:\"moreFrequent\"");
				logger.log("it is a moreFrequent!");
			}
			
			logger.log("Checking lessFrequent");
			if ((frequency!=null) && (frequency.toLowerCase()=="lessfrequent")) {
				harvestDefinitions = search
					.luceneSearch("+TYPE:\"reporting:harvestDefinition\" +@reporting\\:harvestFrequency:\"lessFrequent\"");
				logger.log("It is a lessFrequent!");
			}
			
			logger.log("Checking harvestAll");
			if ((frequency!=null) && (frequency=="all")) {
				harvestDefinitions = search
					.luceneSearch("TYPE:\"reporting:harvestDefinition\"");
				logger.log("It is an All!");
			}
			
			logger.log("Checking harvestRef!=null");
			if (harvestNode!=null){ 
				var harvestDefinitions = new Array();
				//var noderef = search.findNode(harvestRef);
				if (harvestNode.type=="{http://www.alfresco.org/model/reporting/1.0}harvestDefinition"){
					harvestDefinitions.push(harvestNode);
					logger.log("There is valid a nodeRef!! " + harvestNode.name);
				} else {
					logger.log("There is a lousy nodeRef!! " + harvestNode.name);
					harvestDefinitions = search.luceneSearch("TYPE:\"reporting:harvestDefinition\"");
					logger.log(">> It is an All after all!");
				}
			}
			
			logger.log("Number of results: " + harvestDefinitions.length);
			
			for (var h in harvestDefinitions){
				var harvestDefinition = harvestDefinitions[h];
				logger.log("main: (" + h + ") Processing: " + harvestDefinition.name);
				
				// and this is how it should be done:
				var harvestAction = actions.create("harvesting-executer");
				harvestAction.execute(harvestDefinition);
			} // end for
		} // end if isHarvestEnabled
		else {
			logger.log("Harvesting not enabled...")
			}
	} catch (exception){
		// can we catch the unhandled exception from the Scheduled Job?
		// nothing
		logger.log("Bad news!: " + exception);
	}
	try{
		reporting.setAllStatusesDoneForTable();
	} catch (exception) {
		logger.log("@@@@ Exception!: reporting.setAllStatusesDoneForTable failed " + exception);
	}
} // end main

// ------------------------------------------------------------------------

function trim(instring) {
	try {
		while (instring.indexOf(" ") == 0) {
			instring = instring.substring(1, instring.length);
		}
		// TODO and the end too
	} catch (e) {
		// nothing
	}
	return instring;
}



// --------------------------------------------------------------------------

function stringLength(input) {
	var myInput = new String(input);
	if (myInput.length < 2) {
		myInput = "0" + myInput;
	}
	return myInput;
}

// --------------------------------------------------------------------------

function setQueryDate(tablename, timestamp) {
	logger.log("Enter setQueryDate for timestamp=" + timestamp + " and table="
			+ tablename);
	// prepare the new time string

	var timeString = "";
	if (timestampsAreDateOnly) {
		// store date only. Replace time by 00:00:00 in order to find all
		// documents created within this day
		timeString = timestamp.getFullYear() + "-"
				+ stringLength(timestamp.getMonth() + 1) + "-"
				+ stringLength(timestamp.getDate()) + "T00:00:00";
	} else {
		timeString = timestamp.getFullYear() + "-"
				+ stringLength(timestamp.getMonth() + 1) + "-"
				+ stringLength(timestamp.getDate()) + "T"
				+ stringLength(timestamp.getHours()) + ":"
				+ stringLength(timestamp.getMinutes()) + ":"
				+ stringLength(timestamp.getSeconds());
	}
	reporting.setLastTimestampAndStatusDone(tablename, timeString);
	logger.log("Enter setQueryDate for timeString=" + timeString);

}

