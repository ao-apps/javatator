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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

/**
 * The Interbase connection class. Implements things which the driver doesn't do using JDBC.
 */
public class InterbaseConnector extends JDBCConnector {

	/**
	 * Instantiate a new InterbaseConnector.
	 */
	public InterbaseConnector(Settings settings) {
		super(settings);
	}

	/**
	 * Adds an index on the specified column.
	 *
	 * @param indexName the name of the index.
	 * @param column the name of the column.
	 */
	@Override
	public void addIndex(String indexName, String column) throws SQLException, IOException {
		executeUpdate("CREATE INDEX "+indexName+" ON "+getSettings().getTable()+" ("+column+')');
	}

	/**
	 * Adds a primary key to the specified table.
	 *
	 * @param column the name of the column to add as a primary key.
	 */
	@Override
	public void addPrimaryKey(String column) throws SQLException, IOException {
		String table=settings.getTable();

		Connection conn=DatabasePool.getConnection(getSettings());
		try {
			StringBuffer sql=new StringBuffer("ALTER TABLE ");
			StringBuffer cols=new StringBuffer();
			sql.append(table);
			ResultSet results=conn.getMetaData().getPrimaryKeys(null,null,table);
			try {
				if(results.next()) {
					sql
					.append(" DROP CONSTRAINT ")
					.append(results.getString(6))
					.append(", ")
					;
					cols.append(',').append(results.getString(4));
				}
			} finally {
				results.close();
			}
			sql
				.append(" ADD PRIMARY KEY(")
				.append(column)
				.append(cols)
				.append(')');
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
	 * Adds a unique index on the specified column.
	 *
	 * @param indexName the name of the index.
	 * @param column the name of the column.
	 */
	@Override
	public void addUniqueIndex(String indexName, String column) throws SQLException, IOException {
		executeUpdate("CREATE UNIQUE INDEX "+indexName+" ON "+getSettings().getTable()+" ("+column+')');
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
	@Override
	public void createTable(
		String[] newColumn,
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
		sql.append("CREATE TABLE ").append(settings.getTable()).append(" (");
		for(int i=0;i<newColumn.length;i++) {
			if(i>0) sql.append(", ");
			sql.append(newColumn[i]).append(' ').append(newType[i]);
			if(newLength[i].length()>0) sql.append('(').append(newLength[i]).append(')');
			sql.append(' ').append(newNull[i]);
			if(uniqueKey[i]) sql.append(" UNIQUE");
			if(newDefault[i].length()>0) sql.append(" DEFAULT ").append(Util.escapeSQLValue(newDefault[i]));
			if(primaryKey[i]) sql.append(", PRIMARY KEY (").append(newColumn[i]).append(')');
		}
		sql.append(" )");

		// Execute the update next
		executeUpdate(sql.toString());

		// Check for indexes to add
		for(int i=0;i<newColumn.length;i++) {
			if(indexKey[i]) addIndex(newColumn[i], newColumn[i]);
		}
	}

	/**
	 * Drops the current database.
	 */
	@Override
	public void dropDatabase() throws SQLException, IOException {
		executeUpdate("DROP DATABASE");
	}

	/**
	 * Drops the specified index from the table.
	 *
	 * @param indexName the name of the index to drop.
	 */
	@Override
	public void dropIndex(String indexName) throws SQLException, IOException {
		executeUpdate("DROP INDEX "+indexName);
	}

	/**
	 * Deletes a primary key entry.
	 *
	 * @param column the name of the column to add as a primary key.
	 */
	@Override
	public void dropPrimaryKey(String column) throws SQLException, IOException {
		PrimaryKeys primaryKeys=getPrimaryKeys();
		List<String> columns=primaryKeys.getColumns();
		int i=columns.indexOf(column);
		if(i>-1) {
			StringBuffer sql=new StringBuffer("ALTER TABLE ");
			sql
				.append(settings.getTable())
				.append(" DROP CONSTRAINT ")
				.append(primaryKeys.getNames().get(i));
			int size=columns.size();
			if(size>1) {
				sql.append(", ADD PRIMARY KEY(");
				boolean hasBeen=false;
				for(int c=0;c<size;c++) {
					if(c!=i) {
						if(hasBeen) sql.append(',');
						else hasBeen=true;
						sql.append(columns.get(c));
					}
				}
				sql.append(')');
			}
			executeUpdate(sql.toString());
		} else throw new SQLException("The column "+column+" does not appear to be a primary key.");
	}

	/**
	 * @see JDBCConnector#editColumn
	 *
	 * It appears that Interbase can only change the column name and the column type.
	 * Therefore the length, default, null, etc. parameters will be ignored.
	 */
	@Override
	public void editColumn(String column,
		String newColumn,
		String newType,
		String newLength,
		String newDefault,
		String newNull,
		String newRemarks
	) throws SQLException, IOException {
		StringBuffer sql=new StringBuffer();
		sql
			.append("ALTER TABLE ")
			.append(settings.getTable());
		if(!column.equals(newColumn)) {
			sql
				.append(" ALTER COLUMN ")
				.append(column)
				.append(" TO ")
				.append(newColumn)
				.append(',');
		}
		sql
			.append(" ALTER COLUMN ")
			.append(newColumn)
			.append(" TYPE ")
			.append(newType);
		executeUpdate(sql.toString());
	}

	/**
	 * Gets a list of all the unique SQL functions supported by this database.
	 */
	@Override
	public List<String> getFunctionList() throws SQLException, IOException {
		//return executeListQuery("SELECT p.proname as Function FROM pg_proc p, pg_type t WHERE p.prorettype = t.oid and (pronargs = 0 or oidvectortypes(p.proargtypes) != '') GROUP BY Function ORDER BY Function");
		return Collections.emptyList();
	}

	/**
	 * Gets the functions that may return the provided type.
	 */
	@Override
	public List<String> getFunctionList(String type) throws SQLException, IOException {
		//return executeListQuery("SELECT p.proname as Function FROM pg_proc p, pg_type t WHERE p.prorettype = t.oid and t.typname=lower(?) and (pronargs = 0 or oidvectortypes(p.proargtypes) != '') GROUP BY Function ORDER BY Function", type);
		return Collections.emptyList();
	}

	/**
	 * Gets a SELECT query on the specified database.
	 *
	 * @param selectCols the names of the columns to retrieve.
	 * @param colNames the names of the columns to compare.
	 * @param colValues the values of the columns to compare.
	 */
	public String getSelectQuery(
		List<String> selectCols,
		List<String> colNames,
		List<String> colValues
	) throws SQLException, IOException {
		// Build the SQL
		StringBuffer sql=new StringBuffer("SELECT ");
		for(int i=0;i<selectCols.size();i++) {
			if(i>0) sql.append(',');
			sql.append(selectCols.get(i));
		}
		sql
			.append(" FROM ")
			.append(getSettings().getTable());
		boolean hasBeen=false;
		for(int i=0;i<colNames.size();i++) {
			if(colNames.get(i).length()>0) {
				if(hasBeen) sql.append(" AND ");
				else {
					sql.append(" WHERE ");
					hasBeen=true;
				}
				if(colValues.get(i)==null) {
					sql
					.append(colNames.get(i))
					.append(" ISNULL");
				} else {
					sql
					.append(colNames.get(i))
					.append(" LIKE ?");
				}
			}
		}
		String S=sql.toString();

		Connection conn=DatabasePool.getConnection(getSettings());
		try {
			PreparedStatement stmt=conn.prepareStatement(S);
			try {
				stmt.setEscapeProcessing(false);
				int num=1;
				for(int i=0;i<colValues.size();i++) {
					if(colNames.get(i).length()>0 && colValues.get(i)!=null) {
						stmt.setString(num++, colValues.get(i));
					}
				}
				return stmt.toString();
			} finally {
				stmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Renames an existing table. Not supported by Interbase.
	 *
	 * @param newTable the new name for the table.
	 */
	@Override
	public void renameTable(String newTable) throws SQLException, IOException {
		throw new SQLException("Sorry, Interbase does not currently support renaming tables.");
	}

	@Override
	public boolean isKeyword(String identifier) {
		return false;
	}

	@Override
	public String quoteTable(String table) {
		return table;
	}

	@Override
	public String quoteColumn(String column) {
		return column;
	}
}
