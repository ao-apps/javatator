/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Dan Armstrong.
 *     dan@dans-home.com
 *
 * Copyright (C) 2015, 2019, 2020  AO Industries, Inc.
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

import Acme.JPM.Encoders.GifEncoder;
import com.aoindustries.collections.AoCollections;
import com.aoindustries.io.ContentType;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet that generates a GIF image of the database schema.
 */
@WebServlet("/schema.gif")
public class SchemaImage extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * The different structures are based on
	 *
	 *   1 2 3
	 *  4 5 6 7
	 *   8 9 a
	 *
	 *
	 *    1 2 3 4 5
	 *   6 h i j k 7
	 *  8 s p q r t 9
	 *   a l m n o b
	 *    c d e f g
	 *
	 *
	 *     1 2 3 4 5 6 7
	 *    8 v w y x z A 9
	 *   a J K L M N O P b
	 *  c B t r p q s u C d
	 *   e Q R S T U V W f
	 *    g D E F G H I h
	 *     i j k l m n o
	 */
	private static final short[][][] structures={
		{
			{  0,  1,  0,  2,  0,  3,  0 },
			{  4,  0,  5,  0,  6,  0,  7 },
			{  0,  8,  0,  9,  0, 10,  0 }
		},{
			{  0,  0,  1,  0,  2,  0,  3,  0,  4,  0,  5,  0,  0 },
			{  0,  6,  0, 20,  0, 21,  0, 22,  0, 23,  0,  7,  0 },
			{  8,  0, 28,  0, 18,  0, 17,  0, 19,  0, 29,  0,  9 },
			{  0, 10,  0, 24,  0, 25,  0, 26,  0, 27,  0, 11,  0 },
			{  0,  0, 12,  0, 13,  0, 14,  0, 15,  0, 16,  0,  0 }
		},{
			{  0,  0,  0,  1,  0,  2,  0,  3,  0,  4,  0,  5,  0,  6,  0,  7,  0,  0,  0 },
			{  0,  0,  8,  0, 31,  0, 32,  0, 33,  0, 34,  0, 35,  0, 36,  0,  9,  0,  0 },
			{  0, 10,  0, 45,  0, 46,  0, 47,  0, 48,  0, 49,  0, 50,  0, 51,  0, 11,  0 },
			{ 12,  0, 37,  0, 29,  0, 27,  0, 25,  0, 26,  0, 28,  0, 30,  0, 38,  0, 13 },
			{  0, 14,  0, 52,  0, 53,  0, 54,  0, 55,  0, 56,  0, 57,  0, 58,  0, 15,  0 },
			{  0,  0, 16,  0, 39,  0, 40,  0, 41,  0, 42,  0, 43,  0, 44,  0, 17,  0,  0 },
			{  0,  0,  0, 18,  0, 19,  0, 20,  0, 21,  0, 22,  0, 23,  0, 24,  0,  0,  0 }
		}
	};

	/**
	 * The spread of the arrow, in radians
	 */
	private static final double ARROW_SPREAD=Math.PI/2;

	/**
	 * The length of the arrow, in pixels
	 */
	private static final int ARROW_LENGTH=8;

	/**
	 * The background color of the image.
	 */
	private static final Color background=new Color(0xecf8ff);

	/**
	 * The color of the connecting lines.
	 */
	private static final Color connectorColor=new Color(0x6f6f6f);

	/**
	 * The minimum space between tables horizontally.
	 */
	private static final int HORIZONTAL_SPACE=40;

	/**
	 * The minimum space between tables vertically.
	 */
	private static final int VERTICAL_SPACE=40;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		resp.setContentType(ContentType.GIF);
		try (OutputStream out = resp.getOutputStream()) {
			Settings settings = new Settings(getServletContext(), req);

			List<SchemaTable> tables = settings.getJDBCConnector().getDatabaseSchema();
			int len = tables.size();

			// Draw the image

			// Get the font
			Map<TextAttribute,Object> textAttributes = AoCollections.newHashMap(2);
			textAttributes.put(TextAttribute.FAMILY, "Helvetica");
			textAttributes.put(TextAttribute.SIZE, 14f);
			Font font = new Font(textAttributes);
			//FontMetrics FM=toolkit.getFontMetrics(font);

			// Figure out the structure to use
			short[][] structure = null;
			int len2 = structures.length;
			for (int d = 0; d < len2; d++) {
				short[][] tstructure = structures[d];
				// Count the number of slots for tables
				short slots = 0;
				int len3 = tstructure.length;
				for (int e = 0; e < len3; e++) {
					short[] line = tstructure[e];
					int len4 = line.length;
					for (int f = 0; f < len4; f++) {
						if (line[f] != 0) {
							slots++;
						}
					}
				}
				if (slots >= len) {
					structure = tstructure;
				}
			}
			if (structure == null) {
				throw new ServletException("No more than 58 tables are currently supported.");
			}

			// Figure out the number of columns and rows
			int cols = structure[0].length;
			int rows = structure.length;

			BufferedImage sizingImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
			FontMetrics FM;
			Graphics sizingG = sizingImage.getGraphics();
			try {
				FM = sizingG.getFontMetrics(font);
			} finally {
				sizingG.dispose();
			}

			// Figure out the widest and tallest for each row and column
			int[] colWidest = new int[cols];
			int[] rowHighest = new int[rows];
			int currentTable = 0;
			Loop:
			for (int y = 0; y < rows; y++) {
				short[] line = structure[y];
				for (int x = 0; x < cols; x++) {
					if (currentTable >= len) {
						break Loop;
					}
					short priority = line[x];
					if (priority > 0 && priority <= len) {
						SchemaTable table = tables.get(currentTable++);
						int width = table.getWidth(FM);
						if (width > colWidest[x]) {
							colWidest[x] = width;
						}
						int height = table.getHeight(FM);
						if (height > rowHighest[y]) {
							rowHighest[y] = height;
						}
					}
				}
			}

			// Determine the total dimensions for the image
			int imageWidth = 0;
			for (int x = 0; x < cols; x++) {
				if (x > 0) {
					imageWidth += HORIZONTAL_SPACE;
				}
				imageWidth += colWidest[x];
			}
			int imageHeight = 0;
			for (int y = 0; y < rows; y++) {
				if (y > 0) {
					imageHeight += VERTICAL_SPACE;
				}
				imageHeight += rowHighest[y];
			}

			// Make the image
			BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
			Graphics2D G = image.createGraphics();

			// Set the antialiasing
			RenderingHints hints = new RenderingHints(
				RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			G.addRenderingHints(hints);

			// Fill the background
			G.setColor(background);
			G.fillRect(0, 0, imageWidth, imageHeight);

			// Determine the top left corner of each table
			int[] xs = new int[len];
			int[] ys = new int[len];
			currentTable = 0;
			int ypos = 0;
			Loop2:
			for (int y = 0; y < rows; y++) {
				if (y > 0) {
					ypos += VERTICAL_SPACE;
				}
				short[] line = structure[y];
				int xpos = 0;
				for (int x = 0; x < cols; x++) {
					if (currentTable >= len) {
						break Loop2;
					}
					if (x > 0) {
						xpos += HORIZONTAL_SPACE;
					}
					short priority = line[x];
					if (priority > 0 && priority <= len) {
						SchemaTable table = tables.get(currentTable);
						// Center in available space
						int width = table.getWidth(FM);
						xs[currentTable] = xpos + (colWidest[x] - width) / 2;
						int height = table.getHeight(FM);
						ys[currentTable] = ypos + (rowHighest[y] - height) / 2;
						currentTable++;
					}
					xpos += colWidest[x];
				}
				ypos += rowHighest[y];
			}

			// Draw the connections between all tables
			G.setColor(connectorColor);
			List<Point> points = new ArrayList<>();
			for (int c = 0; c < len; c++) {
				SchemaTable table = tables.get(c);
				List<SchemaRow> urows = table.getRows();
				len2 = urows.size();
				for (int d = 0; d < len2; d++) {
					SchemaRow row = urows.get(d);
					List<SchemaForeignKey> keys = row.getForeignKeys();
					int len3 = keys.size();
					for (int e = 0; e < len3; e++) {
						SchemaForeignKey key = keys.get(e);

						// Find the other table
						SchemaTable foreignTable = null;
						int foreignIndex = -1;
						for (int f = 0; f < len; f++) {
							SchemaTable temp = tables.get(f);
							if (temp.getName().equals(key.getForeignTableName())) {
								foreignTable = temp;
								foreignIndex = f;
								break;
							}
						}
						if (foreignTable == null) {
							throw new AssertionError("Unable to find table: " + key.getForeignTableName());
						}

						// Get the row link y position for both tables
						int linky1 = ys[c] + table.getRowLinkY(row.getName(), FM);
						int linky2 = ys[foreignIndex] + foreignTable.getRowLinkY(key.getForeignRowName(), FM);

						// Figure out which x coordinates to use
						int link1x1 = xs[c];
						int width1 = table.getWidth(FM);
						int link1x2 = link1x1 + width1;
						int link2x1 = xs[foreignIndex];
						int width2 = foreignTable.getWidth(FM);
						int link2x2 = link2x1 + width2;
						int x1, x2;
						if ((link1x1 <= link2x1 && link1x2 >= link2x2) || (link1x1 >= link2x1 && link1x2 <= link2x2)) {
							if (link1x2 == link2x2 || link1x1 != link2x1) {
								x1 = link1x1 - 2;
								x2 = link2x1 - 2;
							} else {
								x1 = link1x2;
								x2 = link2x2;
							}
						} else if (link2x2 < link1x1) {
							x2 = link2x2;
							x1 = link1x1 - 2;
						} else if (link1x2 < link2x1) {
							x1 = link1x2;
							x2 = link2x1 - 2;
						} else if (link2x1 < link1x1) {
							x2 = link2x1 - 2;
							x1 = link1x1 - 2;
						} else {
							x1 = link1x2;
							x2 = link2x2;
						}

						// Draw the link
						G.drawLine(x1, linky1, x2, linky2);
						G.drawLine(x1 + 1, linky1, x2 + 1, linky2);
						G.drawLine(x1, linky1 + 1, x2, linky2 + 1);
						G.drawLine(x1 + 1, linky1 + 1, x2 + 1, linky2 + 1);

						// Draw the arrow at point 2
						int y1 = linky1;
						int y2 = linky2;
						double angle;
						if (x1 < x2) {
							angle = Math.atan(((double) (y2 - y1)) / ((double) (x2 - x1)));
						} else {
							angle = Math.PI + Math.atan(((double) (y2 - y1)) / ((double) (x2 - x1)));
						}
						double angle1 = angle - ARROW_SPREAD / 2;
						double angle2 = angle + ARROW_SPREAD / 2;
						int ax1 = (int) Math.round(x1 + ARROW_LENGTH * Math.cos(angle1));
						int ay1 = (int) Math.round(y1 + ARROW_LENGTH * Math.sin(angle1));
						int ax2 = (int) Math.round(x1 + ARROW_LENGTH * Math.cos(angle2));
						int ay2 = (int) Math.round(y1 + ARROW_LENGTH * Math.sin(angle2));
						G.drawLine(x1, linky1, ax1, ay1);
						G.drawLine(x1 + 1, linky1, ax1 + 1, ay1);
						G.drawLine(x1, linky1 + 1, ax1, ay1 + 1);
						G.drawLine(x1 + 1, linky1 + 1, ax1 + 1, ay1 + 1);
						G.drawLine(x1, linky1, ax2, ay2);
						G.drawLine(x1 + 1, linky1, ax2 + 1, ay2);
						G.drawLine(x1, linky1 + 1, ax2, ay2 + 1);
						G.drawLine(x1 + 1, linky1 + 1, ax2 + 1, ay2 + 1);

						//points.addElement(new Point(x1,linky1));
						points.add(new Point(x2, linky2));
					}
				}
			}

			// Draw each table
			G.setFont(font);
			for (int c = 0; c < len; c++) {
				SchemaTable table = tables.get(c);
				table.draw(G, FM, xs[c], ys[c]);
			}

			// Draw all of the connecting points
			G.setColor(connectorColor);
			len = points.size();
			for (int c = 0; c < len; c++) {
				Point P = points.get(c);
				G.fillOval(P.x - 2, P.y - 2, 6, 6);
			}

			// Compress the GIF file
			GifEncoder encoder = new GifEncoder(image, out, true);
			encoder.encode();
		} catch (Exception err) {
			// TODO: servlet.log instead of printStackTrace, or just throw in ServletException
			err.printStackTrace();
		}
	}
}
