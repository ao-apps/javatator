/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2009, 2018, 2019, 2020, 2021, 2022, 2023, 2024, 2025  AO Industries, Inc.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Methods to view and manipulate database tables.
 */
public class Table {

  /**
   * The current settings.
   */
  private final Settings settings;

  /**
   * Instantiate a new {@link Table}.
   */
  public Table(Settings settings) {
    this.settings = settings;
  }

  public Settings addCheckConstraint(JavatatorWriter out) throws SQLException, IOException {
    final String constraint = settings.getParameter("constraint");
    final String checkClause = settings.getParameter("checkclause");
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : added CHECK constraint ");
    out.print(constraint);
    out.print("</h2>\n");
    settings.getJdbcConnector().addCheckConstraint(constraint, checkClause);
    out.print("Constraint ");
    out.print(constraint);
    out.print(" added successfully with CHECK clause:<br><br>\n");
    Util.printEscapedHtml(out, checkClause);
    return printTableProperties(out);
  }

  public Settings addForeignKey(JavatatorWriter out) throws SQLException, IOException {
    final String constraint = settings.getParameter("constraint");
    final String primaryKey = settings.getParameter("primarykey");
    final String foreignTable = settings.getParameter("foreigntable");
    final String foreignKey = settings.getParameter("foreignkey");
    String matchType = settings.getParameter("match");
    if ("".equals(matchType)) {
      matchType = null;
    }
    final String onDelete = settings.getParameter("ondelete");
    final String onUpdate = settings.getParameter("onupdate");
    final boolean isDeferrable = "DEFERRABLE".equals(settings.getParameter("deferrable"));
    final String initially = settings.getParameter("initially");
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : added foreign key ");
    out.print(constraint);
    out.print("</h2>\n");
    settings.getJdbcConnector().addForeignKey(constraint,
        primaryKey,
        foreignTable,
        foreignKey,
        matchType,
        onDelete,
        onUpdate,
        isDeferrable,
        initially
    );
    out.print("Foreign key ");
    out.print(constraint);
    out.print(" added successfully.<br><br>\n");
    return printTableProperties(out);
  }

  public Settings changePrivileges(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : Change Privileges</h2>\n");
    if ("GRANT".equals(settings.getParameter("type"))) {
      settings.getJdbcConnector().grantPrivileges(settings.getParameter("user"),
          settings.getParameterValues("privileges"));
    } else {
      settings.getJdbcConnector().revokePrivileges(settings.getParameter("user"),
          settings.getParameterValues("privileges"));
    }
    out.print("Privileges for this table have been updated<br><br>\n");
    return printPrivileges(out);
  }

