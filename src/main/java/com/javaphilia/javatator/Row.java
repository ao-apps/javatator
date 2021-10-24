/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2019, 2020, 2021  AO Industries, Inc.
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
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Various methods for manipulating rows.
 */
public class Row {

	/**
	 * The {@link Settings} currently in use.
	 */
	private final Settings settings;

	/**
	 * Instantiate a new {@link Row}.
	 *
	 * @param settings the {@link Settings} to use.
	 */
	public Row(Settings settings) {
		this.settings=settings;
	}

	/**
	 * Shows a screen asking the user if they want to delete this row.
	 */
	public Settings confirmDeleteRow(JavatatorWriter out) throws SQLException {
		String primaryKeysS=settings.getParameter("primarykeys");
		StringTokenizer keys=new StringTokenizer(primaryKeysS, ",");
		String primaryValuesS=settings.getParameter("values");
		StringTokenizer values=new StringTokenizer(primaryValuesS, ",");
		String[] primaryKeys=new String[keys.countTokens()];
		String[] primaryValues=new String[keys.countTokens()];
		for(int i=0;keys.hasMoreTokens();i++) {
			primaryKeys[i]=keys.nextToken();
			primaryValues[i]=values.nextToken();
		}

		out.print("<h2>Database ");
		out.print(settings.getDatabase());
		out.print(" : table ");
		out.print(settings.getTable());
		out.print("</h2>\n"
			+ "Are you <b>sure</b> you want to delete this row?"
			+ "<input type='hidden' name='primarykeys' value=\"");
		Util.printEscapedInputValue(out, primaryKeysS);
		out.print("\">"
			+ "<input type='hidden' name='values' value=\"");
		Util.printEscapedInputValue(out, primaryValuesS);
		out.print("\">"
			+ "<input type='hidden' name='startpos' value='");
		out.print(getStartPos());
		out.print("'>"
			+ "<input type='submit' value='YES' onClick=\"return selectAction('dodelete_row')\"> "
			+ "<input type='submit' value='NO' onClick=\"history.go(-1);return false;\"> ");
		return settings;
	}

	/**
	 * Deletes the current row
	 */
	public Settings deleteRow(JavatatorWriter out) throws SQLException, IOException {
		StringTokenizer keys=new StringTokenizer(settings.getParameter("primarykeys"),",");
		StringTokenizer values=new StringTokenizer(settings.getParameter("values"),",");
		String[] primaryKeys=new String[keys.countTokens()];
		String[] primaryKeyValues=new String[keys.countTokens()];
		for(int i=0;keys.hasMoreTokens();i++) {
			primaryKeys[i]=keys.nextToken();
			primaryKeyValues[i]=values.nextToken();
			if(!"null".equals(primaryKeyValues[i]))primaryKeyValues[i]=primaryKeyValues[i].substring(1,primaryKeyValues[i].length()-1);
			else primaryKeyValues[i]=null;
		}

		out.print("<h2>Database ");
		out.print(settings.getDatabase());
		out.print(" : table ");
		out.print(settings.getTable());
		out.print("</h2>\n");
		settings.getJDBCConnector().deleteRow(primaryKeys, primaryKeyValues);
		out.print("Row deleted successfully.");
		return new Table(settings).select(out);
	}

