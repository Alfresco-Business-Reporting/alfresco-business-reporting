/**
 * Copyright (C) 2011 - 2013 Alfresco Business Reporting project
 *
 * This file is part of the Alfresco Business Reporting project.
 *
 * Licensed under the GNU LGPL, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.gnu.org/licenses/lgpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.alfresco.reporting.execution;

import java.io.File;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JasperReporting implements Reportable{

	private NodeRef inputNodeRef;
	private NodeRef outputNodeRef;
	private String format="";
	private Connection conn;
	private ServiceRegistry serviceRegistry;
	private String dataType = "JDBC";
	
	private String user = "";
	private String pass = "";
	private String url = "";
	private String driver = "";
	private String jndiName = ""; // unused in JasperReports
	private Properties globalProperties;
	
	//public static String EXTENSION = ".jasper";
	
	private static Log logger = LogFactory.getLog(JasperReporting.class);
	
	@Override
	public void setDataSourceType(String dataType){
		this.dataType = dataType;
	}
	
	@Override
	public void setUsername(String user){
		this.user= user; 
	}
		
	@Override
	public void setPassword(String pass){
		this.pass= pass; 
	}
		
	@Override
	public void setUrl(String url){
		this.url= url; 
	}
	
	@Override
    public void setMimetype(String ext){
    	if ("pdf".equals(ext.toLowerCase())){
    		//ContentReader content = serviceRegistry.getContentService().getReader(), ContentModel.PROP_CONTENT);
            //content.setMimetype(MimetypeMap.MIMETYPE_PDF);
    	}
    	if (("xls".equals(ext.toLowerCase())) || ("xlsx".equals(ext.toLowerCase()))){
    		//ContentReader content = serviceRegistry.getContentService().getReader(output, ContentModel.PROP_CONTENT);
            //content.setMimetype(MimetypeMap.MIMETYPE_EXCEL);
    	}	 
    }
		
	@Override
	public void setDriver(String driver){
		this.driver= driver; 
	}
	
	@Override
	public void setJndiName(String name){
		this.jndiName=name;
	}
	
	@Override
	public void setReportDefinition(NodeRef input){
		inputNodeRef = input;
	}
	
	@Override
	public void setResultObject(NodeRef output){
		outputNodeRef = output;
	}
	
	@Override
	public void setOutputFormat(String format){
		this.format = format;
	}
	
	@Override
	public void setParameter(String key, String value){
		
	}
	
	/*
	@Override
	public void setConnection(Connection conn){
		this.conn = conn;
	}
	*/
	
	@Override
	public void setServiceRegistry(ServiceRegistry serviceRegistry){
		this.serviceRegistry = serviceRegistry;
	}
	
	/*
	public static JRDataSource convertReportData(Object value) throws IllegalArgumentException {
		if (value instanceof JRDataSource) {
			return (JRDataSource) value;
		}
		else if (value instanceof Collection) {
			return new JRBeanCollectionDataSource((Collection) value);
		}
		else if (value instanceof Object[]) {
			return new JRBeanArrayDataSource((Object[]) value);
		}
		else if (value instanceof JRDataSourceProvider) {
			return null;
		}
		else {
			throw new IllegalArgumentException("Value [" + value + "] cannot be converted to a JRDataSource");
		}
	}
	*/
	
	public void processReport(){
		Map parameters = new HashMap();
        //parameters.put("format", format.toLowerCase());
        OutputStream reportOS = null;
        File tempFile = null;
		try {
	        ContentReader contentReader = serviceRegistry.getContentService().getReader(inputNodeRef, ContentModel.PROP_CONTENT);
	        String name = serviceRegistry.getNodeService().getProperty(inputNodeRef, QName.createQName("http://www.alfresco.org/model/content/1.0", "name")).toString();
	        // Don't trust Alfresco's TempFileProvider, the temp file will be deleted before you blink with your eyes... 
	        tempFile = File.createTempFile("reporting", EXTENSION);
	    	logger.debug("Prepping tempFile: "+ tempFile.getAbsolutePath());
	    	contentReader.getContent(tempFile);
	    	
	        logger.debug("Stored tempfile all right");
			//String jasperReportCS = contentReader.getContentString();
			//logger.debug("Got contentstream");
	 
	        
			JRExporter exporter = null;
			if (format.equalsIgnoreCase("pdf")){
				exporter = new JRPdfExporter(); 
			}
			if (format.equalsIgnoreCase("html")){
				exporter = new JRHtmlExporter(); 
			}
			if (format.equalsIgnoreCase("xls")){
				exporter = new JRXlsExporter(); 
			}
			if (format.equalsIgnoreCase("doc")){
				exporter = new JRDocxExporter(); 
			}
	        logger.debug("The exporter has a value.");
	        
	        JasperPrint jasperPrint = null;
	        // either it is a plain XML, then we need to compile
	        if (name.endsWith(".jrxml")){
	        	logger.debug("It is a .jrxml");
	        	JasperReport jasperReport = JasperCompileManager.compileReport(tempFile.getAbsolutePath());
	        	logger.debug("Just compiled the Report " + jasperReport);
	        	jasperPrint = JasperFillManager.fillReport( jasperReport, parameters, conn );
	        } else {
	        	// or it is a .jasper, then it is compiled already
	        	logger.debug("It is a .jasper");
	        	jasperPrint = JasperFillManager.fillReport( tempFile.getAbsolutePath(), parameters, conn );	        	
	        }
			
	        logger.debug("Just filled the report");
	        
	        ContentWriter contentWriter = serviceRegistry.getContentService().getWriter(outputNodeRef, ContentModel.PROP_CONTENT, true);
	        logger.debug("got the contentWriter");
	        reportOS = contentWriter.getContentOutputStream();
	        logger.debug("got the outputStream");
	        exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
			exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, reportOS);
		logger.debug("Just before exportReport");
			exporter.exportReport();
		} catch (JRException e) {
			//nodeService.deleteNode(reportNodeRef);
			logger.error("Error occurred in generating report " + inputNodeRef +" and storing into "+ outputNodeRef);
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try{
				logger.debug("closing the stream");
				reportOS.close();
			} catch (Exception e){
				logger.error("Cannot close connection after generating report...");
			}
			if ((tempFile!=null) && tempFile.canWrite()){
	     		tempFile.delete();
			} else {
				tempFile.deleteOnExit();
			}
		}
		
	}
	
    /**
    private File getTempFolder() throws Exception{
    	File tempFolder = new File(System.getProperty("CATALINA_TMPDIR"));
		if (tempFolder==null ) {
			tempFolder  = new File (System.getProperty("tmp")); // linux most obvious
		}
		if (tempFolder==null ) {
			tempFolder  = new File (System.getProperty("TMP"));
		}
		if (tempFolder==null ) {
			tempFolder  = new File (System.getProperty("temp")); // Windows most obvious
		}
		if (tempFolder==null ) {
			tempFolder  = new File (System.getProperty("TEMP"));
		}
		if (tempFolder==null ) {
			tempFolder  = new File (System.getProperty("CATALINA_TMPDIR"));
		}
		if (tempFolder==null) {
			throw new Exception("Cannot find any temp dir environment variable (CATALINA_TMPDIR, TMP, TEMP, tmp, temp"); 
		}
    	return tempFolder;
    }
    **/
    /**
     * downloadContent
     * @param fileName the name of the file, including folder relative to baseDirAlfresco
     * @param baseDirAlfresco the name of the basedir, the folderpath of the JasperReport main file
     * @param basePathFS the name of the base dir on the filesystem. The subfolder in the temp space.
     * The method gets the content node by name path (baseDirAlfresco + fileName). The method creates a file on baseDirFS + fileName
     **/
    /**
    private boolean downloadContent(String fileName, String baseDirAlfresco, String basePathFS){
    	String alfrescoPath = baseDirAlfresco + "/" + fileName;
    	String filePath = basePathFS + File.separator + fileName;
    	logger.debug("Move content from " + alfrescoPath + " to " + filePath);
    	//serviceRegistry.getNodeService().getChildByName(null, cm:content, arg2)
    	return true;
    }
    **/
    /**
    private String replaceJasperXML(String content, String basePath) throws ParserConfigurationException, SAXException, IOException{
    	 DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    	 DocumentBuilder db = dbf.newDocumentBuilder();
    	 Document doc = db.parse(content);
    	 doc.getDocumentElement().normalize();
    	 // Find the baseDir for the subreport
    	 // parameter[@name="SUBREPORTDIR"]/defaultValueExpression
    	 logger.debug("Root element " + doc.getDocumentElement().getNodeName());

    	 String baseDir = "";
    	 NodeList reportDirs = doc.getElementsByTagName("parameter[@name=\"SUBREPORTDIR\"]/defaultValueExpression");
    	 for (int i=0; i<reportDirs.getLength(); i++){
    		 Node dir = reportDirs.item(i);
    		 baseDir = dir.getTextContent();
    		 logger.debug("Found baseDir: " + baseDir);
    	 }
    		
    	 // Find the subreport definition
    	 NodeList nodeList = doc.getElementsByTagName("/band/subreport/subreportExpression");
    	 for (int i=0; i<nodeList.getLength(); i++){
    		 Node sub = nodeList.item(i);
    		 logger.debug("Found subReport before: " + sub.getTextContent());
    		 sub.setTextContent(sub.getTextContent().replaceAll(baseDir,"tralala loempia"));
    		 logger.debug("Found subReport after : " + sub.getTextContent());
    		 // copy the subreport to the location specified
    		 //serviceRegistry.getNodeService().getChildByName(null, cm:content, arg2)
    		 //ToDo
    	 }

    	 return doc.toString();
    }
    **/
    /**
    private JasperReport getCompiledJasperReport(NodeRef jrxmlNodeRef) throws JRException, Exception{
    	// get the report definition from the Alfresco repository
        ContentReader contentReader = serviceRegistry.getContentService().getReader(jrxmlNodeRef, ContentModel.PROP_CONTENT);
        String name = serviceRegistry.getNodeService().getProperty(jrxmlNodeRef, QName.createQName("http://www.alfresco.org/model/content/1.0", "name")).toString();
		//Input jasperReportIS = new BufferedInputStream(contentReader.getContentInputStream(), 4096);
		String jasperReport = contentReader.getContentString();
		logger.debug("Got the content");
		if (jasperReport.indexOf("subreportExpression")>-1){
			// download the thingies locally
			Date now = new Date();
			String folderName = getTempFolder().getAbsolutePath()+"jasper"+now.getTime();
			String mainName = folderName+ File.separator + name;
			logger.debug("mainName: " + mainName);
			jasperReport = replaceJasperXML(jasperReport, mainName);
			if (new File(mainName).createNewFile()){
				File mainFile = new File(mainName);
				BufferedWriter output = new BufferedWriter(new FileWriter(mainFile));
				output.write(jasperReport);
				output.close();
			}
			
		}
		return JasperCompileManager.compileReport(jasperReport);
		
    }
    **/
	
	 public void setGlobalProperties(Properties properties){
	    	this.globalProperties = properties;
	    }

	@Override
	public void setDatabaseHelper(DatabaseHelperBean dbhb) {
		// TODO Auto-generated method stub
		
	}
	
}
