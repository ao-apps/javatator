/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2009, 2015, 2017, 2018, 2019, 2020, 2021, 2022, 2023, 2024  AO Industries, Inc.
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
import java.util.regex.Pattern;

/**
 * The PostgreSQL connection class. Implements things which the driver doesn't do using JDBC.
 */
public class PostgresqlConnector extends JdbcConnector {

  /**
   * Instantiate a new PostgresqlConnector.
   */
  public PostgresqlConnector(Settings settings) {
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
  protected void appendIsNull(StringBuilder sb, String column) {
    sb.append(quoteColumn(column)).append(" IS NULL");
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
    StringBuilder sql = new StringBuilder();
    sql.append("CREATE TABLE ").append(quoteTable(settings.getTable())).append(" (");
    for (int i = 0; i < newColumn.length; i++) {
      if (i > 0) {
        sql.append(", ");
      }
      sql.append(quoteColumn(newColumn[i])).append(' ').append(newType[i]);
      if (newLength[i].length() > 0) {
        sql.append('(').append(newLength[i]).append(')');
      }
      sql.append(' ').append(newNull[i]);
      if (uniqueKey[i]) {
        sql.append(" UNIQUE");
      }
      if (newDefault[i].length() > 0) {
        sql.append(" DEFAULT ").append(Util.escapeSqlValue(newDefault[i]));
      }
      if (primaryKey[i]) {
        sql.append(", PRIMARY KEY (").append(quoteColumn(newColumn[i])).append(')');
      }
    }
    sql.append(" )");

    // Execute the update next
    executeUpdate(sql.toString());

    // Check for indexes to add
    for (int i = 0; i < newColumn.length; i++) {
      if (indexKey[i]) {
        addIndex(newColumn[i], newColumn[i]);
      }
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
    //Connection conn=DatabasePool.getConnection(databaseProduct, username, password, url, database);
    //try {
    //  conn.setCatalog(database);
    //  PreparedStatement pstmt=conn.prepareStatement("CREATE TEMPORARY TABLE "+quoteTable(table+"_")+" AS SELECT * FROM "+quoteTable(table)+";"
    //    + "DROP TABLE "+quoteTable(table)+";"
    //    + "CREATE TABLE "+quoteTable(table)
    //  );
    //  try {
    //    pstmt.executeUpdate();
    //  } finally {
    //    pstmt.close();
    //  }
    //} finally {
    //   conn.close();
    //}
  }

  /**
   * Drops the database. Note: for PostgreSQL we cannot be connected to the database to be dropped.
   */
  @Override
  public void dropDatabase() throws SQLException, IOException {
    System.out.println("closing database: " + settings.getDatabase());
    DatabasePool.closeDatabase(settings);
    String sql = "DROP DATABASE " + settings.getDatabase();
    try (
        Connection conn = DatabasePool.getConnection(settings.setDatabase("template1"));
        Statement stmt = conn.createStatement()
        ) {
      stmt.executeUpdate(sql);
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
   * {@inheritDoc}
   *
   * @see JdbcConnector#editColumn
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
    StringBuilder sql = new StringBuilder();
    if (!column.equals(newColumn)) {
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
    if (newDefault == null) {
      sql.append(" DROP DEFAULT");
    } else {
      sql.append(" SET DEFAULT ");
      if (newDefault.charAt(0) == 'F') {
        sql.append(newDefault.substring(1));
      } else {
        sql.append(Util.escapeSqlValue(newDefault.substring(1)));
      }
    }
    executeUpdate(sql.toString());
  }

  @Override
  public CheckConstraints getCheckConstraints() throws SQLException, IOException {
    String table = getSettings().getTable();
    List<String> names = new ArrayList<>();
    List<String> checkClauses = new ArrayList<>();
    try (Connection conn = DatabasePool.getConnection(getSettings())) {
      DatabaseMetaData metaData = conn.getMetaData();
      String version = metaData.getDatabaseProductVersion();
      try (Statement stmt = conn.createStatement()) {
        ResultSet r;
        if (version.startsWith("7.")) {
          r = stmt.executeQuery(
              "SELECT\n"
                  + "  rcname,\n"
                  + "  rcsrc\n"
                  + "FROM\n"
                  + "  pg_relcheck r,\n"
                  + "  pg_class c\n"
                  + "WHERE\n"
                  // TODO: PreparedStatement
                  + "  c.relname='" + table + "'\n"
                  + "  AND c.oid=r.rcrelid"
          );
        } else {
          r = stmt.executeQuery(
              "SELECT\n"
                  + "  co.conname,\n"
                  + "  pg_get_constraintdef(co.oid)\n"
                  + "FROM\n"
                  + "  pg_catalog.pg_class cl\n"
                  + "  inner join pg_catalog.pg_constraint co on cl.oid=co.conrelid\n"
                  + "WHERE\n"
                  // TODO: PreparedStatement
                  + "  cl.relname='" + table + "'\n"
                  + "  and co.contype = 'c'"
          );
        }
        try {
          while (r.next()) {
            names.add(r.getString(1));
            checkClauses.add(r.getString(2));
          }
        } finally {
          r.close();
        }
      }
    }
    return new CheckConstraints(names, checkClauses);
  }

  @Override
  protected Columns getColumns(String table) throws SQLException, IOException {
    List<String> names = new ArrayList<>();
    List<String> types = new ArrayList<>();
    List<String> lengths = new ArrayList<>();
    List<Boolean> areNullable = new ArrayList<>();
    List<String> remarks = new ArrayList<>();
    try (
        Connection conn = DatabasePool.getConnection(settings);
        ResultSet r = conn.getMetaData().getColumns(null, null, table, "%")
        ) {
      while (r.next()) {
        names.add(r.getString(4));
        types.add(r.getString(6));
        lengths.add(r.getString(7));
        int nullable = r.getInt(11);
        areNullable.add(
            (nullable == DatabaseMetaData.columnNoNulls) ? Boolean.FALSE
                : (nullable == DatabaseMetaData.columnNullable) ? Boolean.TRUE
                : Boolean.UNKNOWN
        );
        String rem = r.getString(12);
        remarks.add((rem != null) ? rem : "");
      }
    }
    List<String> defaults = getDefaults(names);
    return new Columns(names, types, lengths, areNullable, defaults, remarks);
  }

  /**
   * Gets the corresponding foreign key constraint name for the specified column name.
   *
   * @param column the name of the column.
   */
  private String getConstraintName(String column) throws SQLException, IOException {
    String table = getSettings().getTable();

    try (Connection conn = DatabasePool.getConnection(getSettings())) {
      DatabaseMetaData metaData = conn.getMetaData();
      String version = metaData.getDatabaseProductVersion();
      if (version.startsWith("7.")) {
        try (
            Statement stmt = conn.createStatement();
            ResultSet r = stmt.executeQuery("select tgargs from pg_trigger")
            ) {
          while (r.next()) {
            String s = r.getString(1);
            int pos = s.indexOf("\\000");
            if (pos > -1) {
              String constraintName = s.substring(0, pos);
              int pos2 = s.indexOf("\\000", pos + 4);
              String localTable = s.substring(pos + 4, pos2);
              if (table.equals(localTable)) {
                pos = s.indexOf("\\000", pos2 + 4);
                pos2 = s.indexOf("\\000", pos + 4);
                pos = s.indexOf("\\000", pos2 + 4);
                String localColumn = s.substring(pos2 + 4, pos);
                if (localColumn.equals(column)) {
                  return constraintName;
                }
              }
            }
          }
          return null;
        }
      } else {
        ForeignKeys foreignKeys = getForeignKeys(table, true);
        if (foreignKeys != null) {
          for (int c = 0; c < foreignKeys.getSize(); c++) {
            if (column.equals(foreignKeys.getForeignKey(c))) {
              return foreignKeys.getConstraintName(c);
            }
          }
        }
        return null;
      }
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
   *          start with a {@code 'V'} and a function will start
   *          with a {@code 'F'}.
   */
  private List<String> getDefaults(List<String> columns) throws SQLException, IOException {
    // Fetch the value from the database, release the connection, then
    // parse to obtain the result.  This minimizes the amount of time
    // the database resource is locked.

    List<String> defaults = new ArrayList<>();
    List<String> colNames = new ArrayList<>();
    try (Connection conn = DatabasePool.getConnection(getSettings())) {
      DatabaseMetaData metaData = conn.getMetaData();
      String version = metaData.getDatabaseProductVersion();
      try (
          PreparedStatement pstmt = conn.prepareStatement(
              "SELECT " + (version.startsWith("7.") ? "d.adsrc" : "pg_get_expr(d.adbin, d.adrelid)") + ", a.attname"
                  + " FROM pg_attrdef d, pg_class c, pg_attribute a"
                  + " WHERE c.relname = ?"
                  + "   AND c.oid = d.adrelid"
                  + "   AND a.attrelid = c.oid"
                  + "   AND a.attnum > 0"
                  + "   AND d.adnum = a.attnum"
                  + " ORDER BY a.attnum"
          )
          ) {
        pstmt.setString(1, getSettings().getTable());
        try (ResultSet results = pstmt.executeQuery()) {
          while (results.next()) {
            defaults.add(results.getString(1));
            colNames.add(results.getString(2));
          }
        }
      }
    }

    int size = defaults.size();
    int len = columns.size();
    List<String> out = new ArrayList<>(len);
    for (int c = 0; c < len; c++) {
      out.add(null);
    }
    for (int c = 0; c < size; c++) {
      String col = colNames.get(c);
      int i;
      for (i = 0; i < len; i++) {
        if (col.equals(columns.get(i))) {
          break;
        }
      }
      String def = defaults.get(c);
      // Look for null
      if (def == null) {
        out.set(i, null);
      } else {
        // Look for boolean value
        int defLen = def.length();
        if (
            defLen == 9
                && def.charAt(0) == '\''
                && def.endsWith("'::bool")
        ) {
          char ch = def.charAt(1);
          switch (ch) {
            case 't':
              out.set(i, "Vtrue");
              break;
            case 'f':
              out.set(i, "Vfalse");
              break;
            default:
              throw new SQLException("Unknown default value for bool type: " + def);
          }
        } else if (
            defLen >= 2
                && def.charAt(0) == '\''
                && def.charAt(defLen - 1) == '\''
        ) {
          // Look for a String constant
          out.set(i, 'V' + def.substring(1, defLen - 1));
        } else {
          // Look for a numerical constant
          boolean isNumber = true;
          for (int d = 0; d < defLen; d++) {
            char ch = def.charAt(d);
            if (
                (ch < '0' || ch > '9')
                    && ch != '-'
                    && ch != '.'
                    && ch != 'e'
                    && ch != 'E'
                    && ch != '+'
            ) {
              isNumber = false;
              break;
            }
          }
          if (isNumber) {
            out.set(i, 'V' + def);
          } else {
            // Otherwise assume it is a function
            out.set(i, 'F' + def);
          }
        }
      }
    }
    if (columns.size() != out.size()) {
      throw new AssertionError();
    }
    // Trim any trailing ::text from the defaults
    for (int c = 0; c < out.size(); c++) {
      String def = out.get(c);
      if (def != null && def.startsWith("F'") && def.endsWith("'::text")) {
        out.set(c, "V" + def.substring(2, def.length() - 7));
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
    String table = getSettings().getTable();

    try (
        Connection conn = DatabasePool.getConnection(getSettings());
        Statement stmt = conn.createStatement();
        ResultSet r = stmt.executeQuery("select tgname, tgargs, proname from pg_proc, pg_trigger where tgfoid = pg_proc.oid and proname like 'RI_FKey_%_del'")
        ) {
      while (r.next()) {
        String s = r.getString(2);
        int pos = s.indexOf("\\000");
        if (pos > -1) {
          String tmp = s.substring(0, pos);
          if (constraint.equals(tmp)) {
            int pos2 = s.indexOf("\\000", pos + 1);
            if (pos2 > -1) {
              if (table.equals(s.substring(pos + 4, pos2))) {
                String rule = r.getString(3);
                return rule.substring(8, rule.length() - 4);
              }
            }
          }
        }
      }
      return "";
    }
  }

  /**
   * Gets the foreign key description for the given constraint name.
   *
   * @param constraint the constraint name.
   */
  public String getForeignKey(String constraint) throws SQLException, IOException {
    String table = getSettings().getTable();
    try (Connection conn = DatabasePool.getConnection(getSettings())) {
      DatabaseMetaData metaData = conn.getMetaData();
      String version = metaData.getDatabaseProductVersion();
      if (version.startsWith("7.")) {
        try (
            Statement stmt = conn.createStatement();
            ResultSet r = stmt.executeQuery("select tgargs from pg_trigger")
            ) {
          while (r.next()) {
            String s = r.getString(1);
            int pos = s.indexOf("\\000");
            if (pos > -1) {
              String tmp = s.substring(0, pos);
              if (constraint.equals(tmp)) {
                int pos2 = s.indexOf("\\000", pos + 1);
                if (pos2 > -1) {
                  if (table.equals(s.substring(pos + 4, pos2))) {
                    pos = s.indexOf("\\000", pos2 + 1);
                    String foreignTable = "";
                    if (pos > -1) {
                      foreignTable = s.substring(pos2 + 4, pos);
                      pos = s.indexOf("\\000", pos + 1);
                    }
                    if (pos > -1) {
                      pos = s.indexOf("\\000", pos + 1);
                    }
                    if (pos > -1) {
                      pos2 = s.indexOf("\\000", pos + 1);
                      if (pos2 > -1) {
                        return foreignTable + "." + s.substring(pos + 4, pos2);
                      }
                    }
                  }
                }
              }
            }
          }
          return "";
        }
      } else {
        ForeignKeys foreignKeys = getForeignKeys(table, true);
        for (int c = 0; c < foreignKeys.getSize(); c++) {
          if (constraint.equals(foreignKeys.getConstraintName(c))) {
            return foreignKeys.getPrimaryTable(c) + "." + foreignKeys.getPrimaryKey(c);
          }
        }
        return "";
      }
    }
  }

  /**
   * Gets the foreign key data for the current table.
   *
   * @param isImported only get the imported keys?
   */
  @Override
  protected ForeignKeys getForeignKeys(String table, boolean isImported) throws SQLException, IOException {
    try (Connection conn = DatabasePool.getConnection(getSettings())) {
      List<String> foreignKeys = new ArrayList<>();
      List<String> foreignTables = new ArrayList<>();
      List<String> primaryKeys = new ArrayList<>();
      List<String> primaryTables = new ArrayList<>();
      List<String> constraintNames = new ArrayList<>();
      List<String> insertRules = new ArrayList<>();
      List<String> deleteRules = new ArrayList<>();
      List<String> updateRules = new ArrayList<>();
      List<Boolean> isDeferrable = new ArrayList<>();
      List<Boolean> isInitiallyDeferred = new ArrayList<>();

      DatabaseMetaData metaData = conn.getMetaData();
      String version = metaData.getDatabaseProductVersion();
      if (version.startsWith("7.")) {
        try (
            Statement stmt = conn.createStatement();
            ResultSet r = stmt.executeQuery(
                "SELECT tgargs, "
                    + "CASE WHEN proname LIKE 'RI_FKey_%' "
                    + "THEN substring(proname from 9 for (char_length(proname)-12)) END, "
                    + "tgdeferrable, tginitdeferred "
                    + "FROM pg_proc, pg_trigger WHERE tgfoid = pg_proc.oid ORDER BY tgname"
            )
            ) {
          while (r.next()) {
            String s = r.getString(1);
            int pos = s.indexOf("\\000");
            if (pos > -1) {
              String constraintName = s.substring(0, pos);
              int pos2 = s.indexOf("\\000", pos + 1);
              if (pos2 > -1) {
                String primaryTable = s.substring(pos + 4, pos2);
                if (!isImported || table.equals(primaryTable)) {
                  pos = s.indexOf("\\000", pos2 + 1);
                  if (pos > -1) {
                    String foreignTable = s.substring(pos2 + 4, pos);
                    if (isImported || table.equals(foreignTable)) {
                      pos = s.indexOf("\\000", pos + 1);
                      if (pos > -1) {
                        pos2 = s.indexOf("\\000", pos + 1);
                        if (pos2 > -1) {
                          String primaryKey = s.substring(pos + 4, pos2);
                          pos = s.indexOf("\\000", pos2 + 1);
                          if (pos > -1) {
                            constraintNames.add(constraintName);
                            foreignTables.add(foreignTable);
                            primaryTables.add(primaryTable);
                            primaryKeys.add(primaryKey);
                            foreignKeys.add(s.substring(pos2 + 4, pos));
                            isDeferrable.add(r.getBoolean(3) ? Boolean.TRUE : Boolean.FALSE);
                            isInitiallyDeferred.add(r.getBoolean(4) ? Boolean.TRUE : Boolean.FALSE);
                            insertRules.add(r.getString(2));
                            if (r.next()) {
                              deleteRules.add(r.getString(2));
                            }
                            if (r.next()) {
                              updateRules.add(r.getString(2));
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      } else {
        List<Long> foreignTableOids = new ArrayList<>();
        List<List<Short>> foreignKeyAttNums = new ArrayList<>();
        List<Long> primaryTableOids = new ArrayList<>();
        List<List<Short>> primaryKeyAttNums = new ArrayList<>();
        try (
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
                    + "  " + (isImported ? "cl" : "ft") + ".relname=?\n"
                    + "  and co.contype='f'\n"
                    + "ORDER BY\n"
                    + "  cl.relname,\n"
                    + "  co.conname"
            )
            ) {
          pstmt.setString(1, table);
          try (ResultSet results = pstmt.executeQuery()) {
            while (results.next()) {
              constraintNames.add(results.getString("conname"));
              // Foreign keys
              foreignTableOids.add(results.getLong("foreign_table_oid"));
              foreignKeyAttNums.add(Arrays.asList((Short[]) results.getArray("conkey").getArray()));
              foreignTables.add(results.getString("foreign_table"));
              // Primary keys
              primaryTableOids.add(results.getLong("primary_table_oid"));
              primaryKeyAttNums.add(Arrays.asList((Short[]) results.getArray("confkey").getArray()));
              primaryTables.add(results.getString("primary_table"));
              // Rules and the rest
              insertRules.add(getMatchRule(results.getString("insert_rule")));
              deleteRules.add(getActionRule(results.getString("delete_rule")));
              updateRules.add(getActionRule(results.getString("update_rule")));
              isDeferrable.add(results.getBoolean("condeferrable") ? Boolean.TRUE : Boolean.FALSE);
              isInitiallyDeferred.add(results.getBoolean("condeferred") ? Boolean.TRUE : Boolean.FALSE);
            }
          }
        }
        for (int c = 0; c < foreignKeyAttNums.size(); c++) {
          // Foreign keys
          List<Short> foreignKeyAttNum = foreignKeyAttNums.get(c);
          if (foreignKeyAttNum.size() != 1) {
            throw new SQLException("Only single-column foreign keys currently supported");
          }
          foreignKeys.add(getColumnName(conn, foreignTableOids.get(c), foreignKeyAttNum.get(0)));
          // Primary keys
          List<Short> primaryKeyAttNum = primaryKeyAttNums.get(c);
          if (primaryKeyAttNum.size() != 1) {
            throw new SQLException("Only single-column primary keys currently supported");
          }
          primaryKeys.add(getColumnName(conn, primaryTableOids.get(c), primaryKeyAttNum.get(0)));
        }
      }
      int size = constraintNames.size();
      if (size < 1) {
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
    }
  }

  private static String getColumnName(Connection conn, long tableOid, int attNum) throws SQLException {
    try (PreparedStatement pstmt = conn.prepareStatement("SELECT attname FROM pg_catalog.pg_attribute WHERE attrelid=? AND attnum=?")) {
      pstmt.setLong(1, tableOid);
      pstmt.setInt(2, attNum);
      try (ResultSet result = pstmt.executeQuery()) {
        if (!result.next()) {
          throw new SQLException("No row returned"); // TODO: NoRowException move to ao-sql
        }
        String name = result.getString(1);
        if (result.next()) {
          throw new SQLException("More than one row returned"); // TODO: ExtraRowException move to ao-sql
        }
        return name;
      }
    }
  }

  /**
   * From <code>parsenodes.h</code>.
   *
   * <pre>FKCONSTR_ACTION_NOACTION        'a'
   * FKCONSTR_ACTION_RESTRICT        'r'
   * FKCONSTR_ACTION_CASCADE         'c'
   * FKCONSTR_ACTION_SETNULL         'n'
   * FKCONSTR_ACTION_SETDEFAULT      'd'</pre>
   */
  private static String getActionRule(String rule) {
    if ("a".equals(rule)) {
      return "no action";
    }
    if ("r".equals(rule)) {
      return "restrict";
    }
    if ("c".equals(rule)) {
      return "cascade";
    }
    if ("n".equals(rule)) {
      return "set null";
    }
    if ("d".equals(rule)) {
      return "set default";
    }
    return rule;
  }

  /**
   * From <code>parsenodes.h</code>.
   *
   * <pre>FKCONSTR_MATCH_FULL             'f'
   * FKCONSTR_MATCH_PARTIAL          'p'
   * FKCONSTR_MATCH_UNSPECIFIED      'u'</pre>
   */
  private static String getMatchRule(String rule) {
    if ("f".equals(rule)) {
      return "full";
    }
    if ("p".equals(rule)) {
      return "partial";
    }
    if ("u".equals(rule)) {
      return "unspecified";
    }
    return rule;
  }

  /**
   * Gets a list of all the unique SQL functions supported by this database.
   */
  @Override
  public List<String> getFunctionList() throws SQLException, IOException {
    return executeListQuery("SELECT p.proname as Function FROM pg_proc p, pg_type t WHERE"
        + " p.prorettype = t.oid"
        + " and (pronargs = 0 or oidvectortypes(p.proargtypes) != '')"
        + " GROUP BY Function ORDER BY Function");
  }

  /**
   * Gets the functions that may return the provided type.
   */
  @Override
  public List<String> getFunctionList(String type) throws SQLException, IOException {
    return executeListQuery("SELECT p.proname as Function FROM pg_proc p, pg_type t WHERE"
        + " p.prorettype = t.oid"
        + " and t.typname=lower(?)"
        + " and (pronargs = 0 or oidvectortypes(p.proargtypes) != '')"
        + " GROUP BY Function ORDER BY Function", type);
  }

  /**
   * Gets a list of indexes for the selected table.
   */
  @Override
  public Indexes getIndexes() throws SQLException, IOException {
    List<String> names = new ArrayList<>();
    List<String> columns = new ArrayList<>();
    List<Boolean> areUnique = new ArrayList<>();
    try (
        Connection conn = DatabasePool.getConnection(settings);
        Statement stmt = conn.createStatement();
        ResultSet r = stmt.executeQuery(
            "SELECT ic.relname as PK_NAME, "
                + " a.attname AS COLUMN_NAME,"
                + " i.indisunique as UNIQUE_KEY"
                + " FROM pg_class bc, pg_class ic, pg_index i,"
                + "  pg_attribute a, pg_attribute ta"
                + " WHERE"
                + "   bc.relkind = 'r'"
                // TODO: This case-insensitive UPPER is probably not correct now that we have quoted tables
                + "   AND UPPER(bc.relname)=UPPER('"
                // TODO: PreparedStatement
                + getSettings().getTable() + "')"
                + "   AND i.indrelid = bc.oid"
                + "   AND i.indexrelid = ic.oid"
                + "   AND ic.oid = a.attrelid"
                + "   AND a.attrelid = i.indexrelid"
                + "   AND ta.attrelid = i.indrelid"
                + "   AND ta.attnum = i.indkey[a.attnum-1]"
                + " ORDER BY pk_name"
        )
        ) {
      while (r.next()) {
        names.add(r.getString(1));
        columns.add(r.getString(2));
        areUnique.add(r.getBoolean(3) ? Boolean.TRUE : Boolean.FALSE);
      }
    }
    return new Indexes(names, areUnique, columns);
  }

  /**
   * Gets the ON INSERT foreign key check rule for the specified constraint.
   *
   * @param constraint the constraint name.
   */
  public String getInsertRule(String constraint) throws SQLException, IOException {
    String table = getSettings().getTable();

    try (
        Connection conn = DatabasePool.getConnection(getSettings());
        Statement stmt = conn.createStatement();
        ResultSet r = stmt.executeQuery("select tgname, tgargs, proname from pg_proc, pg_trigger where tgfoid = pg_proc.oid and proname like 'RI_FKey_%_ins'")
        ) {
      while (r.next()) {
        String s = r.getString(2);
        int pos = s.indexOf("\\000");
        if (pos > -1) {
          String tmp = s.substring(0, pos);
          if (constraint.equals(tmp)) {
            int pos2 = s.indexOf("\\000", pos + 1);
            if (pos2 > -1) {
              if (table.equals(s.substring(pos + 4, pos2))) {
                String rule = r.getString(3);
                return rule.substring(8, rule.length() - 4);
              }
            }
          }
        }
      }
      return "";
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
    return new StringBuilder()
        .append("limit ")
        .append(numRows)
        .append(" offset ")
        .append(startPos)
        .toString();
  }

  private static final List<String> possiblePrivileges = new ArrayList<>(4);
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
  @SuppressWarnings("AssignmentToForLoopParameter")
  public List<String> getPossibleValues(String column, String type) throws SQLException, IOException {
    // Do not search if the settings are less than 1
    int fkeyrows = getSettings().getForeignKeyRows();
    if (fkeyrows > 0) {
      // Performing other database operations before allocating a Connection
      // avoids possible deadlock
      String constraint = getConstraintName(column);
      if (constraint != null) {
        // Keep the use of Connections serial
        String key = getForeignKey(constraint);
        int pos = key.indexOf('.');
        String keyColumn = key.substring(pos + 1);
        String keyTable = key.substring(0, pos);

        // Only return entries if less than or equal to fkeyrows possibilities
        int count = getIntQuery("SELECT COUNT(*) FROM " + quoteTable(keyTable));
        if (count <= fkeyrows) {
          StringBuilder sql = new StringBuilder("SELECT ")
              .append(quoteColumn(keyColumn))
              .append(" FROM ")
              .append(quoteTable(keyTable));
          Indexes indexes = getIndexes();
          List<String> names = indexes.getNames();
          List<String> columns = indexes.getColumns();
          List<Boolean> areUnique = indexes.areUnique();
          int size = columns.size();
          boolean isMultiple = false;
          for (int i = 0; i < size; i++) {
            if (column.equals(columns.get(i))) {
              if (areUnique.get(i) == Boolean.TRUE) {
                // Check that this is not part of a multiple column unique clause
                for (int n = 0; n < size; n++) {
                  if (n != i && names.get(i).equals(names.get(n))) {
                    isMultiple = true;
                    n = size;
                  }
                }
                if (!isMultiple) {
                  sql
                      .append(" WHERE (SELECT COUNT(*) FROM ")
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
          List<String> v = executeListQuery(sql.toString());
          if (!v.isEmpty()) {
            return v;
          }
        }
      }
    }
    // Boolean types only have two possibilities
    if ("bool".equalsIgnoreCase(type)) {
      List<String> values = new ArrayList<>(2);
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
    String table = getSettings().getTable();

    try (
        Connection conn = DatabasePool.getConnection(getSettings());
        Statement stmt = conn.createStatement();
        ResultSet r = stmt.executeQuery("select tgargs from pg_trigger")
        ) {
      while (r.next()) {
        String s = r.getString(1);
        int pos = s.indexOf("\\000");
        if (pos > -1) {
          String tmp = s.substring(0, pos);
          if (constraint.equals(tmp)) {
            int pos2 = s.indexOf("\\000", pos + 1);
            if (pos2 > -1) {
              if (table.equals(s.substring(pos + 4, pos2))) {
                pos = s.indexOf("\\000", pos2 + 1);
                if (pos > -1) {
                  pos = s.indexOf("\\000", pos + 1);
                }
                if (pos > -1) {
                  pos2 = s.indexOf("\\000", pos + 1);
                }
                if (pos2 > -1) {
                  return table + "." + s.substring(pos + 4, pos2);
                }
              }
            }
          }
        }
      }
      return "";
    }
  }

  /**
   * Gets the remark for the specified column.
   *
   * @param column the name of the column.
   *
   * @return the remark or {@code null} for none.
   */
  @Override
  public String getRemark(String column) throws SQLException, IOException {
    String r = super.getRemark(column);
    // the "no remarks" is the default.
    return "no remarks".equals(r) ? null : r;
  }

  @Override
  @SuppressWarnings("AssignmentToForLoopParameter")
  public TablePrivileges getTablePrivileges() throws SQLException, IOException {
    try (
        Connection conn = DatabasePool.getConnection(settings);
        Statement stmt = conn.createStatement();
        ResultSet r = stmt.executeQuery(
            "SELECT u.usename, c.relacl "
                + "FROM pg_class c, pg_user u "
                + "WHERE (c.relkind='r' OR relkind='S') AND "
                + "       c.relname !~ '^pg_'"
                // TODO: PreparedStatement
                + "  AND c.relname ~ '^" + settings.getTable() + "'"
                + "  AND c.relowner = u.usesysid"
                + " LIMIT 1 offset 0"
        )
        ) {
      List<String> grantees = new ArrayList<>();
      List<String> privileges = new ArrayList<>();
      String grantor = "";
      if (r.next()) {
        grantor = r.getString(1);
        String s = r.getString(2);
        int size = s.length();
        for (int i = 0; i < size; i++) {
          if (s.charAt(i) == '"') {
            i++;
            int n = s.indexOf('=', i);
            String grantee = s.substring(i, n);
            if (grantee.startsWith("group ")) {
              grantee = s.substring(6);
            }
            grantees.add(grantee);
            n++;
            i = s.indexOf('"', n);
            String privs = s.substring(n, i);
            StringBuilder p = new StringBuilder();
            if (privs.contains("arwR")) {
              p.append("ALL");
            } else {
              if (privs.indexOf('r') > -1) {
                p.append("SELECT");
              }
              if (privs.indexOf('w') > -1) {
                if (p.length() > 0) {
                  p.append(", ");
                }
                p.append("UPDATE/DELETE");
              }
              if (privs.indexOf('a') > -1) {
                if (p.length() > 0) {
                  p.append(", ");
                }
                p.append("INSERT");
              }
              if (privs.indexOf('a') > -1) {
                if (p.length() > 0) {
                  p.append(", ");
                }
                p.append("RULE");
              }
            }
            privileges.add(p.toString());
          }
        }
      }
      int size = grantees.size();
      List<String> grantors = new ArrayList<>(size);
      List<Boolean> isGrantable = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        grantors.add(grantor);
        isGrantable.add(Boolean.UNKNOWN);
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
    String table = getSettings().getTable();

    try (
        Connection conn = DatabasePool.getConnection(getSettings());
        Statement stmt = conn.createStatement();
        ResultSet r = stmt.executeQuery("select tgname, tgargs, proname from pg_proc, pg_trigger where tgfoid = pg_proc.oid and proname like 'RI_FKey_%_upd'")
        ) {
      while (r.next()) {
        String s = r.getString(2);
        int pos = s.indexOf("\\000");
        if (pos > -1) {
          String tmp = s.substring(0, pos);
          if (constraint.equals(tmp)) {
            int pos2 = s.indexOf("\\000", pos + 1);
            if (pos2 > -1) {
              if (table.equals(s.substring(pos + 4, pos2))) {
                String rule = r.getString(3);
                return rule.substring(8, rule.length() - 4);
              }
            }
          }
        }
      }
      return "";
    }
  }

  /**
   * Gets the DEFERRABLE constraint.
   *
   * @param constraint the constraint name.
   */
  public Boolean isDeferrable(String constraint) throws SQLException, IOException {
    String table = getSettings().getTable();

    try (
        Connection conn = DatabasePool.getConnection(getSettings());
        Statement stmt = conn.createStatement();
        ResultSet r = stmt.executeQuery("select tgargs, tgdeferrable from pg_trigger")
        ) {
      while (r.next()) {
        String s = r.getString(1);
        int pos = s.indexOf("\\000");
        if (pos > -1) {
          String tmp = s.substring(0, pos);
          if (constraint.equals(tmp)) {
            int pos2 = s.indexOf("\\000", pos + 1);
            if (pos2 > -1) {
              if (table.equals(s.substring(pos + 4, pos2))) {
                return "t".equals(r.getString(2)) ? Boolean.TRUE : Boolean.FALSE;
              }
            }
          }
        }
      }
      // Not found
      return Boolean.UNKNOWN;
    }
  }

  /**
   * Gets the INITALLY DEFERRED constraint.
   *
   * @param constraint the constraint name.
   */
  public Boolean isInitiallyDeferred(String constraint) throws SQLException, IOException {
    String table = getSettings().getTable();

    try (
        Connection conn = DatabasePool.getConnection(getSettings());
        Statement stmt = conn.createStatement();
        ResultSet r = stmt.executeQuery("select tgargs, tginitdeferred from pg_trigger")
        ) {
      while (r.next()) {
        String s = r.getString(1);
        int pos = s.indexOf("\\000");
        if (pos > -1) {
          String tmp = s.substring(0, pos);
          if (constraint.equals(tmp)) {
            int pos2 = s.indexOf("\\000", pos + 1);
            if (pos2 > -1) {
              if (table.equals(s.substring(pos + 4, pos2))) {
                return "t".equals(r.getString(2)) ? Boolean.TRUE : Boolean.FALSE;
              }
            }
          }
        }
      }
      // Not found
      return Boolean.UNKNOWN;
    }
  }

  /**
   * Does the database product support CHECK constraints?.
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
    StringBuilder sb = new StringBuilder("INSERT INTO ").append(quoteTable(table)).append(" (");
    for (int i = 0; i < column.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(quoteColumn(column[i]));
    }
    sb.append(") VALUES (");
    for (int i = 0; i < column.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      if (function[i] != null && function[i].length() > 0) {
        sb.append(function[i]);
      } else {
        String type = getCastType(columns.getType(columns.getId(column[i])));
        if ("bool".equals(type) || "boolean".equals(type)) {
          sb.append("?");
        } else {
          sb.append("?::").append(quoteType(type));
        }
      }
    }
    sb.append(')');
    String sql = sb.toString();

    try (
        Connection conn = DatabasePool.getConnection(settings);
        PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
      // stmt.setEscapeProcessing(false);
      int pos = 1;
      for (int i = 0; i < column.length; i++) {
        if (function[i] == null || function[i].length() == 0) {
          String type = getCastType(columns.getType(columns.getId(column[i])));
          String val = values[i];
          if ("bool".equals(type) || "boolean".equals(type)) {
            if (val == null) {
              stmt.setNull(pos++, Types.BOOLEAN);
            } else if ("true".equals(val)) {
              stmt.setBoolean(pos++, true);
            } else if ("false".equals(val)) {
              stmt.setBoolean(pos++, false);
            } else {
              throw new AssertionError("value should be null, \"true\", or \"false\": " + val);
            }
          } else {
            stmt.setString(pos++, val);
          }
        }
      }
      stmt.executeUpdate();
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

    StringBuilder sb = new StringBuilder("DELETE FROM ").append(quoteTable(table));
    for (int i = 0; i < primaryKeys.length; i++) {
      sb.append(i == 0 ? " WHERE " : " AND ");
      if (primaryKeyValues[i] == null) {
        appendIsNull(sb, primaryKeys[i]);
      } else {
        String type = getCastType(columns.getType(columns.getId(primaryKeys[i])));
        sb.append(quoteColumn(primaryKeys[i])).append("=?::").append(quoteType(type));
      }
    }
    String sql = sb.toString();

    try (
        Connection conn = DatabasePool.getConnection(settings);
        PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
      // stmt.setEscapeProcessing(false);
      int pos = 1;
      for (String primaryKeyValue : primaryKeyValues) {
        if (primaryKeyValue != null) {
          stmt.setString(pos++, primaryKeyValue);
        }
      }
      stmt.executeUpdate();
    }
  }

  /**
   * Gets a converted type to be used for casts.
   */
  private static String getCastType(String rawType) {
    if ("serial".equals(rawType)) {
      return "integer";
    }
    if ("bigserial".equals(rawType)) {
      return "bigint";
    }
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
    StringBuilder sb = new StringBuilder("SELECT * FROM ").append(quoteTable(table));
    for (int i = 0; i < primaryKeys.size(); i++) {
      sb.append(i == 0 ? " WHERE " : " AND ");
      if (primaryValues.get(i) == null) {
        appendIsNull(sb, primaryKeys.get(i));
      } else {
        String type = getCastType(columns.getType(columns.getId(primaryKeys.get(i))));
        sb.append(quoteColumn(primaryKeys.get(i))).append("=?::").append(quoteType(type));
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
    StringBuilder sb = new StringBuilder("UPDATE ").append(quoteTable(table)).append(" SET ");
    for (int i = 0; i < column.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(quoteColumn(column[i])).append("=");
      if (function[i] != null && function[i].length() > 0) {
        sb.append(function[i]);
      } else {
        String type = getCastType(columns.getType(columns.getId(column[i])));
        sb.append("?::").append(quoteType(type));
      }
    }
    for (int i = 0; i < primaryKeys.length; i++) {
      sb.append(i == 0 ? " WHERE " : " AND ");
      if (primaryKeyValues[i] == null) {
        appendIsNull(sb, primaryKeys[i]);
      } else {
        String type = getCastType(columns.getType(columns.getId(primaryKeys[i])));
        sb.append(quoteColumn(primaryKeys[i])).append("=?::").append(quoteType(type));
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
      for (String primaryKeyValue : primaryKeyValues) {
        if (primaryKeyValue != null) {
          stmt.setString(pos++, primaryKeyValue);
        }
      }
      stmt.executeUpdate();
    }
  }

  @Override
  public boolean isKeyword(String identifier) {
    return Server.ReservedWord.isReservedWord(identifier);
  }

  private static final Pattern TIME_PATTERN = Pattern.compile(
      "^time(stamp)? *(\\( *[0-6] *\\))? +with(out)? +time +zone$",
      Pattern.CASE_INSENSITIVE
  );

  /**
   * Allows a few types that contains spaces.
   * See <a href="https://www.postgresql.org/docs/current/datatype-datetime.html">Date/Time Types</a>.
   */
  @Override
  public String quoteType(String type) {
    if (TIME_PATTERN.matcher(type).matches()) {
      return type;
    } else {
      return super.quoteType(type);
    }
  }
}
