
<#assign el=args.htmlid?html>


<div id="${el}-body" class="reporting-console">
 	<div class="header-bar"><div class="yui-u first">
        <div class="title">Alfresco Business Reporting</div>
    </div></div>
	<div id="${el}-main">
	    
		<div id="${el}-inputTabs" class="yui-navset">
		    <ul class="yui-nav">
		        <li class="selected"><a href="#itab1"><em>Reporting Database</em></a></li>
	        
		    </ul>            
		    <div id="${el}-displayReportingDatabase" class="yui-content">
		    <table cellpadding="2">
		    <#--
		    <tr>
		    	<td colspan="3"></td>
		    	<th colspan="4" align="center">Number of rows...</th>
		    </tr>
		    -->
		    <tr>
				<th>Table</th>
		    	<th>Last run/batch (workspace)</th>
		    	<th>Last run/batch (archive)</th>
		    	<th>Status</th>
		    	<th>#rows</th>
		    </tr>
		    <#list reportingtables as reportingtable>
		    
		    	<tr>
		    		<td> ${reportingtable.table!""} </td>
		    		<td align="center"> ${reportingtable.last_run_w!""} &nbsp;</td>
		    		<td align="center"> ${reportingtable.last_run_a!""} &nbsp;</td>
		    		<td> ${reportingtable.status!""} &nbsp;</td>
		    		<td align="right"> ${reportingtable.number_of_rows!""} &nbsp;</td>
		    	</tr>
		    
		    </#list>
		    </table>
		    <p>&nbsp;</p>
		    <p><i>The status Done means the table is not active. Previous run or batch was completed as expected. Status Running means the tool is actively working against this table (of failed to complete normally). There is a (potential) last run timestamp for the Workspace and the Archive SpaceStore, since these can differ </i></p>
		    </div>
		</div>
	</div>
</div>
