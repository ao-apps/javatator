/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2018, 2019, 2021  AO Industries, Inc.
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
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The MySQL connection class. Implements things which the driver doesn't do using JDBC.
 */
public class MySQLConnector extends JDBCConnector {

	/**
	 * These types are stored in a static final variable for faster access.
	 */
	private static final List<String> modifiableTypes = new ArrayList<>(25);
	private static final List<String> unmodifiableTypes = Collections.unmodifiableList(modifiableTypes);
	static {
		modifiableTypes.add("TINYINT");
		modifiableTypes.add("SMALLINT");
		modifiableTypes.add("MEDIUMINT");
		modifiableTypes.add("INT");
		modifiableTypes.add("BIGINT");
		modifiableTypes.add("FLOAT");
		modifiableTypes.add("DOUBLE");
		modifiableTypes.add("DECIMAL");
		modifiableTypes.add("DATE");
		modifiableTypes.add("DATETIME");
		modifiableTypes.add("TIMESTAMP");
		modifiableTypes.add("TIME");
		modifiableTypes.add("YEAR");
		modifiableTypes.add("CHAR");
		modifiableTypes.add("VARCHAR");
		modifiableTypes.add("TINYTEXT");
		modifiableTypes.add("MEDIUMTEXT");
		modifiableTypes.add("TEXT");
		modifiableTypes.add("LONGTEXT");
		modifiableTypes.add("TINYBLOB");
		modifiableTypes.add("MEDIUMBLOB");
		modifiableTypes.add("BLOB");
		modifiableTypes.add("LONGBLOB");
		modifiableTypes.add("SET");
		modifiableTypes.add("ENUM");
	}

	/**
	 * Instantiate a new MySQLConnector.
	 */
	public MySQLConnector(Settings settings) {
		super(settings);
	}

