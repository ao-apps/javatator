/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Dan Armstrong.
 *     dan@dans-home.com
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

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

/**
 * Object hierarchical representation of the foreign key relationships.
 */
public class SchemaTable {

	/**
	 * The color of the border.
	 */
	private static final Color borderColor=new Color(0xa8dda0);

	/**
	 * The background color of the table.
	 */
	private static final Color background=new Color(0xebffed);

	/**
	 * The color of the name.
	 */
	private static final Color nameColor=new Color(0x6f6f6f);

	final private String name;

	final private List<SchemaRow> rows = new ArrayList<>();

	public SchemaTable(String name) {
		this.name=name;
	}

	public void draw(Graphics G, FontMetrics metrics, int x, int y) {
		G.setColor(background);
		int width=getWidth(metrics);
		int height=getHeight(metrics);
		G.fillRect(x, y, width, height);
		G.setColor(borderColor);
		G.drawRect(x, y, width-1, height-1);
		G.drawLine(x, y+metrics.getHeight(), x+width-1, y+metrics.getHeight());

		G.setColor(nameColor);
		int nameWidth=metrics.stringWidth(name);
		G.drawString(name, x+(width-nameWidth)/2, y+metrics.getHeight()-metrics.getDescent());

		// Draw all the rows
		x += 1;
		y += metrics.getHeight()+1;
		int len=rows.size();
		for(int c=0;c<len;c++) {
			SchemaRow row=rows.get(c);
			row.draw(G, metrics, x, y);
			y+=row.getHeight(metrics);
		}
	}

	public int getHeight(FontMetrics metrics) {
		int height=3+metrics.getHeight();
		int size=rows.size();
		for(int c=0;c<size;c++) {
			height+=rows.get(c).getHeight(metrics);
		}
		return height;
	}

	public String getName() {
		return name;
	}

	synchronized public SchemaRow getRow(String rowName) {
		int size=rows.size();
		for(int c=0;c<size;c++) {
			SchemaRow row=rows.get(c);
			if(row.getName().equals(rowName)) return row;
		}
		SchemaRow row=new SchemaRow(this, rowName);
		rows.add(row);
		return row;
	}

	/**
	 * Gets the y position of the center of a row, relative to top of table.
	 */
	public int getRowLinkY(String rowName, FontMetrics metrics) {
		int size=rows.size();
		int ypos=2+metrics.getHeight();
		for(int c=0;c<size;c++) {
			SchemaRow row=rows.get(c);
			int rowHeight=row.getHeight(metrics);
			if(row.getName().equals(rowName)) {
				// Find center and return
				return ypos+rowHeight/2-1;
			}
			ypos+=rowHeight;
		}
		// Row not found, throw error
		throw new NullPointerException("Row not found: "+rowName);
	}

	public List<SchemaRow> getRows() {
		return rows;
	}

	public int getWidth(FontMetrics metrics) {
		int widest=metrics.stringWidth(name)+4;
		int size=rows.size();
		for(int c=0;c<size;c++) {
			int rowWidth=rows.get(c).getWidth(metrics);
			if(rowWidth>widest) widest=rowWidth;
		}
		return widest+2;
	}
}