	/**
	 * Edits the current row.
	 */
	public Settings editRow(JavatatorWriter out) throws SQLException, IOException {
		StringTokenizer keys=new StringTokenizer(settings.getParameter("primarykeys"),",");
		StringTokenizer values=new StringTokenizer(settings.getParameter("values"),",");
		String[] primaryKeys=new String[keys.countTokens()];
		String[] primaryKeyValues=new String[keys.countTokens()];
		for(int i=0;keys.hasMoreTokens();i++) {
			primaryKeys[i]=keys.nextToken(); // TODO: encode/decode these?
			primaryKeyValues[i]=values.nextToken(); // TODO: encode/decode these?
			if(!"null".equals(primaryKeyValues[i]))primaryKeyValues[i]=primaryKeyValues[i].substring(1,primaryKeyValues[i].length()-1);
			else primaryKeyValues[i]=null;
		}

		int count=0;
		while(settings.getParameter("column"+count)!=null) count++;
		String[] column=new String[count];
		String[] function=new String[count];
		String[] value=new String[count];
		for(int i=0;i<count;i++) {
			column[i]=settings.getParameter("column"+i);
			String func=settings.getParameter("function"+i);
			if(func!=null && func.length()==0) func=null;
			String val=settings.getParameter("value"+i);
			if(val.length()==0 && settings.getParameter("null"+i)!=null) val=null;
			if("[NULL]".equals(val)) val=null;

			// Handle no function
			if(func==null) {
				// Nothing to do

				// Handle user function
			} else if(func.equals("F")) {
				func=val;
				val=null;

				// Handle predefined function
			} else {
				func=func+'('+Util.escapeSQLValue(val)+')';
				val=null;
			}

			function[i]=func;
			value[i]=val;
		}

		out.print("<h2>Database ");
		// TODO: Encode here and lots of other places
		out.print(settings.getDatabase());
		out.print(" : table ");
		out.print(settings.getTable());
		out.print("</h2>\n");
		settings.getJDBCConnector().editRow(
			column,
			function,
			value,
			primaryKeys,
			primaryKeyValues
		);
		out.print("Table updated successfully.");
		String nextAction=settings.getParameter("nextaction");
		if(nextAction !=null && !"".equals(nextAction)) return processRequest(out, nextAction);
		else return new Table(settings).select(out);
	}

	private int getStartPos() {
		String s = settings.getParameter("startpos");
		if(s != null && s.length() > 0) return Integer.parseInt(s);
		return 0;
	}

	/**
	 * Inserts a new row.
	 */
	public Settings insert(JavatatorWriter out) throws SQLException, IOException {
		int count=0;
		while(settings.getParameter("column"+count)!=null) count++;
		String[] newColumn=new String[count];
		String[] newFunction=new String[count];
		String[] newValue=new String[count];
		for(int i=0;i<count;i++) {
			newColumn[i]=settings.getParameter("column"+i);
			String function=settings.getParameter("function"+i);
			if(function!=null && function.length()==0) function=null;
			String value=settings.getParameter("value"+i);
			if(value.length()==0 && settings.getParameter("null"+i)!=null) value=null;
			if("[NULL]".equals(value)) value=null;

			// Handle no function
			if(function==null) {
				// Nothing to do

				// Handle user function
			} else if(function.equals("F")) {
				function=value;
				value=null;

				// Handle predefined function
			} else {
				function=function+'('+Util.escapeSQLValue(value)+')';
				value=null;
			}

			newFunction[i]=function;
			newValue[i]=value;
		}

		out.print("<h2>Database ");
		out.print(settings.getDatabase());
		out.print(" : table ");
		out.print(settings.getTable());
		out.print("</h2>\n");
		settings.getJDBCConnector().insertRow(newColumn, newFunction, newValue);
		out.print("Data successfully inserted.");
		String nextAction=settings.getParameter("nextaction");
		if(nextAction!=null && !"".equals(nextAction)) {
			System.out.println("there was a nextaction=["+nextAction+"]");
			return processRequest(out, nextAction);
		} else {
			return new Table(settings).select(out);
		}
	}

