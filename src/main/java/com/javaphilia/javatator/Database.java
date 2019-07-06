/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2019  AO Industries, Inc.
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

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Various methods for manipulating databases.
 */
public class Database {

	/**
	 * The settings contain the name of the database to use.
	 */
	private final Settings settings;

	/**
	 * Instantiate a new {@link Database}.
	 *
	 * @param settings the {@link Settings} to use.
	 */
	public Database(Settings settings) {
		this.settings=settings;
	}

	/**
	 * Displays a screen asking the user if they want to drop this database.
	 */
	public Settings confirmDropDatabase(JavatatorWriter out) throws SQLException, IOException {
		out.print("<h2>Database ");
		out.print(settings.getDatabase());
		out.print("</h2>"
			+ "Are you <b>sure</b> you want to drop this database?"
			+ "<input type='submit' value='YES' onClick=\"return selectAction('dodrop_database')\"> "
			+ "<input type='submit' value='NO' onClick=\"javascript:history.go(-1);return false;\">");
		return settings;
	}

	/**
	 * Creates a new database.
	 */
	public Settings createDatabase(JavatatorWriter out) throws SQLException, IOException {
		String db=settings.getParameter("createdb");
		out.print("<h2>Creating database ");
		out.print(db);
		out.print("</h2>");
		settings.getJDBCConnector().createDatabase(db);
		out.print("Database created successfully.\n"
			+ "<script language=javascript><!--\n"
			+ "top.top_frame.reloadMenu();\n"
			+ "//--></script>\n");
		return new Database(settings.setDatabase(db)).printDatabaseDetails(out);
	}

	/**
	 * Executes some user-specified SQL on the current database.
	 */
	public Settings doSQL(JavatatorWriter out,
		String sql,
		int startPos,
		int numrows
	) throws SQLException, IOException {
		JDBCConnector conn=settings.getJDBCConnector();
		boolean countRows=false;

		out.print("<h2>Database ");
		out.print(settings.getDatabase());
		out.print("</h2>"
			+ "Results of query: ");
		Util.printEscapedHTML(out, sql);
		out.print("<br><br>\n");
		int totalRows=conn.countRecords();
		printPreviousNext(out, startPos, numrows, totalRows, 1);

		int numberOfRows;

		out.startTable(null, "cellspacing=1");
		try {
			Connection dbconn=DatabasePool.getConnection(settings);
			try {
				try (
					Statement stmt = dbconn.createStatement();
					ResultSet results = stmt.executeQuery(sql)
				) {
					ResultSetMetaData resultMetaData = results.getMetaData();
					int numberOfColumns=resultMetaData.getColumnCount();
					out.startTR();
					if(numberOfColumns>0) {
						for(int i=1;i<=numberOfColumns;i++) {
							String col=resultMetaData.getColumnName(i);
							String order="asc";
							if(col.equals(settings.getSortColumn()) && "asc".equals(settings.getSortOrder())) order="desc";
							out.printTH("<A href=\"javascript:setSortColumn('"+col+"');"
								+ "setSortOrder('"+order+"');"
									+ "selectAction('dosql');"
									+ "\">"
								+ Util.escapeHTML(col)
								+ "</A>");
						}
						out.printTH("Options");
						out.endTR();
						for(numberOfRows=0;results.next();numberOfRows++) {
							if(countRows || (numberOfRows>=startPos && numberOfRows<startPos+numrows)) {
								out.startTR();
								for(int i=1;i<=numberOfColumns;i++) {
									String value=results.getString(i);
									out.printTD(
										(value==null)?""
											: (value.length()==0)?"&nbsp;"
												: Util.escapeHTML(value));
								}
								out.endTR();
							}
						}
					} else {
						out.printTH("Query executed successfully. No data returned.");
						out.endTR();
					}
				}
			} finally {
				DatabasePool.releaseConnection(dbconn);
			}
		} finally {
			out.endTable();
		}

		out.print("<br><br>\n");
		printPreviousNext(out, startPos, numrows, totalRows, 1);
		return settings;
	}

	/**
	 * Drops the current database.
	 */
	public Settings dropDatabase(JavatatorWriter out) throws SQLException, IOException {
		out.print("<h2>Dropping database ");
		out.print(settings.getDatabase());
		out.print("</h2>");
		settings.getJDBCConnector().dropDatabase();
		out.print("Database dropped successfully.\n"
			+ "<script language=javascript><!--\n"
			+ "var t=top.top_frame;\n"
			+ "t.deleteDatabase('");
		out.print(settings.getDatabase());
		out.print("');\n"
			+ "t.setParentDB(-1);\n"
			+ "t.drawMenu(top.left_frame.window.document);\n"
			+ "//--></script>\n");
		return new Database(settings.setDatabase("")).printDefaultPage(out);
	}