  /**
   * Shows a screen asking if the user wants to delete the specified table.
   */
  public Settings confirmDeleteTable(JavatatorWriter out) throws SQLException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    String table = settings.getTable();
    out.print(table);
    out.print("</h2>\n");
    out.print("Are you sure you want to drop this table <b>");
    out.print(table);
    out.print("</b>?"
        + "<br>\n"
        + "<input type='submit' value='YES' onClick=\"return selectAction('dodelete_table')\"> "
        + "<input type='submit' value='NO' onClick=\"history.go(-1);return false;\"> "
    );
    return settings;
  }

  /**
   * Shows a screen asking if the user doesn't want this column as a foreign key.
   */
  public Settings confirmDropConstraint(JavatatorWriter out) throws SQLException {
    String constraint = settings.getParameter("constraint");
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : constraint ");
    out.print(constraint);
    out.print("</h2>\n"
        + "<b>Are you sure you want to drop constraint ");
    out.print(constraint);
    out.print("?</b><br>\n"
        + "<input type=hidden name=constraint value='");
    Util.printEscapedInputValue(out, constraint);
    out.print("'><br>\n"
        + "Behaviour: <select name=behaviour>\n"
        + "<option value='RESTRICT'>RESTRICT\n"
        + "<option value='CASCADE'>CASCADE\n"
        + "</select>\n"
        + "<input type='submit' value='YES' onClick=\"return selectAction('dodrop_constraint')\"> "
        + "<input type='submit' value='NO' onClick=\"history.go(-1);return false;\">");
    return settings;
  }

  /**
   * Shows a screen asking the user if they want to drop the specified index.
   */
  public Settings confirmDropIndex(JavatatorWriter out) throws SQLException, IOException {
    String indexName = settings.getParameter("indexname");
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : drop index ");
    out.print(indexName);
    out.print("?</h2>\n"
        + "Are you sure you want to drop this index?<br>\n"
        + "<input type='hidden' name='indexname' value=\""
    );
    out.print(indexName);
    out.print("\">"
        + "<input type='submit' value='YES' onClick=\"return selectAction('dodrop_index')\"> "
        + "<input type='submit' value='NO' onClick=\"history.go(-1);return false;\">"
    );
    return settings;
  }

  /**
   * Asks the user to confirm that they want to empty the current table.
   */
  public Settings confirmEmptyTable(JavatatorWriter out) throws SQLException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    String table = settings.getTable();
    out.print(table);
    out.print("</h2>\n"
        + "Are you sure you want to empty this table <b>");
    out.print(table);
    out.print("</b>?"
        + "<br>\n"
        + "<input type='submit' value='YES' onClick=\"return selectAction('doempty_table')\"> "
        + "<input type='submit' value='NO' onClick=\"history.go(-1);return false;\"> ");
    return settings;
  }

  /**
   * Creates a new table using the specified parameters. Shows the details of the current database.
   */
  public Settings createTable(JavatatorWriter out) throws SQLException, IOException {
    int count = 0;
    while (settings.getParameter("newcolumn" + count) != null) {
      count++;
    }
    String[] newColumn = new String[count];
    String[] newType = new String[count];
    String[] newLength = new String[count];
    String[] newDefault = new String[count];
    String[] newNull = new String[count];
    String[] newRemarks = new String[count];
    boolean[] primaryKey = new boolean[count];
    boolean[] indexKey = new boolean[count];
    boolean[] uniqueKey = new boolean[count];
    for (int i = 0; i < count; i++) {
      newColumn[i] = settings.getParameter("newcolumn" + i);
      newType[i] = settings.getParameter("newtype" + i);
      newLength[i] = settings.getParameter("newlength" + i);
      newDefault[i] = settings.getParameter("newdefault" + i);
      newNull[i] = settings.getParameter("newnull" + i);
      newRemarks[i] = settings.getParameter("newremarks" + i);
      primaryKey[i] = settings.getParameter("primarykey" + i) != null;
      indexKey[i] = settings.getParameter("indexkey" + i) != null;
      uniqueKey[i] = settings.getParameter("uniquekey" + i) != null;
    }

    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : created table ");
    out.print(settings.getTable());
    out.print("</h2>\n");
    settings.getJdbcConnector().createTable(
        newColumn,
        newType,
        newLength,
        newDefault,
        newNull,
        newRemarks,
        primaryKey,
        indexKey,
        uniqueKey
    );
    out.print("Table created successfully.\n"
        + "<script language=javascript><!--\n"
        + "top.top_frame.reloadMenu();\n"
        + "//--></script>\n");
    return new Database(settings).printDatabaseDetails(out);
  }

  /**
   * Deletes the current table and shows a success message. Shows the details of the current database.
   */
  public Settings deleteTable(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : deleting table ");
    out.print(settings.getTable());
    out.print("</h2>\n");
    settings.getJdbcConnector().dropTable();
    out.print("Table deleted successfully.\n"
        + "<script language=javascript><!--\n"
        + "top.top_frame.deleteTable('");
    out.print(settings.getTable());
    out.print("');\n"
        + "top.top_frame.drawMenu(top.left_frame.window.document);\n"
        + "//--></script>\n");
    return new Database(settings.setTable("")).printDatabaseDetails(out);
  }

  /**
   * Removes the specified constraint.
   */
  public Settings dropConstraint(JavatatorWriter out) throws SQLException, IOException {
    final String constraint = settings.getParameter("constraint");
    final String behaviour = settings.getParameter("behaviour");
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : constraint ");
    out.print(constraint);
    out.print(" dropped</h2>\n");
    settings.getJdbcConnector().dropConstraint(constraint, behaviour);
    out.print("Constraint ");
    out.print(constraint);
    out.print(" has been dropped successfully.");
    return printTableProperties(out);
  }

  /**
   * Drops the specified index from this table.
   */
  public Settings dropIndex(JavatatorWriter out) throws SQLException, IOException {
    String indexName = settings.getParameter("indexname");
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : dropping index ");
    out.print(indexName);
    out.print("</h2>\n");
    settings.getJdbcConnector().dropIndex(indexName);
    out.print("Index dropped successfully.");
    return printTableProperties(out);
  }

  public Settings dumpTable(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : dumped table ");
    out.print(settings.getTable());
    out.print("</h2>\n"
        + "#<br>\n"
        + "# Data dump generated by Javatator ");
    out.print(Maven.properties.get("project.version"));
    out.print("<br>\n"
        + "# Please report anything strange to jason@javatator.com<br>\n"
        + "#<br><br>\n");

    HtmlWriter html = new HtmlWriter(out);
    JdbcConnector conn = settings.getJdbcConnector();
    if (settings.getParameter("structure") != null) {
      conn.dumpTableStructure(html);
    }
    if (settings.getParameter("data") != null) {
      conn.dumpTableContents(html);
    }
    return printTableProperties(out);
  }

  /**
   * Empties the current table and shows a success message. Shows the details of the current database.
   */
  public Settings emptyTable(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : emptied table ");
    out.print(settings.getTable());
    out.print("</h2>\n");
    settings.getJdbcConnector().emptyTable();
    out.print("Table emptied successfully.");
    return new Database(settings).printDatabaseDetails(out);
  }

  /**
   * Gets the startpos from the current {@link Settings}.
   */
  private int getStartPos() {
    String s = settings.getParameter("startpos");
    if (s != null && s.length() > 0) {
      return Integer.parseInt(s);
    }
    return 0;
  }

  public Settings printAddCheckConstraint(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : add CHECK constraint</h2>\n"
        + "Constraint name: <input type=text name=constraint value=''><br>\n"
        + "CHECK clause: <input type=text name=checkclause value=''><br>\n"
        + "<input type=submit value='Go!' onClick=\"return selectAction('doadd_checkconstraint')\">\n "
        + "<input type=submit value='<< Back' onClick=\"history.go(-1);return false;\">");
    return settings;
  }

  public Settings printAddForeignKey(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : add foreign key</h2>\n");
    final JdbcConnector conn = settings.getJdbcConnector();
    final List<String> tables = conn.getTables();
    final List<String> columns = conn.getColumns().getNames();
    out.startTable(null, "cellspacing=1");
    out.startTr();
    out.printTd("Constraint name:");
    out.printTd("<input type=text name=constraint value=''>");
    out.endTr();
    out.startTr();
    out.printTd("Column:");
    out.startTd();
    out.print("<select name=primarykey>\n");
    int size = columns.size();
    for (int i = 0; i < size; i++) {
      out.print("<option value='");
      out.print(columns.get(i));
      out.print("'>");
      out.print(columns.get(i));
      out.print('\n');
    }
    out.print("</select>\n");
    out.endTd();
    out.endTr();
    out.startTr();
    out.startTd("colspan=2");
    out.print("References table <select name=foreigntable>\n");
    size = tables.size();
    for (int i = 0; i < size; i++) {
      out.print("<option value='");
      out.print(tables.get(i));
      out.print("'>");
      out.print(tables.get(i));
      out.print('\n');
    }
    out.print("</select>\n");
    out.print("column <input type=text name=foreignkey value=''>\n");
    out.endTd();
    out.endTr();
    out.startTr();
    out.printTd("MATCH:");
    out.printTd("<select name=match>\n"
        + " <option value=''>[DEFAULT]\n"
        + " <option value=PARTIAL>PARTIAL\n"
        + " <option value=ALL>ALL\n"
        + "</select>");
    out.endTr();
    out.startTr();
    out.printTd("ON DELETE:");
    out.printTd("<select name=ondelete>\n"
        + " <option value='NO ACTION'>NO ACTION\n"
        + " <option value=RESTRICT>RESTRICT\n"
        + " <option value=CASCADE>CASCADE\n"
        + " <option value='SET NULL'>SET NULL\n"
        + " <option value='SET DEFAULT'>SET DEFAULT\n"
        + "</select>");
    out.endTr();
    out.startTr();
    out.printTd("ON UPDATE:");
    out.printTd("<select name=onupdate>\n"
        + " <option value='NO ACTION'>NO ACTION\n"
        + " <option value=RESTRICT>RESTRICT\n"
        + " <option value=CASCADE>CASCADE\n"
        + " <option value='SET NULL'>SET NULL\n"
        + " <option value='SET DEFAULT'>SET DEFAULT\n"
        + "</select>");
    out.endTr();
    out.startTr();
    out.printTd("<select name=deferrable>\n"
        + "<option value='NOT DEFERRABLE'>NOT DEFERRABLE\n"
        + "<option value='DEFERRABLE'>DEFERRABLE\n"
        + "</select>", "colspan=2");
    out.endTr();
    out.startTr();
    out.printTd("INITIALLY:");
    out.printTd("<select name=initially>\n"
        + "<option value=IMMEDIATE>IMMEDIATE\n"
        + "<option value=DEFERRED>DEFERRED\n"
        + "</select>");
    out.endTr();
    out.endTable();
    out.print("<input type=submit value='Go!' onClick=\"return selectAction('doadd_foreignkey')\">\n "
        + "<input type=submit value='<< Back' onClick=\"history.go(-1);return false;\">");
    return settings;
  }

  /**
   * Displays a form enabling the user to create a new table.
   */
  public Settings printCreateTable(JavatatorWriter out) throws SQLException, IOException {
    final int columns = Integer.parseInt(settings.getParameter("numcolumns"));
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print("</h2>\n");

    out.startTable(null, "cellspacing=1");

    out.startTr();
    out.printTh("Column");
    out.printTh("Type");
    out.printTh("Length/Set");
    out.printTh("Nullable");
    out.printTh("Default");
    out.printTh("Remarks");
    out.printTh("Primary");
    out.printTh("Index");
    out.printTh("Unique");
    out.endTr();

    List<String> types = settings.getJdbcConnector().getTypes();
    int size = types.size();

    for (int i = 0; i < columns; i++) {
      out.startTr();

      out.startTd();
      out.print("<input type='text' name='newcolumn");
      out.print(i);
      out.print("' size=10>");
      out.endTd();

      out.startTd();
      out.print("<select name='newtype");
      out.print(i);
      out.print("'>\n");
      for (int c = 0; c < size; c++) {
        out.print("<option value=\"");
        String type = types.get(c);
        out.print(type);
        out.print("\">");
        out.print(type);
      }
      out.print("</select>");
      out.endTd();

      out.startTd();
      out.print("<input type='text' name='newlength");
      out.print(i);
      out.print("' size=8>");
      out.endTd();

      out.startTd();
      out.print("<select name='newnull");
      out.print(i);
      out.print("'>\n"
          + "<option value='not null' selected>NO</option>\n"
          + "<option value='null'>YES</option>\n"
          + "</select>");
      out.endTd();

      out.startTd();
      out.print("<input type='text' name='newdefault");
      out.print(i);
      out.print("' size=12>");
      out.endTd();

      out.startTd();
      out.print("<input type='text' name='newremarks");
      out.print(i);
      out.print("' size=12>");
      out.endTd();

      out.startTd();
      out.print("<input type='checkbox' name='primarykey");
      out.print(i);
      out.print("' size=12 value='yes'>");
      out.endTd();

      out.startTd();
      out.print("<input type='checkbox' name='indexkey");
      out.print(i);
      out.print("' size=12 value='yes'>");
      out.endTd();

      out.startTd();
      out.print("<input type='checkbox' name='uniquekey");
      out.print(i);
      out.print("' size=12 value='yes'>");
      out.endTd();

      out.endTr();
    }
    out.endTable();
    out.print("<br><input type='submit' value='Save!' onClick=\"return selectAction('docreate_table')\">");
    return settings;
  }

  public Settings printDumpTable(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : dumping table ");
    out.print(settings.getTable());
    out.print("</h2>\n"
        + "<input type=checkbox name=structure value=yes> Structure<br>\n"
        + "<input type=checkbox name=data value=yes> Data<br>\n"
        + "<input type=checkbox name=send value=yes> Send<br>\n"
        + "<br><input type=submit value='Go!' onClick=\"return selectAction('dodump_table');\">\n"
        + "<input type=submit value='<< Back' onClick=\"history.go(-1);return false;\">");
    return settings;
  }

  private void printPreviousNext(JavatatorWriter out, int startPos, int numrows, int totalRows, int which) {
    if (startPos > 0) {
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
          + " return selectAction('doselect')\">&nbsp;&nbsp;&nbsp;");
    }

    // Only show the remaining if there are some that are not visible
    int remaining = totalRows - startPos - numrows;
    out.print("<b>Next:</b> <input type='text' name='newnumrows");
    out.print(which);
    out.print("' size=4 value='");
    out.print(numrows);
    out.print("'> rows starting at: "
        + "<input type='text' size=4 name='startpos");
    out.print(which);
    out.print("' value='");
    out.print(startPos + numrows);
    out.print("'> <input type='submit' value='Go!' onClick=\"setNumRows(this.form.newnumrows");
    out.print(which);
    out.print(".value); setStartPos(this.form.startpos");
    out.print(which);
    out.print(".value); return selectAction('doselect');\">");
  }

  /**
   * Allows the user to grant/revoke access to this table.
   */
  public Settings printPrivileges(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : Privileges</h2>\n"
        + "Privileges for this table:<br><br>\n");
    JdbcConnector conn = settings.getJdbcConnector();
    out.startTable(null, "cellspacing=1");
    out.startTr();
    out.printTh("Grantor");
    out.printTh("Grantee");
    out.printTh("Privileges");
    out.printTh("Grantable?");
    out.endTr();
    try {
      TablePrivileges tp = conn.getTablePrivileges();
      List<String> grantors = tp.getGrantors();
      List<String> grantees = tp.getGrantees();
      List<String> privileges = tp.getPrivileges();
      List<JdbcConnector.Boolean> isGrantable = tp.getIsGrantable();
      int size = grantors.size();
      for (int i = 0; i < size; i++) {
        out.startTr();
        out.printTd(grantors.get(i));
        out.printTd(grantees.get(i));
        out.printTd(privileges.get(i));
        JdbcConnector.Boolean g = isGrantable.get(i);
        out.printTd((g == JdbcConnector.Boolean.TRUE) ? "YES" : (g == JdbcConnector.Boolean.FALSE) ? "NO" : "Unknown");
        out.endTr();
      }
    } finally {
      out.endTable();
    }
    out.print("<br><br>");
    out.startTable(null, "cellspacing=1");
    try {
      out.startTr();
      out.printTd("Action:");
      out.printTd("<select name=type>\n"
          + "<option value=GRANT>GRANT\n"
          + "<option value=REVOKE>REVOKE\n"
          + "</select>");
      out.endTr();
      out.startTr();
      out.printTd("User:");
      out.printTd("<input type=text name=user>");
      out.endTr();
      out.startTr();
      out.printTd("Privileges:");
      out.startTd();
      try {
        List<String> v = conn.getPossiblePrivileges();
        int size = v.size();
        for (int i = 0; i < size; i++) {
          String s = v.get(i);
          out.print("<input type=checkbox name=privileges value='");
          out.print(s);
          out.print("'> ");
          out.print(s);
          out.print('\n');
        }
      } finally {
        out.endTd();
        out.endTr();
      }
      out.startTr();
      out.printTd("On table:");
      out.printTd(settings.getTable());
      out.endTr();
    } finally {
      out.endTable();
    }
    out.print("<br><input type=submit value='Go!' "
        + "onClick=\"return selectAction('change_table_privileges')\">\n");
    return settings;
  }

  /**
   * Shows a screen allowing the user to issue SELECT query from the current table.
   */
  public Settings printSelect(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print("</h2>\n"
        + "SELECT columns (at least one):<br>\n"
        + "<select multiple name='columns' size=10>");

    JdbcConnector conn = settings.getJdbcConnector();
    Columns columns = conn.getColumns();
    final List<String> names = columns.getNames();
    final List<String> types = columns.getTypes();
    // List<String> lengths=columns.getLengths();
    final List<JdbcConnector.Boolean> areNullable = columns.areNullable();
    final List<String> defaults = columns.getDefaults();
    int size = names.size();
    for (int i = 0; i < size; i++) {
      out.print("<option selected value='");
      String column = names.get(i);
      out.print(column);
      out.print("'>");
      out.print(column);
      out.print("</option>\n");
    }
    out.print("</select><br><br>\n"
        + "Show: <input type='text' name='newnumrows' size=4 value='");
    out.print(settings.getNumRows());
    out.print("'> rows per page.<br><br>\n"
        + "Search Conditions:\n"
        + "<input type=text name='selectwhere' value=''>\n"
        + "(Body of the WHERE clause)<br><br>\n"
        + "Search by example (LIKE comparator - use % as wildcard):<br><br>\n");

    out.startTable(null, "cellspacing=1");

    out.startTr();
    out.printTh("Column");
    out.printTh("Type");
    out.printTh("Value");
    out.endTr();

    for (int i = 0; i < size; i++) {
      final String columnName = names.get(i);
      final String columnType = types.get(i);
      final String columnDefault = defaults.get(i);
      final JdbcConnector.Boolean isNullable = areNullable.get(i);

      out.startTr();

      out.startTd();
      out.print(columnName);
      out.print("<input type='hidden' name='scolumn");
      out.print(i);
      out.print("' value='");
      out.print(columnName);
      out.print("'>");
      out.endTd();

      out.printTd(columnType);

      out.startTd();

      if ((columnDefault == null || columnDefault.charAt(0) != 'F') && columnType.toUpperCase().endsWith("TEXT")) {
        out.print("<textarea rows=16 cols=80 name='value");
        out.print(i);
        out.print("'>");
        if (columnDefault != null) {
          Util.printEscapedInputValue(out, columnDefault.substring(1));
        }
        out.print("</textarea>");
      } else {
        out.print("<input type='text' size=32 name='value");
        out.print(i);
        out.print("' value='");
        if (columnDefault != null) {
          Util.printEscapedInputValue(out, columnDefault.substring(1));
        }
        out.print("'>");
      }
      if (isNullable == JdbcConnector.Boolean.TRUE) {
        out.print("<input type='checkbox' name='null");
        out.print(i);
        out.print("' value='NULL'");
        if (columnDefault == null) {
          out.print(" checked");
        }
        out.print("> NULL");
      }
      out.endTd();

      out.endTr();
    }
    out.endTable();

    out.print("<br><br>\n"
        + "<input type='submit' value='Go!' onClick=\"setNumRows(this.form.newnumrows.value); return selectAction('doselect')\">");
    return settings;
  }

  /**
   * Prints the columns and attributes of the current table.
   */
  public Settings printTableProperties(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print("</h2>\n");

    out.startTable(null, "cellspacing=1");

    JdbcConnector conn = settings.getJdbcConnector();

    try {
      out.startTr();
      out.printTh("Column");
      out.printTh("Type");
      out.printTh("Length/Set");
      out.printTh("Nullable");
      out.printTh("Default");
      out.printTh("Remarks");
      out.printTh("Options");
      out.endTr();

      Columns columns = conn.getColumns();
      List<String> names = columns.getNames();
      List<String> types = columns.getTypes();
      List<String> lengths = columns.getLengths();
      List<JdbcConnector.Boolean> areNullable = columns.areNullable();
      List<String> defaults = columns.getDefaults();
      int size = names.size();
      for (int i = 0; i < size; i++) {
        final String columnName = names.get(i);
        final String columnDefault = defaults.get(i);
        out.startTr();
        out.printTd(columnName);
        out.printTd(types.get(i));
        out.printTd(lengths.get(i));
        out.printTd(JdbcConnector.getBooleanString(areNullable.get(i)));
        if (columnDefault == null) {
          out.printTd("");
        } else if (columnDefault.length() == 1) {
          out.printTd("&nbsp;");
        } else {
          out.printTd(columnDefault.substring(1));
        }
        String rem = conn.getRemark(columnName);
        if (rem == null) {
          out.printTd("");
        } else if (rem.length() == 0) {
          out.printTd("&nbsp;");
        } else {
          out.printTd(rem);
        }

        out.startTd();
        out.print("<a href=\"javascript:setColumn('");
        out.print(columnName);
        out.print("'); selectAction('edit_column');\">Edit</a>&nbsp;|&nbsp;"
            + "<a href=\"javascript:setColumn('");
        out.print(columnName);
        out.print("'); selectAction('delete_column');\">Drop</a>&nbsp;|&nbsp;"
            + "<a href=\"javascript:setColumn('");
        out.print(columnName);
        out.print("'); selectAction('add_primarykey');\">Primary</a>&nbsp;|&nbsp;"
            + "<a href=\"javascript:setColumn('");
        out.print(columnName);
        out.print("'); selectAction('add_index');\">Index</a>&nbsp;|&nbsp;"
            + "<a href=\"javascript:setColumn('");
        out.print(columnName);
        out.print("'); selectAction('add_uniqueindex');\">Unique</a>");
        out.endTd();

        out.endTr();
      }
    } finally {
      out.endTable();
    }
    out.print("<br><br>\n"
        + "<a href=\"javascript:selectAction('doselect');\">Explore</a> "
        + "| <a href=\"javascript:selectAction('insert');\">Insert new row</a> "
        + "| <a href=\"javascript:selectAction('delete_table');\">Drop</a> "
        + "| <a href=\"javascript:selectAction('select');\">Select</a> "
        + "| <a href=\"javascript:selectAction('empty_table');\">Empty</a> "
        + "| <a href=\"javascript:selectAction('dump_table');\">Dump</a> "
        + "<br><br>\n"
        + "<a href=\"javascript:selectAction('add_column');\">Add New Column</a>\n"
        + "<br><br>\n");

    // Obtain all the primary key names and columns for this table
    PrimaryKeys pkeys = conn.getPrimaryKeys();
    List<String> primaryKeyNames = pkeys.getNames();
    List<String> primaryKeyColumns = pkeys.getColumns();
    int primaryKeyCount = primaryKeyNames.size();

    if (primaryKeyCount > 0) {
      out.print("<b>Primary Keys:</b><br>\n");
      out.startTable(null, "cellspacing=1");
      try {
        out.startTr();
        out.printTh("Key name");
        out.printTh("Column");
        out.printTh("Options");
        out.endTr();

        for (int i = 0; i < primaryKeyCount; i++) {
          out.startTr();
          out.printTd(primaryKeyNames.get(i));
          out.printTd(primaryKeyColumns.get(i));
          out.startTd();
          out.print("<a href=\"javascript:setColumn('");
          out.print(primaryKeyColumns.get(i));
          out.print("');selectAction('drop_primarykey');\">Drop</a>");
          out.endTd();
          out.endTr();
        }
      } finally {
        out.endTable();
        out.print("<br><br>");
      }
    }

    Indexes indexes = conn.getIndexes();
    List<String> indexNames = indexes.getNames();
    List<JdbcConnector.Boolean> areUnique = indexes.areUnique();
    List<String> colNames = indexes.getColumns();
    int size = indexNames.size();
    if (size > 0) {
      out.print("<input type='hidden' name='indexname'>"
          + "<b>Indexes:</b><br>\n");

      out.startTable(null, "cellspacing=1");
      try {
        out.startTr();
        out.printTh("Key name");
        out.printTh("Column");
        out.printTh("Unique");
        out.printTh("Options");
        out.endTr();

        for (int i = 0; i < size; i++) {
          out.startTr();

          out.printTd(indexNames.get(i));
          out.printTd(colNames.get(i));
          out.printTd(JdbcConnector.getBooleanString(areUnique.get(i)));

          out.startTd();
          out.print("<a href=\"javascript:setColumn('");
          out.print(colNames.get(i));
          out.print("'); setIndexName('");
          out.print(indexNames.get(i));
          out.print("');selectAction('drop_index');\">Drop</a>");
          out.endTd();

          out.endTr();
        }
      } finally {
        out.endTable();
        out.print("<br><br>\n");
      }
    }

    if (conn.supportsForeignKeys()) {
      ForeignKeys importedKeys = conn.getImportedKeys();
      ForeignKeys exportedKeys = conn.getExportedKeys();
      int importedSize = (importedKeys != null) ? importedKeys.getSize() : -1;
      int exportedSize = (exportedKeys != null) ? exportedKeys.getSize() : -1;
      if (importedSize > 0 || exportedSize > 0) {
        out.print("<b>Foreign Keys:</b><br>\n"
            + "<input type=hidden name=constraint value=''>");

        out.startTable(null, "cellspacing=1");
        try {
          out.startTr();
          out.printTh("Name");
          out.printTh("Foreign Key");
          out.printTh("Primary Key");
          out.printTh("Insert Rule");
          out.printTh("Delete Rule");
          out.printTh("Update Rule");
          out.printTh("Deferrability");
          out.printTh("Initially Deferred");
          out.printTh("Options");
          out.endTr();

          if (importedSize > 0) {
            assert importedKeys != null;
            List<String> constraintNames = importedKeys.getConstraintNames();
            List<String> foreignTables = importedKeys.getForeignTables();
            List<String> foreignKeys = importedKeys.getForeignKeys();
            List<String> primaryTables = importedKeys.getPrimaryTables();
            List<String> primaryKeys = importedKeys.getPrimaryKeys();
            List<String> insertRules = importedKeys.getInsertRules();
            List<String> deleteRules = importedKeys.getDeleteRules();
            List<String> updateRules = importedKeys.getUpdateRules();
            List<JdbcConnector.Boolean> areDeferrable = importedKeys.areDeferrable();
            List<JdbcConnector.Boolean> areInitiallyDeferred = importedKeys.areInitiallyDeferred();

            for (int i = 0; i < importedSize; i++) {
              String constraint = constraintNames.get(i);

              out.startTr();
              out.printTd(Util.escapeHtml(constraint));
              out.printTd(Util.escapeHtml(foreignTables.get(i) + '.' + foreignKeys.get(i)));
              out.printTd(Util.escapeHtml(primaryTables.get(i) + '.' + primaryKeys.get(i)));
              out.printTd(insertRules.get(i));
              out.printTd(deleteRules.get(i));
              out.printTd(updateRules.get(i));
              out.printTd(JdbcConnector.getBooleanString(areDeferrable.get(i)));
              out.printTd(JdbcConnector.getBooleanString(areInitiallyDeferred.get(i)));
              out.printTd("<a href=\"javascript:setConstraint('"
                  + Util.escapeJavaScript(constraint)
                  + "');selectAction('drop_constraint');\">Drop</a>");
              out.endTr();
            }
          }

          if (exportedSize > 0) {
            assert exportedKeys != null;
            List<String> constraintNames = exportedKeys.getConstraintNames();
            List<String> foreignTables = exportedKeys.getForeignTables();
            List<String> foreignKeys = exportedKeys.getForeignKeys();
            List<String> primaryTables = exportedKeys.getPrimaryTables();
            List<String> primaryKeys = exportedKeys.getPrimaryKeys();
            List<String> insertRules = exportedKeys.getInsertRules();
            List<String> deleteRules = exportedKeys.getDeleteRules();
            List<String> updateRules = exportedKeys.getUpdateRules();
            List<JdbcConnector.Boolean> areDeferrable = exportedKeys.areDeferrable();
            List<JdbcConnector.Boolean> areInitiallyDeferred = exportedKeys.areInitiallyDeferred();

            for (int i = 0; i < exportedSize; i++) {
              String constraint = constraintNames.get(i);

              out.startTr();
              out.printTd(Util.escapeHtml(constraint));
              out.printTd(Util.escapeHtml(foreignTables.get(i) + '.' + foreignKeys.get(i)));
              out.printTd(Util.escapeHtml(primaryTables.get(i) + '.' + primaryKeys.get(i)));
              out.printTd(insertRules.get(i));
              out.printTd(deleteRules.get(i));
              out.printTd(updateRules.get(i));
              out.printTd(JdbcConnector.getBooleanString(areDeferrable.get(i)));
              out.printTd(JdbcConnector.getBooleanString(areInitiallyDeferred.get(i)));
              out.printTd("<a href=\"javascript:setConstraint('"
                  + Util.escapeJavaScript(constraint)
                  + "');selectAction('drop_constraint');\">Drop</a>");
              out.endTr();
            }
          }
        } finally {
          out.endTable();
          out.print("<br><br>\n");
        }
      }
      out.print("<a href=\"javascript:selectAction('add_foreignkey')\">Add new foreign key</a><br><br>\n");
    }
    if (conn.supportsCheckConstraints()) {
      CheckConstraints checks = conn.getCheckConstraints();
      List<String> names = checks.getNames();
      size = names.size();
      if (size > 0) {
        List<String> checkClauses = checks.getCheckClauses();
        out.print("<b>Check Constraints:</b><br>\n");
        out.startTable(null, "cellspacing=1");
        try {
          out.startTr();
          out.printTh("Constraint");
          out.printTh("Check Clause");
          out.printTh("Options");
          out.endTr();
          for (int i = 0; i < size; i++) {
            out.startTr();
            out.printTd(names.get(i));
            out.startTd();
            Util.printEscapedHtml(out, checkClauses.get(i));
            out.endTd();
            out.endTr();
          }
        } finally {
          out.endTable();
        }
        out.print("<br><a href=\"javascript:selectAction('add_checkconstraint')\">Add new CHECK constraint</a><br><br>\n");
      }
    }
    out.print("<b>Rename table to: </b>"
        + "<input type='text' name='newname'>\n"
        + "<input type='submit' value='Go!' onClick=\"return selectAction('rename_table');\">");
    return settings;
  }

  /**
   * Process the {@link Settings} object and decide what to do.
   */
  public Settings processRequest(JavatatorWriter out) throws SQLException, IOException {
    String action = settings.getAction();
    if ("explore".equals(action)) {
      return select(out);
    } else if ("properties".equals(action)) {
      return printTableProperties(out);
    } else if ("delete_table".equals(action)) {
      return confirmDeleteTable(out);
    } else if ("dodelete_table".equals(action)) {
      return deleteTable(out);
    } else if ("empty_table".equals(action)) {
      return confirmEmptyTable(out);
    } else if ("doempty_table".equals(action)) {
      return emptyTable(out);
    } else if ("create_table".equals(action)) {
      return printCreateTable(out);
    } else if ("docreate_table".equals(action)) {
      return createTable(out);
    } else if ("rename_table".equals(action)) {
      return renameTable(out);
    } else if ("drop_index".equals(action)) {
      return confirmDropIndex(out);
    } else if ("dodrop_index".equals(action)) {
      return dropIndex(out);
    } else if ("select".equals(action)) {
      return printSelect(out);
    } else if ("doselect".equals(action)) {
      return select(out);
    } else if ("table_privileges".equals(action)) {
      return printPrivileges(out);
    } else if ("change_table_privileges".equals(action)) {
      return changePrivileges(out);
    } else if ("drop_constraint".equals(action)) {
      return confirmDropConstraint(out);
    } else if ("dodrop_constraint".equals(action)) {
      return dropConstraint(out);
    } else if ("dump_table".equals(action)) {
      return printDumpTable(out);
    } else if ("dodump_table".equals(action)) {
      return dumpTable(out);
    } else if ("add_checkconstraint".equals(action)) {
      return printAddCheckConstraint(out);
    } else if ("doadd_checkconstraint".equals(action)) {
      return addCheckConstraint(out);
    } else if ("add_foreignkey".equals(action)) {
      return printAddForeignKey(out);
    } else if ("doadd_foreignkey".equals(action)) {
      return addForeignKey(out);
    } else if ("add_column".equals(action)) {
      return new Column(settings).printAddColumn(out);
    } else {
      return new Row(settings).processRequest(out);
    }
  }

  /**
   * Renames an existing table.
   */
  public Settings renameTable(JavatatorWriter out) throws SQLException, IOException {
    String newTable = settings.getParameter("newname");
    String table = settings.getTable();

    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : renaming table ");
    out.print(table);
    out.print(" to ");
    out.print(newTable);
    out.print("</h2>\n");
    settings.getJdbcConnector().renameTable(newTable);
    out.print("Table ");
    out.print(table);
    out.print(" renamed to ");
    out.print(newTable);
    out.print('.');
    return new Database(settings.setTable(newTable)).printDatabaseDetails(out);
  }

  /**
   * Allows exploration/browsing of a database table.
   */
  public Settings select(JavatatorWriter out) throws SQLException, IOException {
    // Print out the name of the database that is currently being accessed
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print("</h2>\n");

    // Get the connector that will be used to access the database
    JdbcConnector conn = settings.getJdbcConnector();

    // Build the SQL select statement
    int startPos = getStartPos();         // The row number of the first that will be returned
    int numrows = settings.getNumRows();  // The maximum number of rows for this query
    String selectCols;                  // The list of all columns that are being selected
    String selectWhere;                 // The settings provided where clause
    String fullQuery;                   // The result of the SQL generation
    {
      int count = 0;
      final String limitClause = conn.getLimitClause(startPos, numrows);
      final String sortColumn = settings.getSortColumn();

      while (settings.getParameter("scolumn" + count) != null) {
        count++;
      }

      String[] colNames = new String[count];
      String[] colValues = new String[count];
      boolean[] likeComparators = new boolean[count];
      for (int i = 0; i < count; i++) {
        colNames[i] = settings.getParameter("scolumn" + i);
        if (settings.getParameter("null" + i) != null) {
          colValues[i] = null;
        } else {
          colValues[i] = settings.getParameter("value" + i);
        }
        likeComparators[i] = settings.getParameter("like" + i) != null;
      }
      String[] selectColNames = settings.getParameterValues("columns");

      if (count > 0 && "doselect".equals(settings.getAction())) {
        StringBuilder sb = new StringBuilder();
        int size = (selectColNames == null) ? 0 : selectColNames.length;
        for (int i = 0; i < size; i++) {
          if (i > 0) {
            sb.append(',');
          }
          sb.append(conn.quoteColumn(selectColNames[i]));
        }
        if (size == 0) {
          sb.append('*');
        }
        selectCols = sb.toString();
        sb.setLength(0);
        sb.append(conn.getSelectWhereClause(colNames, colValues));
        if (!"".equals(settings.getParameter("selectwhere"))) {
          if (sb.length() > 0) {
            sb.append(" AND ");
          }
          sb.append(settings.getParameter("selectwhere"));
        }
        selectWhere = sb.toString();
      } else {
        selectCols = settings.getParameter("selectcols");
        selectWhere = settings.getParameter("selectwhere");
      }
      StringBuilder query = new StringBuilder()
          .append("SELECT ")
          .append((selectCols == null || "".equals(selectCols))
              ? "*" :
              selectCols
          ).append(" FROM ")
          .append(conn.quoteTable(settings.getTable()));
      if (selectWhere != null && !"".equals(selectWhere)) {
        query.append(" WHERE ").append(selectWhere);
      }

      //String sortClause = "";
      if (sortColumn != null && !"".equals(sortColumn)) {
        query.append(" ORDER BY ").append(conn.quoteColumn(sortColumn)).append(' ').append(settings.getSortOrder());
      }
      if (limitClause != null) {
        query.append(' ').append(limitClause);
      }
      fullQuery = query.toString();
    }

    int totalRows;    // The total number of rows in the database
    if (selectCols == null && selectWhere == null) {
      totalRows = conn.countRecords();
      out.print("<b>Exploring records ");
      out.print(startPos);
      out.print(" - ");
      out.print(Math.min(totalRows, startPos + numrows - 1));
      out.print(" out of ");
      out.print(totalRows);
      out.print(" total.</b><br><br>\n");
    } else {
      totalRows = 0;
      out.print("<b>Results of query:</b> ");
      Util.printEscapedHtml(out, fullQuery);
      out.print("<br><br>\n");
    }
    // Store the hidden fields for later use
    out.print("<input type=hidden name='selectcols' value='");
    out.print((selectCols == null) ? "" : Util.escapeInputValue(selectCols));
    out.print("'>\n" + "<input type=hidden name='selectwhere' value='");
    out.print((selectWhere == null) ? "" : Util.escapeInputValue(selectWhere));
    out.print("'>\n" + "<input type=hidden name='primarykeys'>\n" + "<input type=hidden name='values'>\n" + "<input type=hidden name='startpos' value='");
    out.print(startPos);
    out.print("'>\n");

    // Print out the previous and next form
    printPreviousNext(out, startPos, numrows, totalRows, 1);
    out.print("<br>\n");
    out.print("<a href=\"javascript:selectAction('insert');\">Insert new row</a><br><br>\n");

    // Print the table of results
    out.startTable(null, "cellspacing=1");
    try {
      try (Connection dbcon = DatabasePool.getConnection(settings)) {
        // Get the list of all primary keys for this database/table
        List<String> primaryKeyCols = conn.getPrimaryKeys().getColumns();
        StringBuilder primaryKeysSb = new StringBuilder();
        String primaryKeysString;

        // The number of columns in the results
        int columnCount;

        // The unique IDs of the imported key for each column (what this column references)
        List<Integer> importedKeyIds;
        ForeignKeys importedKeys = conn.getImportedKeys();
        //for (int c=0;c<importedKeys.getSize();c++) {
        //    System.err.println("importedKeys: "+importedKeys.getForeignTable(c)+"."+importedKeys.getForeignKey(c)+"→"+importedKeys.getPrimaryTable(c)+"."+importedKeys.getPrimaryKey(c));
        //}
        // The unique IDs of the columns that reference this table
        List<List<Integer>> exportedIds;
        ForeignKeys exportedKeys = conn.getExportedKeys();
        //for (int c=0;c<exportedKeys.getSize();c++) {
        //    System.err.println("exportedKeys: "+exportedKeys.getForeignTable(c)+"."+exportedKeys.getForeignKey(c)+"→"+exportedKeys.getPrimaryTable(c)+"."+exportedKeys.getPrimaryKey(c));
        //}

        // The type of each column
        List<String> columnTypes;

        // The name for each column
        List<String> columnNames;

        // The results that are returned for each column
        List<List<String>> resultCopies;

        // The number of results that are returned
        int resultSize;

        try (
            Statement stmt = dbcon.createStatement();
            ResultSet results = stmt.executeQuery(fullQuery)
            ) {
          out.startTr();
          // Compile the meta data about the table and print the table header
          ResultSetMetaData metaData = results.getMetaData();
          columnCount = metaData.getColumnCount();
          importedKeyIds = new ArrayList<>(columnCount);
          exportedIds = new ArrayList<>(columnCount);
          columnTypes = new ArrayList<>(columnCount);
          columnNames = new ArrayList<>(columnCount);
          resultCopies = new ArrayList<>(columnCount);
          for (int i = 1; i <= columnCount; i++) {
            String col = metaData.getColumnName(i);
            columnTypes.add(metaData.getColumnTypeName(i));
            columnNames.add(metaData.getColumnName(i));
            String order = "asc";
            if (col.equals(settings.getSortColumn()) && "asc".equals(settings.getSortOrder())) {
              order = "desc";
            }
            out.printTh("<A href=\"javascript:setSortColumn('" + Util.escapeJavaScript(col) + "');setSortOrder('" + order + "');selectAction('doselect');\">" + Util.escapeHtml(col) + "</A>");

            // Build up the list of primary key columns as we iterate through the columns
            if (primaryKeyCols.isEmpty() || primaryKeyCols.contains(col)) {
              if (primaryKeysSb.length() > 0) {
                primaryKeysSb.append(',');
              }
              primaryKeysSb.append(col);
            }

            if (importedKeys != null) {
              importedKeyIds.add(importedKeys.getForeignId(col));
            } else {
              importedKeyIds.add(-1);
            }
            if (exportedKeys != null) {
              exportedIds.add(exportedKeys.getForeignIds(col));
              int exportedIdsSize = exportedIds.get(i - 1).size();
              if (exportedIdsSize > 0) {
                for (int c = 0; c < exportedIdsSize; c++) {
                  List<Integer> ids = exportedIds.get(i - 1);
                  int z = ids.get(c);
                  String foreignTable = exportedKeys.getForeignTable(z);
                  // Also add the column name if this table is referenced more than once
                  boolean foundOther = false;
                  for (int d = 0; d < exportedIdsSize; d++) {
                    if (d != c) {
                      int y = ids.get(d);
                      if (foreignTable.equals(exportedKeys.getForeignTable(y))) {
                        foundOther = true;
                        break;
                      }
                    }
                  }
                  if (foundOther) {
                    out.printTh(foreignTable + "<br>." + exportedKeys.getForeignKey(z));
                  } else {
                    out.printTh(foreignTable);
                  }
                }
              }
            } else {
              List<Integer> emptyList = Collections.emptyList();
              exportedIds.add(emptyList);
            }
            resultCopies.add(new ArrayList<>());
          }
          out.printTh("Options");
          out.endTr();
          primaryKeysString = Util.escapeJavaScript(primaryKeysSb.toString());
          resultSize = 0;
          while (resultSize < numrows && results.next()) {
            resultSize++;
            for (int i = 1; i <= columnCount; i++) {
              String s = results.getString(i);
              resultCopies.get(i - 1).add(s);
            }
          }
        }

        // This pass displays the results
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < resultSize; row++) {
          sb.setLength(0);
          out.startTr();
          for (int column = 0; column < columnCount; column++) {
            String s = resultCopies.get(column).get(row);
            out.printTd(
                (s == null) ? ""
                    : (s.length() == 0) ? "&nbsp;"
                    : (importedKeyIds.get(column) < 0) ? Util.escapeHtml(s)
                    : "<A href=\"javascript:select('"
                    + Util.escapeJavaScript(
                    importedKeys.getPrimaryTable(importedKeyIds.get(column))
                ) + "','" + Util.escapeJavaScript(
                    conn.quoteColumn(importedKeys.getPrimaryKey(importedKeyIds.get(column)))
                        + "='" + Util.escapeJavaScript(s) + '\''
                ) + "');\">" + Util.escapeHtml(s) + "</A>",
                ("DATE".equalsIgnoreCase(columnTypes.get(column))) ? "nowrap"
                    : ("TIME".equalsIgnoreCase(columnTypes.get(column))) ? "nowrap"
                    : ("DATETIME".equalsIgnoreCase(columnTypes.get(column))) ? "nowrap"
                    : ("TIMESTAMP".equalsIgnoreCase(columnTypes.get(column))) ? "nowrap"
                    : "");

            // Get the number of columns that reference this column
            int exportedIdsSize = exportedIds.get(column).size();
            if (exportedIdsSize > 0) {
              assert exportedKeys != null;
              for (int c = 0; c < exportedIdsSize; c++) {
                int z = exportedIds.get(column).get(c);
                String foreignTable = exportedKeys.getForeignTable(z);
                String foreignKey = exportedKeys.getForeignKey(z);

                int tmp;
                if (s == null) {
                  tmp = -1;
                } else {
                  // TODO: Could put this into a single query to avoid round-trips
                  String sql =
                      "SELECT\n"
                          + "  COUNT(*)\n"
                          + "FROM\n"
                          + "  "
                          + conn.quoteTable(foreignTable) + "\n"
                          + "WHERE\n"
                          + "  " + conn.quoteColumn(foreignKey) + "='" + Util.escapeSql(s) + "'";
                  try (
                      Statement stmt2 = dbcon.createStatement();
                      ResultSet results2 = stmt2.executeQuery(sql)
                      ) {
                    if (results2.next()) {
                      tmp = results2.getInt(1);
                    } else {
                      tmp = -1;
                    }
                  } catch (SQLException e) {
                    System.err.println("sql = " + sql);
                    throw e;
                  }
                }

                out.printTd(
                    (tmp > 0) ? "<A href=\"javascript:select('"
                        + Util.escapeJavaScript(exportedKeys.getForeignTable(exportedIds.get(column).get(c)))
                        + "','"
                        + Util.escapeJavaScript(
                        conn.quoteColumn(exportedKeys.getForeignKey(exportedIds.get(column).get(c)))
                            + "='" + Util.escapeJavaScript(s) + '\''
                    )
                        + "');\">" + tmp + "</A>"
                        : "",
                    "align=center");
              }
            }

            // Build a list of primary key values
            if (primaryKeyCols.isEmpty() || primaryKeyCols.contains(columnNames.get(column))) {
              if (sb.length() > 0) {
                sb.append(",");
              }
              if (s != null) {
                sb.append('\'');
              }
              sb.append(s);
              if (s != null) {
                sb.append('\'');
              }
            }
          }
          String primaryKeyValues = Util.escapeJavaScript(sb.toString());

          out.startTd();
          out.print("<a href=\"javascript:setPrimaryKeys('");
          out.print(primaryKeysString);
          out.print("','");

          out.print(primaryKeyValues);
          out.print("'); selectAction('edit_row');\">Edit</a>" + "&nbsp;|&nbsp;<a href=\"javascript:setPrimaryKeys('");
          out.print(primaryKeysString);
          out.print("','");
          out.print(primaryKeyValues);
          out.print("'); selectAction('delete_row');\">Delete</a>");
          out.endTd();

          out.endTr();
        }
      }
    } finally {
      out.endTable();
    }

    // Print out the bottom insert row link
    out.print("<br>\n" + "<a href=\"javascript:selectAction('insert');\">Insert new row</a><br>\n");

    // Print out the bottom previous/next form
    printPreviousNext(out, startPos, numrows, totalRows, 2);

    return settings;
  }
}
