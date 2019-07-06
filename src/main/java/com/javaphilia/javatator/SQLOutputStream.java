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
import java.io.OutputStream;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Executes SQL on-the-fly from a stream.
 */
public class SQLOutputStream extends OutputStream {

	private Statement stmt;

	private boolean doubleQuotes=false;
	private boolean singleQuotes=false;
	private int check=-1;
	private int count=0;
	private final StringBuffer SB=new StringBuffer();

	/**
	 * Constructs this <code>SQLOutputStream</code>.
	 *
	 * @param stmt the <code>Statement</code> for executing the query.
	 */
	public SQLOutputStream(Statement stmt) {
		this.stmt=stmt;
	}

	@Override
	public void close() throws IOException {
		SB.setLength(0);
	}

	/**
	 * Executes the specified SQL query.
	 */
	private void executeSQL(String sql) throws IOException {
		try {
			stmt.executeUpdate(sql);
		} catch(SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void flush() throws IOException {
		super.flush();
	}

	@Override
	public void write(byte[] b) throws IOException {
		write(b, 0, b.length);
	}

	/**
	 * Writes to the stream.
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		int I=check+count-off;
		for(int i=off;i<len;i++) {
			SB.append(b[i]);
			if(i>I) {
				if(b[i]=='\\') check=count+i+1-off;
				else if(!singleQuotes && b[i]=='"') doubleQuotes=!doubleQuotes;
				else if(!doubleQuotes && b[i]=='\'') singleQuotes=!singleQuotes;
				else if(!singleQuotes && !doubleQuotes && b[i]==';') {
					executeSQL(SB.toString());
					SB.setLength(0);
					check=-1;
					count=0;
				}
			}
		}
	}

	public void write(int b) throws IOException {
		write(new byte[]{(byte)b}, 0, 1);
	}
}
