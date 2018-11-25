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
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Contains all the default JDBC methods to connect to a database.
 * Override methods as needed if the driver has not implemented everything.
 *
 * @author Jason Davies
 * @version 0.2
 */
public class JDBCConnector {

	public enum Boolean {
		/**
		 * Expanded boolean <code>FALSE</code> is returned when something is known to be false.
		 */
		FALSE,

		/**
		 * Expanded boolean <code>TRUE</code> is returned when something is known to be true.
		 */
		TRUE,

		/**
		 * Expanded boolean <code>UNKNOWN</code> is returned when something is not known to be true or false.
		 */
		UNKNOWN,

		/**
		 * Expanded boolean <code>NA</code> is returned when something is known to not apply to this database.
		 */
		NA
	}

	protected static final String[] defaultTableTypes=new String[]{"TABLE"};

	/**
	 * The parameters passed to the JDBCConnector constructor.
	 */
	private final static Class[] paramTypes={Settings.class};

	/**
	 * The <code>Settings</code> store all the configuration parameters.
	 */
	final protected Settings settings;

	/**
	 * Instantiate a new JDBCConnector.
	 *
	 * @param settings  the <code>Settings</code> to use.
	 */
	public JDBCConnector(Settings settings) {
		this.settings=settings;
	}

	/**
	 * Adds a new CHECK constraint to this table.
	 */
	public void addCheckConstraint(String constraint, String checkClause) throws SQLException, IOException {
		executeUpdate("ALTER TABLE " + quoteTable(settings.getTable()) + " ADD CONSTRAINT " + constraint + " CHECK(" + checkClause + ')');
	}

	/**
	 * Adds a new column to this table.
	 *
	 * @param name the name of the column.
	 * @param type the type of the column.
	 * @param length the length/set of the column.
	 * @param dfault the default value for the column.
	 * @param nullClause can be "null" or "not null".
	 * @param remarks at the moment this is only used for the "auto_increment" value in MySQL.
	 */
	public void addColumn(
		String name,
		String type,
		String length,
		String dfault,
		String nullClause,
		String remarks
	) throws SQLException, IOException {
		StringBuffer sql=new StringBuffer("ALTER TABLE ");
		sql
			.append(quoteTable(settings.getTable()))
			.append(" ADD ")
			.append(quoteColumn(name))
			.append(' ')
			.append(type)
			.append(' ')
			;
		boolean hasLength=!type.toUpperCase().endsWith("TEXT");
		if(hasLength && length!=null && length.length()>0)
			sql
			.append('(')
			.append(length)
			.append(')')
			;
		if(dfault!=null) {
			sql.append(" DEFAULT ");
			if(dfault.charAt(0)=='F') sql.append(dfault.substring(1));
			else sql.append(Util.escapeSQLValue(dfault.substring(1)));
		}
		sql
			.append(' ')
			.append(nullClause)
			.append(' ')
			.append(remarks)
			;
		executeUpdate(sql.toString());
	}

	/**
	 * Adds a new foreign key to this table.
	 *
	 * @param constraint the name of the constraint.
	 * @param primaryKey the name of the primary key.
	 */
	public void addForeignKey(
		String constraint,
		String primaryKey,
		String foreignTable,
		String foreignKey,
		String matchType,
		String onDelete,
		String onUpdate,
		boolean isDeferrable,
		String initially
	) throws SQLException, IOException {
		StringBuffer sql=new StringBuffer("ALTER TABLE ");
		sql
			.append(quoteTable(settings.getTable()))
			.append(" ADD CONSTRAINT ")
			.append(constraint)
			.append(" FOREIGN KEY(")
			.append(primaryKey)
			.append(") REFERENCES ")
			.append(foreignTable)
			.append(" (")
			.append(foreignKey)
			.append(")")
			;
		if(matchType!=null) sql.append(" MATCH ").append(matchType);
		sql
			.append(" ON DELETE ")
			.append(onDelete)
			.append(" ON UPDATE ")
			.append(onUpdate)
			.append(isDeferrable?" DEFERRABLE":" NOT DEFERRABLE")
			.append(" INITIALLY ")
			.append(initially)
			;
		executeUpdate(sql.toString());
	}

	/**
	 * Adds an index on the specified column.
	 *
	 * @param indexName the name of the index.
	 * @param column the name of the column.
	 */
	public void addIndex(String indexName, String column) throws SQLException, IOException {
		executeUpdate("ALTER TABLE " + quoteTable(settings.getTable()) + " ADD INDEX " + indexName + " (" + quoteColumn(column) + ')');
	}

