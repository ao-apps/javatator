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
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Shows info about the database product.
 *
 * @author Jason Davies
 */
public class Info {

	private Info() {
	}

	/**
	 * Prints information about the current database product in use.
	 *
	 * @param settings  the database in use
	 */
	public static Settings printDatabaseInfo(JavatatorWriter out, Settings settings) throws SQLException, IOException {
		Connection conn=DatabasePool.getConnection(settings);
		try {
			DatabaseMetaData metaData=conn.getMetaData();
			out.print("<b>More database info:</b> Please note that these values result from querying the database driver "
				+ "and some values may be incorrect.<br><br>\n");

			out.startTable(null, "cellspacing=1");
			try {
				out.startTR();
				out.printTH("Description");
				out.printTH("Value");
				out.endTR();

				printInfoRow(out, "DatabaseProduct", metaData.getDatabaseProductName()+' '+metaData.getDatabaseProductVersion());
				printInfoRow(out, "Driver", metaData.getDriverName()+' '+metaData.getDriverVersion());
				printInfoRow(out, "Does the database store tables in a local file?", metaData.usesLocalFiles());
				printInfoRow(out, "Does the database use a file for each table?", metaData.usesLocalFilePerTable());
				printInfoRow(out, "Does the database support mixed case unquoted SQL identifiers?", metaData.supportsMixedCaseIdentifiers());
				printInfoRow(out, "Does the database store mixed case unquoted SQL identifiers in upper case?", metaData.storesUpperCaseIdentifiers());
				printInfoRow(out, "Does the database store mixed case unquoted SQL identifiers in lower case?", metaData.storesLowerCaseIdentifiers());
				printInfoRow(out, "Does the database store mixed case unquoted SQL identifiers in mixed case?", metaData.storesMixedCaseIdentifiers());
				printInfoRow(out, "Does the database support mixed case quoted SQL identifiers?", metaData.supportsMixedCaseQuotedIdentifiers());
				printInfoRow(out, "Does the database store mixed case quoted SQL identifiers in upper case?", metaData.storesUpperCaseQuotedIdentifiers());
				printInfoRow(out, "Does the database store mixed case quoted SQL identifiers in lower case?", metaData.storesLowerCaseQuotedIdentifiers());
				printInfoRow(out, "Does the database store mixed case quoted SQL identifiers in mixed case?", metaData.storesMixedCaseQuotedIdentifiers());
				printInfoRow(out, "What's the string used to quote SQL identifiers?", metaData.getIdentifierQuoteString());
				printInfoRow(out, "A comma separated list of all a database's SQL keywords that are NOT also SQL92 keywords.", Util.addSpaceToCommas(metaData.getSQLKeywords()));
				printInfoRow(out, "A comma separated list of math functions.", Util.addSpaceToCommas(metaData.getNumericFunctions()));
				printInfoRow(out, "A comma separated list of string functions.", Util.addSpaceToCommas(metaData.getStringFunctions()));
				printInfoRow(out, "A comma separated list of system functions.", Util.addSpaceToCommas(metaData.getSystemFunctions()));
				printInfoRow(out, "A comma separated list of time and date functions.", Util.addSpaceToCommas(metaData.getTimeDateFunctions()));
				printInfoRow(out, "This is the string that can be used to escape '_' or '%' in the string pattern style catalog search parameters.<br>The '_' character represents any single character.<br>The '%' character represents any sequence of zero or more characters.", metaData.getSearchStringEscape());
				printInfoRow(out, "Get all the \"extra\" characters that can be used in unquoted identifier names (those beyond a-z, 0-9 and _).", metaData.getExtraNameCharacters());
				printInfoRow(out, "Is \"ALTER TABLE\" with add column supported?", metaData.supportsAlterTableWithAddColumn());
				printInfoRow(out, "Is \"ALTER TABLE\" with drop column supported?", metaData.supportsAlterTableWithDropColumn());
				printInfoRow(out, "Is column aliasing supported?", metaData.supportsColumnAliasing());
				printInfoRow(out, "Are concatenations between NULL and non-NULL values NULL?", metaData.nullPlusNonNullIsNull());
				printInfoRow(out, "Is the CONVERT function between SQL types supported?", metaData.supportsConvert());
				printInfoRow(out, "Are table correlation names supported?", metaData.supportsTableCorrelationNames());
				printInfoRow(out, "If table correlation names are supported, are they restricted to be different from the names of the tables?", metaData.supportsDifferentTableCorrelationNames());
				printInfoRow(out, "Are expressions in \"ORDER BY\" lists supported?", metaData.supportsExpressionsInOrderBy());
				printInfoRow(out, "Can an \"ORDER BY\" clause use columns not in the SELECT?", metaData.supportsOrderByUnrelated());
				printInfoRow(out, "Is some form of \"GROUP BY\" clause supported?", metaData.supportsGroupBy());
				printInfoRow(out, "Can a \"GROUP BY\" clause use columns not in the SELECT?", metaData.supportsGroupByUnrelated());
				printInfoRow(out, "Can a \"GROUP BY\" clause add columns not in the SELECT provided it specifies all the columns in the SELECT?", metaData.supportsGroupByBeyondSelect());
				printInfoRow(out, "Is the escape character in \"LIKE\" clauses supported?", metaData.supportsLikeEscapeClause());
				printInfoRow(out, "Are multiple ResultSets from a single execute supported?", metaData.supportsMultipleResultSets());
				printInfoRow(out, "Can we have multiple transactions open at once (on different connections)?", metaData.supportsMultipleTransactions());
				printInfoRow(out, "Can columns be defined as non-nullable?", metaData.supportsNonNullableColumns());
				printInfoRow(out, "Is the ODBC Minimum SQL grammar supported?", metaData.supportsMinimumSQLGrammar());
				printInfoRow(out, "Is the ODBC Core SQL grammar supported?", metaData.supportsCoreSQLGrammar());
				printInfoRow(out, "Is the ODBC Extended SQL grammar supported?", metaData.supportsExtendedSQLGrammar());
				printInfoRow(out, "Is the ANSI92 entry level SQL grammar supported?", metaData.supportsANSI92EntryLevelSQL());
				printInfoRow(out, "Is the ANSI92 intermediate SQL grammar supported?", metaData.supportsANSI92IntermediateSQL());
				printInfoRow(out, "Is the ANSI92 full SQL grammar supported?", metaData.supportsANSI92FullSQL());
				printInfoRow(out, "Is the SQL Integrity Enhancement Facility supported?", metaData.supportsIntegrityEnhancementFacility());
				printInfoRow(out, "Is some form of outer join supported?", metaData.supportsOuterJoins());
				printInfoRow(out, "Are full nested outer joins supported?", metaData.supportsFullOuterJoins());
				printInfoRow(out, "Is there limited support for outer joins?", metaData.supportsLimitedOuterJoins());

				if(metaData.supportsCatalogsInDataManipulation()) {
					printInfoRow(out, "Does a catalog appear at the start of a qualified table name? (Otherwise it appears at the end)", metaData.isCatalogAtStart());
					printInfoRow(out, "What's the maximum length of a catalog name?", metaData.getMaxCatalogNameLength());
					printInfoRow(out, "What's the separator between catalog and table name?", metaData.getCatalogSeparator());
					printInfoRow(out, "Can a catalog name be used in a data manipulation statement?", metaData.supportsCatalogsInDataManipulation());
					printInfoRow(out, "Can a catalog name be used in a procedure call statement?", metaData.supportsCatalogsInProcedureCalls());
					printInfoRow(out, "Can a catalog name be used in a table definition statement?", metaData.supportsCatalogsInTableDefinitions());
					printInfoRow(out, "Can a catalog name be used in a index definition statement?", metaData.supportsCatalogsInIndexDefinitions());
					printInfoRow(out, "Can a catalog name be used in a privilege definition statement?", metaData.supportsCatalogsInPrivilegeDefinitions());
				}

				if(metaData.supportsSchemasInDataManipulation()) {
					printInfoRow(out, "What's the maximum length allowed for a schema name?", metaData.getMaxSchemaNameLength());
					printInfoRow(out, "Can a schema name be used in a data manipulation statement?", metaData.supportsSchemasInDataManipulation());
					printInfoRow(out, "Can a schema name be used in a procedure call statement?", metaData.supportsSchemasInProcedureCalls());
					printInfoRow(out, "Can a schema name be used in a table definition statement?", metaData.supportsSchemasInTableDefinitions());
					printInfoRow(out, "Can a schema name be used in an index definition statement?", metaData.supportsSchemasInIndexDefinitions());
					printInfoRow(out, "Can a schema name be used in a privilege definition statement?", metaData.supportsSchemasInPrivilegeDefinitions());
				}

				printInfoRow(out, "Is positioned DELETE supported?", metaData.supportsPositionedDelete());
				printInfoRow(out, "Is positioned UPDATE supported?", metaData.supportsPositionedUpdate());
				printInfoRow(out, "Is SELECT for UPDATE supported?", metaData.supportsSelectForUpdate());
				printInfoRow(out, "Are stored procedure calls using the stored procedure escape syntax supported?", metaData.supportsStoredProcedures());
				printInfoRow(out, "Are subqueries in comparison expressions supported?", metaData.supportsSubqueriesInComparisons());
				printInfoRow(out, "Are subqueries in exists expressions supported?", metaData.supportsSubqueriesInExists());
				printInfoRow(out, "Are subqueries in \"in\" statements supported?", metaData.supportsSubqueriesInIns());
				printInfoRow(out, "Are subqueries in quantified expressions supported?", metaData.supportsSubqueriesInQuantifieds());
				printInfoRow(out, "Are correlated subqueries supported?", metaData.supportsCorrelatedSubqueries());
				printInfoRow(out, "Is SQL UNION supported?", metaData.supportsUnion());
				printInfoRow(out, "Is SQL UNION ALL supported?", metaData.supportsUnionAll());
				printInfoRow(out, "Can cursors remain open across commits?", metaData.supportsOpenCursorsAcrossCommit());
				printInfoRow(out, "Can cursors remain open across rollbacks?", metaData.supportsOpenCursorsAcrossRollback());
				printInfoRow(out, "Can statements remain open across commits?", metaData.supportsOpenStatementsAcrossCommit());
				printInfoRow(out, "Can statements remain open across rollbacks?", metaData.supportsOpenStatementsAcrossRollback());
				printInfoRow(out, "What's the max length for a character literal?", metaData.getMaxCharLiteralLength());
				printInfoRow(out, "What's the limit on column name length?", metaData.getMaxColumnNameLength());
				printInfoRow(out, "What's the maximum number of columns in a \"GROUP BY\" clause?", metaData.getMaxColumnsInGroupBy());
				printInfoRow(out, "What's the maximum number of columns allowed in an index?", metaData.getMaxColumnsInIndex());
				printInfoRow(out, "What's the maximum number of columns in an \"ORDER BY\" clause?", metaData.getMaxColumnsInOrderBy());
				printInfoRow(out, "What's the maximum number of columns in a \"SELECT\" list?", metaData.getMaxColumnsInSelect());
				printInfoRow(out, "What's maximum number of columns in a table?", metaData.getMaxColumnsInTable());
				printInfoRow(out, "How many active connections can we have at a time to this database?", metaData.getMaxConnections());
				printInfoRow(out, "What's the maximum cursor name length?", metaData.getMaxCursorNameLength());
				printInfoRow(out, "What's the maximum length of an index (in bytes)?", metaData.getMaxIndexLength());

				printInfoRow(out, "What's the maximum length of a procedure name?", metaData.getMaxProcedureNameLength());

				printInfoRow(out, "What's the maximum length of a single row?", metaData.getMaxRowSize());
				printInfoRow(out, "Does the maximum length of a single row (above) include LONGVARCHAR and LONGVARBINARY blobs?", metaData.doesMaxRowSizeIncludeBlobs());
				printInfoRow(out, "What's the maximum length of a SQL statement?", metaData.getMaxStatementLength());
				printInfoRow(out, "How many active statements can we have open at one time to this database?", metaData.getMaxStatements());
				printInfoRow(out, "What's the maximum length of a table name?", metaData.getMaxTableNameLength());
				printInfoRow(out, "What's the maximum number of tables in a SELECT?", metaData.getMaxTablesInSelect());
				printInfoRow(out, "What's the maximum length of a user name?", metaData.getMaxUserNameLength());
				printInfoRow(out, "What's the database's default transaction isolation level?", metaData.getDefaultTransactionIsolation());
				printInfoRow(out, "Are transactions supported?", metaData.supportsTransactions());
				printInfoRow(out, "Are both data definition and data manipulation statements within a transaction supported?", metaData.supportsDataDefinitionAndDataManipulationTransactions());
				printInfoRow(out, "Are only data manipulation statements within a transaction supported?", metaData.supportsDataManipulationTransactionsOnly());
				printInfoRow(out, "Does a data definition statement within a transaction force the transaction to commit?", metaData.dataDefinitionCausesTransactionCommit());
				printInfoRow(out, "Is a data definition statement within a transaction ignored?", metaData.dataDefinitionIgnoredInTransactions());
			} finally {
				out.endTable();
			}
		} finally {
			DatabasePool.releaseConnection(conn);
		}
		return settings;
	}

	private static void printInfoRow(JavatatorWriter out, String title, int value) {
		printInfoRow(out, title, String.valueOf(value));
	}

	/**
	 * Prints one row of the info tables.
	 */
	private static void printInfoRow(JavatatorWriter out, String title, String value) {
		out.startTR();
		out.printTD(title);
		out.printTD(value);
		out.endTR();
	}

	private static void printInfoRow(JavatatorWriter out, String title, boolean value) {
		printInfoRow(out, title, value?"true":"false");
	}
}
