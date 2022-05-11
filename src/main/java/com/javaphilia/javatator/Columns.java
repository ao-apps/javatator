/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2019, 2021, 2022  AO Industries, Inc.
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

import com.javaphilia.javatator.JdbcConnector.Boolean;
import java.util.List;

/**
 * Object representation of a table's columns.
 */
public class Columns {

  /**
   * The column names.
   */
  private final List<String> names;

  /**
   * The column types.
   */
  private final List<String> types;

  /**
   * The length/set of each column.
   */
  private final List<String> lengths;

  /**
   * Is NULL allowed?.
   */
  private final List<Boolean> areNullable;

  /**
   * Default values.
   */
  private final List<String> defaults;

  /**
   * Column remarks.
   */
  private final List<String> remarks;

  /**
   * The number of columns stored in this {@link Columns} object.
   */
  private final int size;

  /**
   * Instantiate a new {@link Column} object.
   *
   * @param names an array containing the names of the columns.
   * @param types an array of the columns' types.
   * @param lengths an array of the columns' lengths.
   * @param areNullable can the columns be NULL?
   *                    Possible {@link Boolean} values are:
   *                    <ul>
   *                    <li>{@link Boolean#TRUE} if NULL is allowed,</li>
   *                    <li>{@link Boolean#FALSE} if NULL is not allowed,</li>
   *                    <li>{@link Boolean#UNKNOWN} if unknown.</li>
   *                    </ul>
   * @param defaults an array containing the default values for the columns.
   * @param remarks an array of column remarks.
   */
  public Columns(
      List<String> names,
      List<String> types,
      List<String> lengths,
      List<Boolean> areNullable,
      List<String> defaults,
      List<String> remarks
  ) {
    this.names = names;
    size = names.size();
    if (types.size() != size) {
      throw new AssertionError();
    }
    this.types = types;
    if (lengths.size() != size) {
      throw new AssertionError();
    }
    this.lengths = lengths;
    if (areNullable.size() != size) {
      throw new AssertionError();
    }
    this.areNullable = areNullable;
    if (defaults.size() != size) {
      throw new AssertionError();
    }
    this.defaults = defaults;
    if (remarks.size() != size) {
      throw new AssertionError();
    }
    this.remarks = remarks;
  }

  /**
   * Can the columns contain NULL values?.
   */
  public List<Boolean> areNullable() {
    return areNullable;
  }

  /**
   * Gets the default value of the column at the specified index.
   */
  public String getDefault(int i) {
    return defaults.get(i);
  }

  /**
   * Gets the default values for the columns.
   */
  public List<String> getDefaults() {
    return defaults;
  }

  /**
   * Gets the id of the specified column in the array.
   */
  public int getId(String column) {
    for (int i = 0; i < size; i++) {
      if (names.get(i).equals(column)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Gets the length of the column at the specified index.
   */
  public String getLength(int i) {
    return lengths.get(i);
  }

  /**
   * Gets the length/set of each column.
   */
  public List<String> getLengths() {
    return lengths;
  }

  /**
   * Gets the names of the columns.
   */
  public List<String> getNames() {
    return names;
  }

  /**
   * Gets the column remark at the specified index.
   */
  public String getRemark(int i) {
    return remarks.get(i);
  }

  /**
   * Gets the column remarks.
   */
  public List<String> getRemarks() {
    return remarks;
  }

  /**
   * Gets the number of columns stored here.
   */
  public int getSize() {
    return size;
  }

  /**
   * Gets the type of the column at the specified index.
   */
  public String getType(int i) {
    return types.get(i);
  }

  /**
   * Gets the types of the columns.
   */
  public List<String> getTypes() {
    return types;
  }

  /**
   * Does the column at the specified index allow NULL values?.
   */
  public Boolean isNullable(int i) {
    return areNullable.get(i);
  }
}
