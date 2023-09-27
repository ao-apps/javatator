/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Dan Armstrong.
 *     dan@dans-home.com
 *
 * Copyright (C) 2019, 2022, 2023  AO Industries, Inc.
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

import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Provides additional print methods for writing tables.
 */
public class JavatatorWriter extends PrintWriter {

  /**
   * Constructs this {@link JavatatorWriter}.
   */
  public JavatatorWriter(OutputStream out) {
    super(out);
  }

  /**
   * Ends the standard table.
   */
  public void endTable() {
    print("</table></td></tr></table>\n");
  }

  /**
   * Prints the ending part of a table element.
   */
  public void endTd() {
    print("</td>\n");
  }

  /**
   * Prints the ending of a table row.
   */
  public void endTr() {
    print("</tr>\n");
  }

  /**
   * Prints a standard table element.
   */
  public void printTd(int content) {
    startTd();
    print(content);
    endTd();
  }

  /**
   * Prints a standard table element.
   */
  public void printTd(Object content) {
    if ("".equals(content)) {
      startAltTd("");
    } else {
      startTd();
    }
    print(content);
    endTd();
  }

  /**
   * Prints a standard table element.
   */
  public void printTd(String content) {
    if ("".equals(content)) {
      startAltTd("");
    } else {
      startTd();
    }
    print(content);
    endTd();
  }

  /**
   * Prints a standard table element with additional attributes.
   */
  public void printTd(String content, String attributes) {
    if ("".equals(content)) {
      startAltTd(attributes);
    } else {
      startTd(attributes);
    }
    print(content);
    endTd();
  }

  /**
   * Prints a standard table header.
   */
  public void printTh(String label) {
    print("<th>");
    print(label);
    print("</th>\n");
  }

  /**
   * Starts the alternate table.
   */
  public void startAltTable(String width) {
    print("<table ");
    if (width != null) {
      print("width='");
      print(width);
      print("' ");
    }
    print("border=0 cellspacing=0><tr><td class='ALTBORDER'><table ");
    if (width != null) {
      print("width='");
      print(width);
      print("' ");
    }
    print("border=0 cellpadding=3 cellspacing=0>\n");
  }

  /**
   * Prints the beginning part of a table element with additional attributes.
   */
  public void startAltTd(String attributes) {
    print("<td class='ALTBG' ");
    print(attributes);
    print('>');
  }

  /**
   * Starts the body of the HTML page.
   */
  public void startBody() {
    print("<body>\n");
  }

  /**
   * Starts the standard table.
   */
  public void startTable(String width) {
    print("<table ");
    if (width != null) {
      print("width='");
      print(width);
      print("' ");
    }
    print("border=0 cellspacing=0><tr><td class='NORMBORDER'><table ");
    if (width != null) {
      print("width='");
      print(width);
      print("' ");
    }
    print("border=0 cellpadding=3 cellspacing=0>\n");
  }

  /**
   * Starts the standard table with the provided attributes.
   */
  public void startTable(String width, String attributes) {
    print("<table ");
    if (width != null) {
      print("width='");
      print(width);
      print("' ");
    }
    print("border=0 cellspacing=0><tr><td class='NORMBORDER'><table ");
    if (width != null) {
      print("width='");
      print(width);
      print("' ");
    }
    print("border=0 cellpadding=3 ");
    if (!attributes.contains("cellspacing")) {
      print("cellspacing=0 ");
    }
    print(attributes);
    print(">\n");
  }

  /**
   * Prints the beginning part of a table element.
   */
  public void startTd() {
    print("<td>");
  }

  /**
   * Prints the beginning part of a table element with additional attributes.
   */
  public void startTd(String attributes) {
    print("<td ");
    print(attributes);
    print('>');
  }

  /**
   * Prints the beginning of a table row.
   */
  public void startTr() {
    print("<tr>\n");
  }
}
