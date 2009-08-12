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

import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Provides additional print methods for writing tables.
 *
 * @author Dan Armstrong
 */
public class JavatatorWriter extends PrintWriter {

	/**
	 * Constructs this <code>JavatatorWriter</code>.
	 */
	public JavatatorWriter(OutputStream out) {
        super(out);
	}

    /**
	 * Ends the body of the HTML page.
	 */
	public void endBody() {
        print("</body>\n");
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
	public void endTD() {
        print("</td>\n");
	}

    /**
	 * Prints the ending of a table row.
	 */
	public void endTR() {
        print("</tr>\n");
	}

    /**
	 * Prints a standard table element.
	 */
	public void printTD(int content) {
        startTD();
        print(content);
        endTD();
	}

    /**
	 * Prints a standard table element.
	 */
	public void printTD(Object content) {
        if("".equals(content)) startAltTD("");
        else startTD();
        print(content);
        endTD();
	}

    /**
	 * Prints a standard table element.
	 */
	public void printTD(String content) {
        if("".equals(content)) startAltTD("");
        else startTD();
        print(content);
        endTD();
	}

    /**
	 * Prints a standard table element with additional attributes.
	 */
	public void printTD(String content, String attributes) {
        if("".equals(content)) startAltTD(attributes);
        else startTD(attributes);
        print(content);
        endTD();
	}

    /**
	 * Prints a standard table header.
	 */
	public void printTH(String label) {
        print("<th>");
        print(label);
        print("</th>\n");
	}

    /**
	 * Starts the alternate table.
	 */
	public void startAltTable(String width) {
        print("<table ");
        if(width!=null) {
            print("width='");
            print(width);
            print("' ");
        }
        print("border=0 cellspacing=0><tr><td class='ALTBORDER'><table ");
        if(width!=null) {
            print("width='");
            print(width);
            print("' ");
        }
        print("border=0 cellpadding=3 cellspacing=0>\n");
	}

    /**
	 * Prints the beginning part of a table element with additional attributes.
	 */
	public void startAltTD(String attributes) {
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
        if(width!=null) {
            print("width='");
            print(width);
            print("' ");
        }
        print("border=0 cellspacing=0><tr><td class='NORMBORDER'><table ");
        if(width!=null) {
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
        if(width!=null) {
            print("width='");
            print(width);
            print("' ");
        }
        print("border=0 cellspacing=0><tr><td class='NORMBORDER'><table ");
        if(width!=null) {
            print("width='");
            print(width);
            print("' ");
        }
        print("border=0 cellpadding=3 ");
        if(attributes.indexOf("cellspacing")==-1) print("cellspacing=0 ");
        print(attributes);
        print(">\n");
	}

    /**
	 * Prints the beginning part of a table element.
	 */
	public void startTD() {
        print("<td>");
	}

    /**
	 * Prints the beginning part of a table element with additional attributes.
	 */
	public void startTD(String attributes) {
        print("<td ");
        print(attributes);
        print('>');
	}

    /**
	 * Prints the beginning of a table row.
	 */
	public void startTR() {
        print("<tr>\n");
	}
}
