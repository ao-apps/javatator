/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2019, 2020  AO Industries, Inc.
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

import com.aoindustries.util.WrappedException;
import java.io.File;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * The configuration for the current application
 */
@WebListener
public class DatabaseConfigurationContext implements ServletContextListener {

	private static final String CONFIGURATION_ATTRIBUTE = DatabaseConfiguration.class.getName();

	/**
	 * Gets the database configuration for the current application.
	 */
	public static DatabaseConfiguration getConfiguration(ServletContext servletContext) {
		DatabaseConfiguration configuration = (DatabaseConfiguration)servletContext.getAttribute(CONFIGURATION_ATTRIBUTE);
		if(configuration == null) throw new IllegalStateException("Configuration not found in application attribute " + CONFIGURATION_ATTRIBUTE + ", context-listener added to web.xml?");
		return configuration;
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			ServletContext servletContext = sce.getServletContext();
			DatabaseConfiguration configuration;
			String filename = servletContext.getInitParameter(CONFIGURATION_ATTRIBUTE);
			servletContext.log(DatabaseConfigurationContext.class.getName() + ": " + CONFIGURATION_ATTRIBUTE + '=' + filename);
			if(filename != null && !filename.isEmpty()) {
				configuration = new DatabaseConfiguration(new File(filename));
			} else {
				configuration = new DatabaseConfiguration();
			}
			servletContext.setAttribute(CONFIGURATION_ATTRIBUTE, configuration);
		} catch(IOException e) {
			throw new WrappedException(e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// Nothing to do
	}
}
