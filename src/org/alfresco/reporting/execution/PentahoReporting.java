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

import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import javax.naming.NamingException;

import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
//import org.pentaho.reporting.engine.classic.core.DataFactory;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.AbstractReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.csv.CSVProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.FlowReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.csv.CSVReportUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.csv.FlowCSVOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.csv.helper.CSVOutputProcessorMetaData;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.ExcelReportUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.FlowExcelOutputProcessor;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;


import org.alfresco.model.ContentModel;
import org.alfresco.reporting.FileHelper;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class PentahoReporting implements Reportable {

	private NodeRef input;
	private NodeRef output;
	private String format="";
	private ServiceRegistry serviceRegistry;

	private Properties parameters = new Properties();

	
	private File tempFile;
	
	private DatabaseHelperBean dbhb = new DatabaseHelperBean();
	private Properties globalProperties;
	//private DataSource dataSource = "JNDI";
	public static String EXTENSION = ".prpt";
	
 	 /**
     * Performs the basic initialization required to generate a report
     */
	public PentahoReporting(){
		 // Initialize the reporting engine
		 ClassicEngineBoot.getInstance().start();
	}
	
	/*
	// jdbc:mysql://${reporting.db.host}:${reporting.db.port}/${reporting.db.name}
	private String getServer(){
		String server = url.substring(url.indexOf("://")+3, url.indexOf(":", url.indexOf("://")+5));
		logger.debug("getServer returning: "+server);
		return server;
	}
	
	private String getDatabase(){
		String database = url.substring(url.lastIndexOf("/")+1, url.length());
		logger.debug("getDatabase returning: " + database);
		return database;
	}
	
	private int getPort(){
		String port = url.substring(url.indexOf(":", 13)+1, url.lastIndexOf("/"));
		logger.debug("getPort returning: "+ port);
		return Integer.parseInt(port);
	}
	
	*/
	private static Log logger = LogFactory.getLog(PentahoReporting.class);
	
	@Override
	public void setUsername(String user){
//		this.user= user; 
	}
		
	@Override
	public void setPassword(String pass){
//		this.pass= pass; 
	}
		
	@Override
	public void setUrl(String url){
//		this.url= url; 
	}
		
	@Override
	public void setDriver(String driver){
//		this.driver= driver; 
	}
		
		@Override
		public void setReportDefinition(NodeRef input){
			this.input = input;
		}
		
		@Override
		public void setResultObject(NodeRef output){
			this.output = output;
		}
		
		@Override
		public void setOutputFormat(String format){
			this.format = format;
		}
		
		@Override
		public void setParameter(String key, String value){
			logger.debug("setParameter: key="+ key + "=" + value);
			parameters.put(key, value);
		}

		
		@Override
		public void setServiceRegistry(ServiceRegistry serviceRegistry){
			this.serviceRegistry = serviceRegistry;
		}

		@Override
		public void setDataSourceType(String dataType){
			//this.dataType = dataType;
		}
		
		@Override
		public void setJndiName(String name){
			//logger.debug("setJndiName: Setting jndi name="+name);
			//this.jndiName=name;
		}
		/**
         * The supported output types for this sample
         */
        public static enum OutputType {
                 PDF, EXCEL, CSV
        }
        
        @Override
        public void setMimetype(String ext){
        	/*
        	if ("pdf".equals(ext.toLowerCase())){
        		ContentReader content = serviceRegistry.getContentService().getReader(output, ContentModel.PROP_CONTENT);
                content.setMimetype(MimetypeMap.MIMETYPE_PDF);
        	}
        	if (("xls".equals(ext.toLowerCase())) || ("xlsx".equals(ext.toLowerCase()))){
        		ContentReader content = serviceRegistry.getContentService().getReader(output, ContentModel.PROP_CONTENT);
                content.setMimetype(MimetypeMap.MIMETYPE_EXCEL);
        	}
        	*/

        }

		private MasterReport getReportDefinition() throws ContentIOException, IOException {
			logger.debug("getReportDefinition start");
			File tempFile = null;
			
		    try {
		    	ContentReader contentReader = serviceRegistry.getContentService().getReader(input, ContentModel.PROP_CONTENT);
		    	// Don't trust Alfresco's TempFileProvider, the temp file will be deleted before you blink your eyes...
		    	tempFile = File.createTempFile("reporting_", EXTENSION);
		    	//tempFile = TempFileProvider.createTempFile("reporting_", EXTENSION);
		    	contentReader.getContent(tempFile);
		        logger.debug("Wrote the tempFile: "+ tempFile.getAbsolutePath());
		    
		        //tempFile = updateJndiElementInReportDefinition(tempFile);
		        
		        
		    	final ResourceManager resourceManager = new ResourceManager();
		    	String prefix = "";
		     	resourceManager.registerDefaults();
		     	
		     	if (FileHelper.isWindows()) prefix = "file:/";
		     	if (FileHelper.isUnix()) prefix = "file://";
		     	if (FileHelper.isMac()) prefix = "file://";
		     	if (FileHelper.isSolaris()) prefix = "file://";
		     	
		     	String contentUrl = prefix+tempFile.getAbsolutePath();
		     	logger.debug("The new contentUrl=" + contentUrl.toString());
		     	
		     	String[] folders = contentUrl.split("\\\\");
		     	String newUrl="";

		     	for (int k=0;k<folders.length;k++){
		     		if (!newUrl.equals("")){
		     			newUrl+="/";
		     		}
		     		newUrl += folders[k]; 
		     	}
		     	logger.debug("The new url Path=" + newUrl.toString());
		     	
		     	final Resource report = resourceManager.createDirectly(newUrl, MasterReport.class);
			    
		     	logger.debug("getReportDefinition: Returning the MasterReport: " + report.toString() );
		     	if (tempFile.canWrite()){
		     		tempFile.delete();
				} else {
					tempFile.deleteOnExit();
				}
			    return (MasterReport) report.getResource();
		    } catch (ResourceException e) {
		    	e.printStackTrace();
		    	logger.fatal(e.getMessage());
		    } catch (Exception e){
		    	e.printStackTrace();
		    	logger.fatal(e.getMessage());
		    }
		    logger.debug("getReportDefinition end");
		    return null;
		}
		  


		/**
         * Generates the report in the specified <code>outputType</code> and writes
         * it into the specified <code>outputStream</code>.
         * <p/>
         * It is the responsibility of the caller to close the
         * <code>outputStream</code> after this method is executed.
         *
         * @param outputType
         *            the output type of the report (HTML, PDF, HTML)
         * @param outputStream
         *            the stream into which the report will be written
         * @throws IllegalArgumentException
         *             indicates the required parameters were not provided
         * @throws ReportProcessingException
         *             indicates an error generating the report
		 * @throws IOException 
		 * @throws ContentIOException 
		 * @throws SQLException 
		 * @throws NamingException 
		 * @throws ClassNotFoundException 
         */

        public void generateReport(final OutputType outputType, ContentWriter contentWriter /*OutputStream outputStream*/) 
        	throws IllegalArgumentException, ReportProcessingException, ContentIOException, IOException, NamingException, SQLException, ClassNotFoundException {
        	logger.debug("generateReport start");
        	File tempCsv = null;
        	
            // make sure we have a DB connection
        	// not; tjp 20140911 uitgekommentaard omdat er write locks zijn.
        	dbhb.openReportingConnection();
        	
            ClassicEngineBoot.getInstance().start();
            MasterReport report = getReportDefinition();
            
            Enumeration keys = parameters.keys();
            while (keys.hasMoreElements()){
            	String key = (String)keys.nextElement();
            	logger.debug("generateReport: setting key="+key+"="+parameters.getProperty(key));
            	report.getParameterValues().put(key, parameters.getProperty(key));	
            }

             // Prepare to generate the report
        	OutputStream outputStream = contentWriter.getContentOutputStream();
        	if (outputStream == null) {
                throw new IllegalArgumentException("The output stream was not specified");}
        	AbstractReportProcessor reportProcessor = null;
            try {
             	// Create the report processor for the specified output type
               switch (outputType) {
                   case PDF: {
                        final PdfOutputProcessor outputProcessor = new PdfOutputProcessor(
                        	report.getConfiguration(), outputStream, report.getResourceManager());
                        reportProcessor = new PageableReportProcessor(report, outputProcessor);
                        break;
                   }

                   case EXCEL: {
                	   
                        final FlowExcelOutputProcessor target = new FlowExcelOutputProcessor(
                        	report.getConfiguration(), outputStream, report.getResourceManager());
                        reportProcessor = new FlowReportProcessor(report, target);
                       
                        break;
                   }

                   case CSV: {
                	   // actually.... do nothing!
                	   // (use a different processReport() instead)
                	   break;
                   }

               } // end switch
               

               // Generate the report
               logger.debug("just before processReport()");
               if (outputType==OutputType.CSV){
            	   CSVReportUtil.createCSV(report, outputStream, "UTF-8");
               } 
               if (outputType==OutputType.EXCEL){
            	   ExcelReportUtil.createXLSX(report, outputStream);
               } else {
               
            	   reportProcessor.processReport();
               }

               logger.debug("Just after processReport()");
            } catch (Exception e) {
            	e.printStackTrace();  	  
            	logger.error("processReport: "+ parameters.toString());
            	logger.error("processReport: "+ e.getMessage());
              
			} finally {
                  if (reportProcessor != null) {
                	 reportProcessor.close();
                  }
                 // make sure we gently close the connection
          		 dbhb.closeReportingConnection();
          		
             }
        }
 
		
		public void processReport() throws IllegalStateException, SecurityException {
			logger.debug("processReport start");
			try{
				ContentWriter contentWriter = 
						serviceRegistry.getContentService().
							getWriter(output, ContentModel.PROP_CONTENT, true);
				
				String filename = serviceRegistry.getNodeService().getProperty(
						output, 
						ContentModel.PROP_NAME).toString();
				
				String mimetype = serviceRegistry.getMimetypeService().guessMimetype(filename);
				contentWriter.setMimetype(mimetype);
				
				logger.debug("Found: " +  filename + " mimetype: " + mimetype);
						
				//OutputStream outputStream = contentWriter.getContentOutputStream();
				//logger.debug("Got the outputstream: " + outputStream);
				
				if ("pdf".equalsIgnoreCase(format)){
					generateReport(OutputType.PDF, contentWriter);
				}
				if ("excel".equalsIgnoreCase(format)){
					generateReport(OutputType.EXCEL, contentWriter);
				}
				if ("csv".equalsIgnoreCase(format)){
					generateReport(OutputType.CSV, contentWriter);
				}
					
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (ReportProcessingException e) {
				e.printStackTrace();
			} catch (ContentIOException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NamingException e) {
				e.printStackTrace();
			} catch (SQLException e) {
				e.printStackTrace();
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (Exception e){
				e.printStackTrace();
			}
			logger.debug("processReport end");
		}
		
	    public void setGlobalProperties(Properties properties){
	    	this.globalProperties = properties;
	    }
	    
	    public void setDatabaseHelper(DatabaseHelperBean dbhb){
	    	this.dbhb = dbhb;
	    }
	}
