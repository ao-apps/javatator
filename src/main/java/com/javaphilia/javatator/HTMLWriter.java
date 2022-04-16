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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * Provides additional print methods for writing tables.
 */
public class HTMLWriter extends FilterWriter {

	// TODO: Switch to ao-encoding?
	private static final char[] LT=new char[] {'&', '#', '6', '0', ';'};
	private static final char[] AMP=new char[] {'&', '#', '3', '8', ';'};
	private static final char[] DQ=new char[] {'&', '#', '3', '4', ';'};
	private static final char[] SQ=new char[] {'&', '#', '3', '9', ';'};
	private static final char[] BR=new char[] {'<', 'b', 'r', ' ', '/', '>'};

	/**
	 * Constructs this {@link HTMLWriter}.
	 */
	public HTMLWriter(Writer out) {
		super(out);
	}

	/**
	 * Write a portion of an array of characters.
	 *
	 * @param  cbuf  Buffer of characters to be written
	 * @param  off   Offset from which to start reading characters
	 * @param  len   Number of characters to be written
	 *
	 * @exception  IOException  If an I/O error occurs
	 */
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		for(int i=off;i<len;i++) {
			switch(cbuf[i]) {
				case '<':
					out.write(LT);
					break;
				case '&':
					out.write(AMP);
					break;
				case '"':
					out.write(DQ);
					break;
				case '\'':
					out.write(SQ);
					break;
				case '\n':
					out.write(BR);
					break;
				default:
					out.write(cbuf[i]);
					break;
			}
		}
	}

	/**
	 * Write a single character.
	 *
	 * @exception  IOException  If an I/O error occurs
	 */
	@Override
	public void write(int c) throws IOException {
		switch(c) {
			case '<':
				out.write(LT);
				break;
			case '&':
				out.write(AMP);
				break;
			case '"':
				out.write(DQ);
				break;
			case '\'':
				out.write(SQ);
				break;
			case '\n':
				out.write(BR);
				break;
			default:
				out.write(c);
				break;
		}
	}

	/**
	 * Write a portion of a string.
	 *
	 * @param  str  String to be written
	 * @param  off  Offset from which to start reading characters
	 * @param  len  Number of characters to be written
	 *
	 * @exception  IOException  If an I/O error occurs
	 */
	@Override
	public void write(String str, int off, int len) throws IOException {
		write(str.toCharArray(), off, len);
	}
}
