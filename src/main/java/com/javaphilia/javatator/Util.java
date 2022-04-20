/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2000  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2018, 2019, 2021, 2022  AO Industries, Inc.
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
public final class Util {

  /** Make no instances. */
  private Util() {
    throw new AssertionError();
  }

  private static final char[] hexChars={'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

  /**
   * Adds a space to commas to make certain strings wrap properly in tables
   */
  public static String addSpaceToCommas(String s) {
    int len = s.length();
    StringBuilder sb = new StringBuilder(len);
    for (int c = 0; c < len; c++) {
      char ch = s.charAt(c);
      if (ch == ',') {
        sb.append(", ");
      } else {
        sb.append(ch);
      }
    }
    return sb.toString();
  }

  /**
   * Escapes HTML so that it can be displayed properly in browsers.
   *
   * @param s the string to be escaped.
   */
  public static String escapeHTML(String s) {
    if (s != null) {
      int len = s.length();
      StringBuilder sb = new StringBuilder(len);
      for (int c = 0; c < len; c++) {
        char ch = s.charAt(c);
        switch (ch) {
          case '<':
            sb.append("&#60;");
            break;
          case '&':
            sb.append("&#38;");
            break;
          case '"':
            sb.append("&#34;");
            break;
          case '\'':
            sb.append("&#39;");
            break;
          case '\n':
            sb.append("<BR>"); // TODO: lower-case all HTML
            break;
          default:
            sb.append(ch);
            break;
        }
      }
      return sb.toString();
    }
    return null;
  }

  /**
   * Escapes HTML so that it can be put in between <code>&lt;TEXTAREA&gt;&lt;/TEXTAREA&gt;</code> etc.
   *
   * @param s the string to be escaped.
   */
  public static String escapeInputValue(String s) {
    if (s != null) {
      int len = s.length();
      StringBuilder sb = new StringBuilder(len);
      for (int c = 0; c < len; c++) {
        char ch = s.charAt(c);
        switch (ch) {
          case '<':
            sb.append("&#60;");
            break;
          case '&':
            sb.append("&#38;");
            break;
          case '"':
            sb.append("&#34;");
            break;
          case '\'':
            sb.append("&#39;");
            break;
          default:
            sb.append(ch);
            break;
        }
      }
      return sb.toString();
    }
    return null;
  }

  /**
   * Escapes the specified {@link String} so that it can be put in a JavaScript string.
   *
   * @param s the string to be escaped.
   */
  public static String escapeJavaScript(String s) {
    if (s != null) {
      int len = s.length();
      StringBuilder sb = new StringBuilder(len);
      for (int c = 0; c < len; c++) {
        // TODO: Use ao-encoding and onclick scripts: SB.append("\\\"");
        char ch = s.charAt(c);
        switch (ch) {
          case '"':
            sb.append("&quot;");
            break;
          case '\'':
            sb.append("\\'");
            break;
          case '\n':
            sb.append("\\n");
            break;
          case '\\':
            sb.append("\\\\");
            break;
          default:
            sb.append(ch);
            break;
        }
      }
      return sb.toString();
    }
    return null;
  }

  /**
   * Escapes the '' returned by ENUMs in MySQL.
   */
  @SuppressWarnings("AssignmentToForLoopParameter")
  public static String escapeMySQLQuotes(String s) {
    int len = s.length();
    int len2 = len - 1;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      if (i < len2 && c == '\'' && s.charAt(i + 1) == '\'') {
        sb.append("\'");
        i++;
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }

  /**
   * Escapes SQL so that it can be used safely in queries.
   *
   * @param s the string to be escaped.
   */
  public static String escapeSQL(String s) {
    int i;
    StringBuilder b = new StringBuilder();
    escapeSQL(s, b);
    return b.toString();
  }

  /**
   * Escapes SQL so that it can be used safely in queries.
   *
   * @param s the string to be escaped.
   * @param sb the {@link StringBuilder} to append to.
   */
  public static void escapeSQL(String s, StringBuilder sb) {
    int i;
    for (i = 0; i < s.length(); i++) {
      char c = s.charAt(i);

      if (c == '\\' || c == '\'' || c == '"') {
        sb.append('\\');
      }
      sb.append(c);
    }
  }

  /**
   * Escapes a value to be put in an SQL query i.e. returns <code>'blah'</code>.
   *
   * @param s the string to be escaped.
   */
  public static String escapeSQLValue(String s) {
    if (s == null) {
      return "null";
    } else {
      StringBuilder b = new StringBuilder();
      b.append('\'');
      escapeSQL(s, b);
      return b.append('\'').toString();
    }
  }

  /**
   * Gets comma-separated list from a {@link List} of {@link String}.
   */
  public static String getCommaList(List<String> list) {
    StringBuilder sb = new StringBuilder();
    int size = list.size();
    for (int i = 0; i < size; i++) {
      if (sb.length() > 0) {
        sb.append(',');
      }
      sb.append(list.get(i));
    }
    return sb.toString();
  }

  private static char getHex(int value) {
    return hexChars[value&15];
  }

  /**
   * Escapes HTML for displaying in browsers and writes to the specified {@link JavatatorWriter}.
   *
   * @param out the {@link JavatatorWriter} to write to.
   * @param s the string to be escaped.
   */
  public static void printEscapedHTML(JavatatorWriter out, String s) {
    if (s != null) {
      int len = s.length();
      for (int c = 0; c < len; c++) {
        char ch = s.charAt(c);
        switch (ch) {
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
   * @param s the string to be escaped.
   */
  public static void printEscapedInputValue(JavatatorWriter out, String s) {
    if (s != null) {
      int len = s.length();
      for (int c = 0; c < len; c++) {
        char ch = s.charAt(c);
        switch (ch) {
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
   * @param s the string to be escaped.
   */
  public static void printEscapedJavaScript(JavatatorWriter out, String s) {
    if (s != null) {
      int len = s.length();
      for (int c = 0; c < len; c++) {
        char ch = s.charAt(c);
        switch (ch) {
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
   * @param s the string to be escaped.
   */
  public static void printEscapedSQL(Writer out, String s) throws IOException {
    int i;
    for (i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '\n') {
        out.write("\\n");
      } else {
        if (c == '\\' || c == '\'' || c == '"') {
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
   * @param s the string to be escaped.
   */
  public static void printEscapedSQLValue(Writer out, String s) throws IOException {
    if (s == null) {
      out.write("null");
    } else {
      out.write('\'');
      printEscapedSQL(out, s);
      out.write('\'');
    }
  }

  /**
   * Prints a value that may be placed in a URL.
   */
  public static void printEscapedURLValue(JavatatorWriter out, String value) {
    int len = value.length();
    for (int c = 0; c < len; c++) {
      char ch = value.charAt(c);
      if (ch == ' ') {
        out.print('+');
      } else {
        if (
           (ch >= '0' && ch <= '9')
           || (ch >= 'a' && ch <= 'z')
           || (ch >= 'A' && ch <= 'Z')
        ) {
          out.print(ch);
        } else {
          out.print('%');
          out.print(getHex(ch >>> 4));
          out.print(getHex(ch));
        }
      }
    }
  }
}