	/**
	 * Shows a screen to edit the current row.
	 */
	public Settings printEditRow(JavatatorWriter out) throws SQLException, IOException {
		String primaryKeysS=settings.getParameter("primarykeys");
		StringTokenizer keys=new StringTokenizer(primaryKeysS, ",");
		String primaryValuesS=settings.getParameter("values");
		StringTokenizer values=new StringTokenizer(primaryValuesS, ",");
		List<String> primaryKeys=new ArrayList<>(keys.countTokens());
		List<String> primaryValues=new ArrayList<>(keys.countTokens());
		for(int i=0;keys.hasMoreTokens();i++) {
			primaryKeys.add(keys.nextToken());
			String value = values.nextToken();
			if(!"null".equals(value)) value = value.substring(1, value.length()-1);
			else value = null;
			primaryValues.add(value);
		}

		out.print("<h2>Database ");
		out.print(settings.getDatabase());
		out.print(" : table ");
		out.print(settings.getTable());
		out.print("</h2>\n"
			+ "<input type='hidden' name='startpos' value='");
		out.print(getStartPos());
		out.print("'>"
			+ "<input type='hidden' name='primarykeys' value='");
		Util.printEscapedInputValue(out, primaryKeysS);
		out.print("'>"
			+ "<input type='hidden' name='values' value='");
		Util.printEscapedInputValue(out, primaryValuesS);
		out.print("'>");
		String selectCols=settings.getParameter("selectcols");
		if(selectCols!=null && !"".equals(selectCols)) {
			out.print("<input type='hidden' name='selectcols' value='");
			Util.printEscapedInputValue(out, selectCols);
			out.print("'>");
		}
		String selectWhere=settings.getParameter("selectwhere");
		if(selectWhere!=null && !"".equals(selectWhere)) {
			out.print("<input type='hidden' name='selectwhere' value='");
			Util.printEscapedInputValue(out, selectWhere);
			out.print("'>");
		}

		out.startTable(null, "cellspacing=1");

		out.startTR();
		out.printTH("Column");
		out.printTH("Type");
		out.printTH("Function");
		out.printTH("Value");
		out.endTR();
		try {
			JDBCConnector conn=settings.getJDBCConnector();
			List<String> rowValues=conn.getRow(primaryKeys, primaryValues);
			Columns columns=conn.getColumns();
			List<String> names=columns.getNames();
			List<String> types=columns.getTypes();
			List<String> lengths=columns.getLengths();
			List<JDBCConnector.Boolean> areNullable=columns.areNullable();
			//List<String> defaults=columns.getDefaults();
			int size=names.size();
			for(int i=0;i<size;i++) {
				String columnName=names.get(i);
				String columnType=types.get(i);
				String columnLength=lengths.get(i);
				String currentValue=rowValues.get(i);
				JDBCConnector.Boolean isNullable=areNullable.get(i);

				out.startTR();

				out.startTD();
				out.print(columnName);
				out.print("<input type='hidden' name='column");
				out.print(i);
				out.print("' value='");
				out.print(columnName);
				out.print("'>");
				out.endTD();

				out.printTD(columnType);

				out.startTD();
				// Don't show the the functions if all possible values are displayed
				List<String> pvalues=conn.getPossibleValues(columnName, columnType);
				if(pvalues==null) {
					List<String> functions=conn.getFunctionList(conn.getEffectiveType(columnType));
					int fSize=functions.size();
					out.print("<select name='function");
					out.print(i);
					out.print("'>\n"
						+"<option value='' selected>[VALUE-->]</option>\n"
						+"<option value='F'>[FUNCTION-->]</option>\n");
					for(int c=0;c<fSize;c++) {
						String function=functions.get(c);
						out.print("<option value='");
						out.print(function);
						out.print("'>");
						out.print(function);
						out.print("</option>\n");
					}
					out.print("</select>");
				} else out.print("&nbsp;");
				out.endTD();

				out.startTD();
				if(pvalues!=null) {
					out.print("<select name='value");
					out.print(i);
					out.print("'>\n");
					boolean found=false;

					// Add the null if null is allowed
					if(isNullable==JDBCConnector.Boolean.TRUE) {
						out.print("<option value='[NULL]'");
						if(currentValue==null) {
							out.print(" selected");
							found=true;
						}
						out.print(">[NULL]</option>\n");
					}

					if("bool".equals(columnType)) currentValue = Boolean.toString("t".equals(currentValue));
					int vsize=pvalues.size();
					for(int c=0;c<vsize;c++) {
						String value=pvalues.get(c);
						out.print("<option value='");
						Util.printEscapedInputValue(out, value);
						out.print('\'');
						if(!found && value.equals(currentValue)) {
							out.print(" selected");
							found=true;
						}
						out.print('>');
						Util.printEscapedInputValue(out, value);
						out.print("</option>\n");
					}
					// Add to end if not found in list of possible values (this should not happen)
					if(!found) {
					out.print("<option value='");
					Util.printEscapedInputValue(out, currentValue);
					out.print("' selected>");
					Util.printEscapedInputValue(out, currentValue);
					out.print("</option>\n");
					}
					out.print("</select>");
				} else {
					if(columnType.toUpperCase().endsWith("TEXT")
					   && (
							settings.useMultiLine()
							|| (currentValue!=null && currentValue.indexOf('\n')>-1)
					   )
					) {
						out.print("<textarea rows=16 cols=80 name='value");
						out.print(i);
						out.print("'>");
						if(currentValue!=null) Util.printEscapedInputValue(out, currentValue);
						out.print("</textarea>");
					} else {
						out.print("<input type='text' name='value");
						out.print(i);
						out.print("' value='");
						if(currentValue!=null) Util.printEscapedInputValue(out, currentValue);
						out.print('\'');
						if(columnType.toUpperCase().endsWith("CHAR")) {
							out.print(" maxlength=");
							out.print(columnLength);
						}
						out.print('>');
					}
					if(isNullable==JDBCConnector.Boolean.TRUE) {
						out.print("<input type='checkbox' name='null");
						out.print(i);
						out.print("' value='NULL'");
						if(rowValues.get(i)==null) out.print(" checked");
						out.print("> NULL");
					}
				}
				out.endTD();

				out.endTR();

			}
		} finally {
			out.endTable();
		}
		out.print("<br><br>\n"
			+ "<input type='submit' value='Save!' onClick=\"return selectAction('doedit_row')\">");
			//+ " <input type='submit' value='Save and edit next row!' "
			//+ "onClick=\"setNextAction('edit_row');return selectAction('doedit_row');\">");
		return settings;
	}

