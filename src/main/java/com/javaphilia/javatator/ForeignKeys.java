/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2009, 2019  AO Industries, Inc.
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

import java.util.ArrayList;
import java.util.List;

/**
 * Object representation of foreign key data for a table.
 */
public class ForeignKeys {

	/**
	 * The constraint names.
	 */
	final private List<String> constraintNames;

	/**
	 * The names of the foreign key columns.
	 */
	final private List<String> foreignKeys;

	/**
	 * The names of the foreign tables.
	 */
	final private List<String> foreignTables;

	/**
	 * The names of the primary keys.
	 */
	final private List<String> primaryKeys;

	/**
	 * The names of the primary tables.
	 */
	final private List<String> primaryTables;

	/**
	 * The number of foreign keys stored in this {@link ForeignKeys} object.
	 */
	final private int size;

	/**
	 * The INSERT rules.
	 */
	final private List<String> insertRules;

	/**
	 * The DELETE rules.
	 */
	final private List<String> deleteRules;

	/**
	 * The UPDATE rules.
	 */
	final private List<String> updateRules;

	/**
	 * Are the keys deferrable?
	 */
	final private List<JDBCConnector.Boolean> areDeferrable;

	/**
	 * Are the keys initially deferred?
	 */
	final private List<JDBCConnector.Boolean> areInitiallyDeferred;

	public ForeignKeys(
		List<String> constraintNames,
		List<String> foreignKeys,
		List<String> foreignTables,
		List<String> primaryKeys,
		List<String> primaryTables,
		List<String> insertRules,
		List<String> deleteRules,
		List<String> updateRules,
		List<JDBCConnector.Boolean> areDeferrable,
		List<JDBCConnector.Boolean> areInitiallyDeferred
	) {
		this.constraintNames=constraintNames;
		this.foreignKeys=foreignKeys;
		this.foreignTables=foreignTables;
		this.primaryKeys=primaryKeys;
		this.primaryTables=primaryTables;
		this.insertRules=insertRules;
		this.deleteRules=deleteRules;
		this.updateRules=updateRules;
		this.areDeferrable=areDeferrable;
		this.areInitiallyDeferred=areInitiallyDeferred;
		size=constraintNames.size();
	}

	/**
	 * Are the keys deferrable?
	 */
	public List<JDBCConnector.Boolean> areDeferrable() {
		return areDeferrable;
	}

	/**
	 * Are the keys initially deferred?
	 */
	public List<JDBCConnector.Boolean> areInitiallyDeferred() {
		return areInitiallyDeferred;
	}

	/**
	 * Gets the constraint name at the specified index.
	 */
	public String getConstraintName(int i) {
		return constraintNames.get(i);
	}

	/**
	 * Gets an array of the constraint names.
	 */
	public List<String> getConstraintNames() {
		return constraintNames;
	}

	/**
	 * Gets an array of the DELETE rules.
	 */
	public List<String> getDeleteRules() {
		return deleteRules;
	}

	/**
	 * Gets the id of the specified foreign key in the array.
	 */
	public int getForeignID(String column) {
		for(int i=0;i<size;i++) if(foreignKeys.get(i).equals(column)) return i;
		return -1;
	}

	/**
	 * Gets the ids of the specified foreign key in the array.
	 */
	public List<Integer> getForeignIDs(String column) {
		// List<String> hasBeen=new ArrayList<String>();
		List<Integer> ids=new ArrayList<>();
		for(int i=0;i<size;i++) {
			if(primaryKeys.get(i).equals(column) /*&& !hasBeen.contains(foreignTables.get(i))*/) {
				//hasBeen.add(foreignTables.get(i));
				ids.add(i);
			}
		}
		return ids;
	}

	/**
	 * Gets the foreign key (the column name) at the specified index.
	 */
	public String getForeignKey(int i) {
		return foreignKeys.get(i);
	}

	/**
	 * Gets an array of the foreign key names (the column names).
	 */
	public List<String> getForeignKeys() {
		return foreignKeys;
	}

	/**
	 * Gets the foreign table name at the specified index.
	 */
	public String getForeignTable(int i) {
		return foreignTables.get(i);
	}

	/**
	 * Gets an array of the foreign table names.
	 */
	public List<String> getForeignTables() {
		return foreignTables;
	}

	/**
	 * Gets an array of the INSERT rules.
	 */
	public List<String> getInsertRules() {
		return insertRules;
	}

	/**
	 * Gets the id of the specified primary key in the array.
	 */
	public int getPrimaryID(String column) {
		for(int i=0;i<size;i++) if(primaryKeys.get(i).equals(column)) return i;
		return -1;
	}

	/**
	 * Gets the primary key (the column name) at the specified index.
	 */
	public String getPrimaryKey(int i) {
		return primaryKeys.get(i);
	}

	/**
	 * Gets an array of the primary keys (the column names).
	 */
	public List<String> getPrimaryKeys() {
		return primaryKeys;
	}

	/**
	 * Gets the primary table name at the specified index.
	 */
	public String getPrimaryTable(int i) {
		return primaryTables.get(i);
	}

	/**
	 * Gets an array of the primary table names.
	 */
	public List<String> getPrimaryTables() {
		return primaryTables;
	}

	/**
	 * Gets the number of foreign keys stored here.
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Gets an array of the UPDATE rules.
	 */
	public List<String> getUpdateRules() {
		return updateRules;
	}
}
