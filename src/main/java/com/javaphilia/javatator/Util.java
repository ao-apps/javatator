/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2000  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2018, 2019  AO Industries, Inc.
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
import java.io.Writer;
import java.util.List;

/**
 * Utility class.
 */
public class Util {

	private Util() {
	}

	private static final char[] hexChars={'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

	/**
	 * Adds a space to commas to make certain strings wrap properly in tables
	 */
	public static String addSpaceToCommas(String S) {
		int len=S.length();
		StringBuilder SB=new StringBuilder(len);
		for(int c=0;c<len;c++) {
			char ch=S.charAt(c);
			if(ch==',') SB.append(", ");
			else SB.append(ch);
		}
		return SB.toString();
	}

	/**
	 * Escapes HTML so that it can be displayed properly in browsers.
	 *
	 * @param S the string to be escaped.
	 */
	public static String escapeHTML(String S) {
		if(S!=null) {
			int len=S.length();
			StringBuilder SB=new StringBuilder(len);
			for(int c=0;c<len;c++) {
				char ch=S.charAt(c);
				switch(ch) {
					case '<':
						SB.append("&#60;");
						break;
					case '&':
						SB.append("&#38;");
						break;
					case '"':
						SB.append("&#34;");
						break;
					case '\'':
						SB.append("&#39;");
						break;
					case '\n':
						SB.append("<BR>"); // TODO: lower-case all HTML
						break;
					default:
						SB.append(ch);
						break;
				}
			}
			return SB.toString();
		}
		return null;
	}

	/**
	 * Escapes HTML so that it can be put in between <code>&lt;TEXTAREA&gt;&lt;/TEXTAREA&gt;</code> etc.
	 *
	 * @param S the string to be escaped.
	 */
	public static String escapeInputValue(String S) {
		if(S!=null) {
			int len=S.length();
			StringBuilder SB=new StringBuilder(len);
			for(int c=0;c<len;c++) {
				char ch=S.charAt(c);
				switch(ch) {
					case '<':
						SB.append("&#60;");
						break;
					case '&':
						SB.append("&#38;");
						break;
					case '"':
						SB.append("&#34;");
						break;
					case '\'':
						SB.append("&#39;");
						break;
					default:
						SB.append(ch);
						break;
				}
			}
			return SB.toString();
		}
		return null;
	}

	/**
	 * Escapes the specified {@link String} so that it can be put in a JavaScript string.
	 *
	 * @param S the string to be escaped.
	 */
	public static String escapeJavaScript(String S) {
		if(S!=null) {
			int len=S.length();
			StringBuilder SB=new StringBuilder(len);
			for(int c=0;c<len;c++) {
				// TODO: Use ao-encoding and onclick scripts: SB.append("\\\"");
				char ch=S.charAt(c);
				switch(ch) {
					case '"':
						SB.append("&quot;");
						break;
					case '\'':
						SB.append("\\'");
						break;
					case '\n':
						SB.append("\\n");
						break;
					case '\\':
						SB.append("\\\\");
						break;
					default:
						SB.append(ch);
						break;
				}
			}
			return SB.toString();
		}
		return null;
	}

	/**
	 * Escapes the '' returned by ENUMs in MySQL.
	 */
	public static String escapeMySQLQuotes(String S) {
		int len=S.length();
		int len2=len-1;
		StringBuilder SB=new StringBuilder();
		for(int i=0;i<len;i++) {
			char c=S.charAt(i);
			if(i<len2 && c=='\'' && S.charAt(i+1)=='\'') {
				SB.append("\'");
				i++;
			} else SB.append(c);
		}
		return SB.toString();
	}

	/**
	 * Escapes SQL so that it can be used safely in queries.
	 *
	 * @param S the string to be escaped.
	 */
	public static String escapeSQL(String S) {
		int i;
		StringBuilder B=new StringBuilder();
		escapeSQL(S, B);
		return B.toString();
	}

	/**
	 * Escapes SQL so that it can be used safely in queries.
	 *
	 * @param S the string to be escaped.
	 * @param B the {@link StringBuilder} to append to.
	 */
	public static void escapeSQL(String S, StringBuilder B) {
		int i;
		for (i=0;i<S.length();i++) {
			char c = S.charAt(i);

			if (c=='\\' || c=='\'' || c=='"') {
				B.append('\\');
			}
			B.append(c);
		}
	}

	/**
	 * Escapes a value to be put in an SQL query i.e. returns <code>'blah'</code>.
	 *
	 * @param S the string to be escaped.
	 */
	public static String escapeSQLValue(String S) {
		if(S==null) {
			return "null";
		} else {
			StringBuilder B=new StringBuilder();
			B.append('\'');
			escapeSQL(S, B);
			return B.append('\'').toString();
		}
	}

	/**
	 * Gets comma-separated list from a {@link List} of {@link String}.
	 */
	public static String getCommaList(List<String> V) {
		StringBuilder SB=new StringBuilder();
		int size=V.size();
		for(int i=0;i<size;i++) {
			if(SB.length()>0) SB.append(',');
			SB.append(V.get(i));
		}
		return SB.toString();
	}

	private static char getHex(int value) {
		return hexChars[value&15];
	}

	/**
	 * Escapes HTML for displaying in browsers and writes to the specified {@link JavatatorWriter}.
	 *
	 * @param out the {@link JavatatorWriter} to write to.
	 * @param S the string to be escaped.
	 */
	public static void printEscapedHTML(JavatatorWriter out, String S) {
		if(S!=null) {
			int len=S.length();
			for(int c=0;c<len;c++) {
				char ch=S.charAt(c);
				switch(ch) {
					case '<':
						out.print("&#60;");
						break;
					case '&':
						out.print("&#38;");
						break;
					case '"':
						out.print("&#34;");
						break;
					case '\'':
						out.print("&#39;");
						break;
					case '\n':
						out.print("<BR>");
						break;
					default:
						out.print(ch);
						break;
				}
			}
		}
	}

	/**
	 * Escapes HTML for displaying in browsers and writes to the specified {@link JavatatorWriter}.
	 *
	 * @param out the {@link JavatatorWriter} to write to.
	 * @param S the string to be escaped.
	 */
	public static void printEscapedInputValue(JavatatorWriter out, String S) {
		if(S!=null) {
			int len=S.length();
			for(int c=0;c<len;c++) {
				char ch=S.charAt(c);
				switch(ch) {
					case '<':
						out.print("&#60;");
						break;
					case '&':
						out.print("&#38;");
						break;
					case '"':
						out.print("&#34;");
						break;
					case '\'':
						out.print("&#39;");
						break;
					default:
						out.print(ch);
						break;
				}
			}
		}
	}

	/**
	 * Escapes the specified {@link String} so that it can be put in a JavaScript string.
	 * Writes to the specified {@link JavatatorWriter}.
	 *
	 * @param out the {@link JavatatorWriter} to write to.
	 * @param S the string to be escaped.
	 */
	public static void printEscapedJavaScript(JavatatorWriter out, String S) {
		if(S!=null) {
			int len=S.length();
			for(int c=0;c<len;c++) {
				char ch=S.charAt(c);
				switch(ch) {
					case '"':
						out.print("\\\"");
						break;
					case '\'':
						out.print("\\'");
						break;
					case '\n':
						out.print("\\n");
						break;
					case '\t':
						out.print("\\t");
						break;
					default:
						out.print(ch);
						break;
				}
			}
		}
	}

	/**
	 * Escapes SQL so that it can be used safely in queries.
	 * Writes to the specified {@link JavatatorWriter}.
	 *
	 * @param out the {@link JavatatorWriter} to write to.
	 * @param S the string to be escaped.
	 */
	public static void printEscapedSQL(Writer out, String S) throws IOException {
		int i;
		for (i=0;i<S.length();i++) {
			char c=S.charAt(i);
			if(c=='\n') out.write("\\n");
			else {
				if (c=='\\' || c=='\'' || c=='"') {
					out.write('\\');
				}
				out.write(c);
			}
		}
	}

	/**
	 * Escapes a value to be put in an SQL query i.e. <code>'blah'</code>.
	 * Writes to the specified {@link JavatatorWriter}.
	 *
	 * @param out the {@link JavatatorWriter} to write to.
	 * @param S the string to be escaped.
	 */
	public static void printEscapedSQLValue(Writer out, String S) throws IOException {
		if(S==null) {
			out.write("null");
		} else {
			out.write('\'');
			printEscapedSQL(out, S);
			out.write('\'');
		}
	}

	/**
	 * Prints a value that may be placed in a URL.
	 */
	public static void printEscapedURLValue(JavatatorWriter out, String value) {
		int len=value.length();
		for(int c=0;c<len;c++) {
			char ch=value.charAt(c);
			if(ch==' ') out.print('+');
			else {
				if(
				   (ch>='0' && ch<='9')
				   || (ch>='a' && ch<='z')
				   || (ch>='A' && ch<='Z')
				) {
					out.print(ch);
				} else {
					out.print('%');
					out.print(getHex(ch>>>4));
					out.print(getHex(ch));
				}
			}
		}
	}
}
