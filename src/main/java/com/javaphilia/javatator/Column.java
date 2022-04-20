/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2019, 2022  AO Industries, Inc.
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
import java.sql.SQLException;
import java.util.List;

/**
 * Various methods for manipulating columns
 */
public class Column {

  private final Settings settings;

  /**
   * Instantiate a new {@link Column}.
   *
   * @param  settings  the {@link Settings} to use
   */
  public Column(Settings settings) {
    this.settings=settings;
  }

  /**
   * Adds a new column to this table.
   */
  public Settings addColumn(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : added new column ");
    out.print(settings.getColumn());
    out.print("</h2>");

    // Determine what the default is
    String newdefault=null;

    String newdefaulttype=settings.getParameter("newdefaulttype");
    if (newdefaulttype.length()>0) {
      if (newdefaulttype.charAt(0) == 'F') {
        newdefault='F'+settings.getParameter("newdefaultvalue");
      } else if (newdefaulttype.charAt(0) == 'V') {
        if (newdefaulttype.length()>1) {
          newdefault=newdefaulttype;
        } else {
          newdefault='V'+settings.getParameter("newdefaultvalue");
        }
      }
    }

    settings.getJDBCConnector().addColumn(
      settings.getColumn(),
      settings.getParameter("newtype"),
      settings.getParameter("newlength"),
      newdefault,
      settings.getParameter("newnull"),
      settings.getParameter("newremarks")
    );
    out.print("Column added successfully.\n");
    return new Table(settings).printTableProperties(out);
  }

  /**
   * Adds an index on the current column.
   */
  public Settings addIndex(JavatatorWriter out) throws SQLException, IOException {
    String indexName=settings.getParameter("indexname");
    String column=settings.getColumn();
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : index ");
    out.print(indexName);
    out.print(" added on ");
    out.print(column);
    out.print("</h2>\n");
    settings.getJDBCConnector().addIndex(indexName, column);
    out.print("An index has been added on ");
    out.print(column);
    out.print('.');
    return new Table(settings).printTableProperties(out);
  }

  /**
   * Makes this column a primary key.
   */
  public Settings addPrimaryKey(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : primary key ");
    out.print(settings.getColumn());
    out.print(" added</h2>\n");
    settings.getJDBCConnector().addPrimaryKey(settings.getColumn());
    out.print("Primary key ");
    out.print(settings.getColumn());
    out.print(" has been added successfully.");
    return new Table(settings).printTableProperties(out);
  }

  /**
   * Adds a unique index on the current column.
   */
  public Settings addUniqueIndex(JavatatorWriter out) throws SQLException, IOException {
    String indexName=settings.getParameter("indexname");
    String column=settings.getColumn();
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : unique index ");
    out.print(indexName);
    out.print(" added on ");
    out.print(column);
    out.print("</h2>\n");
    settings.getJDBCConnector().addUniqueIndex(indexName, column);
    out.print("A unique index has been added on ");
    out.print(column);
    out.print('.');
    return new Table(settings).printTableProperties(out);
  }

