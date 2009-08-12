package com.javaphilia.javatator;

/**
 * Javatator - multi-database admin tool
 * 
 * Copyright (C) 2001  Jason Davies.
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
 * 
 * If you want to help or want to report any bugs, please email me:
 * jason@javaphilia.com
 * 
 */

/**
 * Object hierarchical representation of the foreign key relationships.
 *
 * @author Dan Armstrong
 */
public class SchemaForeignKey {

	private SchemaRow row;

	private String foreignTableName;

	private String foreignRowName;

	public SchemaForeignKey(
        SchemaRow row,
        String tableName,
        String rowName
    ) {
        this.row=row;
        this.foreignTableName=tableName;
        this.foreignRowName=rowName;
	}

    public String getForeignRowName() {
        return foreignRowName;
	}

    public String getForeignTableName() {
        return foreignTableName;
	}

    public SchemaRow getRow() {
        return row;
	}
}
