/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2020  AO Industries, Inc.
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

import com.aoindustries.web.resources.registry.Style;
import com.aoindustries.web.resources.servlet.RegistryEE;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener("Registers the styles in RegistryEE.")
public class JavatatorStyles implements ServletContextListener {

	public static final String GROUP = "javatator";

	public static final Style JAVATATOR = new Style("/javatator.css");

	@Override
	public void contextInitialized(ServletContextEvent event) {
		// Add our CSS files
		RegistryEE.get(event.getServletContext()).getGroup(GROUP).styles.add(JAVATATOR);
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		// Do nothing
	}
}
