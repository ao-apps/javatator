package com.javaphilia.javatator;

/**
 * Javatator - multi-database admin tool
 * 
 * Copyright (C) 2001  Jason Davies.
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
 * 
 * If you want to help or want to report any bugs, please email me:
 * jason@javaphilia.com
 * 
 */

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * The configuration for the database is stored in a properties file
 *
 * @author Jason Davies
 */
final public class DatabaseConfiguration {

	/**
	 * The properties are kept here.
	 */
	private static Properties props;

	/**
	 * Make no instances.
	 */
	private DatabaseConfiguration() {}

	/**
	 * Gets the list of hosts that may be accessed for the specified dbProduct.
	 */
	public static List<String> getAllowedHosts(String dbProduct) throws IOException {
		String hostList=getProperty("hostname", dbProduct);
		if(hostList==null) return Collections.emptyList();
		return splitStringCommaSpace(hostList);
	}

	/**
	 * Gets the list of databases that may be selected.
	 */
	public static List<String> getAvailableDatabaseProducts() throws IOException {
		List<String> products=new ArrayList<String>();
		String dbproduct=getProperty("dbproduct");
		if(dbproduct!=null && dbproduct.length()>0) products.add(dbproduct);
		else {
			Enumeration E=props.propertyNames();
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
	public static String getProperty(String name) throws IOException {
		loadIfNeeded();

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
	public static String getProperty(String name, String databaseProduct) throws IOException {
		loadIfNeeded();

		// Look for db.dbproduct.name
		String S=props.getProperty("db."+databaseProduct+'.'+name);
		if(S!=null) return S;

		// Look for db.*.name
		return props.getProperty("db.*."+name);
	}

	public static Boolean getBooleanProperty(String name, String databaseProduct) throws IOException {
		String S = getProperty(name, databaseProduct);
		if(S == null || S.isEmpty()) return null;
		if("true".equalsIgnoreCase(S)) return true;
		if("false".equalsIgnoreCase(S)) return false;
		throw new IOException("Unable to parse boolean: " + S);
	}

	/**
	 * Loads the properties if not already loaded.
	 */
	private static void loadIfNeeded() throws IOException {
		synchronized(DatabaseConfiguration.class) {
			if(props==null) {
				InputStream in=DatabaseConfiguration.class.getResourceAsStream("database.properties");
						Properties newProps=new Properties();
						try {
							newProps.load(in);
						} finally {
							in.close();
						}
				props=newProps;
			}
		}
	}

	/**
	 * Splits a string into multiple words on either whitespace or commas
	 * @return java.lang.String[]
	 * @param line java.lang.String
	 */
	public static List<String> splitStringCommaSpace(String line) {
		List<String> words=new ArrayList<String>();
		int len=line.length();
		int pos=0;
		while(pos<len) {
			// Skip past blank space
			char ch;
			while(pos<len && ((ch=line.charAt(pos))<=' ' || ch==',')) pos++;
			int start=pos;
			// Skip to the next blank space
			while(pos<len && (ch=line.charAt(pos))>' ' && ch!=',') pos++;
			if(pos>start) words.add(line.substring(start,pos));
		}
		return words;
	}
}
