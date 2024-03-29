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

import java.util.List;

/**
 * Object representation of primary key data for a table.
 */
public class PrimaryKeys {

  /**
   * The names of the columns.
   */
  private final List<String> columns;

  /**
   * The names of the primary keys.
   */
  private final List<String> names;

  public PrimaryKeys(List<String> columns, List<String> names) {
    this.columns = columns;
    this.names = names;
  }

  /**
   * Gets the column names.
   */
  public List<String> getColumns() {
    return columns;
  }

  /**
   * Gets the primary key names.
   */
  public List<String> getNames() {
    return names;
  }
}
