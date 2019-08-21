/*
 * javatator - Multi-database admin tool.
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

import com.aoindustries.servlet.http.HttpServletUtil;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Redirects requests from old Main servlet URL to now at /
 */
public class MainRedirect extends HttpServlet {

	private static final long serialVersionUID = 1L;

	/**
	 * Handles the GET request.
	 */
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		HttpServletUtil.sendRedirect(req, resp, "/", HttpServletResponse.SC_MOVED_PERMANENTLY);
	}

	/**
	 * Handles the POST request.
	 */
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		HttpServletUtil.sendRedirect(req, resp, "/", HttpServletResponse.SC_MOVED_PERMANENTLY);
	}

	@Override
	protected long getLastModified(HttpServletRequest req) {
		return Main.UPTIME;
	}
}