	/**
	 * Prints a screen for inserting a row.
	 */
	public Settings printInsert(JavatatorWriter out) throws SQLException, IOException {
		out.print("<h2>Database ");
		out.print(settings.getDatabase());
		out.print(" : table ");
		out.print(settings.getTable());
		out.print("</h2>\n"
			+ "<input type='hidden' name='startpos' value='");
		out.print(getStartPos());
		out.print("'>");
		String selectCols=settings.getParameter("selectcols");
		if(selectCols!=null && !"".equals(selectCols)) {
			out.print("<input type='hidden' name='selectcols' value='");
			Util.printEscapedInputValue(out, selectCols);
			out.print("'>");
		}
		String selectWhere=settings.getParameter("selectwhere");
		if(selectWhere!=null && !"".equals(selectWhere)) {
			out.print("<input type='hidden' name='selectwhere' value='");
			Util.printEscapedInputValue(out, selectWhere);
			out.print("'>");
		}

		out.startTable(null, "cellspacing=1");

		out.startTR();
		out.printTH("Column");
		out.printTH("Type");
		out.printTH("Function");
		out.printTH("Value");
		out.endTR();

		JDBCConnector conn=settings.getJDBCConnector();

		Columns columns=conn.getColumns();
		List<String> names=columns.getNames();
		List<String> types=columns.getTypes();
		List<String> lengths=columns.getLengths();
		List<JDBCConnector.Boolean> areNullable=columns.areNullable();
		List<String> defaults=columns.getDefaults();
		int size=names.size();
		for(int i=0;i<size;i++) {
			String columnName=names.get(i);
			String columnType=types.get(i);
			String columnLength=lengths.get(i);
			JDBCConnector.Boolean isNullable=areNullable.get(i);
			String columnDefault=defaults.get(i);
			out.startTR();

			out.startTD();
			out.print(columnName);
			out.print("<input type='hidden' name='column");
			out.print(i);
			out.print("' value='");
			out.print(columnName);
			out.print("'>");
			out.endTD();

			out.printTD(columnType);

			out.startTD();

			// Do not show the functions if all possible values are listed and the default is not a function
			List<String> values=conn.getPossibleValues(columnName, columnType);
			if(values==null || (columnDefault!=null && columnDefault.charAt(0)=='F')) {
				out.print("<select name='function");
				out.print(i);
				out.print("'>\n");

				// Show the blank option, select if no function is default
				out.print("<option value=''");
				if(columnDefault==null || columnDefault.charAt(0)!='F') {
					out.print(" selected");
				}
				out.print(">[VALUE-->]</option>\n");

				// Show the function option, select if default is function
				out.print("<option value='F'");
				if(columnDefault!=null && columnDefault.charAt(0)=='F') {
					out.print(" selected");
				}
				out.print(">[FUNCTION-->]</option>\n");

				// Add the default functions to the available list
				List<String> fv = conn.getFunctionList(conn.getEffectiveType(columnType));
				int fSize = fv.size();
				for(int c = 0; c < fSize; c++) {
					out.print("<option value='");
					String function = fv.get(c);
					out.print(function);
					out.print("'>");
					out.print(function);
					out.print("</option>\n");
				}
				out.print("</select>");
			} else out.print("&nbsp;");
			out.endTD();

			out.startTD();
			// Only show the list of possible values when available and the default is not a function
			if(values!=null && (columnDefault==null || columnDefault.charAt(0)=='V')) {
				String def=columnDefault==null?null:columnDefault.substring(1);
				out.print("<select name='value");
				out.print(i);
				out.print("'>\n");
				boolean found=false;

				// Add the null option if nullable
				if(isNullable==JDBCConnector.Boolean.TRUE) {
					out.print("<option value='[NULL]'");
					if(columnDefault==null) {
						out.print(" selected");
						found=true;
					}
					out.print(">[NULL]</option>\n");
				}

				int vsize=values.size();
				for(int c=0;c<vsize;c++) {
					String value=values.get(c);
					out.print("<option value='");
					Util.printEscapedInputValue(out, value);
					out.print('\'');
					if(!found && value.equals(def)) {
						out.print(" selected");
						found=true;
					}
					out.print('>');
					Util.printEscapedInputValue(out, value);
					out.print("</option>\n");
				}
				out.print("</select>");
			} else {
				if(
					(columnDefault==null || columnDefault.charAt(0)!='F')
					&& columnType.toUpperCase().endsWith("TEXT")
					&& settings.useMultiLine()
				) {
					out.print("<textarea rows=16 cols=80 name='value");
					out.print(i);
					out.print("'>");
					if(columnDefault!=null) Util.printEscapedInputValue(out, columnDefault.substring(1));
					out.print("</textarea>");
				} else {
					out.print("<input type='text' size=32 name='value");
					out.print(i);
					out.print("' value='");
					if(columnDefault!=null) Util.printEscapedInputValue(out, columnDefault.substring(1));
					out.print('\'');
					if(columnType.toUpperCase().endsWith("CHAR")) {
						out.print(" maxlength=");
						out.print(columnLength);
					}
					out.print('>');
				}
				if(isNullable==JDBCConnector.Boolean.TRUE) {
					out.print("<input type='checkbox' name='null");
					out.print(i);
					out.print("' value='NULL'");
					if(columnDefault==null) out.print(" checked");
					out.print("> NULL");
				}
			}
			out.endTD();

			out.endTR();
		}
		out.endTable();
		out.print("<br><br>\n"
			+ "<input type='hidden' name='nextaction' value=''>\n"
			+ "<input type='submit' value='Save!' onClick=\"return selectAction('doinsert')\"> "
			+ "<input type='submit' value='Save and insert another row!' "
			+ "onClick=\"setNextAction('insert');return selectAction('doinsert');\">");
		return settings;
	}

	/**
	 * Process the {@link Settings} object and decide what to do.
	 */
	public Settings processRequest(JavatatorWriter out) throws SQLException, IOException {
		String action=settings.getAction();
		return processRequest(out, action);
	}

	/**
	 * Process the {@link Settings} object and decide what to do.
	 */
	private Settings processRequest(JavatatorWriter out, String action) throws SQLException, IOException {
		if("insert".equals(action)) return printInsert(out);
		else if("doinsert".equals(action)) return insert(out);
		else if("edit_row".equals(action)) return printEditRow(out);
		else if("doedit_row".equals(action)) return editRow(out);
		else if("delete_row".equals(action)) return confirmDeleteRow(out);
		else if("dodelete_row".equals(action)) return deleteRow(out);
		else if(settings.getColumn()!=null) return new Column(settings).processRequest(out);
		else {
			out.print("Row: Unknown value of action: ");
			out.print(action);
			return settings;
		}
	}
}
