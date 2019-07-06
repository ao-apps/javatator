/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2009, 2015, 2017, 2018, 2019  AO Industries, Inc.
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

import com.aoindustries.aoserv.client.postgresql.Server;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The PostgreSQL connection class. Implements things which the driver doesn't do using JDBC.
 */
public class PgSQLConnector extends JDBCConnector {

	/**
	 * Instantiate a new PgSQLConnector.
	 */
	public PgSQLConnector(Settings settings) {
		super(settings);
	}

	/**
	 * Adds a new CHECK constraint to this table.
	 */
	@Override
	public void addCheckConstraint(String constraint, String checkClause) throws SQLException, IOException {
		throw new SQLException("PostgreSQL doesn't support ALTER TABLE table ADD CONSTRAINT name CHECK (...) at the moment.");
	}

	/**
	 * Adds an index on the specified column.
	 *
	 * @param indexName the name of the index.
	 * @param column the name of the column.
	 */
	@Override
	public void addIndex(String indexName, String column) throws SQLException, IOException {
		executeUpdate("CREATE INDEX " + indexName + " ON " + quoteTable(getSettings().getTable()) + " (" + quoteColumn(column) + ')');
	}

	/**
	 * Adds a primary key to the specified table.
	 *
	 * @param column the name of the column to add as a primary key.
	 */
	@Override
	public void addPrimaryKey(String column) throws SQLException, IOException {
		throw new SQLException("You cannot easily add new primary keys in PostgreSQL.");
	}

	/**
	 * Adds a unique index on the specified column.
	 *
	 * @param indexName the name of the index.
	 * @param column the name of the column.
	 */
	@Override
	public void addUniqueIndex(String indexName, String column) throws SQLException, IOException {
		executeUpdate("CREATE UNIQUE INDEX " + indexName + " ON " + quoteTable(getSettings().getTable()) + " (" + quoteColumn(column) + ')');
	}

