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

import java.util.List;

/**
 * Object representation of table privileges.
 *
 * @author Jason Davies
 */
public class TablePrivileges {

	final private List<String> grantors;
	final private List<String> grantees;
	final private List<String> privileges;
	final private List<JDBCConnector.Boolean> isGrantable;

	/**
	 * Instantiate a new <code>TablePrivileges</code> object.
	 *
	 * @param grantors an array containing the users who have granted privileges to the table.
	 * @param grantees an array containing the users who have been granted privileges to the table.
	 * @param privileges an array containing the privileges which have been granted.
	 * @param isGrantable
	 */
	public TablePrivileges(
        List<String> grantors,
        List<String> grantees,
        List<String> privileges,
        List<JDBCConnector.Boolean> isGrantable
    ) {
        this.grantors=grantors;
        this.grantees=grantees;
        this.privileges=privileges;
        this.isGrantable=isGrantable;
	}

    public List<String> getGrantees() {
        return grantees;
	}

    public List<String> getGrantors() {
        return grantors;
	}

    public List<JDBCConnector.Boolean> getIsGrantable() {
        return isGrantable;
	}

    public List<String> getPrivileges() {
        return privileges;
	}
}
