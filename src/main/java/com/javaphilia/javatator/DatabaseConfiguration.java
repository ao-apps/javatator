/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2018, 2019, 2020  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.javaphilia.javatator;

import com.aoindustries.util.StringUtility;
import com.aoindustries.util.WrappedException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * The configuration for the database is stored in a properties file
 */
public class DatabaseConfiguration {

	private static final String INIT_PARAM = DatabaseConfiguration.class.getName();

	private static final String APPLICATION_ATTRIBUTE = DatabaseConfiguration.class.getName();

	@WebListener
	public static class Initializer implements ServletContextListener {
		@Override
		public void contextInitialized(ServletContextEvent event) {
			getInstance(event.getServletContext());
		}
		@Override
		public void contextDestroyed(ServletContextEvent event) {
			// Do nothing
		}
	}

	/**
	 * Gets the database configuration for the current application.
	 */
	public static DatabaseConfiguration getInstance(ServletContext servletContext) {
		DatabaseConfiguration instance = (DatabaseConfiguration)servletContext.getAttribute(APPLICATION_ATTRIBUTE);
		if(instance == null) {
			try {
				String filename = StringUtility.trimNullIfEmpty(servletContext.getInitParameter(INIT_PARAM));
				servletContext.log(DatabaseConfiguration.class.getName() + ": " + INIT_PARAM + '=' + filename);
				if(filename != null) {
					instance = new DatabaseConfiguration(new File(filename));
				} else {
					instance = new DatabaseConfiguration();
				}
				servletContext.setAttribute(APPLICATION_ATTRIBUTE, instance);
			} catch(IOException e) {
				throw new WrappedException(e);
			}
		}
		return instance;
	}

	/**
	 * The properties are kept here.
	 */
	private final Properties props;

	/**
	 * Loads the default configuration from the bundled database.properties file.
	 */
	private DatabaseConfiguration() throws IOException {
		Properties newProps = new Properties();
		try (InputStream in = DatabaseConfiguration.class.getResourceAsStream("database.properties")) {
			if(in == null) throw new IOException("database.properties not found as resource");
			newProps.load(in);
		}
		props = newProps;
	}

	/**
	 * Loads the default configuration from a provided file.
	 */
	private DatabaseConfiguration(File file) throws IOException {
		Properties newProps = new Properties();
		try (InputStream in = new FileInputStream(file)) {
			newProps.load(in);
		}
		props = newProps;
	}

	/**
	 * Gets the list of hosts that may be accessed for the specified dbProduct.
	 */
	public List<String> getAllowedHosts(String dbProduct) {
		String hostList=getProperty("hostname", dbProduct);
		if(hostList==null) return Collections.emptyList();
		return StringUtility.splitStringCommaSpace(hostList);
	}

	/**
	 * Gets the list of databases that may be selected.
	 */
	public List<String> getAvailableDatabaseProducts() {
		List<String> products=new ArrayList<>();
		String dbproduct=getProperty("dbproduct");
		if(dbproduct!=null && dbproduct.length()>0) products.add(dbproduct);
		else {
			Enumeration<?> E=props.propertyNames();
			while(E.hasMoreElements()) {
				String tmp=(String)E.nextElement();
				if(tmp.startsWith("db.") && tmp.endsWith(".name")) products.add(tmp.substring(3, tmp.length()-5));
			}
		}
		return products;
	}

	/**
	 * Gets the specified property from the file.  If <code>db.name</code> exists, returns the value.
	 */
	public String getProperty(String name) {
		// Look for db.name
		return props.getProperty("db."+name);
	}

	/**
	 * Gets the specified property from the properties file, using the database product string.
	 * If <code>db.databaseProduct.name</code> is defined in the properties file, then that is returned.
	 * If <code>db.*.name</code> is defined in the properties file, then that is returned.
	 *
	 * @param name the name of the property to get.
	 * @param databaseProduct the name of the database product being used.
	 */
	public String getProperty(String name, String databaseProduct) {
		// Look for db.dbproduct.name
		String S=props.getProperty("db."+databaseProduct+'.'+name);
		if(S!=null) return S;

		// Look for db.*.name
		return props.getProperty("db.*."+name);
	}

	public Boolean getBooleanProperty(String name, String databaseProduct) {
		String S = getProperty(name, databaseProduct);
		if(S == null || S.isEmpty()) return null;
		if("true".equalsIgnoreCase(S)) return true;
		if("false".equalsIgnoreCase(S)) return false;
		throw new IllegalArgumentException("Unable to parse boolean: " + S);
	}
}
