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
 * Object representation of the check constraints in a table.
 */
public class CheckConstraints {

	/**
	 * The constraint names.
	 */
	private final List<String> names;

	/**
	 * The check clauses.
	 */
	private final List<String> checkClauses;

	public CheckConstraints(List<String> names, List<String> checkClauses) {
		this.names=names;
		this.checkClauses=checkClauses;
	}

	/**
	 * Gets the CHECK clauses.
	 */
	public List<String> getCheckClauses() {
		return checkClauses;
	}

	/**
	 * Gets the names of the CHECK constraints.
	 */
	public List<String> getNames() {
		return names;
	}
}
