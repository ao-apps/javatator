/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Dan Armstrong.
 *     dan@dans-home.com
 *
 * Copyright (C) 2019, 2021  AO Industries, Inc.
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
public class SchemaRow {

	private static final Color nameColor=new Color(0x6f6f6f);

	private final SchemaTable table;

	private final String name;

	private final List<SchemaForeignKey> foreignKeys=new ArrayList<>();

	public SchemaRow(
		SchemaTable table,
		String name
	) {
		this.table=table;
		this.name=name;
	}

	public void addForeignKey(String table, String row) {
		foreignKeys.add(new SchemaForeignKey(this, table, row));
	}

	public void draw(Graphics G, FontMetrics metrics, int x, int y) {
		int width=getWidth(metrics);

		G.setColor(nameColor);
		int nameWidth=metrics.stringWidth(name);
		G.drawString(name, x+(width-nameWidth)/2, y+metrics.getHeight()-metrics.getDescent());
	}

	public List<SchemaForeignKey> getForeignKeys() {
		return foreignKeys;
	}

	public int getHeight(FontMetrics FM) {
		return FM.getHeight();
	}

	public String getName() {
		return name;
	}

	public SchemaTable getTable() {
		return table;
	}

	public int getWidth(FontMetrics FM) {
		return FM.stringWidth(name)+4;
	}
}
