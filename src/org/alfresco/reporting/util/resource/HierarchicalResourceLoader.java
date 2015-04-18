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

package org.alfresco.reporting.util.resource;

import javax.sql.DataSource;

import org.alfresco.util.PropertyCheck;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * Locate resources by using a class hierarchy to drive the search.  The well-known
 * placeholder {@link #DEFAULT_DIALECT_PLACEHOLDER} is replaced with successive class
 * names starting from the {@link #setDialectClass(String) dialect class} and
 * progressing up the hierarchy until the {@link #setDialectBaseClass(String) base class}
 * is reached.  A full resource search using Spring's {@link DefaultResourceLoader} is
 * done at each point until the resource is found or the base of the class hierarchy is
 * reached.
 * <p/>
 * For example assume classpath resources:<br/>
 * <pre>
 *    RESOURCE 1: config/ibatis/org.hibernate.dialect.Dialect/SqlMap-DOG.xml
 *    RESOURCE 2: config/ibatis/org.hibernate.dialect.MySQLInnoDBDialect/SqlMap-DOG.xml
 *    RESOURCE 3: config/ibatis/org.hibernate.dialect.Dialect/SqlMap-CAT.xml
 *    RESOURCE 4: config/ibatis/org.hibernate.dialect.MySQLDialect/SqlMap-CAT.xml
 * </pre>
 * and<br/>
 * <pre>
 *    dialectBaseClass = org.hibernate.dialect.Dialect
 * </pre>
 * For dialect <b>org.hibernate.dialect.MySQLInnoDBDialect</b> the following will be returned:<br>
 * <pre>
 *    config/ibatis/#resource.dialect#/SqlMap-DOG.xml == RESOURCE 2
 *    config/ibatis/#resource.dialect#/SqlMap-CAT.xml == RESOURCE 4
 * </pre>
 * For dialect<b>org.hibernate.dialect.MySQLDBDialect</b> the following will be returned:<br>
 * <pre>
 *    config/ibatis/#resource.dialect#/SqlMap-DOG.xml == RESOURCE 1
 *    config/ibatis/#resource.dialect#/SqlMap-CAT.xml == RESOURCE 4
 * </pre>
 * For dialect<b>org.hibernate.dialect.Dialect</b> the following will be returned:<br>
 * <pre>
 *    config/ibatis/#resource.dialect#/SqlMap-DOG.xml == RESOURCE 1
 *    config/ibatis/#resource.dialect#/SqlMap-CAT.xml == RESOURCE 3
 * </pre>
 * 
 * @author Tjarda Peelen, based on work of Derek Hulley, Alfresco
 * DefaultResourceLoader
 */
public class HierarchicalResourceLoader extends org.alfresco.util.resource.HierarchicalResourceLoader
{
    public static final String DEFAULT_DIALECT_PLACEHOLDER = "#reporting.resource.dialect#";
    public static final String DEFAULT_DIALECT_REGEX = "\\#reporting\\.resource\\.dialect\\#";
    
    private String dialectBaseClass;
    private String dialectClass;
    private DataSource datasource; 
    
    private String mySqlClassName = "org.hibernate.dialect.MySQLInnoDBDialect";
    private String postgreSqlClassName = "org.hibernate.dialect.PostgreSQLDialect";
    private String oracleClassName;
    private String msSqlClassName;
    
    private String resourcePath="";
    private String databaseVendor="";


	/**
     * Create a new HierarchicalResourceLoader.
     */
    public HierarchicalResourceLoader()
    {
        super();
    }

    private void setDatabaseVendor(String vendor){
    	this.databaseVendor = vendor;
    }
    
    public String getDatabaseVendor(){
    	return this.databaseVendor;
    }
    
    /**
     * Set the class to be used during hierarchical dialect replacement.  Searches for the
     * configuration location will not go further up the hierarchy than this class.
     * 
     * @param className     the name of the class or interface
     */
    public void setDialectBaseClass(String className)
    {
        this.dialectBaseClass = className;
    }
    
    public void setDialectClass(String className)
    {
        this.dialectClass = className;
    }
    
    
    public void setDatasource(DataSource datasource)
    {
        this.datasource = datasource;
        this.dialectClass = "";
    }
    
    public void setMySqlClassName(String mySqlClassName)
    {
        this.mySqlClassName = mySqlClassName;
    }

    public void setPostgreSqlClassName(String postgreSqlClassName)
    {
        this.postgreSqlClassName = postgreSqlClassName;
    }

    public void setOracleClassName(String oracleSqlClassName)
    {
        this.oracleClassName = oracleSqlClassName;
    }

    public void setMsSqlClassName(String msSqlClassName)
    {
        this.msSqlClassName = msSqlClassName;
    }

    public void afterPropertiesSet() throws Exception
    {
        String database = datasource.getConnection().getMetaData().getDatabaseProductName();
        setDatabaseVendor(database);
        //System.out.println("Tha database = " + database);
        if ("PostgreSQL".equalsIgnoreCase(database)){
        	this.dialectClass = postgreSqlClassName; 
        }
        if ("MySQL".equalsIgnoreCase(database)){
        	this.dialectClass =mySqlClassName;
        }
        if ("oracle".equalsIgnoreCase(database)){
        	this.dialectClass = oracleClassName; 
        }
        if ("sqlserver".equalsIgnoreCase(database)){
        	this.dialectClass =msSqlClassName;
        }
        
        PropertyCheck.mandatory(super.getClass(), "dialectBaseClass", dialectBaseClass);
        PropertyCheck.mandatory(super.getClass(), "dialectClass", dialectClass);
    }
    
    /**
     * Get a resource using the defined class hierarchy as a search path.
     * 
     * @param location          the location including a {@link #DEFAULT_DIALECT_PLACEHOLDER placeholder}
     * @return                  a resource found by successive searches using class name replacement, or
     *                          <tt>null</tt> if not found.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Resource getResource(String location)
    {
        if (dialectClass == null || dialectBaseClass == null)
        {
            return super.getResource(location);
        }
        
        // If a property value has not been substituted, extract the property name and load from system
        String dialectBaseClassStr = dialectBaseClass;
        if (!PropertyCheck.isValidPropertyString(dialectBaseClass))
        {
            String prop = PropertyCheck.getPropertyName(dialectBaseClass);
            dialectBaseClassStr = System.getProperty(prop, dialectBaseClass);
        }
        String dialectClassStr = dialectClass;
        if (!PropertyCheck.isValidPropertyString(dialectClass))
        {
            String prop = PropertyCheck.getPropertyName(dialectClass);
            dialectClassStr = System.getProperty(prop, dialectClass);
        }

        Class dialectBaseClazz;
        try
        {
            dialectBaseClazz = Class.forName(dialectBaseClassStr);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Dialect base class not found: " + dialectBaseClassStr);
        }
        Class dialectClazz;
        try
        {
            dialectClazz = Class.forName(dialectClassStr);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException("Dialect class not found: " + dialectClassStr);
        }
        // Ensure that we are dealing with classes and not interfaces
        if (!Object.class.isAssignableFrom(dialectBaseClazz))
        {
            throw new RuntimeException(
                    "Dialect base class must be derived from java.lang.Object: " +
                    dialectBaseClazz.getName());
        }
        if (!Object.class.isAssignableFrom(dialectClazz))
        {
            throw new RuntimeException(
                    "Dialect class must be derived from java.lang.Object: " +
                    dialectClazz.getName());
        }
        // We expect these to be in the same hierarchy
        if (!dialectBaseClazz.isAssignableFrom(dialectClazz))
        {
            throw new RuntimeException(
                    "Non-existent HierarchicalResourceLoader hierarchy: " +
                    dialectBaseClazz.getName() + " is not a superclass of " + dialectClazz);
        }
        
        Class<? extends Object> clazz = dialectClazz;
        Resource resource = null;
        while (resource == null)
        {
            // Do replacement
            String newLocation = location.replaceAll(DEFAULT_DIALECT_REGEX, clazz.getName());
            resource = super.getResource(newLocation);
            if (resource != null && resource.exists())
            {
                // Found
                break;
            }
            // Not found
            resource = null;
            // Are we at the base class?
            if (clazz.equals(dialectBaseClazz))
            {
                // We don't go any further
                break;
            }
            // Move up the hierarchy
            clazz = clazz.getSuperclass();
            if (clazz == null)
            {
                throw new RuntimeException(
                        "Non-existent HierarchicalResourceLoaderBean hierarchy: " +
                        dialectBaseClazz.getName() + " is not a superclass of " + dialectClazz);
            }
        }
        //System.out.println("HierarchyResourceLoader returning: " + resource.toString());
        setResourcePath(resource.toString());
		
        return resource;
    }
    
    private void setResourcePath(String path){
    	this.resourcePath = path;
    }
    public String getResourcePath(){
    	return this.resourcePath;
    }
}