	@Override
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
				String column = r.getString(4);
				names.add(column);
				String type = r.getString(6);
				types.add(type);
				if("ENUM".equalsIgnoreCase(type) || "SET".equalsIgnoreCase(type)) {
					List<String> v = getPossibleValues(column, type);
					int size = v.size();
					StringBuilder sb = new StringBuilder();
					for(int i = 0; i < size; i++) {
						if(i > 0) sb.append(',');
						sb
							.append('\'')
							.append(v.get(i))
							.append('\'');
					}
					lengths.add(sb.toString());
				} else lengths.add(r.getString(7));
				int nullable = r.getInt(11);
				areNullable.add(
					(nullable==DatabaseMetaData.columnNoNulls) ? Boolean.FALSE
						: (nullable==DatabaseMetaData.columnNullable) ? Boolean.TRUE
							: Boolean.UNKNOWN);
				String def = r.getString(13);
				int defLen = def.length();
				if(
					defLen >= 2
					&& def.charAt(0) == '\''
					&& def.charAt(defLen - 1) == '\''
				) {
					defaults.add('V' + def.substring(1, defLen - 1));
				} else if(defLen > 0) {
					defaults.add('V' + def);
				} else {
					defaults.add(null);
				}
				String rem = r.getString(12);
				remarks.add((rem != null) ? rem : "");
			}
		}
		return new Columns(names, types, lengths, areNullable, defaults, remarks);
	}

	/**
	 * Not defined in MySQL.
	 */
	public String getDeleteRule(String constraint) throws SQLException, IOException {
		return "";
	}

	/**
	 * Not defined in MySQL.
	 */
	public String getForeignKey(String constraint) throws SQLException, IOException {
		return "";
	}

	/**
	 * Not defined in MySQL.
	 */
	public String getInsertRule(String constraint) throws SQLException, IOException {
		return "";
	}

	/**
	 * Gets the limit for a specified part of a table.
	 *
	 * @param startPos the starting row number to read from.
	 * @param numRows the numbers of rows to read.
	 */
	@Override
	public String getLimitClause(int startPos, int numRows) throws SQLException, IOException {
		return new StringBuilder()
			.append("limit ")
			.append(startPos)
			.append(',')
			.append(numRows)
			.toString();
	}

	/**
	 * Gets the possible values for a column.  For an <code>ENUM</code> or <code>SET</code> type return all
	 * the possible values, for any other return {@code null}.
	 *
	 * @return the list of all possible values or {@code null} if not known
	 */
	@Override
	@SuppressWarnings("AssignmentToForLoopParameter")
	public List<String> getPossibleValues(String column, String type) throws SQLException, IOException {
		boolean enum0="ENUM".equalsIgnoreCase(type) || "SET".equalsIgnoreCase(type);
		if(enum0) {
			try (
				Connection conn = DatabasePool.getConnection(getSettings());
				PreparedStatement pstmt = conn.prepareStatement("SHOW COLUMNS FROM " + getSettings().getTable() + " LIKE ?")
			) {
				pstmt.setString(1, column);
				try (ResultSet results = pstmt.executeQuery()) {
					if(results.next()) {
						String s = results.getString(2);
						int len = s.length();
						if(enum0) {
							s = s.substring(6, len - 2);
							len -= 10;
						} else {
							s = s.substring(5, len - 2);
							len -= 9;
						}
						//Split up into tokens on ','
						List<String> v = new ArrayList<>();
						int start = 0;
						for(int i = 0; i < len; i++) {
							if(
								s.charAt(i) == '\''
								&& s.charAt(i + 1) == ','
								&& s.charAt(i + 2) == '\''
							) {
								v.add(Util.escapeMySQLQuotes(s.substring(start, i)));
								start = i + 3;
								i += 3;
							}
						}
						v.add(Util.escapeMySQLQuotes(s.substring(start)));
						return v;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Not defined in MySQL.
	 */
	@Override
	public String getPrimaryKey(String constraint) throws SQLException, IOException {
		return "";
	}

	/**
	 * Gets the WHERE clause for a SELECT query on the specified database.
	 *
	 * @param colNames the names of the columns to compare.
	 * @param colValues the values of the columns to compare.
	 */
	@Override
	public String getSelectWhereClause(String[] colNames, String[] colValues) throws SQLException, IOException {
		// Build the SQL
		StringBuilder sql=new StringBuilder();
		boolean hasBeen=false;
		for(int i=0;i<colNames.length;i++) {
			if(colNames[i].length()>0) {
				if(colValues[i]==null) {
					if(hasBeen) sql.append(" AND ");
					else hasBeen=true;
					sql
						.append(" ISNULL(")
						.append(colNames[i])
						.append(')');
				} else if(!"".equals(colValues[i])) {
					if(hasBeen) sql.append(" AND ");
					else hasBeen=true;
					sql
						.append(colNames[i])
						.append(" LIKE ")
						.append(Util.escapeSQLValue(colValues[i]));
				}
			}
		}
		return sql.toString();
	}

	/**
	 * Returns a {@link List} containing all the tables in the current database.
	 */
	@Override
	public List<String> getTables() throws SQLException, IOException {
		return executeListQuery("SHOW TABLES");
	}

	/**
	 * Gets a list of types supported by the database.
	 */
	@Override
	public List<String> getTypes() {
		return unmodifiableTypes;
	}

	/**
	 * Not defined in MySQL.
	 */
	public String getUpdateRule(String constraint) throws SQLException, IOException {
		return "";
	}

	@Override
	public void grantPrivileges(String user, String[] privileges) throws SQLException, IOException {
		super.grantPrivileges(user, privileges);
		executeUpdate("FLUSH PRIVILEGES");
	}

	/**
	 * Does not apply to MySQL.
	 */
	public Boolean isDeferrable(String constraint) throws SQLException, IOException {
		return Boolean.NA;
	}

	/**
	 * Does not apply to MySQL.
	 */
	public Boolean isInitiallyDeferred(String constraint) throws SQLException, IOException {
		return Boolean.NA;
	}

	/**
	 * Renames an existing table.
	 *
	 * @param newTable the new name for the table.
	 */
	@Override
	public void renameTable(String newTable) throws SQLException, IOException {
		executeUpdate("ALTER TABLE "+getSettings().getTable()+" RENAME "+newTable);
	}

	@Override
	public void revokePrivileges(String user, String[] privileges) throws SQLException, IOException {
		super.revokePrivileges(user, privileges);
		executeUpdate("FLUSH PRIVILEGES");
	}

	/**
	 * Does the database product support foreign keys?
	 */
	@Override
	public boolean supportsForeignKeys() {
		return false;
	}

	@Override
	public boolean isKeyword(String identifier) {
		return Server.ReservedWord.isReservedWord(identifier);
	}

	// TODO: How to scape ' in table name?
	@Override
	public String quoteTable(String table) {
		return defaultQuote("`", "\"", "'", table);
	}

	// TODO: How to scape ' in column name?
	@Override
	public String quoteColumn(String column) {
		return defaultQuote("`", "\"", "'", column);
	}
}