	/**
	 * Adds a primary key to the specified table.
	 *
	 * @param column the name of the column to add as a primary key.
	 */
	public void addPrimaryKey(String column) throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			StringBuffer SB=new StringBuffer();
			ResultSet results=conn.getMetaData().getPrimaryKeys(null, null, settings.getTable());
			try {
				while(results.next()) {
					SB.append(results.getString(4));
					SB.append(", ");
				}
				SB.append(quoteColumn(column));
			} finally {
				results.close();
			}
			PreparedStatement pstmt=conn.prepareStatement("ALTER TABLE " + quoteTable(settings.getTable()) + " DROP PRIMARY KEY, ADD PRIMARY KEY(" + SB.toString() + ")");
			try {
				pstmt.executeUpdate();
			} finally {
				pstmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Adds a unique index on the specified column.
	 *
	 * @param indexName the name of the index.
	 * @param column the name of the column.
	 */
	public void addUniqueIndex(String indexName, String column) throws SQLException, IOException {
		executeUpdate("ALTER TABLE " + quoteTable(settings.getTable()) + " ADD UNIQUE " + indexName + " (" + quoteColumn(column) + ')');
	}

	protected void appendIsNull(StringBuffer SB, String column) {
		SB
			.append("ISNULL(")
			.append(quoteColumn(column))
			.append(')');
	}

	/**
	 * Counts the number of records in the table.
	 */
	public int countRecords() throws SQLException, IOException {
		String table=settings.getTable();
		if(table!=null) return getIntQuery("select count(*) from " + quoteTable(table));
		return -1;
	}

	/**
	 * Creates a new database.
	 *
	 * @param database the new name for the database.
	 */
	public void createDatabase(String database) throws SQLException, IOException {
		executeUpdate("CREATE DATABASE "+database);
	}

	/**
	 * Creates a new table in the current database.
	 *
	 * @param newColumn the names of the new columns to create
	 * @param newType the corresponding SQL types of the columns
	 * @param newLength the corresponding lengths/sets of the columns
	 * @param newDefault the corresponding default values of the columns
	 * @param newNull the corresponding null/not null properties of the columns
	 * @param newRemarks the corresponding remarks for the columns
	 * @param primaryKey should the corresponding columns be primary keys?
	 * @param indexKey should the corresponding columns be an index?
	 * @param uniqueKey should the corresponding columns be a unique index?
	 */
	public void createTable(String[] newColumn,
		String[] newType,
		String[] newLength,
		String[] newDefault,
		String[] newNull,
		String[] newRemarks,
		boolean[] primaryKey,
		boolean[] indexKey,
		boolean[] uniqueKey
	) throws SQLException, IOException {
		// Build the SQL first
		StringBuffer sql=new StringBuffer();
		sql.append("CREATE TABLE ").append(quoteTable(settings.getTable())).append(" (");
		for(int i=0;i<newColumn.length;i++) {
			if(i>0) sql.append(", ");
			sql.append(quoteColumn(newColumn[i])).append(' ').append(newType[i]);
			if(newLength[i].length()>0) sql.append('(').append(newLength[i]).append(')');
			if(newDefault[i].length()>0) sql.append(" DEFAULT ").append(Util.escapeSQLValue(newDefault[i]));
			sql.append(' ').append(newNull[i]);
			if(primaryKey[i]) sql.append(", PRIMARY KEY (").append(quoteColumn(newColumn[i])).append(')');
			if(indexKey[i]) sql.append(", INDEX (").append(quoteColumn(newColumn[i])).append(')');
			if(uniqueKey[i]) sql.append(", UNIQUE (").append(quoteColumn(newColumn[i])).append(')');
		}
		sql.append(" )");

		// Execute the update next
		executeUpdate(sql.toString());
	}

	/**
	 * Deletes the specified column.
	 *
	 * @param column the name of the column to delete.
	 */
	public void deleteColumn(String column) throws SQLException, IOException {
		executeUpdate("ALTER TABLE " + quoteTable(settings.getTable()) + " DROP " + quoteColumn(column));
	}

	/**
	 * Deletes a row.
	 *
	 * @param primaryKeys the names of the primary keys.
	 * @param primaryKeyValues the values of the primary keys.
	 */
	public void deleteRow(
		String[] primaryKeys,
		String[] primaryKeyValues
	) throws SQLException, IOException {
		StringBuffer SB = new StringBuffer("DELETE FROM ").append(quoteTable(settings.getTable()));
		for (int i = 0; i < primaryKeys.length; i++) {
			SB.append(i == 0 ? " WHERE " : " AND ");
			if (primaryKeyValues[i] == null) {
				appendIsNull(SB, primaryKeys[i]);
			} else {
				SB.append(primaryKeys[i]).append("=?");
			}
		}
		String sql = SB.toString();

		Connection conn = DatabasePool.getConnection(settings);
		try {
			PreparedStatement stmt = conn.prepareStatement(sql);
			try {
				stmt.setEscapeProcessing(false);
				int pos = 1;
				for (int i = 0; i < primaryKeyValues.length; i++) {
					if (primaryKeyValues[i] != null) {
						stmt.setString(pos++, primaryKeyValues[i]);
					}
				}
				stmt.executeUpdate();
			} finally {
				stmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Deletes a constraint.
	 *
	 * @param constraint the name of the constraint to delete.
	 * @param behaviour the behaviour when dropping.
	 */
	public void dropConstraint(String constraint, String behaviour) throws SQLException, IOException {
		executeUpdate("ALTER TABLE "
				  + quoteTable(settings.getTable())
				  + " DROP CONSTRAINT "
				  + constraint+' '+behaviour);
	}

	/**
	 * Drops the database.
	 */
	public void dropDatabase() throws SQLException, IOException {
		executeUpdate("DROP DATABASE "+settings.getDatabase());
	}

	/**
	 * Drops the specified index from the table.
	 *
	 * @param indexName the name of the index to drop.
	 */
	public void dropIndex(String indexName) throws SQLException, IOException {
		executeUpdate("ALTER TABLE " + quoteTable(settings.getTable()) + " DROP INDEX " + indexName);
	}

	/**
	 * Deletes a primary key entry.
	 *
	 * @param column drop the primary key for this column.
	 */
	public void dropPrimaryKey(String column) throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			StringBuffer SB=new StringBuffer();
			ResultSet results=conn.getMetaData().getPrimaryKeys(null, null, settings.getTable());
			try {
				while(results.next()) {
					if(!results.getString(4).equals(column)) {
						if(SB.length()>0)SB.append(", ");
						SB.append(results.getString(4));
					}
				}
			} finally {
				results.close();
			}
			StringBuffer sql=new StringBuffer("ALTER TABLE ").append(quoteTable(settings.getTable())).append(" DROP PRIMARY KEY");
			if(SB.length()>0) {
				sql.append(", ADD PRIMARY KEY(");
				sql.append(SB.toString());
				sql.append(")");
			}

			PreparedStatement pstmt=conn.prepareStatement(sql.toString());
			try {
				pstmt.executeUpdate();
			} finally {
				pstmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Drops the selected table from the database.
	 */
	public void dropTable() throws SQLException, IOException {
		executeUpdate("DROP TABLE " + quoteTable(settings.getTable()));
	}

	/**
	 * Dumps the contents of the table.
	 *
	 * @param out a <code>Writer</code> to dump the SQL to.
	 */
	public void dumpTableContents(Writer out) throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			Statement stmt=conn.createStatement();
			try {
				String table=settings.getTable();
				ResultSet R=stmt.executeQuery("SELECT * FROM " + quoteTable(table));
				try {
					int count=R.getMetaData().getColumnCount();
					while(R.next()) {
						out.write("INSERT INTO ");
						out.write(quoteTable(table));
						out.write(" VALUES (");
						boolean hasBeen=false;
						for(int i=1;i<=count;i++) {
							if(hasBeen) out.write(',');
							else hasBeen=true;
							Util.printEscapedSQLValue(out, R.getString(i));
						}
						out.write(");\n");
					}
				} finally {
					R.close();
				}
			} finally {
				stmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Dumps the structure of the table.
	 *
	 * @param out a <code>Writer</code> to dump the SQL to.
	 */
	public void dumpTableStructure(Writer out) throws SQLException, IOException {
		Columns columns=getColumns();
		List<String> names=columns.getNames();
		List<String> types=columns.getTypes();
		List<String> lengths=columns.getLengths();
		List<Boolean> areNullable=columns.areNullable();
		List<String> defaults=columns.getDefaults();
		out.write("CREATE TABLE ");
		out.write(quoteTable(settings.getTable()));
		out.write(" (");
		boolean hasBeen=false;
		int size=names.size();
		for(int i=0;i<size;i++) {
			if(hasBeen) out.write(", ");
			else hasBeen=true;
			out.write(quoteColumn(names.get(i)));
			out.write(' ');
			out.write(types.get(i));
			out.write(' ');
			if(areNullable.get(i)==Boolean.FALSE) out.write(" NOT NULL");
			else if(areNullable.get(i)==Boolean.TRUE) out.write(" NULL");

			if(defaults.get(i)!=null && defaults.get(i).length()>1) {
				out.write(" DEFAULT ");
				if(defaults.get(i).charAt(0)=='V') Util.printEscapedSQLValue(out, defaults.get(i).substring(1));
				else if(defaults.get(i).charAt(0)=='F') out.write(defaults.get(i).substring(1));
			}
			String remark=getRemark(names.get(i));
			if("auto_increment".equalsIgnoreCase(remark)) out.write(" AUTO_INCREMENT");
		}
		out.write(");");
		Indexes indexes=getIndexes();
		List<String> indexNames=indexes.getNames();
		List<Boolean> areUnique=indexes.areUnique();
		List<String> colNames=indexes.getColumns();
		size=indexNames.size();
		for(int i=0;i<size;i++) {
			// TODO: This seems incomplete
		}
	}

	/**
	 * Edits a column's attributes.
	 *
	 * @param column the name of the column to edit.
	 * @param newColumn the new name of the column.
	 * @param newType the new type of the column.
	 * @param newLength the new length/set of the column.
	 * @param newDefault the new default value for the column
	 *                   <ul>
	 *                     <code>null</code> - no default<br>
	 *                     <code>F*</code> - a function<br>
	 *                     <code>V*</code> - a value
	 *                   </ul>
	 * @param newNull "not null" or "null" - whether the column is nullable.
	 * @param newRemarks extra info added to the end of the statement.
	 */
	public void editColumn(
		String column,
		String newColumn,
		String newType,
		String newLength,
		String newDefault,
		String newNull,
		String newRemarks
	) throws SQLException, IOException {
		// Build the SQL before allocating the connection
		StringBuffer sql=new StringBuffer();
		sql
			.append("ALTER TABLE ")
			.append(quoteTable(settings.getTable()))
			.append(" CHANGE ")
			.append(quoteColumn(column))
			.append(' ')
			.append(quoteColumn(newColumn))
			.append(' ')
			.append(newType)
			;
		boolean hasLength=!newType.toUpperCase().endsWith("TEXT");
		if(hasLength) sql
				  .append('(')
				  .append(newLength)
				  .append(')')
				  ;
		if(newDefault!=null) {
			sql.append(" DEFAULT ");
			if(newDefault.charAt(0)=='F') sql.append(newDefault.substring(1));
			else sql.append(Util.escapeSQLValue(newDefault.substring(1)));
		}
		sql
			.append(' ')
			.append(newNull)
			.append(' ')
			.append(newRemarks)
			;
		executeUpdate(sql.toString());
	}

	/**
	 * Edits a row in a table.
	 *
	 * @param column the names of the columns.
	 * @param function the functions to use.
	 * @param value the new values to put in this row.
	 * @param primaryKeys the names of the primary keys.
	 * @param primaryKeyValues the values of the primary keys.
	 */
	public void editRow(String[] column,
		String[] function,
		String[] value,
		String[] primaryKeys,
		String[] primaryKeyValues
	) throws SQLException, IOException {
		// Build the SQL statement
		StringBuffer SB = new StringBuffer("UPDATE ").append(quoteTable(settings.getTable())).append(" SET ");
		for (int i = 0; i < column.length; i++) {
			if (i > 0) {
				SB.append(", ");
			}
			SB.append(quoteColumn(column[i])).append('=');
			if (function[i] != null && function[i].length() > 0) {
				SB.append(function[i]);
			} else {
				SB.append('?');
			}
		}
		for (int i = 0; i < primaryKeys.length; i++) {
			SB.append(i == 0 ? " WHERE " : " AND ");
			if (primaryKeyValues[i] == null) {
				appendIsNull(SB, primaryKeys[i]);
			} else {
				SB.append(quoteColumn(primaryKeys[i])).append("=?");
			}
		}
		String sql = SB.toString();
		Connection conn = DatabasePool.getConnection(settings);
		try {
			PreparedStatement stmt = conn.prepareStatement(sql);
			try {
				stmt.setEscapeProcessing(false);
				int pos = 1;
				for (int i = 0; i < column.length; i++) {
					if (function[i] == null || function[i].length() == 0) {
						stmt.setString(pos++, value[i]);
					}
				}
				for (int i = 0; i < primaryKeyValues.length; i++) {
					if (primaryKeyValues[i] != null) {
						stmt.setString(pos++, primaryKeyValues[i]);
					}
				}
				stmt.executeUpdate();
			} finally {
				stmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Deletes all the records in the specified table.
	 */
	public void emptyTable() throws SQLException, IOException {
		executeUpdate("DELETE FROM " + quoteTable(settings.getTable()));
	}

	/**
	 * Executes a query and returns a <code>List</code> of <code>String</code> at index 1.
	 */
	final protected List<String> executeListQuery(String sql) throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			Statement stmt=conn.createStatement();
			try {
				ResultSet results=stmt.executeQuery(sql);
				try {
					List<String> V=new ArrayList<String>();
					while(results.next()) V.add(results.getString(1));
					return V;
				} finally {
					results.close();
				}
			} finally {
				stmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Executes a query and returns a <code>Vector</code> of <code>String</code> at index 1.
	 *
	 * @param  sql    the SQL to execute
	 * @param  param  the parameter to the SQL
	 */
	final protected List<String> executeListQuery(String sql, String param) throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			PreparedStatement pstmt=conn.prepareStatement(sql);
			try {
				pstmt.setString(1, param);
				ResultSet results=pstmt.executeQuery();
				try {
					List<String> V=new ArrayList<String>();
					while(results.next()) V.add(results.getString(1));
					return V;
				} finally {
					results.close();
				}
			} finally {
				pstmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Executes an update using a <code>PreparedStatement</code>
	 *
	 * @param  sql    the SQL to execute
	 *
	 * @return  the number of rows updated
	 */
	final protected int executeUpdate(String sql) throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			PreparedStatement pstmt=conn.prepareStatement(sql);
			try {
				return pstmt.executeUpdate();
			} finally {
				pstmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Executes an update using a <code>PreparedStatement</code>
	 *
	 * @param  sql    the SQL to execute
	 * @param  param  the parameter to the SQL
	 *
	 * @return  the number of rows updated
	 */
	final protected int executeUpdate(String sql, String param) throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			PreparedStatement pstmt=conn.prepareStatement(sql);
			try {
				pstmt.setString(1, param);
				return pstmt.executeUpdate();
			} finally {
				pstmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Executes an update using a <code>PreparedStatement</code>
	 *
	 * @param  sql     the SQL to execute
	 * @param  param1  the first parameter to the SQL
	 * @param  param2  the second parameter to the SQL
	 *
	 * @return  the number of rows updated
	 */
	final protected int executeUpdate(String sql, String param1, String param2) throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			PreparedStatement pstmt=conn.prepareStatement(sql);
			try {
				pstmt.setString(1, param1);
				pstmt.setString(2, param2);
				return pstmt.executeUpdate();
			} finally {
				pstmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Gets a <code>String</code> describing one of the
	 * expanded boolean values.
	 */
	public static String getBooleanString(Boolean i) throws SQLException {
		switch(i) {
			case FALSE : return "false";
			case TRUE : return "true";
			case UNKNOWN : return "unknown";
			case NA : return "N/A";
			default : throw new SQLException("Unknown value: "+i);
		}
	}

	/**
	 * Gets the CHECK constraints info for this table.
	 */
	public CheckConstraints getCheckConstraints() throws SQLException, IOException {
		return null;
	}

	/**
	 * Gets a meta data column.
	 */
	private String getColumnMetaData(String column, int index) throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			ResultSet R=conn.getMetaData().getColumns(null, null, settings.getTable(), column);
			try {
				if(R.next()) return R.getString(index);
				else throw new SQLException("Column not found: "+column);
			} finally {
				R.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Gets information about the columns in the current table.
	 */
	final public Columns getColumns() throws SQLException, IOException {
		return getColumns(settings.getTable());
	}

	protected Columns getColumns(String table) throws SQLException, IOException {
		List<String> names=new ArrayList<String>();
		List<String> types=new ArrayList<String>();
		List<String> lengths=new ArrayList<String>();
		List<Boolean> areNullable=new ArrayList<Boolean>();
		List<String> defaults=new ArrayList<String>();
		List<String> remarks=new ArrayList<String>();

		Connection conn=DatabasePool.getConnection(settings);
		try {
			ResultSet R=conn.getMetaData().getColumns(null, null, table, "%");
			try {
				while(R.next()) {
					names.add(R.getString(4));
					types.add(R.getString(6));
					lengths.add(R.getString(7));
					int nullable=R.getInt(11);
					areNullable.add((nullable==DatabaseMetaData.columnNoNulls)?Boolean.FALSE
							   :(nullable==DatabaseMetaData.columnNullable)?Boolean.TRUE
							   :Boolean.UNKNOWN);
					String def=R.getString(13);
					defaults.add((def!=null)?'V'+def:null);
					String rem=R.getString(12);
					remarks.add((rem!=null)?rem:"");
				}
			} finally {
				R.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
		return new Columns(names, types, lengths, areNullable, defaults, remarks);
	}

	/**
	 * Gets the official database product name.
	 */
	public String getDatabaseProductName() throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			DatabaseMetaData metaData=conn.getMetaData();
			return metaData.getDatabaseProductName()+' '+metaData.getDatabaseProductVersion();
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Gets a list of all databases on the server.
	 */
	public List<String> getDatabases() throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			DatabaseMetaData metaData=conn.getMetaData();
			if(metaData.supportsCatalogsInDataManipulation()) {
				ResultSet results=conn.getMetaData().getCatalogs();
				try {
					List<String> V=new ArrayList<String>();
					while(results.next()) V.add(results.getString(1));
					return V;
				} finally {
					results.close();
				}
			} else {
				return Collections.emptyList();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Gets a <code>Vector</code> of <code>SchemaTable</code> to get one
	 * data structure of the entire database schema.
	 */
	public List<SchemaTable> getDatabaseSchema() throws IOException, SQLException {
		List<String> tableNames=getTables();
		int size=tableNames.size();
		List<SchemaTable> schemaTables=new ArrayList<SchemaTable>(size);
		for(int c=0;c<size;c++) {
			String tableName=tableNames.get(c);
			SchemaTable schemaTable=new SchemaTable(tableName);

			// Populate the columns
			List<String> columns=getColumns(tableName).getNames();
			int len=columns.size();
			for(int d=0;d<len;d++) schemaTable.getRow(columns.get(d));

			// Add the foreign key constraints
			ForeignKeys importedKeys=getImportedKeys(tableName);
			if(importedKeys!=null) {
				List<String> primaryKeys=importedKeys.getPrimaryKeys();
				List<String> foreignTables=importedKeys.getForeignTables();
				List<String> foreignKeys=importedKeys.getForeignKeys();
				len=primaryKeys.size();
				for(int d=0;d<len;d++) {
					schemaTable.getRow(primaryKeys.get(d)).addForeignKey(foreignTables.get(d), foreignKeys.get(d));
				}
			}

			// Add to return list
			schemaTables.add(schemaTable);
		}

		// Return the list
		return schemaTables;
	}

	/**
	 * Gets the default value or function for the specified column.
	 *
	 * @param column the name of the column.
	 *
	 * @return  the default value or function for the column.  A value will
	 *          start with a <code>'V'</code> and a function will start
	 *          with a <code>'F'</code>.
	 */
	public String getDefault(String column) throws SQLException, IOException {
		String def=getColumnMetaData(column, 13);
		if(def!=null) return 'V'+def;
		return null;
	}

	/**
	 * Gets the official database driver name.
	 */
	public String getDriverName() throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			DatabaseMetaData metaData=conn.getMetaData();
			return metaData.getDriverName()+' '+metaData.getDriverVersion();
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Gets the effective type of the specified column type.
	 * Useful for deciding which functions to show.
	 *
	 * @param type the column type.
	 */
	public String getEffectiveType(String type) throws SQLException, IOException {
		if("enum".equalsIgnoreCase(type) || "set".equalsIgnoreCase(type)) return "text";
		return type;
	}

	/**
	 * Gets a description of the foreign key columns that reference
	 * the table's primary key columns.
	 */
	public ForeignKeys getExportedKeys() throws SQLException, IOException {
		return getForeignKeys(settings.getTable(), false);
	}

	protected ForeignKeys getForeignKeys(String table, boolean isImported) throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			ResultSet R=(isImported)
				? conn.getMetaData().getImportedKeys(null, null, table)
				: conn.getMetaData().getExportedKeys(null, null, table)
			;
			if(R!=null) {
				List<String> foreignKeys=new ArrayList<String>();
				List<String> foreignTables=new ArrayList<String>();
				List<String> primaryKeys=new ArrayList<String>();
				List<String> primaryTables=new ArrayList<String>();
				List<String> constraintNames=new ArrayList<String>();
				List<String> insertRules=new ArrayList<String>();
				List<String> deleteRules=new ArrayList<String>();
				List<String> updateRules=new ArrayList<String>();
				while(R.next()) {
					primaryTables.add(R.getString(3));
					primaryKeys.add(R.getString(4));
					foreignTables.add(R.getString(7));
					foreignKeys.add(R.getString(8));
					constraintNames.add(R.getString(12));
					insertRules.add("Unknown");
					deleteRules.add(getRuleDescription(R.getInt(11)));
					updateRules.add(getRuleDescription(R.getInt(10)));
				}
				int size=constraintNames.size();
				if(size<1) return null;
				else {
					List<Boolean> isDeferrable=new ArrayList<Boolean>(size);
					List<Boolean> isInitiallyDeferred=new ArrayList<Boolean>(size);
					for(int i=0;i<size;i++) {
						isDeferrable.add(Boolean.UNKNOWN);
						isInitiallyDeferred.add(Boolean.UNKNOWN);
					}
					return new ForeignKeys(
						constraintNames,
						foreignKeys,
						foreignTables,
						primaryKeys,
						primaryTables,
						insertRules,
						deleteRules,
						updateRules,
						isDeferrable,
						isInitiallyDeferred
					);
				}
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
		return null;
	}

	/**
	 * Gets the foreign key data for the current table.
	 *
	 * @param isImported Get the imported keys?
	 */
	final public ForeignKeys getForeignKeys(boolean isImported) throws SQLException, IOException {
		return getForeignKeys(settings.getTable(), isImported);
	}

	/**
	 * Gets a list of all the unique SQL functions supported by this database.
	 */
	public List<String> getFunctionList() throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			Set<String> SV=new HashSet<String>();
			List<String> V=new ArrayList<String>();
			DatabaseMetaData metaData=conn.getMetaData();
			StringTokenizer ST=new StringTokenizer(metaData.getNumericFunctions(),",");
			while(ST.hasMoreTokens()) {
				String S=ST.nextToken();
				if(!SV.contains(S)) {
					SV.add(S);
					V.add(S);
				}
			}
			ST=new StringTokenizer(metaData.getStringFunctions(),",");
			while(ST.hasMoreTokens()) {
				String S=ST.nextToken();
				if(!SV.contains(S)) {
					SV.add(S);
					V.add(S);
				}
			}
			ST=new StringTokenizer(metaData.getSystemFunctions(),",");
			while(ST.hasMoreTokens()) {
				String S=ST.nextToken();
				if(!SV.contains(S)) {
					SV.add(S);
					V.add(S);
				}
			}
			ST=new StringTokenizer(metaData.getTimeDateFunctions(),",");
			while(ST.hasMoreTokens()) {
				String S=ST.nextToken();
				if(!SV.contains(S)) {
					SV.add(S);
					V.add(S);
				}
			}
			return V;
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Gets the functions that may return the provided type.
	 */
	public List<String> getFunctionList(String type) throws SQLException, IOException {
		return getFunctionList();
	}

	/**
	 * Gets a description of the primary key columns that are referenced 
	 * by the table's foreign key columns.
	 */
	final public ForeignKeys getImportedKeys() throws SQLException, IOException {
		return getImportedKeys(settings.getTable());
	}

	protected ForeignKeys getImportedKeys(String table) throws SQLException, IOException {
		return getForeignKeys(table, true);
	}

	/**
	 * Gets a list of indexes for the selected table.
	 */
	public Indexes getIndexes() throws SQLException, IOException {
		List<String> names=new ArrayList<String>();
		List<Boolean> areUnique=new ArrayList<Boolean>();
		List<String> colNames=new ArrayList<String>();
		Connection conn=DatabasePool.getConnection(settings);
		try {
			ResultSet R=conn.getMetaData().getIndexInfo(null, null, settings.getTable(), false, false);
			try {
				while(R.next()) {
					names.add(R.getString(6));
					areUnique.add(R.getBoolean(4)?Boolean.FALSE:Boolean.TRUE);
					colNames.add(R.getString(9));
				}
			} finally {
				R.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
		return new Indexes(names, areUnique, colNames);
	}

	private List<String> getIndexInfo(int index) throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			ResultSet R=conn.getMetaData().getIndexInfo(null, null, settings.getTable(), false, false);
			try {
				List<String> V=new ArrayList<String>();
				while(R.next()) V.add(R.getString(index));
				return V;
			} finally {
				R.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Gets a <code>JDBCConnector</code> of the provided classname and info.
	 *
	 * @param classname  the classname to load
	 */
	public static JDBCConnector getInstance(Settings settings)
	throws
		IOException,
		ClassNotFoundException,
		NoSuchMethodException,
		InstantiationException,
		IllegalAccessException,
		InvocationTargetException
	{
		Object[] initArgs = {settings};
		return
			(JDBCConnector)Class
			.forName(DatabaseConfiguration.getProperty("connector", settings.getDatabaseProduct()))
			.getConstructor(paramTypes)
			.newInstance(initArgs)
			;
	}

	/**
	 * Gets a int from a query given a String using a <code>PreparedStatement</code>.
	 * Returns 0 if no results were returned.
	 */
	final protected int getIntQuery(String sql) throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			PreparedStatement pstmt=conn.prepareStatement(sql);
			try {
				ResultSet results=pstmt.executeQuery();
				try {
					if(results.next()) return results.getInt(1);
					else return 0;
				} finally {
					results.close();
				}
			} finally {
				pstmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Gets a int from a query given a String using a <code>PreparedStatement</code>.
	 * Returns 0 if no results were returned.
	 */
	final protected int getIntQuery(String sql, String param) throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			PreparedStatement pstmt=conn.prepareStatement(sql);
			try {
				pstmt.setString(1, param);
				ResultSet results=pstmt.executeQuery();
				try {
					if(results.next()) return results.getInt(1);
					else return 0;
				} finally {
					results.close();
				}
			} finally {
				pstmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Gets the length/set of the specified column.
	 *
	 * @param column the name of the column,
	 */
	public String getLength(String column) throws SQLException, IOException {
		return getColumnMetaData(column, 7);
	}

	/**
	 * Gets the LIMIT clause (nonstandard) for a specified part of a table.
	 *
	 * @param startPos the starting row number to read from.
	 * @param numRows the numbers of rows to read.
	 */
	public String getLimitClause(int startPos, int numRows) throws SQLException, IOException {
		return null;
	}

	private static final List<String> possiblePrivileges=new ArrayList<String>(6);
	private static final List<String> unmodifiablePrivileges = Collections.unmodifiableList(possiblePrivileges);
	static {
		possiblePrivileges.add("SELECT");
		possiblePrivileges.add("DELETE");
		possiblePrivileges.add("INSERT");
		possiblePrivileges.add("UPDATE");
		possiblePrivileges.add("REFERENCES");
		possiblePrivileges.add("EXECUTE");
	}

	/**
	 * Gets a list of possible privileges which can be granted on a table.
	 */
	public List<String> getPossiblePrivileges() {
		return unmodifiablePrivileges;
	}

	/**
	 * Gets the possible values for a column.  For a type <code>ENUM</code> in MySQL this
	 * would return the valid values, for a foreign key in PostgreSQL this would return all
	 * the foreign rows if less than <code>Settings.fkeyrows</code> exist.
	 *
	 * @return the list of all possible values or <code>null</code> if not known
	 */
	public List<String> getPossibleValues(String column, String type) throws SQLException, IOException {
		return null;
	}

	/**
	 * Gets the primary key description for the given constraint name.
	 *
	 * @param constraint the constraint name.
	 */
	public String getPrimaryKey(String constraint) throws SQLException, IOException {
		throw new SQLException("getPrimaryKey(String constraint) not implemented");
	}

	/**
	 * Gets a list of primary keys in the selected table.
	 */
	public PrimaryKeys getPrimaryKeys() throws SQLException, IOException {
		List<String> columns=new ArrayList<String>();
		List<String> names=new ArrayList<String>();
		Connection conn=DatabasePool.getConnection(settings);
		try {
			ResultSet R=conn.getMetaData().getPrimaryKeys(null, null, settings.getTable());
			try {
				while(R.next()) {
					columns.add(R.getString(4));
					names.add(R.getString(6));
				}
			} finally {
				R.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
		return new PrimaryKeys(columns, names);
	}

	/**
	 * Gets the remark for the specified column.
	 *
	 * @param column the name of the column,
	 */
	public String getRemark(String column) throws SQLException, IOException {
		return getColumnMetaData(column, 12);
	}

	/**
	 * Gets a row specified by one or more primary keys.
	 *
	 * @param primaryKeys the names of the primary keys.
	 * @param primaryValues the values of the primary keys.
	 */
	public List<String> getRow(List<String> primaryKeys, List<String> primaryValues) throws SQLException, IOException {
		// Build the SQL first
		StringBuffer SB = new StringBuffer("SELECT * FROM ").append(quoteTable(settings.getTable()));
		for (int i = 0; i < primaryKeys.size(); i++) {
			SB.append(i == 0 ? " WHERE " : " AND ");
			if (primaryValues.get(i) == null) {
				appendIsNull(SB, primaryKeys.get(i));
			} else {
				SB.append(quoteColumn(primaryKeys.get(i))).append("=?");
			}
		}
		String sql = SB.toString();

		// Then perform the query
		Connection conn = DatabasePool.getConnection(settings);
		try {
			PreparedStatement pstmt = conn.prepareStatement(sql);
			try {
				pstmt.setEscapeProcessing(false);
				int pos = 1;
				for (int i = 0; i < primaryKeys.size(); i++) {
					if (primaryValues.get(i) != null) {
						pstmt.setString(pos++, primaryValues.get(i));
					}
				}
				ResultSet results = pstmt.executeQuery();
				try {
					List<String> V = new ArrayList<String>();
					if (results.next()) {
						int count = results.getMetaData().getColumnCount();
						for (int i = 1; i <= count; i++) {
							V.add(results.getString(i));
						}
					}
					return V;
				} finally {
					results.close();
				}
			} finally {
				pstmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Gets a description of the given foreign key rule.
	 *
	 * @param i the value of one of the <code>DatabaseMetaData</code> defined constants.
	 */
	public String getRuleDescription(int i) throws SQLException {
		if(i==DatabaseMetaData.importedKeyNoAction) return "NO ACTION";
		if(i==DatabaseMetaData.importedKeyCascade) return "CASCADE";
		if(i==DatabaseMetaData.importedKeySetNull) return "SET NULL";
		if(i==DatabaseMetaData.importedKeySetDefault) return "SET DEFAULT";
		if(i==DatabaseMetaData.importedKeyRestrict) return "RESTRICT";

		if(i==DatabaseMetaData.importedKeyInitiallyDeferred) return "INITIALLY DEFERRED";
		if(i==DatabaseMetaData.importedKeyInitiallyImmediate) return "INITIALLY IMMEDIATE";
		if(i==DatabaseMetaData.importedKeyNotDeferrable) return "NOT DEFERRABLE";
		throw new SQLException("Unknown value: "+i);
	}

	/**
	 * Gets the WHERE clause for a SELECT query on the specified database.
	 *
	 * @param colNames the names of the columns to compare.
	 * @param colValues the values of the columns to compare.
	 */
	public String getSelectWhereClause(String[] colNames, String[] colValues) throws SQLException, IOException {
		// Build the SQL
		StringBuffer sql=new StringBuffer();
		boolean hasBeen=false;
		for(int i=0;i<colNames.length;i++) {
			if(colNames[i].length()>0) {
				if(colValues[i]==null) {
					if(hasBeen) sql.append(" AND ");
					else hasBeen=true;
					appendIsNull(sql, colNames[i]);
				} else if(!"".equals(colValues[i])) {
					if(hasBeen) sql.append(" AND ");
					else hasBeen=true;
					sql
						.append(quoteColumn(colNames[i]))
						.append(" LIKE ")
						.append(Util.escapeSQLValue(colValues[i]))
					;
				}
			}
		}
		return sql.toString();
	}

	/**
	 * Gets the <code>Settings</code> of this <code>JDBCConnector</code>.
	 */
	final public Settings getSettings() {
		return settings;
	}

	/**
	 * Gets the privileges for the current table.
	 */
	public TablePrivileges getTablePrivileges() throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			ResultSet R=conn.getMetaData().getTablePrivileges(null, null, settings.getTable());
			try {
				List<String> grantors=new ArrayList<String>();
				List<String> grantees=new ArrayList<String>();
				List<String> privileges=new ArrayList<String>();
				List<Boolean> isGrantable=new ArrayList<Boolean>();
				while(R.next()) {
					grantors.add(R.getString(4));
					grantees.add(R.getString(5));
					privileges.add(R.getString(6));
					String G=R.getString(7);
					isGrantable.add(("YES".equals(G))?Boolean.TRUE:("NO".equals(G))?Boolean.FALSE:Boolean.UNKNOWN);
				}
				return new TablePrivileges(
					grantors,
					grantees,
					privileges,
					isGrantable
				);
			} finally {
				R.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Returns a <code>Vector</code> containing all the tables in the database.
	 */
	public List<String> getTables() throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			ResultSet results=conn.getMetaData().getTables(null, null, "%", defaultTableTypes);
			try {
				List<String> V=new ArrayList<String>();
				while(results.next()) V.add(results.getString(3));
				return V;
			} finally {
				results.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Gets a list of types supported by the database.
	 */
	public List<String> getTypes() throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			ResultSet R=conn.getMetaData().getTypeInfo();
			try {
				List<String> V=new ArrayList<String>();
				while(R.next()) V.add(R.getString(1));
				return V;
			} finally {
				R.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Gets the database server URL
	 */
	public String getURL() throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			DatabaseMetaData metaData=conn.getMetaData();
			return metaData.getURL();
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Grants a user privileges on the current table.
	 *
	 * @param user the user to grant the privileges to.
	 * @param an array of privileges which will be granted.
	 */
	public void grantPrivileges(String user, String[] privileges) throws SQLException, IOException {
		StringBuffer sql=new StringBuffer("GRANT ");
		int size=privileges.length;
		if(size<1) throw new SQLException("No privileges specified!");
		sql.append(privileges[0]);
		for(int i=1;i<size;i++) {
			sql.append(',').append(privileges[i]);
		}
		sql
			.append(" ON ")
			.append(quoteTable(settings.getTable()))
			.append(" TO ")
			.append(user)
		;
		executeUpdate(sql.toString());
	}

	/**
	 * Inserts a new row into a table.
	 *
	 * @param column an array of <code>String</code> objects representing the names of the columns.
	 * @param column an array of <code>String</code> objects representing functions to execute.
	 * @param column an array of <code>String</code> objects representing the values to insert.
	 */
	public void insertRow(String[] column, String[] function, String[] value) throws SQLException, IOException {
		// Build the SQL
		StringBuffer SB=new StringBuffer("INSERT INTO ").append(quoteTable(settings.getTable())).append(" (");
		for(int i=0;i<column.length;i++) {
			if(i>0) SB.append(", ");
			SB.append(quoteColumn(column[i]));
		}
		SB.append(") VALUES (");
		for(int i=0;i<column.length;i++) {
			if(i>0) SB.append(", ");
			if(function[i]!=null && function[i].length()>0) {
				SB.append(function[i]);
			} else {
				SB.append('?');
			}
		}
		SB.append(')');
		String sql=SB.toString();

		Connection conn=DatabasePool.getConnection(settings);
		try {
			PreparedStatement stmt=conn.prepareStatement(sql);
			try {
				stmt.setEscapeProcessing(false);
				int pos=1;
				for(int i=0;i<column.length;i++) {
					if(function[i]==null || function[i].length()==0) stmt.setString(pos++,value[i]);
				}
				stmt.executeUpdate();
			} finally {
				stmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Is the specified column nullable?
	 *
	 * @param column the name of the column,
	 */
	public Boolean isNullable(String column) throws SQLException, IOException {
		return "YES".equals(getColumnMetaData(column, 18))?Boolean.TRUE:Boolean.FALSE;
	}

	/**
	 * Renames an existing table.
	 *
	 * @param newTable the new name for the table.
	 */
	public void renameTable(String newTable) throws SQLException, IOException {
		executeUpdate("ALTER TABLE " + quoteTable(settings.getTable()) + " RENAME TO " + quoteTable(newTable));
	}

	/**
	 * Revokes privileges from a user on the current table.
	 *
	 * @param user the user to revoke the privileges from.
	 * @param an array of privileges which will be revoked.
	 */
	public void revokePrivileges(String user, String[] privileges) throws SQLException, IOException {
		StringBuffer sql=new StringBuffer("REVOKE ");
		int size=privileges.length;
		if(size<1) throw new SQLException("No privileges specified!");
		sql.append(privileges[0]);
		for(int i=1;i<size;i++) {
			sql.append(',').append(privileges[i]);
		}
		sql
			.append(" ON ")
			.append(quoteTable(settings.getTable()))
			.append(" FROM ")
			.append(user)
			;
		executeUpdate(sql.toString());
	}

	/**
	 * Does the database product support CHECK constraints?
	 */
	public boolean supportsCheckConstraints() {
		return false;
	}

	/**
	 * Does the database product support foreign keys?
	 */
	public boolean supportsForeignKeys() {
		return true;
	}

	/**
	 * Gets the type of the specified column.
	 *
	 * @param column the name of the column.
	 */
	public String XgetType(String column) throws SQLException, IOException {
		return getColumnMetaData(column, 6);
	}

	/**
	 * By default, surrounds with '"' if is not all lower-case
	 * alphanumeric.
	 */
	// TODO: Throw exception one way-off characters?
	protected static String defaultQuote(String pre, String doubleQuote, String post, String identifier) {
		if(identifier == null) throw new NullPointerException();
		int len = identifier.length();
		if(len == 0) return pre + post;
		StringBuilder quoted = new StringBuilder(len + pre.length() + post.length());
		quoted.append(pre);
		boolean quotesNeeded = false;
		for(int i = 0; i < len; i++) {
			char ch = identifier.charAt(i);
			if(
				(ch >= 'a' && ch <= 'z')
				|| (i != 0 && ch >= '0' && ch <= '9')
			) {
				quoted.append(ch);
			} else if(ch == '"') {
				quoted.append(doubleQuote);
				quotesNeeded = true;
			} else {
				quoted.append(ch);
				quotesNeeded = true;
			}
		}
		if(quotesNeeded) {
			return quoted.append(post).toString();
		} else {
			return identifier;
		}
	}

	/**
	 * Quotes an identifier used for a table name.
	 *
	 * @see  #defaultQuote(java.lang.String)
	 */
	public String quoteTable(String table) {
		return defaultQuote("\"", "\"\"", "\"", table);
	}

	/**
	 * Quotes an identifier used for a column name.
	 *
	 * @see  #defaultQuote(java.lang.String)
	 */
	public String quoteColumn(String column) {
		return defaultQuote("\"", "\"\"", "\"", column);
	}
}
