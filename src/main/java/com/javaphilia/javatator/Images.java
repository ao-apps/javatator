/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Convenient servlet to serve the images.
 */
public class Images extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final String RESOURCE_DIR="images/";

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		String file=req.getParameter("file");
		String lfile=file.toLowerCase();
		if(lfile.endsWith(".gif")) resp.setContentType("image/gif");
		else if(lfile.endsWith(".jpg") || lfile.endsWith(".jpeg")) resp.setContentType("image/jpeg");
		else throw new ServletException("Invalid file type: "+file);
		// TODO: Verify this against a specific set of allowed files, or just serve directly not through servlet
		InputStream in=new BufferedInputStream(getClass().getResourceAsStream(RESOURCE_DIR+file));
		OutputStream out=new BufferedOutputStream(resp.getOutputStream());
		int b;
		while((b=in.read())>=0) out.write(b);
		in.close();
		out.close();
	}

	@Override
	protected long getLastModified(HttpServletRequest req) {
		return Main.UPTIME;
	}
}
