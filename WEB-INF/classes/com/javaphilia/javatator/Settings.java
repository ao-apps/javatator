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
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.List;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;

/**
 * Wraps all settings of the Javatator tool.  The settings are immutable, and
 * are passed around as an object.  Every setting is written to every page
 * and the settings are changed by JavaScript on the client before the
 * form is resubmitted.
 */
public class Settings {

	private HttpServletRequest request;

	private String databaseProduct;
	private String hostname;
	private int port=-1;
	private String username;
	private String password;
	private String database;
	private String table;
	private String column;
	private String action;
	private String sortColumn;
	private String sortOrder;
	private int numrows=30;
	private int fkeyrows=100;
	private boolean useMultiLine=true;

	private String error;

/**
 * Constructs this <code>Settings</code> object by pulling its values from
 * a <code>HttpServletRequest</code>.  If any value is provided in the
 * configuration, then the value in the form is ignored.
 */
public Settings(HttpServletRequest request) throws IOException {
    this.request = request;

    // These values may by set in the configuration
    databaseProduct = getSetting(request, "dbproduct");
    hostname = getClientSetting(request, "hostname");

    List<String> allowedHosts = DatabaseConfiguration.getAllowedHosts(databaseProduct);
    int allowedLen = allowedHosts.size();
    if (allowedLen == 1)
        hostname = allowedHosts.get(0);

    // Make sure hostname is a valid format
    int len = hostname.length();
    for (int c = 0; c < len; c++) {
        char ch = hostname.charAt(c);
        if ((ch < 'a' || ch > 'z') && (ch < 'A' || ch > 'Z') && (ch < '0' || ch > '9') && ch != '-' && ch != '.') {
            error = "Invalid character in hostname: " + ch;
            hostname = "";
            break;
        }
    }

    // Make sure is part of allowed hostnames
    if (hostname.length() > 0) {
	    if (allowedLen == 0) {
            // Remove hostname if denied in config
            String S = DatabaseConfiguration.getProperty("host.deny", databaseProduct);
            if (S != null && (S = S.trim()).length() > 0) {
                StringTokenizer hosts = new StringTokenizer(S, ",");
                while (hosts.hasMoreTokens()) {
                    String host = hosts.nextToken().trim().toLowerCase();
                    if (host.length() > 0) {
                        InetAddress IA1 = InetAddress.getByName(host);
                        InetAddress IA2 = InetAddress.getByName(hostname);
                        if (IA1 == null ? (IA2 == null) : (IA1.equals(IA2))) {
                            error = "Access to " + hostname + " is denied.";
                            hostname = "";
                        }
                    }
                }
            }
	    } else if(allowedLen>1) {
            boolean found = false;
            for (int c = 0; c < allowedLen; c++) {
                String host = allowedHosts.get(c);
                if (host.equals(hostname)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                error = "Access to " + hostname + " is not allowed.";
                hostname = "";
            }
        }
    }
    port = getIntSetting(request, databaseProduct, "port");
    username = getSetting(request, databaseProduct, "username");
    password = getSetting(request, databaseProduct, "password");
    database = getSetting(request, databaseProduct, "database");

    // These values are always obtained from the client
    table = getClientSetting(request, "table");
    column = getClientSetting(request, "column");
    action = getClientSetting(request, "action");
    sortColumn = getClientSetting(request, "sortcolumn");
    sortOrder = getClientSetting(request, "sortorder");
    String S = request.getParameter("numrows");
    if (S != null && S.length() > 0)
        numrows = Integer.parseInt(S);
    S = request.getParameter("fkeyrows");
    if (S != null && S.length() > 0)
        fkeyrows = Integer.parseInt(S);
    useMultiLine = "true".equals(request.getParameter("usemultiline"));
}
	private Settings(
		     HttpServletRequest request,
		     String databaseProduct,
		     String hostname,
		     int port,
		     String username,
		     String password,
		     String database,
		     String table,
		     String column,
		     String action,
		     String sortColumn,
		     String sortOrder,
		     int numrows,
		     int fkeyrows,
		     boolean useMultiLine
		     ) {
	this.request=request;
	this.databaseProduct=databaseProduct;
	this.hostname=hostname;
	this.port=port;
	this.username=username;
	this.password=password;
	this.database=database;
	this.table=table;
	this.column=column;
	this.action=action;
	this.sortColumn=sortColumn;
	this.sortOrder=sortOrder;
	this.numrows=numrows;
	this.fkeyrows=fkeyrows;
	this.useMultiLine=useMultiLine;
	}
	/**
	 * Gets the action currently being performed or <code>null</code> if not set.
	 */
	public String getAction() {
	return action.length()==0?null:action;
	}
	/**
	 * Gets a setting without checking the config.
	 */
	private static String getClientSetting(HttpServletRequest req, String name) {
	String S=req.getParameter(name);
	return (S==null)?"":S;
	}
	/**
	 * Gets the column currently being accessed or <code>null</code> if not set.
	 */
	public String getColumn() {
	return column.length()==0?null:column;
	}
	/**
	 * Gets the database currently being accessed or <code>null</code> if not set.
	 */
	public String getDatabase() {
	return database.length()==0?null:database;
	}
	/**
	 * Gets the database product in use or <code>null</code> if not set.
	 */
	public String getDatabaseProduct() {
	return databaseProduct.length()==0?null:databaseProduct;
	}
	/**
	 * Gets any error that occured during the creation of this <code>Settings</code> object.
	 *
	 * @return  a description of the error or <code>null</code> for none.
	 */
	public String getError() {
	return error;
	}
	/**
	 * Gets the number of rows for a foreign key choice.
	 */
	public int getForeignKeyRows() {
	return fkeyrows;
	}
	/**
	 * Gets the hostname of the database server being connected to
	 * or <code>null</code> if not set.
	 */
	public String getHostname() {
	return hostname.length()==0?null:hostname;
	}
	/**
	 * Gets a numerical setting from a <code>HttpServletRequest</code>.
	 * If the setting is provided in the configuration for the database
	 * product, then the configuration value is used.
	 */
	private static int getIntSetting(HttpServletRequest req, String databaseProduct, String name) throws IOException {
	// The configuration overrides the client values
	String config=DatabaseConfiguration.getProperty(name, databaseProduct);
	if(config!=null && config.length()>0) return Integer.parseInt(config);

	String S=req.getParameter(name);
	return (S==null)?-1:Integer.parseInt(S);
	}
    /**
     * Gets a <code>JDBCConnector</code> for these <code>Settings</code>.
     */
    public JDBCConnector getJDBCConnector() throws IOException {
	try {
	    return JDBCConnector.getInstance(this);
	} catch(ClassNotFoundException err) {
            IOException ioErr=new IOException();
            ioErr.initCause(err);
            throw ioErr;
	} catch(NoSuchMethodException err) {
            IOException ioErr=new IOException();
            ioErr.initCause(err);
            throw ioErr;
	} catch(InstantiationException err) {
            IOException ioErr=new IOException();
            ioErr.initCause(err);
            throw ioErr;
	} catch(IllegalAccessException err) {
            IOException ioErr=new IOException();
            ioErr.initCause(err);
            throw ioErr;
	} catch(InvocationTargetException err) {
            IOException ioErr=new IOException();
            ioErr.initCause(err);
            throw ioErr;
	}
    }

    /**
     * Gets the number of rows to display at once.
     */
    public int getNumRows() {
	return numrows;
    }

    /**
     * Gets an arbitrary value from the request.  The is the value
     * as provided by the client.  Any overridden configuration
     * values are not returned.
     */
    public String getParameter(String name) {
	return request.getParameter(name);
    }

    /**
     * Gets an arbitrary set of values from the request.  The is the
     * value as provided by the client.  Any overridden configuration
     * values are not returned.
     */
    public String[] getParameterValues(String name) {
	return request.getParameterValues(name);
    }

    /**
     * Gets the password or <code>null</code> if not set.
     */
    public String getPassword() {
	return password.length()==0?null:password;
    }

    /**
     * Gets the port of the database product being connected to.
     *
     * @return  <code>-1</code> if not defined or the port number
     */
    public int getPort() {
	return port;
    }

    /**
     * Gets a setting from a <code>HttpServletRequest</code>.  If the
     * setting is provided in the configuration, then the configuration
     * value is used.
     */
    private static String getSetting(HttpServletRequest req, String name) throws IOException {
	// The configuration overrides the client values
	String config=DatabaseConfiguration.getProperty(name);
	if(config!=null && config.length()>0) return config;

	String S=req.getParameter(name);
	return (S==null)?"":S;
    }
	/**
	 * Gets a setting from a <code>HttpServletRequest</code>.  If the
	 * setting is provided in the configuration for the database
	 * product, then the configuration value is used.
	 */
	private static String getSetting(HttpServletRequest req, String databaseProduct, String name) throws IOException {
	// The configuration overrides the client values
	String config=DatabaseConfiguration.getProperty(name, databaseProduct);
	if(config!=null && config.length()>0) return config;

	String S=req.getParameter(name);
	return (S==null)?"":S;
	}
	/**
	 * Gets the column name to be ordered by or <code>null</code> if not set.
	 */
	public String getSortColumn() {
	return sortColumn.length()==0?null:sortColumn;
	}
	/**
	 * Gets the sorting order or <code>null</code> if not set.
	 */
	public String getSortOrder() {
	return sortOrder.length()==0?null:sortOrder;
	}
	/**
	 * Gets the table currently being accessed or <code>null</code> if not set.
	 */
	public String getTable() {
	return table.length()==0?null:table;
	}
	/**
	 * Generates the proper URL based on the settings and configuration.
	 */
	public String getURL() throws IOException {
	String url=DatabaseConfiguration.getProperty("url", databaseProduct);
	if(url.length()==0) throw new IOException("Unable to find URL for databaseProduct="+databaseProduct);

	// Replace all %h with the host
	if(hostname.length()==0) throw new IOException("hostname not set");
	if(hostname.indexOf("%h")>=0) throw new IOException("hostname may not contain %h: "+hostname);
	int pos;
	while((pos=url.indexOf("%h"))>=0) url=url.substring(0, pos)+hostname+url.substring(pos+2);

	// Replace the %p with the port
	if(port<1 || port>65535) throw new IOException("Invalid port: "+port);
	while((pos=url.indexOf("%p"))>=0) url=url.substring(0, pos)+port+url.substring(pos+2);

	// Replace the %d with the database
	if(database.length()==0) throw new IOException("database not set");
	if(database.indexOf("%d")>=0) throw new IOException("database may not contain %d: "+database);
	while((pos=url.indexOf("%d"))>=0) url=url.substring(0, pos)+database+url.substring(pos+2);

	return url;
	}
	/**
	 * Gets the username or <code>null</code> if not set.
	 */
	public String getUsername() {
	return username.length()==0?null:username;
	}
	/**
	 * Prints the contents of this <code>Settings</code> to a
	 * <code>JavatatorWriter</code>.
	 */
	public void printForm(JavatatorWriter out) throws IOException {
	printHiddenField(out, "dbproduct", databaseProduct);
	printHiddenField(out, "hostname", hostname);
	printHiddenField(out, "port", port);
	printHiddenField(out, "username", username);

	// Only provide the password when not in the config
	String config=DatabaseConfiguration.getProperty("password", databaseProduct);
	if(config==null || config.length()==0) printHiddenField(out, "password", password);
	else printHiddenField(out, "password", "XXXXXXXX");

	printHiddenField(out, "database", database);
	printHiddenField(out, "table", table);
	printHiddenField(out, "column", column);
	printHiddenField(out, "action", action);

	printGlobalForm(out);
	}
	/**
	 * Prints the global preferences of this <code>Settings</code> to a
	 * <code>JavatatorWriter</code>.
	 */
	public void printGlobalForm(JavatatorWriter out) throws IOException {
	printHiddenField(out, "sortcolumn", sortColumn);
	printHiddenField(out, "sortorder", sortOrder);
	printHiddenField(out, "numrows", numrows);
	printHiddenField(out, "fkeyrows", fkeyrows);
	printHiddenField(out, "usemultiline", useMultiLine?"true":"false");
	}
	/**
	 * Prints a hidden field.
	 */
	private static void printHiddenField(JavatatorWriter out, String name, int value) {
	out.print("<INPUT type='hidden' name='");
	out.print(name);
	out.print("' value='");
	out.print(value);
	out.print("'>\n");
	}
	/**
	 * Prints a hidden field.
	 */
	private static void printHiddenField(JavatatorWriter out, String name, String value) {
	out.print("<INPUT type='hidden' name='");
	out.print(name);
	out.print("' value='");
	out.print(value);
	out.print("'>\n");
	}
	private void printParam(JavatatorWriter out, String name, int value) throws IOException {
	out.print(name);
	out.print('=');
	out.print(value);
	}
	private void printParam(JavatatorWriter out, String name, String value) throws IOException {
	out.print(name);
	out.print('=');
	Util.printEscapedURLValue(out, value);
	}
	/**
	 * Prints a URL with the <code>Settings</code> embedded in the URL string.
	 */
	public void printURLParams(String url, JavatatorWriter out) throws IOException {
	out.print(url);
	out.print('?');
		printParam(out, "dbproduct", databaseProduct);
	out.print('&');
		printParam(out, "hostname", hostname);
	out.print('&');
		printParam(out, "port", port);
	out.print('&');
		printParam(out, "username", username);

		// Only provide the password when not in the config
	out.print('&');
		String config=DatabaseConfiguration.getProperty("password", databaseProduct);
		if(config==null || config.length()==0) printParam(out, "password", password);
	else printParam(out, "password", "XXXXXXXX");

	out.print('&');
		printParam(out, "database", database);
	out.print('&');
		printParam(out, "table", table);
	out.print('&');
		printParam(out, "column", column);
	out.print('&');
		printParam(out, "action", action);

	out.print('&');
		printParam(out, "sortcolumn", sortColumn);
	out.print('&');
		printParam(out, "sortorder", sortOrder);
	out.print('&');
		printParam(out, "numrows", numrows);
	out.print('&');
		printParam(out, "fkeyrows", fkeyrows);
	out.print('&');
		printParam(out, "usemultiline", useMultiLine?"true":"false");
	}
	/**
	 * Gets a <code>Settings</code> for access to a new database.
	 */
	public Settings setDatabase(String database) {
	return new Settings(
			    request,
			    databaseProduct,
			    hostname,
			    port,
			    username,
			    password,
			    database,
			    table,
			    column,
			    action,
			    sortColumn,
			    sortOrder,
			    numrows,
			    fkeyrows,
			    useMultiLine
			    );
	}
	/**
	 * Gets a <code>Settings</code> for access to a new table.
	 */
	public Settings setTable(String table) {
	return new Settings(
			    request,
			    databaseProduct,
			    hostname,
			    port,
			    username,
			    password,
			    database,
			    table,
			    column,
			    action,
			    sortColumn,
			    sortOrder,
			    numrows,
			    fkeyrows,
			    useMultiLine
			    );
	}
	/**
	 * Should multiline textareas be used?
	 */
	public boolean useMultiLine() {
	return useMultiLine;
	}
}
