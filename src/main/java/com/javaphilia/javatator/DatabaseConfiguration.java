/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2018, 2019, 2020, 2021, 2022  AO Industries, Inc.
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

import com.aoapps.lang.Strings;
import com.aoapps.servlet.attribute.ScopeEE;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * The configuration for the database is stored in a properties file.
 */
public class DatabaseConfiguration {

  private static final String INIT_PARAM = DatabaseConfiguration.class.getName();

  private static final ScopeEE.Application.Attribute<DatabaseConfiguration> APPLICATION_ATTRIBUTE =
      ScopeEE.APPLICATION.attribute(DatabaseConfiguration.class.getName());

  /**
   * Loads the database configuration during {@linkplain ServletContextListener application start-up}.
   */
  @WebListener("Loads the database configuration during application start-up.")
  public static class Initializer implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
      getInstance(event.getServletContext());
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
      // Do nothing
    }
  }

  /**
   * Gets the database configuration for the current application.
   */
  public static DatabaseConfiguration getInstance(ServletContext servletContext) {
    try {
      return APPLICATION_ATTRIBUTE.context(servletContext).computeIfAbsent(name -> {
        String filename = Strings.trimNullIfEmpty(servletContext.getInitParameter(INIT_PARAM));
        servletContext.log(DatabaseConfiguration.class.getName() + ": " + INIT_PARAM + '=' + filename);
        if (filename != null) {
          return new DatabaseConfiguration(new File(filename));
        } else {
          return new DatabaseConfiguration();
        }
      });
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * The properties are kept here.
   */
  private final Properties props;

  /**
   * Loads the default configuration from the bundled database.properties file.
   */
  private DatabaseConfiguration() throws IOException {
    final String resource = "com/javaphilia/javatator/database.properties";
    InputStream in = DatabaseConfiguration.class.getResourceAsStream("/" + resource);
    if (in == null) {
      // Try ClassLoader for when modules enabled
      ClassLoader classloader = Thread.currentThread().getContextClassLoader();
      in = (classloader != null)
          ? classloader.getResourceAsStream(resource)
          : ClassLoader.getSystemResourceAsStream(resource);
    }
    if (in == null) {
      throw new IOException("database.properties not found as resource");
    }
    try {
      Properties newProps = new Properties();
      newProps.load(in);
      props = newProps;
    } finally {
      in.close();
    }
  }

  /**
   * Loads the default configuration from a provided file.
   */
  private DatabaseConfiguration(File file) throws IOException {
    Properties newProps = new Properties();
    try (InputStream in = new FileInputStream(file)) {
      newProps.load(in);
    }
    props = newProps;
  }

  /**
   * Gets the list of hosts that may be accessed for the specified dbProduct.
   */
  public List<String> getAllowedHosts(String dbProduct) {
    String hostList = getProperty("hostname", dbProduct);
    if (hostList == null) {
      return Collections.emptyList();
    }
    return Strings.splitCommaSpace(hostList);
  }

  /**
   * Gets the list of databases that may be selected.
   */
  public List<String> getAvailableDatabaseProducts() {
    List<String> products = new ArrayList<>();
    String dbproduct = getProperty("dbproduct");
    if (dbproduct != null && dbproduct.length() > 0) {
      products.add(dbproduct);
    } else {
      Enumeration<?> e = props.propertyNames();
      while (e.hasMoreElements()) {
        String tmp = (String) e.nextElement();
        if (tmp.startsWith("db.") && tmp.endsWith(".name")) {
          products.add(tmp.substring(3, tmp.length() - 5));
        }
      }
    }
    return products;
  }

  /**
   * Gets the specified property from the file.  If <code>db.name</code> exists, returns the value.
   */
  public String getProperty(String name) {
    // Look for db.name
    return props.getProperty("db." + name);
  }

  /**
   * Gets the specified property from the properties file, using the database product string.
   * If <code>db.databaseProduct.name</code> is defined in the properties file, then that is returned.
   * If <code>db.*.name</code> is defined in the properties file, then that is returned.
   *
   * @param name the name of the property to get.
   * @param databaseProduct the name of the database product being used.
   */
  public String getProperty(String name, String databaseProduct) {
    // Look for db.dbproduct.name
    String s = props.getProperty("db." + databaseProduct + '.' + name);
    if (s != null) {
      return s;
    }

    // Look for db.*.name
    return props.getProperty("db.*." + name);
  }

  public Boolean getBooleanProperty(String name, String databaseProduct) {
    String s = getProperty(name, databaseProduct);
    if (s == null || s.isEmpty()) {
      return null;
    }
    if ("true".equalsIgnoreCase(s)) {
      return true;
    }
    if ("false".equalsIgnoreCase(s)) {
      return false;
    }
    throw new IllegalArgumentException("Unable to parse boolean: " + s);
  }
}