	/**
	 * Prints the properties for the specified database.
	 */
	public Settings printDatabaseDetails(JavatatorWriter out) throws SQLException, IOException {
		out.print("<h2>Database ");
		out.print(settings.getDatabase());
		out.print("</h2>\n");

		out.startTable(null, "cellspacing=1");
		try {
			out.startTR();
			out.printTH("Table");
			out.printTH("Options");
			out.printTH("Records");
			out.endTR();

			JDBCConnector conn=settings.getJDBCConnector();
			List<String> V=conn.getTables();
			int size=V.size();
			for(int i=0;i<size;i++) {
				String table = V.get(i);
				out.startTR();

				out.printTD(table);

				out.startTD();
				out.print("<a href=\"javascript:selectTable('");
				out.print(table);
				out.print("','doselect');\">Explore</a> | "
					+ "<a href=\"javascript:selectTable('");
				out.print(table);
				out.print("','properties');\">Properties</a> | "
					+ "<a href=\"javascript:selectTable('");
				out.print(table);
				out.print("','select');\">Select</a> | "
					+ "<a href=\"javascript:selectTable('");
				out.print(table);
				out.print("','insert');\">Insert</a> | "
					+ "<a href=\"javascript:selectTable('");
				out.print(table);
				out.print("','delete_table');\">Drop</a> | "
					+ "<a href=\"javascript:selectTable('");
				out.print(table);
				out.print("','empty_table');\">Empty</a> | "
					+ "<a href=\"javascript:selectTable('");
				out.print(table);
				out.print("','table_privileges');\">Privileges</a>");
				out.endTD();

				out.printTD(settings.setTable(table).getJDBCConnector().countRecords());

				out.endTR();
			}
		} finally {
			out.endTable();
		}
		out.print("<br>\n"
			+ "<a href=\"javascript:selectAction('view_schema');\">View schema</a>"
			+ "&nbsp;|&nbsp;<a href=\"javascript:selectAction('drop_database');\">Drop this database</a>"
			+ "<br><br>\n"
			+ "<b>SQL Query/Queries to execute on database ");
		out.print(settings.getDatabase());
		out.print(":</b><br>"
			+ "<textarea cols=80 rows=25 name=sql></textarea><br>"
			+ "<input type='submit' value='Go!' onClick=\"return selectAction('dosql')\">"
			+ "<br><br>"
			+ "<b>Create a new table on database ");
		out.print(settings.getDatabase());
		out.print(":</b>"
			+ "<br>\n"
			+ "Name: <input type='text' name='newtable'><br>\n"
			+ "Columns: <input type='text' size=5 name='numcolumns'> "
			+ "<input type='submit' value='Go!' onClick=\"javascript:selectTable(this.form.newtable.value,'create_table')\">"
			+ "<br>\n");
		return settings;
	}

	/**
	 * Print a default page for this database product.
	 */
	public Settings printDefaultPage(JavatatorWriter out) {
		out.print("<h2>Welcome to Javatator ");
		out.print(Maven.properties.get("project.version"));
		out.print("</h2>"
			+ "This database admin tool is currently under construction. "
			+ "Suggestions/bug reports would be greatly appreciated.<br><br>\n"
			+ "<b>Problems I'm having at the moment:</b>"
			+ "<ul>\n"
			+ "<li>Just need to fix any remaining bugs :).\n"
			+ "</ul>\n"
			+ "<br><br>\n"
			+ "<b>Create new database:</b><br>\n"
			+ "<input type='text' name='createdb'> "
			+ "<input type='submit' value='Go!' "
			+ "onClick=\"return selectAction('create_database');\">");
		return settings;
	}

	private void printPreviousNext(JavatatorWriter out, int startPos, int numrows, int totalRows, int which) {
		if(startPos>0) {
			out.print("<b>Previous:</b> <input type='text' size=4 name='pnewnumrows");
			out.print(which);
			out.print("' value='");
			out.print(Math.min(numrows, startPos));
			out.print("'> rows "
				+ "<input type='submit' value='Go!' onClick=\"setNumRows(this.form.pnewnumrows");
			out.print(which);
			out.print(".value);"
				+ " setStartPos(");
			out.print(startPos);
			out.print("-this.form.pnewnumrows");
			out.print(which);
			out.print(".value);"
				+ " return selectAction('dosql')\">&nbsp;&nbsp;&nbsp;");
		}

		// Only show the remaining if there are some that are not visible
		int remaining=totalRows-startPos-numrows;
		out.print("<b>Next:</b> <input type='text' name='newnumrows");
		out.print(which);
		out.print("' size=4 value='");
		out.print(numrows);
		out.print("'> rows starting at: "
			+ "<input type='text' size=4 name='startpos");
		out.print(which);
		out.print("' value='");
		out.print(startPos+numrows);
		out.print("'> <input type='submit' value='Go!' onClick=\"setNumRows(this.form.newnumrows");
		out.print(which);
		out.print(".value); setStartPos(this.form.startpos");
		out.print(which);
		out.print(".value); return selectAction('dosql');\">");
	}

	/**
	 * Process the {@link Settings} object and decide what to do.
	 */
	public Settings processRequest(JavatatorWriter out) throws SQLException, IOException {
		String action=settings.getAction();
		int startPos=0;
		if(settings.getParameter("startpos")!=null) startPos=Integer.parseInt(settings.getParameter("startpos"));

		if("db_details".equals(action)) return printDatabaseDetails(out);
		else if("create_database".equals(action)) return createDatabase(out);
		else if("view_schema".equals(action)) return viewSchema(out);
		else if("drop_database".equals(action)) return confirmDropDatabase(out);
		else if("dodrop_database".equals(action)) return dropDatabase(out);
		else if("dosql".equals(action)) return doSQL(out, settings.getParameter("sql"), startPos, settings.getNumRows());
		else if(settings.getTable()!=null) return new Table(settings).processRequest(out);
		else if(action==null) return printDefaultPage(out);
		else {
			out.print("Database: Unknown value of action: ");
			out.print(action);
			return settings;
		}
	}

	/**
	 * Views the schema for the database by generating a GIF file.
	 */
	public Settings viewSchema(JavatatorWriter out) throws SQLException, IOException {
		out.print("<h2>Database ");
		out.print(settings.getDatabase());
		out.print("</h2>\n"
			+"<img src='");
		settings.printURLParams(SchemaImage.class.getName(), out);
		out.print("'>\n"
			+ "<br><br>\n"
			+ "<a href=\"javascript:selectAction('db_details');\">View properties</a>");
		return settings;
	}
}
