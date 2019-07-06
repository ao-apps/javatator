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
		StringBuffer SB=new StringBuffer(len);
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
			StringBuffer SB=new StringBuffer(len);
			for(int c=0;c<len;c++) {
				char ch=S.charAt(c);
				if(ch=='<') SB.append("&#60;");
				else if(ch=='&') SB.append("&#38;");
				else if(ch=='"') SB.append("&#34;");
				else if(ch=='\'') SB.append("&#39;");
				else if(ch=='\n') SB.append("<BR>");
				else SB.append(ch);
			}
			return SB.toString();
		}
		return null;
	}

	/**
	 * Escapes HTML so that it can be put in between <code>&lt;TEXTAREA>&lt;/TEXTAREA></code> etc.
	 *
	 * @param S the string to be escaped.
	 */
	public static String escapeInputValue(String S) {
		if(S!=null) {
			int len=S.length();
			StringBuffer SB=new StringBuffer(len);
			for(int c=0;c<len;c++) {
				char ch=S.charAt(c);
				if(ch=='<') SB.append("&#60;");
				else if(ch=='&') SB.append("&#38;");
				else if(ch=='"') SB.append("&#34;");
				else if(ch=='\'') SB.append("&#39;");
				else SB.append(ch);
			}
			return SB.toString();
		}
		return null;
	}

	/**
	 * Escapes the specified <code>String</code> so that it can be put in a JavaScript string.
	 *
	 * @param S the string to be escaped.
	 */
	public static String escapeJavaScript(String S) {
		if(S!=null) {
			int len=S.length();
			StringBuffer SB=new StringBuffer(len);
			for(int c=0;c<len;c++) {
				char ch=S.charAt(c);
				if(ch=='"') SB.append("&quot;"); // TODO: Use ao-encoding and onclick scripts: SB.append("\\\"");
				else if(ch=='\'') SB.append("\\'");
				else if(ch=='\n') SB.append("\\n");
				else if(ch=='\\') SB.append("\\\\");
				else SB.append(ch);
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
		StringBuffer SB=new StringBuffer();
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
		StringBuffer B=new StringBuffer();
		escapeSQL(S, B);
		return B.toString();
	}

	/**
	 * Escapes SQL so that it can be used safely in queries.
	 *
	 * @param S the string to be escaped.
	 * @param B the <code>StringBuffer</code> to append to.
	 */
	public static void escapeSQL(String S, StringBuffer B) {
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
			StringBuffer B=new StringBuffer();
			B.append('\'');
			escapeSQL(S, B);
			return B.append('\'').toString();
		}
	}

	/**
	 * Gets comma-separated list from a <code>Vector</code> of <code>String</code>s.
	 */
	public static String getCommaList(List<String> V) {
		StringBuffer SB=new StringBuffer();
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
	 * Escapes HTML for displaying in browsers and writes to the specified <code>JavatatorWriter</code>.
	 *
	 * @param out the <code>JavatatorWriter</code> to write to.
	 * @param S the string to be escaped.
	 */
	public static void printEscapedHTML(JavatatorWriter out, String S) {
		if(S!=null) {
			int len=S.length();
			for(int c=0;c<len;c++) {
				char ch=S.charAt(c);
				if(ch=='<') out.print("&#60;");
				else if(ch=='&') out.print("&#38;");
				else if(ch=='"') out.print("&#34;");
				else if(ch=='\'') out.print("&#39;");
				else if(ch=='\n') out.print("<BR>");
				else out.print(ch);
			}
		}
	}

	/**
	 * Escapes HTML for displaying in browsers and writes to the specified <code>JavatatorWriter</code>.
	 *
	 * @param out the <code>JavatatorWriter</code> to write to.
	 * @param S the string to be escaped.
	 */
	public static void printEscapedInputValue(JavatatorWriter out, String S) {
		if(S!=null) {
			int len=S.length();
			for(int c=0;c<len;c++) {
				char ch=S.charAt(c);
				if(ch=='<') out.print("&#60;");
				else if(ch=='&') out.print("&#38;");
				else if(ch=='"') out.print("&#34;");
				else if(ch=='\'') out.print("&#39;");
				else out.print(ch);
			}
		}
	}

	/**
	 * Escapes the specified <code>String</code> so that it can be put in a JavaScript string.
	 * Writes to the specified <code>JavatatorWriter</code>.
	 *
	 * @param out the <code>JavatatorWriter</code> to write to.
	 * @param S the string to be escaped.
	 */
	public static void printEscapedJavaScript(JavatatorWriter out, String S) {
		if(S!=null) {
			int len=S.length();
			for(int c=0;c<len;c++) {
				char ch=S.charAt(c);
				if(ch=='"') out.print("\\\"");
				else if(ch=='\'') out.print("\\'");
				else if(ch=='\n') out.print("\\n");
				else if(ch=='\t') out.print("\\t");
				else out.print(ch);
			}
		}
	}

	/**
	 * Escapes SQL so that it can be used safely in queries.
	 * Writes to the specified <code>JavatatorWriter</code>.
	 *
	 * @param out the <code>JavatatorWriter</code> to write to.
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
	 * Writes to the specified <code>JavatatorWriter</code>.
	 *
	 * @param out the <code>JavatatorWriter</code> to write to.
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