	@Override
	protected void appendIsNull(StringBuffer SB, String column) {
		SB.append(quoteColumn(column)).append(" IS NULL");
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
		sql.append("CREATE TABLE ").append(quoteTable(settings.getTable())).append(" (");
		for(int i=0;i<newColumn.length;i++) {
			if(i>0) sql.append(", ");
			sql.append(quoteColumn(newColumn[i])).append(' ').append(newType[i]);
			if(newLength[i].length()>0) sql.append('(').append(newLength[i]).append(')');
			sql.append(' ').append(newNull[i]);
			if(uniqueKey[i]) sql.append(" UNIQUE");
			if(newDefault[i].length()>0) sql.append(" DEFAULT ").append(Util.escapeSQLValue(newDefault[i]));
			if(primaryKey[i]) sql.append(", PRIMARY KEY (").append(quoteColumn(newColumn[i])).append(')');
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
	 * Deletes the specified column.
	 *
	 * @param column the name of the column to delete.
	 */
	@Override
	public void deleteColumn(String column) throws SQLException, IOException {
		throw new SQLException(
			"Sorry, DROP column is not supported yet. "
			+ "It is not easy in PostgreSQL because you lose all your triggers. "
			+ "You have to copy the table into a temporary table, create the new table without the column, "
			+ "then copy the data back into the new table."
		);
		/**
		 * Connection conn=DatabasePool.getConnection(databaseProduct, username, password, url, database);
		 * try {
		 *   conn.setCatalog(database);
		 *   PreparedStatement pstmt=conn.prepareStatement("CREATE TEMPORARY TABLE "+quoteTable(table+"_")+" AS SELECT * FROM "+quoteTable(table)+";"
		 *						  + "DROP TABLE "+quoteTable(table)+";"
		 *						  + "CREATE TABLE "+quoteTable(table)
		 *						  );
		 *   try {
		 *	pstmt.executeUpdate();
		 *   } finally {
		 *	pstmt.close();
		 *   }
		 * } finally {
		 *    DatabasePool.releaseConnection(conn);
		 * }
		 */
	}

	/**
	 * Drops the database. Note: for PostgreSQL we cannot be connected to the database to be dropped. 
	 */
	@Override
	public void dropDatabase() throws SQLException, IOException {
		System.out.println("closing database: "+settings.getDatabase());
		DatabasePool.closeDatabase(settings);
		String sql="DROP DATABASE "+settings.getDatabase();
		Connection conn=DatabasePool.getConnection(settings.setDatabase("template1"));
		try {
			Statement stmt=conn.createStatement();
			try {
				stmt.executeUpdate(sql);
			} finally {
				stmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Drops the specified index from the table.
	 *
	 * @param indexName the name of the index to drop.
	 */
	@Override
	public void dropIndex(String indexName) throws SQLException, IOException {
		// TODO: Quote this and lots more uses of names
		executeUpdate("DROP INDEX " + indexName);
	}

	/**
	 * Deletes a primary key entry.
	 *
	 * @param column the name of the column to add as a primary key.
	 */
	@Override
	public void dropPrimaryKey(String column) throws SQLException, IOException {
		throw new SQLException("PostgreSQL cannot drop primary keys easily at the moment, it's not as easy as it looks :)");
	}

	/**
	 * @see JDBCConnector#editColumn
	 */
	@Override
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
		if(!column.equals(newColumn)) {
			sql
				.append("ALTER TABLE ")
				.append(quoteTable(settings.getTable()))
				.append(" RENAME COLUMN ")
				.append(quoteColumn(column))
				.append(" TO ")
				.append(quoteColumn(newColumn))
				.append(';');
		}
		sql
			.append("ALTER TABLE ")
			.append(quoteTable(settings.getTable()))
			.append(" ALTER COLUMN ")
			.append(quoteColumn(newColumn));
		if(newDefault==null) {
			sql.append(" DROP DEFAULT");
		} else {
			sql.append(" SET DEFAULT ");
			if(newDefault.charAt(0)=='F') sql.append(newDefault.substring(1));
			else sql.append(Util.escapeSQLValue(newDefault.substring(1)));
		}
		executeUpdate(sql.toString());
	}

	@Override
	public CheckConstraints getCheckConstraints() throws SQLException, IOException {
		String table=getSettings().getTable();
		List<String> names=new ArrayList<String>();
		List<String> checkClauses=new ArrayList<String>();
		Connection conn=DatabasePool.getConnection(getSettings());
		try {
			DatabaseMetaData metaData=conn.getMetaData();
			String version = metaData.getDatabaseProductVersion();
			Statement stmt=conn.createStatement();
			try {
				ResultSet R;
				if(version.startsWith("7.")) {
					R=stmt.executeQuery(
						"SELECT\n"
						+ "  rcname,\n"
						+ "  rcsrc\n"
						+ "FROM\n"
						+ "  pg_relcheck r,\n"
						+ "  pg_class c\n"
						+ "WHERE\n"
						// TODO: PreparedStatement
						+ "  c.relname='"+table+"'\n"
						+ "  AND c.oid=r.rcrelid"
					);
				} else if(
					version.startsWith("8.")
					|| version.startsWith("9.")
					// TODO: Assume all versions other than 7. here?
				) {
					R=stmt.executeQuery(
						"SELECT\n"
						+ "  co.conname,\n"
						+ "  co.consrc\n"
						+ "FROM\n"
						+ "  pg_catalog.pg_class cl\n"
						+ "  inner join pg_catalog.pg_constraint co on cl.oid=co.conrelid\n"
						+ "WHERE\n"
						// TODO: PreparedStatement
						+ "  cl.relname='"+table+"'\n"
						+ "  and co.contype = 'c'"
					);
				} else throw new SQLException("Unsupported version: "+version);
				try {
					while(R.next()) {
						names.add(R.getString(1));
						checkClauses.add(R.getString(2));
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
		return new CheckConstraints(names, checkClauses);
	}

	@Override
	protected Columns getColumns(String table) throws SQLException, IOException {
		List<String> names=new ArrayList<String>();
		List<String> types=new ArrayList<String>();
		List<String> lengths=new ArrayList<String>();
		List<Boolean> areNullable=new ArrayList<Boolean>();
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
					areNullable.add(
						(nullable==DatabaseMetaData.columnNoNulls) ? Boolean.FALSE
						: (nullable==DatabaseMetaData.columnNullable) ? Boolean.TRUE
						: Boolean.UNKNOWN
					);
					String rem=R.getString(12);
					remarks.add((rem!=null)?rem:"");
				}
			} finally {
				R.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
		List<String> defaults=getDefaults(names);
		return new Columns(names, types, lengths, areNullable, defaults, remarks);
	}

	/**
	 * Gets the corresponding foreign key constraint name for the specified column name.
	 *
	 * @param column the name of the column.
	 */
	private String getConstraintName(String column) throws SQLException, IOException {
		String table=getSettings().getTable();

		Connection conn=DatabasePool.getConnection(getSettings());
		try {
			DatabaseMetaData metaData=conn.getMetaData();
			String version = metaData.getDatabaseProductVersion();
			if(version.startsWith("7.")) {
				Statement stmt=conn.createStatement();
				try {
					ResultSet R=stmt.executeQuery("select tgargs from pg_trigger");
					try {
						while(R.next()) {
							String S=R.getString(1);
							int pos=S.indexOf("\\000");
							if(pos>-1) {
								String constraintName=S.substring(0, pos);
								int pos2=S.indexOf("\\000", pos+4);
								String localTable=S.substring(pos+4, pos2);
								if(table.equals(localTable)) {
									pos=S.indexOf("\\000", pos2+4);
									pos2=S.indexOf("\\000", pos+4);
									pos=S.indexOf("\\000", pos2+4);
									String localColumn=S.substring(pos2+4, pos);
									if(localColumn.equals(column)) return constraintName;
								}
							}
						}
						return null;
					} finally {
						R.close();
					}
				} finally {
					stmt.close();
				}
			} else if(
				version.startsWith("8.")
				|| version.startsWith("9.")
			) {
				ForeignKeys foreignKeys = getForeignKeys(table, true);
				if(foreignKeys!=null) {
					for(int c=0;c<foreignKeys.getSize();c++) {
						if(column.equals(foreignKeys.getForeignKey(c))) return foreignKeys.getConstraintName(c);
					}
				}
				return null;
			} else throw new SQLException("Unsupported version: "+version);
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Gets a list of all databases on the server.
	 */
	@Override
	public List<String> getDatabases() throws SQLException, IOException {
		return executeListQuery("SELECT datname FROM pg_database ORDER BY datname");
	}

	/**
	 * Gets the default value or function for each column in the table.
	 *
	 * @return  the default values or functions for the columns.  A value will
	 *          start with a <code>'V'</code> and a function will start
	 *          with a <code>'F'</code>.
	 */
	private List<String> getDefaults(List<String> columns) throws SQLException, IOException {
		// Fetch the value from the database, release the connection, then
		// parse to obtain the result.  This minimizes the amount of time
		// the database resource is locked.

		List<String> defaults=new ArrayList<String>();
		List<String> colNames=new ArrayList<String>();
		Connection conn=DatabasePool.getConnection(getSettings());
		try {
			PreparedStatement pstmt=conn.prepareStatement(
				"SELECT d.adsrc, a.attname"
				+ " FROM pg_attrdef d, pg_class c, pg_attribute a"
				+ " WHERE c.relname = ?"
				+ "   AND c.oid = d.adrelid"
				+ "   AND a.attrelid = c.oid"
				+ "   AND a.attnum > 0"
				+ "   AND d.adnum = a.attnum"
				+ " ORDER BY a.attnum"
			);
			try {
				pstmt.setString(1, getSettings().getTable());
				ResultSet results=pstmt.executeQuery();
				try {
					while(results.next()) {
						defaults.add(results.getString(1));
						colNames.add(results.getString(2));
					}
				} finally {
					results.close();
				}
			} finally {
				pstmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}

		int size=defaults.size();
		int len=columns.size();
		List<String> out=new ArrayList<String>(len);
		for(int c=0;c<len;c++) out.add(null);
		int i=0;
		for(int c=0;c<size;c++) {
			String col=colNames.get(c);
			for(i=0;i<len;i++) if(col.equals(columns.get(i))) break;
			String def=defaults.get(c);
			// Look for null
			if(def==null) {
				out.set(i, null);
			} else {
				// Look for boolean value
				int defLen=def.length();
				if(
				   defLen==9
				   && def.charAt(0)=='\''
				   && def.endsWith("'::bool")
				) {
					char ch=def.charAt(1);
					if(ch=='t') out.set(i, "Vtrue");
					else if(ch=='f') out.set(i, "Vfalse");
					else throw new SQLException("Unknown default value for bool type: "+def);
				}
				// Look for a String constant
				else if(
					defLen>=2
					&& def.charAt(0)=='\''
					&& def.charAt(defLen-1)=='\''
				) {
					out.set(i, 'V'+def.substring(1, defLen-1));
				} else {
					// Look for a numerical constant
					boolean isNumber=true;
					for(int d=0;d<defLen;d++) {
						char ch=def.charAt(d);
						if(
						   (ch<'0' || ch>'9')
						   && ch!='-'
						   && ch!='.'
						   && ch!='e'
						   && ch!='E'
						   && ch!='+'
						) {
							isNumber=false;
							break;
						}
					}
					if(isNumber) out.set(i, 'V'+def);

					// Otherwise assume it is a function
					else out.set(i, 'F'+def);
				}
			}
		}
		if(columns.size()!=out.size()) throw new AssertionError();
		// Trim any trailing ::text from the defaults
		for(int c=0;c<out.size();c++) {
			String def = out.get(c);
			if(def!=null && def.startsWith("F'") && def.endsWith("'::text")) {
				out.set(c, "V"+def.substring(2, def.length()-7));
			}
		}
		return out;
	}

	/**
	 * Gets the ON DELETE rule for the specified constraint.
	 *
	 * @param constraint the constraint name.
	 */
	public String getDeleteRule(String constraint) throws SQLException, IOException {
		String table=getSettings().getTable();

		Connection conn=DatabasePool.getConnection(getSettings());
		try {
			Statement stmt=conn.createStatement();
			try {
				ResultSet R=stmt.executeQuery("select tgname, tgargs, proname from pg_proc, pg_trigger where tgfoid = pg_proc.oid and proname like 'RI_FKey_%_del'");
				try {
					while(R.next()) {
						String S=R.getString(2);
						int pos=S.indexOf("\\000");
						if(pos>-1) {
							String tmp=S.substring(0,pos);
							if(constraint.equals(tmp)) {
							int pos2=S.indexOf("\\000",pos+1);
								if(pos2>-1) {
									if(table.equals(S.substring(pos+4,pos2))) {
										String rule=R.getString(3);
										return rule.substring(8,rule.length()-4);
									}
								}
							}
						}
					}
					return "";
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
	 * Gets the foreign key description for the given constraint name.
	 *
	 * @param constraint the constraint name.
	 */
	public String getForeignKey(String constraint) throws SQLException, IOException {
		String table = getSettings().getTable();
		Connection conn = DatabasePool.getConnection(getSettings());
		try {
			DatabaseMetaData metaData=conn.getMetaData();
			String version = metaData.getDatabaseProductVersion();
			if(version.startsWith("7.")) {
				Statement stmt = conn.createStatement();
				try {
					ResultSet R = stmt.executeQuery("select tgargs from pg_trigger");
					try {
						while (R.next()) {
							String S = R.getString(1);
							int pos = S.indexOf("\\000");
							if (pos > -1) {
								String tmp = S.substring(0, pos);
								if (constraint.equals(tmp)) {
									int pos2 = S.indexOf("\\000", pos + 1);
									if (pos2 > -1) {
										if (table.equals(S.substring(pos + 4, pos2))) {
											pos = S.indexOf("\\000", pos2 + 1);
											String foreign_table = "";
											if (pos > -1) {
												foreign_table = S.substring(pos2 + 4, pos);
												pos = S.indexOf("\\000", pos + 1);
											}
											if (pos > -1) {
												pos = S.indexOf("\\000", pos + 1);
											}
											if (pos > -1) {
												pos2 = S.indexOf("\\000", pos + 1);
												if (pos2 > -1) {
													return foreign_table + "." + S.substring(pos + 4, pos2);
												}
											}
										}
									}
								}
							}
						}
						return "";
					} finally {
						R.close();
					}
				} finally {
					stmt.close();
				}
			} else if(
				version.startsWith("8.")
				|| version.startsWith("9.")
			) {
				ForeignKeys foreignKeys = getForeignKeys(table, true);
				for(int c=0;c<foreignKeys.getSize();c++) {
					if(constraint.equals(foreignKeys.getConstraintName(c))) {
						return foreignKeys.getPrimaryTable(c)+"."+foreignKeys.getPrimaryKey(c);
					}
				}
				return "";
			} else throw new SQLException("Unsupported version: "+version);
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Gets the foreign key data for the current table.
	 *
	 * @param isImported only get the imported keys?
	 */
	@Override
	protected ForeignKeys getForeignKeys(String table, boolean isImported) throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(getSettings());
		try {
			List<String> foreignKeys=new ArrayList<String>();
			List<String> foreignTables=new ArrayList<String>();
			List<String> primaryKeys=new ArrayList<String>();
			List<String> primaryTables=new ArrayList<String>();
			List<String> constraintNames=new ArrayList<String>();
			List<String> insertRules=new ArrayList<String>();
			List<String> deleteRules=new ArrayList<String>();
			List<String> updateRules=new ArrayList<String>();
			List<Boolean> isDeferrable=new ArrayList<Boolean>();
			List<Boolean> isInitiallyDeferred=new ArrayList<Boolean>();

			DatabaseMetaData metaData=conn.getMetaData();
			String version = metaData.getDatabaseProductVersion();
			if(version.startsWith("7.")) {
				Statement stmt=conn.createStatement();
				try {
					ResultSet R=stmt.executeQuery(
						"SELECT tgargs, "
						+ "CASE WHEN proname LIKE 'RI_FKey_%' "
						+ "THEN substring(proname from 9 for (char_length(proname)-12)) END, "
						+ "tgdeferrable, tginitdeferred "
						+ "FROM pg_proc, pg_trigger WHERE tgfoid = pg_proc.oid ORDER BY tgname"
					);
					try {
						while(R.next()) {
							String S=R.getString(1);
							int pos=S.indexOf("\\000");
							if(pos>-1) {
								String constraintName=S.substring(0, pos);
								int pos2=S.indexOf("\\000",pos+1);
								if(pos2>-1) {
									String primaryTable=S.substring(pos+4,pos2);
									if(!isImported || table.equals(primaryTable)) {
										pos=S.indexOf("\\000",pos2+1);
										if(pos>-1) {
											String foreignTable=S.substring(pos2+4,pos);
											if(isImported || table.equals(foreignTable)) {
												pos=S.indexOf("\\000",pos+1);
												if(pos>-1) {
													pos2=S.indexOf("\\000",pos+1);
													if(pos2>-1) {
														String primaryKey=S.substring(pos+4,pos2);
														pos=S.indexOf("\\000",pos2+1);
														if(pos>-1) {
															constraintNames.add(constraintName);
															foreignTables.add(foreignTable);
															primaryTables.add(primaryTable);
															primaryKeys.add(primaryKey);
															foreignKeys.add(S.substring(pos2+4, pos));
															isDeferrable.add(R.getBoolean(3)?Boolean.TRUE:Boolean.FALSE);
															isInitiallyDeferred.add(R.getBoolean(4)?Boolean.TRUE:Boolean.FALSE);
															insertRules.add(R.getString(2));
															if(R.next()) deleteRules.add(R.getString(2));
															if(R.next()) updateRules.add(R.getString(2));
														}
													}
												}
											}
										}
									}
								}
							}
						}
					} finally {
						R.close();
					}
				} finally {
					stmt.close();
				}
			} else if(
				version.startsWith("8.")
				|| version.startsWith("9.")
			) {
				List<Long> foreignTableOids = new ArrayList<Long>();
				List<List<Short>> foreignKeyAttNums = new ArrayList<List<Short>>();
				List<Long> primaryTableOids = new ArrayList<Long>();
				List<List<Short>> primaryKeyAttNums = new ArrayList<List<Short>>();
				PreparedStatement pstmt = conn.prepareStatement(
					"SELECT\n"
					+ "  co.conname,\n"
					+ "  ft.oid as primary_table_oid,\n"
					+ "  ft.relname as primary_table,\n"
					+ "  co.confkey,\n"
					+ "  cl.oid as foreign_table_oid,\n"
					+ "  cl.relname as foreign_table,\n"
					+ "  co.conkey,\n"
					+ "  co.confmatchtype as insert_rule,\n"
					+ "  co.confdeltype as delete_rule,\n"
					+ "  co.confupdtype as update_rule,\n"
					+ "  co.condeferrable,\n"
					+ "  co.condeferred\n"
					+ "FROM\n"
					+ "  pg_catalog.pg_class cl\n"
					+ "  inner join pg_catalog.pg_constraint co on cl.oid=co.conrelid\n"
					+ "  inner join pg_class ft on co.confrelid=ft.oid\n"
					+ "WHERE\n"
					+ "  "+(isImported ? "cl" : "ft")+".relname=?\n"
					+ "  and co.contype='f'\n"
					+ "ORDER BY\n"
					+ "  cl.relname,\n"
					+ "  co.conname"
				);
				try {
					pstmt.setString(1, table);
					ResultSet results = pstmt.executeQuery();
					while(results.next()) {
						constraintNames.add(results.getString("conname"));
						// Foreign keys
						foreignTableOids.add(results.getLong("foreign_table_oid"));
						foreignKeyAttNums.add(Arrays.asList((Short[])results.getArray("conkey").getArray()));
						foreignTables.add(results.getString("foreign_table"));
						// Primary keys
						primaryTableOids.add(results.getLong("primary_table_oid"));
						primaryKeyAttNums.add(Arrays.asList((Short[])results.getArray("confkey").getArray()));
						primaryTables.add(results.getString("primary_table"));
						// Rules and the rest
						insertRules.add(getMatchRule(results.getString("insert_rule")));
						deleteRules.add(getActionRule(results.getString("delete_rule")));
						updateRules.add(getActionRule(results.getString("update_rule")));
						isDeferrable.add(results.getBoolean("condeferrable") ? Boolean.TRUE : Boolean.FALSE);
						isInitiallyDeferred.add(results.getBoolean("condeferred") ? Boolean.TRUE : Boolean.FALSE);
					}
				} finally {
					pstmt.close();
				}
				for(int c=0;c<foreignKeyAttNums.size();c++) {
					// Foreign keys
					List<Short> foreignKeyAttNum = foreignKeyAttNums.get(c);
					if(foreignKeyAttNum.size()!=1) throw new SQLException("Only single-column foreign keys currently supported");
					foreignKeys.add(getColumnName(conn, foreignTableOids.get(c), foreignKeyAttNum.get(0)));
					// Primary keys
					List<Short> primaryKeyAttNum = primaryKeyAttNums.get(c);
					if(primaryKeyAttNum.size()!=1) throw new SQLException("Only single-column primary keys currently supported");
					primaryKeys.add(getColumnName(conn, primaryTableOids.get(c), primaryKeyAttNum.get(0)));
				}
			} else throw new SQLException("Unsupported version: "+version);
			int size=constraintNames.size();
			if(size<1) {
				return null;
			} else {
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
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	private static String getColumnName(Connection conn, long tableOid, int attNum) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement("SELECT attname FROM pg_catalog.pg_attribute WHERE attrelid=? AND attnum=?");
		try {
			pstmt.setLong(1, tableOid);
			pstmt.setInt(2, attNum);
			ResultSet result = pstmt.executeQuery();
			try {
				if(!result.next()) throw new SQLException("No row returned");
				String name = result.getString(1);
				if(result.next()) throw new SQLException("More than one row returned");
				return name;
			} finally {
				result.close();
			}
		} finally {
			pstmt.close();
		}
	}

	/**
	 * From parsenodes.h
	 *
	 * FKCONSTR_ACTION_NOACTION        'a'
	 * FKCONSTR_ACTION_RESTRICT        'r'
	 * FKCONSTR_ACTION_CASCADE         'c'
	 * FKCONSTR_ACTION_SETNULL         'n'
	 * FKCONSTR_ACTION_SETDEFAULT      'd'
	 */
	private static String getActionRule(String rule) {
		if("a".equals(rule)) return "no action";
		if("r".equals(rule)) return "restrict";
		if("c".equals(rule)) return "cascade";
		if("n".equals(rule)) return "set null";
		if("d".equals(rule)) return "set default";
		return rule;
	}

	/**
	 * From parsenodes.h
	 *
	 * FKCONSTR_MATCH_FULL             'f'
	 * FKCONSTR_MATCH_PARTIAL          'p'
	 * FKCONSTR_MATCH_UNSPECIFIED      'u'
	 */
	private static String getMatchRule(String rule) {
		if("f".equals(rule)) return "full";
		if("p".equals(rule)) return "partial";
		if("u".equals(rule)) return "unspecified";
		return rule;
	}

	/**
	 * Gets a list of all the unique SQL functions supported by this database.
	 */
	@Override
	public List<String> getFunctionList() throws SQLException, IOException {
		return executeListQuery("SELECT p.proname as Function FROM pg_proc p, pg_type t WHERE p.prorettype = t.oid and (pronargs = 0 or oidvectortypes(p.proargtypes) != '') GROUP BY Function ORDER BY Function");
	}

	/**
	 * Gets the functions that may return the provided type.
	 */
	@Override
	public List<String> getFunctionList(String type) throws SQLException, IOException {
		return executeListQuery("SELECT p.proname as Function FROM pg_proc p, pg_type t WHERE p.prorettype = t.oid and t.typname=lower(?) and (pronargs = 0 or oidvectortypes(p.proargtypes) != '') GROUP BY Function ORDER BY Function", type);
	}

	/**
	 * Gets a list of indexes for the selected table.
	 */
	@Override
	public Indexes getIndexes() throws SQLException, IOException {
		List<String> names=new ArrayList<String>();
		List<String> columns=new ArrayList<String>();
		List<Boolean> areUnique=new ArrayList<Boolean>();
		Connection conn=DatabasePool.getConnection(settings);
		try {
			Statement stmt=conn.createStatement();
			try {
				ResultSet R=stmt.executeQuery(
					"SELECT ic.relname as PK_NAME, "
					+ " a.attname AS COLUMN_NAME,"
					+ " i.indisunique as UNIQUE_KEY"
					+ " FROM pg_class bc, pg_class ic, pg_index i,"
					+ "  pg_attribute a, pg_attribute ta"
					+ " WHERE"
					+ "   bc.relkind = 'r'"
					+ "   AND UPPER(bc.relname)=UPPER('"
				  // TODO: PreparedStatement
					+ getSettings().getTable()+"')"
					+ "   AND i.indrelid = bc.oid"
					+ "   AND i.indexrelid = ic.oid"
					+ "   AND ic.oid = a.attrelid"
					+ "   AND a.attrelid = i.indexrelid"
					+ "   AND ta.attrelid = i.indrelid"
					+ "   AND ta.attnum = i.indkey[a.attnum-1]"
					+ " ORDER BY pk_name"
				);
				try {
					while(R.next()) {
						names.add(R.getString(1));
						columns.add(R.getString(2));
						areUnique.add(R.getBoolean(3)?Boolean.TRUE:Boolean.FALSE);
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
		return new Indexes(names, areUnique, columns);
	}

	/**
	 * Gets the ON INSERT foreign key check rule for the specified constraint.
	 *
	 * @param constraint the constraint name.
	 */
	public String getInsertRule(String constraint) throws SQLException, IOException {
		String table=getSettings().getTable();

		Connection conn=DatabasePool.getConnection(getSettings());
		try {
			Statement stmt=conn.createStatement();
			try {
				ResultSet R=stmt.executeQuery("select tgname, tgargs, proname from pg_proc, pg_trigger where tgfoid = pg_proc.oid and proname like 'RI_FKey_%_ins'");
				try {
					while(R.next()) {
						String S=R.getString(2);
						int pos=S.indexOf("\\000");
						if(pos>-1) {
							String tmp=S.substring(0,pos);
							if(constraint.equals(tmp)) {
								int pos2=S.indexOf("\\000",pos+1);
								if(pos2>-1) {
									if(table.equals(S.substring(pos+4,pos2))) {
										String rule=R.getString(3);
										return rule.substring(8,rule.length()-4);
									}
								}
							}
						}
					}
					return "";
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
	 * Gets the limit for a specified part of a table.
	 *
	 * @param startPos the starting row number to read from.
	 * @param numRows the numbers of rows to read.
	 */
	@Override
	public String getLimitClause(int startPos, int numRows) throws SQLException, IOException {
		return new StringBuffer()
			.append("limit ")
			.append(numRows)
			.append(" offset ")
			.append(startPos)
			.toString();
	}

	private static final List<String> possiblePrivileges=new ArrayList<String>(4);
	private static final List<String> unmodifiablePrivileges = Collections.unmodifiableList(possiblePrivileges);
	static {
		possiblePrivileges.add("SELECT");
		possiblePrivileges.add("DELETE");
		possiblePrivileges.add("INSERT");
		possiblePrivileges.add("UPDATE");
	}

	@Override
	public List<String> getPossiblePrivileges() {
		return unmodifiablePrivileges;
	}

	/**
	 * Gets the possible values for the specified column name if it references a foreign key.
	 *
	 * @param column the name of the column.
	 */
	@Override
	public List<String> getPossibleValues(String column, String type) throws SQLException, IOException {
		// Do not search if the settings are less than 1
		int fkeyrows=getSettings().getForeignKeyRows();
		if(fkeyrows>0) {
			// Performing other database operations before allocating a Connection
			// avoids possible deadlock
			String constraint=getConstraintName(column);
			if(constraint!=null) {
				// Keep the use of Connections serial
				String key=getForeignKey(constraint);
				int pos=key.indexOf('.');
				String keyColumn=key.substring(pos+1);
				String keyTable=key.substring(0,pos);

				// Only return entries if less than or equal to fkeyrows possibilities
				int count=getIntQuery("SELECT COUNT(" + quoteColumn(keyColumn) + ") FROM " + quoteTable(keyTable));
				if(count<=fkeyrows) {
					StringBuffer sql=new StringBuffer("SELECT ")
						.append(quoteColumn(keyColumn))
						.append(" FROM ")
						.append(quoteTable(keyTable));
					Indexes indexes=getIndexes();
					List<String> names=indexes.getNames();
					List<String> columns=indexes.getColumns();
					List<Boolean> areUnique=indexes.areUnique();
					int size=columns.size();
					boolean isMultiple=false;
					for(int i=0;i<size;i++) {
						if(column.equals(columns.get(i))) {
							if(areUnique.get(i)==Boolean.TRUE) {
								// Check that this is not part of a multiple column unique clause
								for(int n=0;n<size;n++) {
									if(n!=i && names.get(i).equals(names.get(n))) {
										isMultiple=true;
										n=size;
									}
								}
								if(!isMultiple) {
									sql
										.append(" WHERE (SELECT COUNT(")
										.append(quoteColumn(column))
										.append(") FROM ")
										.append(quoteTable(settings.getTable()))
										.append(" WHERE ")
										.append(quoteTable(keyTable))
										.append('.')
										.append(quoteColumn(keyColumn))
										.append('=')
										.append(quoteTable(settings.getTable()))
										.append('.')
										.append(quoteColumn(column))
										.append(")=0");
								}
							}
							break;
						}
					}
					sql.append(" ORDER BY ").append(quoteColumn(keyColumn));

					// Return all the values, sorted
					List<String> V=executeListQuery(sql.toString());
					if(V.size()>0) return V;
				}
			}
		}
		// Boolean types only have two possibilities
		if("bool".equalsIgnoreCase(type)) {
			List<String> values=new ArrayList<String>(2);
			values.add("true");
			values.add("false");
			return values;
		}
		return null;
	}

	/**
	 * Gets the primary key description for the given constraint name.
	 *
	 * @param constraint the constraint name.
	 */
	@Override
	public String getPrimaryKey(String constraint) throws SQLException, IOException {
		String table=getSettings().getTable();

		Connection conn=DatabasePool.getConnection(getSettings());
		try {
			Statement stmt=conn.createStatement();
			try {
				ResultSet R=stmt.executeQuery("select tgargs from pg_trigger");
				try {
					while(R.next()) {
						String S=R.getString(1);
						int pos=S.indexOf("\\000");
						if(pos>-1) {
							String tmp=S.substring(0,pos);
							if(constraint.equals(tmp)) {
								int pos2=S.indexOf("\\000",pos+1);
								if(pos2>-1) {
									if(table.equals(S.substring(pos+4,pos2))) {
										pos=S.indexOf("\\000",pos2+1);
										if(pos>-1)pos=S.indexOf("\\000",pos+1);
										if(pos>-1)pos2=S.indexOf("\\000",pos+1);
										if(pos2>-1) {
											return table+"."+S.substring(pos+4, pos2);
										}
									}
								}
							}
						}
					}
					return "";
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
	 * Gets the remark for the specified column.
	 *
	 * @param column the name of the column.
	 *
	 * @return the remark or <code>null</code> for none.
	 */
	@Override
	public String getRemark(String column) throws SQLException, IOException {
		String r=super.getRemark(column);
		// the "no remarks" is the default.
		return "no remarks".equals(r)?null:r;
	}

	@Override
	public TablePrivileges getTablePrivileges() throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			Statement stmt=conn.createStatement();
			try {
				ResultSet R=stmt.executeQuery(
					"SELECT u.usename, c.relacl "
					+ "FROM pg_class c, pg_user u "
					+ "WHERE (c.relkind='r' OR relkind='S') AND "
					+ "       c.relname !~ '^pg_'"
					// TODO: PreparedStatement
					+ "  AND c.relname ~ '^"+settings.getTable()+"'"
					+"  AND c.relowner = u.usesysid"
					+ " LIMIT 1 offset 0"
				);
				try {
					List<String> grantees=new ArrayList<String>();
					List<String> privileges=new ArrayList<String>();
					String grantor="";
					if(R.next()) {
						grantor=R.getString(1);
						String S=R.getString(2);
						int size=S.length();
						for(int i=0;i<size;i++) {
							if(S.charAt(i)=='"') {
								i++;
								int n=S.indexOf('=',i);
								String grantee=S.substring(i,n);
								if(grantee.startsWith("group ")) grantee=S.substring(6);
								grantees.add(grantee);
								n++;
								i=S.indexOf('"',n);
								String privs=S.substring(n,i);
								StringBuffer P=new StringBuffer();
								if(privs.indexOf("arwR")>-1) P.append("ALL");
								else {
									if(privs.indexOf('r')>-1) P.append("SELECT");
									if(privs.indexOf('w')>-1) {
										if(P.length()>0) P.append(", ");
										P.append("UPDATE/DELETE");
									}
									if(privs.indexOf('a')>-1) {
										if(P.length()>0) P.append(", ");
										P.append("INSERT");
									}
									if(privs.indexOf('a')>-1) {
										if(P.length()>0) P.append(", ");
										P.append("RULE");
									}
								}
								privileges.add(P.toString());
							}
						}
					}
					int size=grantees.size();
					List<String> grantors = new ArrayList<String>(size);
					List<Boolean> isGrantable=new ArrayList<Boolean>(size);
					for(int i=0;i<size;i++) {
						grantors.add(grantor);
						isGrantable.add(Boolean.UNKNOWN);
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
				stmt.close();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
	}

	/**
	 * Returns a {@link List} containing all the tables in the current database.
	 */
	@Override
	public List<String> getTables() throws SQLException, IOException {
		return executeListQuery(
			"SELECT\n"
			+ "  c.relname AS Name,\n"
			+ "  'table'::text as Type,\n"
			+ "  u.usename as Owner\n"
			+ "FROM\n"
			+ "  pg_class c,\n"
			+ "  pg_user u\n"
			+ "WHERE\n"
			+ "  c.relowner = u.usesysid\n"
			+ "  AND c.relkind = 'r'\n"
			+ "  AND not exists (select 1 from pg_views where viewname = c.relname)\n"
			+ "  AND c.relname !~ '^pg_'\n"
			+ "  AND c.relname NOT IN (\n"
			+ "    'sql_features',\n"
			+ "    'sql_implementation_info',\n"
			+ "    'sql_languages',\n"
			+ "    'sql_packages',\n"
			+ "    'sql_parts',\n"
			+ "    'sql_sizing',\n"
			+ "    'sql_sizing_profiles'\n"
			+ "  )\n"
			+ "UNION SELECT\n"
			+ "  c.relname as Name,\n"
			+ "  'table'::text as Type,\n"
			+ "  NULL as Owner\n"
			+ "FROM\n"
			+ "  pg_class c\n"
			+ "WHERE\n"
			+ "  c.relkind = 'r'\n"
			+ "  AND not exists (select 1 from pg_views where viewname = c.relname)\n"
			+ "  AND not exists (select 1 from pg_user where usesysid = c.relowner)\n"
			+ "  AND c.relname !~ '^pg_'\n"
			+ "  AND c.relname NOT IN (\n"
			+ "    'sql_features',\n"
			+ "    'sql_implementation_info',\n"
			+ "    'sql_languages',\n"
			+ "    'sql_packages',\n"
			+ "    'sql_parts',\n"
			+ "    'sql_sizing',\n"
			+ "    'sql_sizing_profiles'\n"
			+ "  )\n"
			+ "ORDER BY Name"
		);
	}

	/**
	 * Gets a list of types supported by the database.
	 */
	@Override
	public List<String> getTypes() throws SQLException, IOException {
		return executeListQuery(
			"("
			+ "SELECT typname "
			+ " FROM pg_type pt"
			+ " WHERE typname NOT LIKE '\\\\_%'"
			+ ")"
			+ "EXCEPT"
			+ "("
			+ "SELECT relname"
			+ " FROM pg_class"
			+ ")"
		);
	}

	/**
	 * Gets the ON UPDATE rule for the specified constraint.
	 *
	 * @param constraint the constraint name.
	 */
	public String getUpdateRule(String constraint) throws SQLException, IOException {
		String table=getSettings().getTable();

		Connection conn=DatabasePool.getConnection(getSettings());
		try {
			Statement stmt=conn.createStatement();
			try {
				ResultSet R=stmt.executeQuery("select tgname, tgargs, proname from pg_proc, pg_trigger where tgfoid = pg_proc.oid and proname like 'RI_FKey_%_upd'");
				try {
					while(R.next()) {
						String S=R.getString(2);
						int pos=S.indexOf("\\000");
						if(pos>-1) {
							String tmp=S.substring(0,pos);
							if(constraint.equals(tmp)) {
								int pos2=S.indexOf("\\000",pos+1);
								if(pos2>-1) {
									if(table.equals(S.substring(pos+4,pos2))) {
										String rule=R.getString(3);
										return rule.substring(8,rule.length()-4);
									}
								}
							}
						}
					}
					return "";
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
	 * Gets the DEFERRABLE constraint.
	 *
	 * @param constraint the constraint name.
	 */
	public Boolean isDeferrable(String constraint) throws SQLException, IOException {
		String table=getSettings().getTable();

		Connection conn=DatabasePool.getConnection(getSettings());
		try {
			Statement stmt=conn.createStatement();
			try {
				ResultSet R=stmt.executeQuery("select tgargs, tgdeferrable from pg_trigger");
				try {
					while(R.next()) {
						String S=R.getString(1);
						int pos=S.indexOf("\\000");
						if(pos>-1) {
							String tmp=S.substring(0,pos);
							if(constraint.equals(tmp)) {
								int pos2=S.indexOf("\\000",pos+1);
								if(pos2>-1) {
									if(table.equals(S.substring(pos+4,pos2))) {
										return R.getString(2).equals("t")?Boolean.TRUE:Boolean.FALSE;
									}
								}
							}
						}
					}
					// Not found
					return Boolean.UNKNOWN;
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
	 * Gets the INITALLY DEFERRED constraint.
	 *
	 * @param constraint the constraint name.
	 */
	public Boolean isInitiallyDeferred(String constraint) throws SQLException, IOException {
		String table=getSettings().getTable();

		Connection conn=DatabasePool.getConnection(getSettings());
		try {
			Statement stmt=conn.createStatement();
			try {
				ResultSet R=stmt.executeQuery("select tgargs, tginitdeferred from pg_trigger");
				try {
					while(R.next()) {
						String S=R.getString(1);
						int pos=S.indexOf("\\000");
						if(pos>-1) {
							String tmp=S.substring(0,pos);
							if(constraint.equals(tmp)) {
								int pos2=S.indexOf("\\000",pos+1);
								if(pos2>-1) {
									if(table.equals(S.substring(pos+4,pos2))) {
										return R.getString(2).equals("t")?Boolean.TRUE:Boolean.FALSE;
									}
								}
							}
						}
					}
					// Not found
					return Boolean.UNKNOWN;
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
	 * Does the database product support CHECK constraints?
	 */
	@Override
	public boolean supportsCheckConstraints() {
		return true;
	}

	/**
	 * As of PostgreSQL version 8, requires explicit casts.
	 */
	@Override
	public void insertRow(String[] column, String[] function, String[] values) throws SQLException, IOException {
		String table = settings.getTable();
		Columns columns = getColumns(table);
		// Build the SQL
		StringBuffer SB=new StringBuffer("INSERT INTO ").append(quoteTable(table)).append(" (");
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
				String type = getCastType(columns.getType(columns.getID(column[i])));
				if("bool".equals(type) || "boolean".equals(type)) {
					SB.append("?");
				} else {
					SB.append("?::").append(type);
				}
			}
		}
		SB.append(')');
		String sql=SB.toString();

		Connection conn=DatabasePool.getConnection(settings);
		try {
			PreparedStatement stmt=conn.prepareStatement(sql);
			try {
				stmt.setEscapeProcessing(false); // TODO: Why are these used?
				int pos=1;
				for(int i=0;i<column.length;i++) {
					if(function[i]==null || function[i].length()==0) {
						String type = getCastType(columns.getType(columns.getID(column[i])));
						String val = values[i];
						if("bool".equals(type) || "boolean".equals(type)) {
							if(val==null) stmt.setNull(pos++, Types.BOOLEAN);
							else if("true".equals(val)) stmt.setBoolean(pos++, true);
							else if("false".equals(val)) stmt.setBoolean(pos++, false);
							else throw new AssertionError("value should be null, \"true\", or \"false\": "+val);
						} else {
							stmt.setString(pos++, val);
						}
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
	 * As of PostgreSQL version 8, requires explicit casts.
	 */
	@Override
	public void deleteRow(
		String[] primaryKeys,
		String[] primaryKeyValues
	) throws SQLException, IOException {
		String table = settings.getTable();
		Columns columns = getColumns(table);

		StringBuffer SB = new StringBuffer("DELETE FROM ").append(quoteTable(table));
		for (int i = 0; i < primaryKeys.length; i++) {
			SB.append(i == 0 ? " WHERE " : " AND ");
			if (primaryKeyValues[i] == null) {
				appendIsNull(SB, primaryKeys[i]);
			} else {
				String type = getCastType(columns.getType(columns.getID(primaryKeys[i])));
				SB.append(quoteColumn(primaryKeys[i])).append("=?::").append(type);
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
	 * Gets a converted type to be used for casts.
	 */
	private static String getCastType(String rawType) {
		if("serial".equals(rawType)) return "integer";
		if("bigserial".equals(rawType)) return "bigint";
		return rawType;
	}

	/**
	 * As of PostgreSQL version 8, requires explicit casts.
	 */
	@Override
	public List<String> getRow(List<String> primaryKeys, List<String> primaryValues) throws SQLException, IOException {
		String table = settings.getTable();
		Columns columns = getColumns(table);
		// Build the SQL first
		StringBuffer SB = new StringBuffer("SELECT * FROM ").append(quoteTable(table));
		for (int i = 0; i < primaryKeys.size(); i++) {
			SB.append(i == 0 ? " WHERE " : " AND ");
			if (primaryValues.get(i) == null) {
				appendIsNull(SB, primaryKeys.get(i));
			} else {
				String type = getCastType(columns.getType(columns.getID(primaryKeys.get(i))));
				SB.append(quoteColumn(primaryKeys.get(i))).append("=?::").append(type);
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
	 * As of PostgreSQL version 8, requires explicit casts.
	 */
	@Override
	public void editRow(String[] column,
		String[] function,
		String[] value,
		String[] primaryKeys,
		String[] primaryKeyValues
	) throws SQLException, IOException {
		String table = settings.getTable();
		Columns columns = getColumns(table);
		// Build the SQL statement
		StringBuffer SB = new StringBuffer("UPDATE ").append(quoteTable(table)).append(" SET ");
		for (int i = 0; i < column.length; i++) {
			if (i > 0) {
				SB.append(", ");
			}
			SB.append(quoteColumn(column[i])).append("=");
			if (function[i] != null && function[i].length() > 0) {
				SB.append(function[i]);
			} else {
				String type = getCastType(columns.getType(columns.getID(column[i])));
				SB.append("?::").append(type);
			}
		}
		for (int i = 0; i < primaryKeys.length; i++) {
			SB.append(i == 0 ? " WHERE " : " AND ");
			if (primaryKeyValues[i] == null) {
				appendIsNull(SB, primaryKeys[i]);
			} else {
				String type = getCastType(columns.getType(columns.getID(primaryKeys[i])));
				SB.append(quoteColumn(primaryKeys[i])).append("=?::").append(type);
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

	@Override
	public boolean isKeyword(String identifier) {
		return Server.ReservedWord.isReservedWord(identifier);
	}
}