  /**
   * Shows a screen for the user to make the current column a primary key.
   */
  public Settings confirmAddPrimaryKey(JavatatorWriter out) throws SQLException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : column ");
    out.print(settings.getColumn());
    out.print("</h2>\n"
      + "<b>Are you sure you want to add ");
    out.print(settings.getColumn());
    out.print(" as a primary key?</b><br>\n"
      + "<input type='submit' value='YES' onClick=\"return selectAction('doadd_primarykey')\"> "
      + "<input type='submit' value='NO' onClick=\"history.go(-1);return false;\">");
    return settings;
  }

  /**
   * Shows a screen asking if the user wants to delete the current column.
   */
  public Settings confirmDeleteColumn(JavatatorWriter out) throws SQLException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : column ");
    out.print(settings.getColumn());
    out.print("</h2>");
    out.print("Are you sure you want to drop this column <b>");
    out.print(settings.getColumn());
    out.print("</b>?"
      + "<br>\n"
      + "<input type='submit' value='YES' onClick=\"return selectAction('dodelete_column')\"> "
      + "<input type='submit' value='NO' onClick=\"history.go(-1);return false;\"> ");
    return settings;
  }

  /**
   * Shows a screen asking if the user doesn't want this column as a primary key.
   */
  public Settings confirmDropPrimaryKey(JavatatorWriter out) throws SQLException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : column ");
    out.print(settings.getColumn());
    out.print("</h2>\n"
      + "<b>Are you sure you want to drop primary key ");
    out.print(settings.getColumn());
    out.print("?</b><br>\n"
      + "<input type='submit' value='YES' onClick=\"return selectAction('dodrop_primarykey')\"> "
      + "<input type='submit' value='NO' onClick=\"history.go(-1);return false;\">");
    return settings;
  }

  /**
   * Deletes the current column.
   */
  public Settings deleteColumn(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : deleted column ");
    out.print(settings.getColumn());
    out.print("</h2>\n");
    settings.getJDBCConnector().deleteColumn(settings.getColumn());
    out.print("Column deleted successfully.");
    return new Table(settings).printTableProperties(out);
  }

  /**
   * Removes this column as a primary key.
   */
  public Settings dropPrimaryKey(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : primary key ");
    out.print(settings.getColumn());
    out.print(" dropped</h2>\n");
    settings.getJDBCConnector().dropPrimaryKey(settings.getColumn());
    out.print("Primary key on ");
    out.print(settings.getColumn());
    out.print(" has been dropped successfully.");
    return new Table(settings).printTableProperties(out);
  }

  /**
   * Edits the current column using the specified values.
   */
  public Settings editColumn(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : column ");
    out.print(settings.getColumn());
    out.print(" edited</h2>\n");

    // Determine what the default is
    String newdefault=null;

    String newdefaulttype=settings.getParameter("newdefaulttype");
    if (newdefaulttype.length()>0) {
      if (newdefaulttype.charAt(0) == 'F') {
        newdefault='F'+settings.getParameter("newdefaultvalue");
      } else if (newdefaulttype.charAt(0) == 'V') {
        if (newdefaulttype.length()>1) {
          newdefault=newdefaulttype;
        } else {
          newdefault='V'+settings.getParameter("newdefaultvalue");
        }
      }
    }

    settings.getJDBCConnector().editColumn(
      settings.getColumn(),
      settings.getParameter("newcolumn"),
      settings.getParameter("newtype"),
      settings.getParameter("newlength"),
      newdefault,
      settings.getParameter("newnull"),
      settings.getParameter("newremarks")
    );
    out.print("Column edited successfully.\n");
    return new Table(settings).printTableProperties(out);
  }

  /**
   * Prints a screen allowing the user to add a new column to this table.
   */
  public Settings printAddColumn(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : adding new column</h2>");

    out.startTable(null, "cellspacing=1");

    out.startTR();
    out.printTH("Column");
    out.printTH("Type");
    out.printTH("Length/Set");
    out.printTH("Nullable");
    out.printTH("Default");
    out.printTH("Remarks");
    out.endTR();

    out.startTR();
    out.printTD("<input type='text' name='column' size=10 value=''>");

    try {
      JDBCConnector conn=settings.getJDBCConnector();
      //Columns columns=conn.getColumns();
      List<String> types=conn.getTypes();

      // List all the possible types
      out.startTD();
      out.print("<select name='newtype'>");
      int size=types.size();
      for (int i=0;i<size;i++) {
        String type=types.get(i);
        out.print("<option value='");
        out.print(type);
        out.print("'>");
        out.print(type);
        out.print("</option>\n");
      }
      out.print("</select>");
      out.endTD();

      out.printTD("<input type='text' name='newlength' size=8 value=''>");

      out.printTD("<select name='newnull'>\n"
        + "<option value='not null' selected>NO</option>\n"
        + "<option value='null'>YES</option>\n"
        + "</select>\n");

      out.printTD("<select name='newdefaulttype'>\n"
        + "<option value=''>[NULL]</option>\n"
        + "<option value='V'>[VALUE-->]</option>\n"
        + "<option value='F'>[FUNCTION-->]</option>\n"
        + "</select>\n"
        + "<input type='text' name='newdefaultvalue' size=12 value=''>");

      out.printTD("<input type='text' name='newremarks' size=12 value=''>");
    } finally {
      out.endTR();
      out.endTable();
    }
    out.print("<br><input type='submit' value='Save!' onClick=\"return selectAction('doadd_column');\">");
    return settings;
  }

  /**
   * Shows a screen for adding a new index on the current column.
   */
  public Settings printAddIndex(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : add index on ");
    out.print(settings.getColumn());
    out.print("</h2>\n"
      + "<br>Index Name: <input type='text' name='indexname'> "
      + "<input type='submit' value='Add!' onClick=\"return selectAction('doadd_index')\">");
    return settings;
  }

  /**
   * Shows a screen for adding a new unique index on the current column.
   */
  public Settings printAddUniqueIndex(JavatatorWriter out) throws SQLException, IOException {
    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : add unique index on ");
    out.print(settings.getColumn());
    out.print("</h2>\n"
      + "<br>Unique Index Name: <input type='text' name='indexname'> "
      + "<input type='submit' value='Add!' onClick=\"return selectAction('doadd_uniqueindex')\">");
    return settings;
  }

  /**
   * Prints a screen for editing a column.
   */
  public Settings printEditColumn(JavatatorWriter out) throws SQLException, IOException {
    String column=settings.getColumn();

    out.print("<h2>Database ");
    out.print(settings.getDatabase());
    out.print(" : table ");
    out.print(settings.getTable());
    out.print(" : column ");
    out.print(column);
    out.print("</h2>\n");

    out.startTable(null, "cellspacing=1");

    out.startTR();
    out.printTH("Column");
    out.printTH("Type");
    out.printTH("Length/Set");
    out.printTH("Nullable");
    out.printTH("Default");
    out.printTH("Remarks");
    out.endTR();

    out.startTR();
    out.startTD();
    out.print("<input type='text' name='newcolumn' size=10 value='");
    Util.printEscapedInputValue(out, column);
    out.print("'>");
    out.endTD();

    try {
      JDBCConnector conn=settings.getJDBCConnector();
      Columns columns=conn.getColumns();
      int id=columns.getID(column);
      String columnType=columns.getType(id);
      String columnLength=columns.getLength(id);
      String columnDefault=columns.getDefault(id);
      String columnExtra=columns.getRemark(id);
      JDBCConnector.Boolean isNullable=columns.isNullable(id);
      List<String> types=conn.getTypes();

      // List all the possible types
      out.startTD();
      out.print("<select name='newtype'>");
      boolean done=false;
      int size=types.size();
      for (int i=0;i<size;i++) {
        String type=types.get(i);
        out.print("<option value='");
        out.print(type);
        out.print('\'');
        if (!done && columnType.equalsIgnoreCase(type)) {
          out.print(" selected");
          done=true;
        }
        out.print('>');
        out.print(type);
        out.print("</option>\n");
      }
      // If not found in the possible types, list on its own
      if (!done) {
        out.print("<option value='");
        out.print(columnType);
        out.print("' selected>");
        out.print(columnType);
        out.print("</option>\n");
      }
      out.print("</select>");
      out.endTD();

      out.startTD();
      out.print("<input type='text' name='newlength' size=8 value='");
      Util.printEscapedInputValue(out, columnLength);
      out.print("'>");
      out.endTD();

      out.startTD();
      out.print("<select name='newnull'>");
      if (isNullable == JDBCConnector.Boolean.TRUE) {
        out.print("<option value='null' selected>YES</option>\n"
          + "<option value='not null'>NO</option>\n");
      } else {
        out.print("<option value='not null' selected>NO</option>\n"
          + "<option value='null'>YES</option>\n");
      }
      out.print("</select>");
      out.endTD();

      out.startTD();
      out.print("<select name='newdefaulttype'>\n");
      List<String> pvalues=conn.getPossibleValues(column, columnType);
      if (pvalues != null) {
        // Show empty in choice first
        boolean found=false;
        out.print("<option value=''");
        if (columnDefault == null) {
          out.print(" selected");
          found=true;
        }
        out.print(">[NULL]</option>\n");

        // Show the function option and select if is currently function
        out.print("<option value='F'");
        if (!found && columnDefault != null && columnDefault.charAt(0) == 'F') {
          out.print(" selected");
          found=true;
        }
        out.print(">[FUNCTION-->]</option>\n");

        // Display all the possible values, selecting current value if present in list
        if ("bool".equals(columnType)) {
          if ("t".equals(columnDefault)) {
            columnDefault="Vtrue";
          } else if ("f".equals(columnDefault)) {
            columnDefault="Vfalse";
          }
        }
        int vsize=pvalues.size();
        for (int c=0;c<vsize;c++) {
          String value=pvalues.get(c);
          String vvalue='V'+value;
          out.print("<option value='");
          Util.printEscapedInputValue(out, vvalue);
          out.print('\'');
          if (!found && vvalue.equals(columnDefault)) {
            out.print(" selected");
            found=true;
          }
          out.print('>');
          Util.printEscapedInputValue(out, value);
          out.print("</option>\n");
        }

        // Add to end if not found in list of possible values (this should not happen)
        if (!found) {
          assert columnDefault != null;
          out.print("<option value='");
          Util.printEscapedInputValue(out, columnDefault);
          out.print("' selected>");
          Util.printEscapedInputValue(out, columnDefault.substring(1));
          out.print("</option>\n");
        }
        out.print("</select><input type='text' name='newdefaultvalue' value='");
        if (columnDefault != null && columnDefault.charAt(0) == 'F') {
          Util.printEscapedInputValue(out, columnDefault.substring(1));
        }
        out.print("'>");
      } else {
        out.print("<option value=''");
        if (columnDefault == null) {
          out.print(" selected");
        }
        out.print(">[NULL]</option>\n"
          + "<option value='V'");
        if (columnDefault != null && columnDefault.charAt(0) == 'V') {
          out.print(" selected");
        }
        out.print(">[VALUE-->]</option>\n"
          + "<option value='F'");
        if (columnDefault != null && columnDefault.charAt(0) == 'F') {
          out.print(" selected");
        }
        out.print(">[FUNCTION-->]</option>\n"
          + "</select><input type='text' name='newdefaultvalue' size=12 value=\"");
        out.print(columnDefault == null?"":columnDefault.substring(1));
        out.print("\">");
      }
      out.endTD();

      out.startTD();
      out.print("<input type='text' name='newremarks' size=12 value=\"");
      if (columnExtra != null) {
        out.print(columnExtra);
      }
      out.print("\">");
      out.endTD();
    } finally {
      out.endTR();
      out.endTable();
    }
    out.print("<br><input type='submit' value='Save!' onClick=\"return selectAction('doedit_column');\">");
    return settings;
  }

  /**
   * Process the {@link Settings} object and decide what to do.
   */
  public Settings processRequest(JavatatorWriter out) throws SQLException, IOException {
    String action=settings.getAction();

    if ("edit_column".equals(action)) {
      return printEditColumn(out);
    } else if ("doedit_column".equals(action)) {
      return editColumn(out);
    } else if ("delete_column".equals(action)) {
      return confirmDeleteColumn(out);
    } else if ("dodelete_column".equals(action)) {
      return deleteColumn(out);
    } else if ("doadd_column".equals(action)) {
      return addColumn(out);
    } else if ("add_primarykey".equals(action)) {
      return confirmAddPrimaryKey(out);
    } else if ("doadd_primarykey".equals(action)) {
      return addPrimaryKey(out);
    } else if ("drop_primarykey".equals(action)) {
      return confirmDropPrimaryKey(out);
    } else if ("dodrop_primarykey".equals(action)) {
      return dropPrimaryKey(out);
    } else if ("add_index".equals(action)) {
      return printAddIndex(out);
    } else if ("doadd_index".equals(action)) {
      return addIndex(out);
    } else if ("add_uniqueindex".equals(action)) {
      return printAddUniqueIndex(out);
    } else if ("doadd_uniqueindex".equals(action)) {
      return addUniqueIndex(out);
    } else {
      out.print("Column: Unknown action: ");
      out.print(action);
      return settings;
    }
  }
}
