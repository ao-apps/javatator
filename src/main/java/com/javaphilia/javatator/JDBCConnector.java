/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2018, 2019, 2021, 2022  AO Industries, Inc.
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

import com.aoindustries.aoserv.client.mysql.Server;
import java.io.IOException;
import java.io.Writer;
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
 */
public class JDBCConnector {

	public enum Boolean {
		/**
		 * Expanded boolean {@link #FALSE} is returned when something is known to be false.
		 */
		FALSE,

		/**
		 * Expanded boolean {@link #TRUE} is returned when something is known to be true.
		 */
		TRUE,

		/**
		 * Expanded boolean {@link #UNKNOWN} is returned when something is not known to be true or false.
		 */
		UNKNOWN,

		/**
		 * Expanded boolean {@link #NA} is returned when something is known to not apply to this database.
		 */
		NA
	}

	protected static final String[] defaultTableTypes=new String[]{"TABLE"};

	/**
	 * The {@link Settings} store all the configuration parameters.
	 */
	protected final Settings settings;

	/**
	 * Instantiate a new JDBCConnector.
	 *
	 * @param settings  the {@link Settings} to use.
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
		StringBuilder sql=new StringBuilder("ALTER TABLE ");
		sql
			.append(quoteTable(settings.getTable()))
			.append(" ADD ")
			.append(quoteColumn(name))
			.append(' ')
			.append(type)
			.append(' ');
		boolean hasLength=!type.toUpperCase().endsWith("TEXT");
		if(hasLength && length!=null && length.length()>0)
			sql
				.append('(')
				.append(length)
				.append(')');
		if(dfault!=null) {
			sql.append(" DEFAULT ");
			if(dfault.charAt(0)=='F') sql.append(dfault.substring(1));
			else sql.append(Util.escapeSQLValue(dfault.substring(1)));
		}
		sql
			.append(' ')
			.append(nullClause)
			.append(' ')
			.append(remarks);
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
		StringBuilder sql=new StringBuilder("ALTER TABLE ");
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
			.append(")");
		if(matchType!=null) sql.append(" MATCH ").append(matchType);
		sql
			.append(" ON DELETE ")
			.append(onDelete)
			.append(" ON UPDATE ")
			.append(onUpdate)
			.append(isDeferrable?" DEFERRABLE":" NOT DEFERRABLE")
			.append(" INITIALLY ")
			.append(initially);
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
		try (Connection conn = DatabasePool.getConnection(settings)) {
			StringBuilder sb = new StringBuilder();
			try (ResultSet results = conn.getMetaData().getPrimaryKeys(null, null, settings.getTable())) {
				while(results.next()) {
					sb.append(results.getString(4));
					sb.append(", ");
				}
				sb.append(quoteColumn(column));
			}
			try (PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE " + quoteTable(settings.getTable()) + " DROP PRIMARY KEY, ADD PRIMARY KEY(" + sb.toString() + ")")) {
				pstmt.executeUpdate();
			}
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

	protected void appendIsNull(StringBuilder sb, String column) {
		sb
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
		StringBuilder sql=new StringBuilder();
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
		StringBuilder sb = new StringBuilder("DELETE FROM ").append(quoteTable(settings.getTable()));
		for (int i = 0; i < primaryKeys.length; i++) {
			sb.append(i == 0 ? " WHERE " : " AND ");
			if (primaryKeyValues[i] == null) {
				appendIsNull(sb, primaryKeys[i]);
			} else {
				sb.append(primaryKeys[i]).append("=?");
			}
		}
		String sql = sb.toString();

		try (
			Connection conn = DatabasePool.getConnection(settings);
			PreparedStatement stmt = conn.prepareStatement(sql)
		) {
			// stmt.setEscapeProcessing(false);
			int pos = 1;
			for(String primaryKeyValue : primaryKeyValues) {
				if(primaryKeyValue != null) {
					stmt.setString(pos++, primaryKeyValue);
				}
			}
			stmt.executeUpdate();
		}
	}

	/**
	 * Deletes a constraint.
	 *
	 * @param constraint the name of the constraint to delete.
	 * @param behaviour the behaviour when dropping.
	 */
	public void dropConstraint(String constraint, String behaviour) throws SQLException, IOException {
		executeUpdate(
			"ALTER TABLE "
			+ quoteTable(settings.getTable())
			+ " DROP CONSTRAINT "
			+ constraint+' '+behaviour
		);
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
		try (Connection conn = DatabasePool.getConnection(settings)) {
			StringBuilder sb = new StringBuilder();
			try (ResultSet results = conn.getMetaData().getPrimaryKeys(null, null, settings.getTable())) {
				while(results.next()) {
					if(!results.getString(4).equals(column)) {
						if(sb.length() > 0) sb.append(", ");
						sb.append(results.getString(4));
					}
				}
			}
			StringBuilder sql=new StringBuilder("ALTER TABLE ").append(quoteTable(settings.getTable())).append(" DROP PRIMARY KEY");
			if(sb.length() > 0) {
				sql.append(", ADD PRIMARY KEY(");
				sql.append(sb.toString());
				sql.append(")");
			}

			try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
				pstmt.executeUpdate();
			}
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
	 * @param out a {@link Writer} to dump the SQL to.
	 */
	public void dumpTableContents(Writer out) throws SQLException, IOException {
		try (Connection conn = DatabasePool.getConnection(settings)) {
			String table=settings.getTable();
			try (
				Statement stmt = conn.createStatement();
				ResultSet r = stmt.executeQuery("SELECT * FROM " + quoteTable(table))
			) {
				int count = r.getMetaData().getColumnCount();
				while(r.next()) {
					out.write("INSERT INTO ");
					out.write(quoteTable(table));
					out.write(" VALUES (");
					boolean hasBeen=false;
					for(int i=1;i<=count;i++) {
						if(hasBeen) out.write(',');
						else hasBeen=true;
						Util.printEscapedSQLValue(out, r.getString(i));
					}
					out.write(");\n");
				}
			}
		}
	}

	/**
	 * Dumps the structure of the table.
	 *
	 * @param out a {@link Writer} to dump the SQL to.
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
	 *                     <li>{@code null} - no default</li>
	 *                     <li>{@code "F*"} - a function</li>
	 *                     <li>{@code "V*"} - a value</li>
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
		StringBuilder sql=new StringBuilder();
		sql
			.append("ALTER TABLE ")
			.append(quoteTable(settings.getTable()))
			.append(" CHANGE ")
			.append(quoteColumn(column))
			.append(' ')
			.append(quoteColumn(newColumn))
			.append(' ')
			.append(newType);
		boolean hasLength=!newType.toUpperCase().endsWith("TEXT");
		if(hasLength) {
			sql
				.append('(')
				.append(newLength)
				.append(')');
		}
		if(newDefault!=null) {
			sql.append(" DEFAULT ");
			if(newDefault.charAt(0)=='F') sql.append(newDefault.substring(1));
			else sql.append(Util.escapeSQLValue(newDefault.substring(1)));
		}
		sql
			.append(' ')
			.append(newNull)
			.append(' ')
			.append(newRemarks);
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
		StringBuilder sb = new StringBuilder("UPDATE ").append(quoteTable(settings.getTable())).append(" SET ");
		for (int i = 0; i < column.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(quoteColumn(column[i])).append('=');
			if (function[i] != null && function[i].length() > 0) {
				sb.append(function[i]);
			} else {
				sb.append('?');
			}
		}
		for (int i = 0; i < primaryKeys.length; i++) {
			sb.append(i == 0 ? " WHERE " : " AND ");
			if (primaryKeyValues[i] == null) {
				appendIsNull(sb, primaryKeys[i]);
			} else {
				sb.append(quoteColumn(primaryKeys[i])).append("=?");
			}
		}
		String sql = sb.toString();
		try (
			Connection conn = DatabasePool.getConnection(settings);
			PreparedStatement stmt = conn.prepareStatement(sql)
		) {
			// stmt.setEscapeProcessing(false);
			int pos = 1;
			for (int i = 0; i < column.length; i++) {
				if (function[i] == null || function[i].length() == 0) {
					stmt.setString(pos++, value[i]);
				}
			}
			for(String primaryKeyValue : primaryKeyValues) {
				if(primaryKeyValue != null) {
					stmt.setString(pos++, primaryKeyValue);
				}
			}
			stmt.executeUpdate();
		}
	}

	/**
	 * Deletes all the records in the specified table.
	 */
	public void emptyTable() throws SQLException, IOException {
		executeUpdate("DELETE FROM " + quoteTable(settings.getTable()));
	}

	/**
	 * Executes a query and returns a {@link List} of {@link String} at index 1.
	 */
	protected final List<String> executeListQuery(String sql) throws SQLException, IOException {
		try (
			Connection conn = DatabasePool.getConnection(settings);
			Statement stmt = conn.createStatement();
			ResultSet results = stmt.executeQuery(sql)
		) {
			List<String> v = new ArrayList<>();
			while(results.next()) {
				v.add(results.getString(1));
			}
			return v;
		}
	}

	/**
	 * Executes a query and returns a {@link List} of {@link String} at index 1.
	 *
	 * @param  sql    the SQL to execute
	 * @param  param  the parameter to the SQL
	 */
	protected final List<String> executeListQuery(String sql, String param) throws SQLException, IOException {
		try (
			Connection conn = DatabasePool.getConnection(settings);
			PreparedStatement pstmt = conn.prepareStatement(sql)
		) {
			pstmt.setString(1, param);
			try (ResultSet results = pstmt.executeQuery()) {
				List<String> v = new ArrayList<>();
				while(results.next()) {
					v.add(results.getString(1));
				}
				return v;
			}
		}
	}

	/**
	 * Executes an update using a {@link PreparedStatement}
	 *
	 * @param  sql    the SQL to execute
	 *
	 * @return  the number of rows updated
	 */
	protected final int executeUpdate(String sql) throws SQLException, IOException {
		try (
			Connection conn = DatabasePool.getConnection(settings);
			PreparedStatement pstmt = conn.prepareStatement(sql)
		) {
			return pstmt.executeUpdate();
		}
	}

	/**
	 * Executes an update using a {@link PreparedStatement}
	 *
	 * @param  sql    the SQL to execute
	 * @param  param  the parameter to the SQL
	 *
	 * @return  the number of rows updated
	 */
	protected final int executeUpdate(String sql, String param) throws SQLException, IOException {
		try (
			Connection conn = DatabasePool.getConnection(settings);
			PreparedStatement pstmt = conn.prepareStatement(sql)
		) {
			pstmt.setString(1, param);
			return pstmt.executeUpdate();
		}
	}

	/**
	 * Executes an update using a {@link PreparedStatement}
	 *
	 * @param  sql     the SQL to execute
	 * @param  param1  the first parameter to the SQL
	 * @param  param2  the second parameter to the SQL
	 *
	 * @return  the number of rows updated
	 */
	protected final int executeUpdate(String sql, String param1, String param2) throws SQLException, IOException {
		try (
			Connection conn = DatabasePool.getConnection(settings);
			PreparedStatement pstmt = conn.prepareStatement(sql)
		) {
			pstmt.setString(1, param1);
			pstmt.setString(2, param2);
			return pstmt.executeUpdate();
		}
	}

	/**
	 * Gets a {@link String} describing one of the
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
		try (
			Connection conn = DatabasePool.getConnection(settings);
			ResultSet r = conn.getMetaData().getColumns(null, null, settings.getTable(), column)
		) {
			if(r.next()) return r.getString(index);
			else throw new SQLException("Column not found: "+column);
		}
	}

	/**
	 * Gets information about the columns in the current table.
	 */
	public final Columns getColumns() throws SQLException, IOException {
		return getColumns(settings.getTable());
	}

	protected Columns getColumns(String table) throws SQLException, IOException {
		List<String> names=new ArrayList<>();
		List<String> types=new ArrayList<>();
		List<String> lengths=new ArrayList<>();
		List<Boolean> areNullable=new ArrayList<>();
		List<String> defaults=new ArrayList<>();
		List<String> remarks=new ArrayList<>();

		try (
			Connection conn = DatabasePool.getConnection(settings);
			ResultSet r = conn.getMetaData().getColumns(null, null, table, "%")
		) {
			while(r.next()) {
				names.add(r.getString(4));
				types.add(r.getString(6));
				lengths.add(r.getString(7));
				int nullable = r.getInt(11);
				areNullable.add(
					(nullable==DatabaseMetaData.columnNoNulls) ? Boolean.FALSE
						: (nullable==DatabaseMetaData.columnNullable) ? Boolean.TRUE
							: Boolean.UNKNOWN);
				String def = r.getString(13);
				defaults.add((def != null) ? ('V' + def) : null);
				String rem = r.getString(12);
				remarks.add((rem != null) ? rem : "");
			}
		}
		return new Columns(names, types, lengths, areNullable, defaults, remarks);
	}

	/**
	 * Gets the official database product name.
	 */
	public String getDatabaseProductName() throws SQLException, IOException {
		try (Connection conn = DatabasePool.getConnection(settings)) {
			DatabaseMetaData metaData=conn.getMetaData();
			return metaData.getDatabaseProductName()+' '+metaData.getDatabaseProductVersion();
		}
	}

	/**
	 * Gets a list of all databases on the server.
	 */
	public List<String> getDatabases() throws SQLException, IOException {
		try (Connection conn = DatabasePool.getConnection(settings)) {
			DatabaseMetaData metaData=conn.getMetaData();
			if(metaData.supportsCatalogsInDataManipulation()) {
				try (ResultSet results = conn.getMetaData().getCatalogs()) {
					List<String> v = new ArrayList<>();
					while(results.next()) {
						v.add(results.getString(1));
					}
					return v;
				}
			} else {
				return Collections.emptyList();
			}
		}
	}

	/**
	 * Gets a {@link List} of {@link SchemaTable} to get one
	 * data structure of the entire database schema.
	 */
	public List<SchemaTable> getDatabaseSchema() throws IOException, SQLException {
		List<String> tableNames=getTables();
		int size=tableNames.size();
		List<SchemaTable> schemaTables=new ArrayList<>(size);
		for(int c=0;c<size;c++) {
			String tableName=tableNames.get(c);
			SchemaTable schemaTable=new SchemaTable(tableName);

			// Populate the columns
			List<String> columns = getColumns(tableName).getNames();
			int len = columns.size();
			for(int d = 0; d < len; d++) {
				schemaTable.getRow(columns.get(d));
			}

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
	 *          start with a {@code 'V'} and a function will start
	 *          with a {@code 'F'}.
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
		try (Connection conn = DatabasePool.getConnection(settings)) {
			DatabaseMetaData metaData=conn.getMetaData();
			return metaData.getDriverName()+' '+metaData.getDriverVersion();
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
		try (
			Connection conn = DatabasePool.getConnection(settings);
			ResultSet r = (isImported)
				? conn.getMetaData().getImportedKeys(null, null, table)
				: conn.getMetaData().getExportedKeys(null, null, table)
		) {
			if(r != null) {
				List<String> foreignKeys=new ArrayList<>();
				List<String> foreignTables=new ArrayList<>();
				List<String> primaryKeys=new ArrayList<>();
				List<String> primaryTables=new ArrayList<>();
				List<String> constraintNames=new ArrayList<>();
				List<String> insertRules=new ArrayList<>();
				List<String> deleteRules=new ArrayList<>();
				List<String> updateRules=new ArrayList<>();
				while(r.next()) {
					primaryTables.add(r.getString(3));
					primaryKeys.add(r.getString(4));
					foreignTables.add(r.getString(7));
					foreignKeys.add(r.getString(8));
					constraintNames.add(r.getString(12));
					insertRules.add("Unknown");
					deleteRules.add(getRuleDescription(r.getInt(11)));
					updateRules.add(getRuleDescription(r.getInt(10)));
				}
				int size=constraintNames.size();
				if(size<1) return null;
				else {
					List<Boolean> isDeferrable=new ArrayList<>(size);
					List<Boolean> isInitiallyDeferred=new ArrayList<>(size);
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
		}
		return null;
	}

	/**
	 * Gets the foreign key data for the current table.
	 *
	 * @param isImported Get the imported keys?
	 */
	public final ForeignKeys getForeignKeys(boolean isImported) throws SQLException, IOException {
		return getForeignKeys(settings.getTable(), isImported);
	}

	/**
	 * Gets a list of all the unique SQL functions supported by this database.
	 */
	public List<String> getFunctionList() throws SQLException, IOException {
		try (Connection conn = DatabasePool.getConnection(settings)) {
			Set<String> sv = new HashSet<>();
			List<String> v = new ArrayList<>();
			DatabaseMetaData metaData=conn.getMetaData();
			StringTokenizer st = new StringTokenizer(metaData.getNumericFunctions(), ",");
			while(st.hasMoreTokens()) {
				String s = st.nextToken();
				if(!sv.contains(s)) {
					sv.add(s);
					v.add(s);
				}
			}
			st = new StringTokenizer(metaData.getStringFunctions(), ",");
			while(st.hasMoreTokens()) {
				String s = st.nextToken();
				if(!sv.contains(s)) {
					sv.add(s);
					v.add(s);
				}
			}
			st = new StringTokenizer(metaData.getSystemFunctions(), ",");
			while(st.hasMoreTokens()) {
				String s = st.nextToken();
				if(!sv.contains(s)) {
					sv.add(s);
					v.add(s);
				}
			}
			st = new StringTokenizer(metaData.getTimeDateFunctions(), ",");
			while(st.hasMoreTokens()) {
				String s = st.nextToken();
				if(!sv.contains(s)) {
					sv.add(s);
					v.add(s);
				}
			}
			return v;
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
	public final ForeignKeys getImportedKeys() throws SQLException, IOException {
		return getImportedKeys(settings.getTable());
	}

	protected ForeignKeys getImportedKeys(String table) throws SQLException, IOException {
		return getForeignKeys(table, true);
	}

	/**
	 * Gets a list of indexes for the selected table.
	 */
	public Indexes getIndexes() throws SQLException, IOException {
		List<String> names=new ArrayList<>();
		List<Boolean> areUnique=new ArrayList<>();
		List<String> colNames=new ArrayList<>();
		try (
			Connection conn = DatabasePool.getConnection(settings);
			ResultSet r = conn.getMetaData().getIndexInfo(null, null, settings.getTable(), false, false)
		) {
			while(r.next()) {
				names.add(r.getString(6));
				areUnique.add(r.getBoolean(4) ? Boolean.FALSE : Boolean.TRUE);
				colNames.add(r.getString(9));
			}
		}
		return new Indexes(names, areUnique, colNames);
	}

	private List<String> getIndexInfo(int index) throws SQLException, IOException {
		try (
			Connection conn = DatabasePool.getConnection(settings);
			ResultSet r = conn.getMetaData().getIndexInfo(null, null, settings.getTable(), false, false)
		) {
			List<String> v = new ArrayList<>();
			while(r.next()) {
				v.add(r.getString(index));
			}
			return v;
		}
	}

	/**
	 * Gets a {@link JDBCConnector} of the provided classname and info.
	 *
	 * @param settings  the {@link Settings} to use
	 */
	public static JDBCConnector getInstance(Settings settings)
	throws
		IOException,
		ReflectiveOperationException
	{
		return
			(JDBCConnector)Class
			.forName(settings.getDatabaseConfiguration().getProperty("connector", settings.getDatabaseProduct()))
			.getConstructor(Settings.class)
			.newInstance(settings);
	}

	/**
	 * Gets a int from a query given a String using a {@link PreparedStatement}.
	 * Returns 0 if no results were returned.
	 */
	protected final int getIntQuery(String sql) throws SQLException, IOException {
		try (
			Connection conn = DatabasePool.getConnection(settings);
			PreparedStatement pstmt = conn.prepareStatement(sql);
			ResultSet results = pstmt.executeQuery()
		) {
			if(results.next()) return results.getInt(1);
			else return 0;
		}
	}

	/**
	 * Gets a int from a query given a String using a {@link PreparedStatement}.
	 * Returns 0 if no results were returned.
	 */
	protected final int getIntQuery(String sql, String param) throws SQLException, IOException {
		try (
			Connection conn = DatabasePool.getConnection(settings);
			PreparedStatement pstmt = conn.prepareStatement(sql)
		) {
			pstmt.setString(1, param);
			try (ResultSet results = pstmt.executeQuery()) {
				if(results.next()) return results.getInt(1);
				else return 0;
			}
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

	private static final List<String> possiblePrivileges = new ArrayList<>(6);
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
	 * the foreign rows if less than {@link Settings#fkeyrows} exist.
	 *
	 * @return the list of all possible values or {@code null} if not known
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
		List<String> columns=new ArrayList<>();
		List<String> names=new ArrayList<>();
		try (
			Connection conn = DatabasePool.getConnection(settings);
			ResultSet r = conn.getMetaData().getPrimaryKeys(null, null, settings.getTable())
		) {
			while(r.next()) {
				columns.add(r.getString(4));
				names.add(r.getString(6));
			}
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
		StringBuilder sb = new StringBuilder("SELECT * FROM ").append(quoteTable(settings.getTable()));
		for (int i = 0; i < primaryKeys.size(); i++) {
			sb.append(i == 0 ? " WHERE " : " AND ");
			if (primaryValues.get(i) == null) {
				appendIsNull(sb, primaryKeys.get(i));
			} else {
				sb.append(quoteColumn(primaryKeys.get(i))).append("=?");
			}
		}
		String sql = sb.toString();

		// Then perform the query
		try (
			Connection conn = DatabasePool.getConnection(settings);
			PreparedStatement pstmt = conn.prepareStatement(sql)
		) {
			// pstmt.setEscapeProcessing(false);
			int pos = 1;
			for (int i = 0; i < primaryKeys.size(); i++) {
				if (primaryValues.get(i) != null) {
					pstmt.setString(pos++, primaryValues.get(i));
				}
			}
			try (ResultSet results = pstmt.executeQuery()) {
				List<String> v = new ArrayList<>();
				if (results.next()) {
					int count = results.getMetaData().getColumnCount();
					for (int i = 1; i <= count; i++) {
						v.add(results.getString(i));
					}
				}
				return v;
			}
		}
	}

	/**
	 * Gets a description of the given foreign key rule.
	 *
	 * @param i the value of one of the {@link DatabaseMetaData} defined constants.
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
		StringBuilder sql=new StringBuilder();
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
						.append(Util.escapeSQLValue(colValues[i]));
				}
			}
		}
		return sql.toString();
	}

	/**
	 * Gets the {@link Settings} of this {@link JDBCConnector}.
	 */
	public final Settings getSettings() {
		return settings;
	}

	/**
	 * Gets the privileges for the current table.
	 */
	public TablePrivileges getTablePrivileges() throws SQLException, IOException {
		try (
			Connection conn = DatabasePool.getConnection(settings);
			ResultSet r = conn.getMetaData().getTablePrivileges(null, null, settings.getTable())
		) {
			List<String> grantors=new ArrayList<>();
			List<String> grantees=new ArrayList<>();
			List<String> privileges=new ArrayList<>();
			List<Boolean> isGrantable=new ArrayList<>();
			while(r.next()) {
				grantors.add(r.getString(4));
				grantees.add(r.getString(5));
				privileges.add(r.getString(6));
				String g = r.getString(7);
				isGrantable.add(("YES".equals(g))?Boolean.TRUE:("NO".equals(g))?Boolean.FALSE:Boolean.UNKNOWN);
			}
			return new TablePrivileges(
				grantors,
				grantees,
				privileges,
				isGrantable
			);
		}
	}

	/**
	 * Returns a {@link List} containing all the tables in the database.
	 */
	public List<String> getTables() throws SQLException, IOException {
		try (
			Connection conn = DatabasePool.getConnection(settings);
			ResultSet results = conn.getMetaData().getTables(null, null, "%", defaultTableTypes)
		) {
			List<String> v = new ArrayList<>();
			while(results.next()) {
				v.add(results.getString(3));
			}
			return v;
		}
	}

	/**
	 * Gets a list of types supported by the database.
	 */
	public List<String> getTypes() throws SQLException, IOException {
		try (
			Connection conn = DatabasePool.getConnection(settings);
			ResultSet r = conn.getMetaData().getTypeInfo()
		) {
			List<String> v = new ArrayList<>();
			while(r.next()) {
				v.add(r.getString(1));
			}
			return v;
		}
	}

	/**
	 * Gets the database server URL
	 */
	public String getURL() throws SQLException, IOException {
		try (Connection conn = DatabasePool.getConnection(settings)) {
			DatabaseMetaData metaData=conn.getMetaData();
			return metaData.getURL();
		}
	}

	/**
	 * Grants a user privileges on the current table.
	 *
	 * @param user        the user to grant the privileges to.
	 * @param privileges  an array of privileges which will be granted.
	 */
	public void grantPrivileges(String user, String[] privileges) throws SQLException, IOException {
		StringBuilder sql=new StringBuilder("GRANT ");
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
			.append(user);
		executeUpdate(sql.toString());
	}

	/**
	 * Inserts a new row into a table.
	 *
	 * @param column    an array of {@link String} objects representing the names of the columns.
	 * @param function  an array of {@link String} objects representing functions to execute.
	 * @param value     an array of {@link String} objects representing the values to insert.
	 */
	public void insertRow(String[] column, String[] function, String[] value) throws SQLException, IOException {
		// Build the SQL
		StringBuilder sb = new StringBuilder("INSERT INTO ").append(quoteTable(settings.getTable())).append(" (");
		for(int i = 0; i < column.length; i++) {
			if(i > 0) sb.append(", ");
			sb.append(quoteColumn(column[i]));
		}
		sb.append(") VALUES (");
		for(int i = 0; i < column.length; i++) {
			if(i > 0) sb.append(", ");
			if(function[i] != null && function[i].length() > 0) {
				sb.append(function[i]);
			} else {
				sb.append('?');
			}
		}
		sb.append(')');
		String sql = sb.toString();

		try (
			Connection conn = DatabasePool.getConnection(settings);
			PreparedStatement stmt = conn.prepareStatement(sql)
		) {
			// stmt.setEscapeProcessing(false);
			int pos=1;
			for(int i=0;i<column.length;i++) {
				if(function[i]==null || function[i].length()==0) stmt.setString(pos++, value[i]);
			}
			stmt.executeUpdate();
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
	 * @param user        the user to revoke the privileges from.
	 * @param privileges  an array of privileges which will be revoked.
	 */
	public void revokePrivileges(String user, String[] privileges) throws SQLException, IOException {
		StringBuilder sql=new StringBuilder("REVOKE ");
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
			.append(user);
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
	 * Checks if is a SQL keyword.
	 */
	public boolean isKeyword(String identifier) {
		return
			Server.ReservedWord.isReservedWord(identifier)
			|| com.aoindustries.aoserv.client.postgresql.Server.ReservedWord.isReservedWord(identifier);
	}

	/**
	 * By default, surrounds with '"' if is not all lower-case
	 * alphanumeric.
	 */
	// TODO: Throw exception on way-off special characters?
	protected final String defaultQuote(String pre, String doubleQuote, String post, String identifier) {
		if(identifier == null) throw new NullPointerException();
		int len = identifier.length();
		if(len == 0) return pre + post;
		StringBuilder quoted = new StringBuilder(pre.length() + len + post.length());
		quoted.append(pre);
		boolean quotesNeeded = isKeyword(identifier);
		for(int i = 0; i < len; i++) {
			char ch = identifier.charAt(i);
			if(
				(ch >= 'a' && ch <= 'z')
				|| ch == '_'
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
	 * @see  #defaultQuote(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public String quoteTable(String table) {
		return defaultQuote("\"", "\"\"", "\"", table);
	}

	/**
	 * Quotes an identifier used for a column name.
	 *
	 * @see  #defaultQuote(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public String quoteColumn(String column) {
		return defaultQuote("\"", "\"\"", "\"", column);
	}

	/**
	 * Quotes an identifier used for a type name.
	 *
	 * @see  #defaultQuote(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
	 */
	public String quoteType(String type) {
		return defaultQuote("\"", "\"\"", "\"", type);
	}
}
