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

import com.aoapps.html.servlet.DocumentEE;
import com.aoapps.lang.io.ContentType;
import com.aoapps.web.resources.renderer.Renderer;
import com.aoapps.web.resources.servlet.RegistryEE;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Javatator database admin tool - main page.
 */
@WebServlet("")
public class Main extends HttpServlet {

  private static final long serialVersionUID = 1L;

  /*
   * The version of this release.
   * TODO: Move to changelog.jspx in book
   * <p>
   * 0.5.0 - Added SSL support and quotes for PostgreSQL case-sensitivity.
   * </p>
   */

  /**
   * The time the class was loaded.
   */
  public static final long UPTIME=System.currentTimeMillis()+5000; // TODO: Why plus 5 seconds?

  /**
   * Handles the GET request.
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
    resp.setContentType(ContentType.HTML);
    resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
    try (JavatatorWriter out = new JavatatorWriter(resp.getOutputStream())) {
      Settings settings = new Settings(getServletContext(), req);
      String action = settings.getAction();
      String frame = req.getParameter("frame");

      if ("right".equals(frame)) {
        if (req.getParameter("blank") != null) {
          // TODO: Make a blank.html or blank.jsp?
          out.print("<html></html>");
        } else {
          printRightFrame(resp, out, settings, action);
        }
      } else if ("left".equals(frame)) {
        printLeftFrame(out, settings, action);
      } else if ("top".equals(frame)) {
        printTopFrame(resp, out, settings, action);
      } else {
        printFrames(out, settings, action);
      }
    }
  }

  /**
   * Handles the POST request.
   */
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
    doGet(req, resp);
  }

  /**
   * Prints the frameset.
   */
  // TODO: Move to /frameset.inc.jsp, using HideJspFilter, too.  Add JSP and JSTL dependencies
  private void printFrames(JavatatorWriter out, Settings settings, String action) throws IOException {
    // TODO: Doctype.set(req, Doctype.strict);
    out.print("<html>\n"
      + "  <head>\n"
      + "    <title>Javatator Database Admin</title>\n"
      + "  </head>\n"
      + "  <frameset rows='110,*' border=1>\n"
      + "    <frame src='");
    // TODO: response encodeURL
    out.print(settings.getRequest().getContextPath());
    out.print("/?frame=top' name='top_frame' frameborder=1 marginheight=0 marginwidth=0>\n"
      + "    <frameset cols='200,*' border=1>\n"
      + "      <frame src='");
    // TODO: response encodeURL
    out.print(settings.getRequest().getContextPath());
    out.print("/?frame=left' name='left_frame' frameborder=1>\n"
      + "      <frame src='");
    // TODO: response encodeURL
    out.print(settings.getRequest().getContextPath());
    out.print("/?frame=right&blank=yes' name='right_frame' frameborder=1>\n"
      + "    </frameset>\n"
      + "  </frameset>\n"
      + "</html>\n");
  }

  /**
   * Prints the contents of the left-hand frame.
   */
  private void printLeftFrame(JavatatorWriter out, Settings settings, String action) throws IOException {
    boolean isConnected =
      settings.getDatabaseProduct() != null
      && settings.getHostname() != null
      && settings.getPort()>0
      && settings.getUsername() != null
      && settings.getDatabase() != null;
    if (isConnected) {
      try {
        JDBCConnector conn=settings.getJDBCConnector();
        // TODO: This has no <html> tag?
        out.print("<script language=javascript><!--\n"
          + "var t=top.top_frame;\n"
          + "var db=new Array();\n"
          + "var tb=new Array();\n");
        try {
          List<String> databases=conn.getDatabases();
          int size=databases.size();
          for (int i=0;i<size;i++) {
            out.print("db[");
            out.print(i);
            out.print("]='");
            out.print(databases.get(i));
            out.print("';\n");
          }
          out.print("t.setParentDB(");
          out.print(databases.indexOf(settings.getDatabase()));
          out.print(");\n");
          List<String> tables=conn.getTables();
          int tbSize=tables.size();
          for (int c=0;c<tbSize;c++) {
            String table=tables.get(c);
            out.print("tb[");
            out.print(c);
            out.print("]='");
            out.print(table);
            out.print("';\n");
          }
          out.print("t.setDatabases(db);\n"
            + "t.setTables(tb);\n"
            + "t.drawMenu(document);\n");
          if ("db_details".equals(action)) {
          out.print("var f=t.window.document.theform;\n"
            + "f.frame.value='right';\n"
            + "f.target='right_frame';\n"
            + "f.action.value='db_details';\n"
            + "f.submit();\n");}
        } finally {
          out.print("//--></script>\n");
        }
      } catch (Exception err) {
        err.printStackTrace();
      }
    } else {
      // TODO: Move to /left.inc.jsp
      out.print("<html>\n"
        + "<head>\n"
        + "    <script language=javascript src='");
      // TODO: response encodeURL
      out.print(settings.getRequest().getContextPath());
      out.print("/javatator.js'></script>\n"
        + "</head>\n"
        + "<body>\n"
        + "<script language=javascript>\n"
        + "<!--\n"
        + "drawAdminMenu(document);\n"
        + "//-->"
        + "</script>"
        + "</body>\n"
        + "</html>\n");
    }
  }

  /**
   * Prints the login forms.
   */
  private static void printLoginForm(JavatatorWriter out, Settings settings) throws IOException {
    out.print("<br>\n");
    out.print("<span class='HEADER'>Welcome to Javatator ");
    out.print(Maven.properties.get("project.version"));
    out.print("</span><br><br>\n");
    if (settings.getHostname() != null) {
      out.print("<script language=javascript><!--\n"
        + "top.left_frame.location.href='");
      // TODO: response encodeURL
      out.print(settings.getRequest().getContextPath());
      out.print("/?frame=left';\n"
        + "//--></script>\n");
    }

    DatabaseConfiguration databaseConfiguration = settings.getDatabaseConfiguration();
    // For each product, display all the values, if not in config allow user to edit
    List<String> dbProducts = databaseConfiguration.getAvailableDatabaseProducts();
    int size=dbProducts.size();

    out.print("<table width='100%' border=0 cellspacing=0 cellpadding=0>\n");
    out.startTR();
    try {
      for (int c=0;c<size;c++) {
        String dbProduct=dbProducts.get(c);
        out.print("  <td align=center class=ALTBG>");
        out.print("<form method=post action='");
        // TODO: response encodeURL
        out.print(settings.getRequest().getContextPath());
        out.print("/' target='top_frame'>\n"
          + "<input type=hidden name=frame value=top>");
        out.startTable(null);

        out.startTR();
        out.startTD("colspan=3 align=center");
        out.print("<b>");
        out.print(databaseConfiguration.getProperty("name", dbProduct));
        out.print("</b>");
        out.endTD();
        out.endTR();

        // Hostname
        out.startTR();
        out.printTD("Hostname:");
        out.printTD("&nbsp;", "rowspan=6 width=12 nowrap");
        out.startTD();

        String settingsHostname=settings.getParameter(dbProduct+"_hostname");
        if (settingsHostname == null) {
          settingsHostname=settings.getHostname();
        }
        if (settingsHostname == null) {
          settingsHostname="";
        }

        List<String> configHostnames = databaseConfiguration.getAllowedHosts(dbProduct);
        int configHostnamesLen=configHostnames.size();
        if (configHostnamesLen>1) {
          out.print("<select name='hostname'>\n");
          for (int d=0;d<configHostnamesLen;d++) {
            String hostname=configHostnames.get(d);
            out.print("  <option value='");
            Util.printEscapedJavaScript(out, hostname);
            out.print('\'');
            if (hostname.equals(settingsHostname)) {
              out.print(" selected");
            }
            out.print(">");
            out.print(hostname);
          }
          out.print("</select>");
        } else if (configHostnamesLen == 1) {
          out.print(configHostnames.get(0));
        } else {
          out.print("<input type=text name='hostname' size=16 value='");
          Util.printEscapedJavaScript(out, settingsHostname);
          out.print("'>");
        }
        out.endTD();
        out.endTR();

        // Port
        out.startTR();
        out.printTD("Port:");
        out.startTD();
        String configPort = databaseConfiguration.getProperty("port", dbProduct);
        if (configPort != null && configPort.length()>0) {
          out.print(configPort);
        } else {
          out.print("<input type=text name=port size=5 value='");
          String s = settings.getParameter(dbProduct+"_port");
          if (s != null) {
            Util.printEscapedJavaScript(out, s);
          } else {
            int port=-1;
            // Use settings if on settings product
            if (dbProduct.equals(settings.getDatabaseProduct())) {
              port=settings.getPort();
            }
            if (port <= 0) {
              // Try to get from config
              String portS = databaseConfiguration.getProperty("defaultport", dbProduct);
              if (portS != null && portS.length()>0) {
                port=Integer.parseInt(portS);
              }
            }
            if (port>0) {
              out.print(port);
            }
          }
          out.print("'>");
        }
        out.endTD();
        out.endTR();

        // SSL
        out.startTR();
        out.printTD("SSL:");
        out.startTD();
        Boolean ssl = databaseConfiguration.getBooleanProperty("ssl", dbProduct);
        if (ssl != null) {
          out.print(ssl ? "Enabled" : "Disabled");
        } else {
          out.print("<input type=checkbox name=ssl value='true'");

          ssl = settings.getBooleanParameter(dbProduct+"_ssl");
          if (ssl != null) {
            if (ssl) {
              out.print(" checked");
            }
          } else {
            // Use settings if on settings product
            if (dbProduct.equals(settings.getDatabaseProduct())) {
              ssl = settings.getSsl();
            }
            if (ssl == null) {
              // Try to get from config
              ssl = databaseConfiguration.getBooleanProperty("defaultssl", dbProduct);
            }
            if (ssl != null && ssl) {
              out.print(" checked");
            }
          }
          out.print(">");
        }
        out.endTD();
        out.endTR();

        // Username
        out.startTR();
        out.printTD("Username:");
        out.startTD();
        String configUsername = databaseConfiguration.getProperty("username", dbProduct);
        if (configUsername != null && configUsername.length()>0) {
          out.print(configUsername);
        } else {
          out.print("<input type=text name=username size=16 value='");
          String username=settings.getParameter(dbProduct+"_username");
          if (username == null) {
            username=settings.getUsername();
          }
          if (username == null) {
            username="";
          }
          Util.printEscapedJavaScript(out, username);
          out.print("'>");
        }
        out.endTD();
        out.endTR();

        // Password
        out.startTR();
        out.printTD("Password:");
        out.startTD();
        String configPassword = databaseConfiguration.getProperty("password", dbProduct);
        if (configPassword != null && configPassword.length()>0) {
          out.print("XXXXXXXXXXXX");
        } else {
          out.print("<input type=password name=password size=16 value='");
          String password=settings.getParameter(dbProduct+"_password");
          if (password == null) {
            password=settings.getPassword();
          }
          if (password == null) {
            password="";
          }
          Util.printEscapedJavaScript(out, password);
          out.print("'>");
        }
        out.endTD();
        out.endTR();

        // Database
        out.startTR();
        out.printTD("Database:");
        out.startTD();
        String configDatabase = databaseConfiguration.getProperty("database", dbProduct);
        if (configDatabase != null && configDatabase.length()>0) {
          out.print(configDatabase);
        } else {
          out.print("<input type=text name=database size=16 value='");
          String database=settings.getParameter(dbProduct+"_database");
          if (database == null) {
            database=settings.getDatabase();
          }
          if (database == null) {
            database="";
          }
          Util.printEscapedJavaScript(out, database);
          out.print("'>");
        }
        out.endTD();
        out.endTR();

        out.startTR();
        out.printTD("&nbsp;", "colspan=3");
        out.endTR();

        out.startTR();
        out.startTD("align='center' colspan=3");
        out.print("<input type=hidden name=action value='db_details'>"
          + "<input type=hidden name=dbproduct value='");
        out.print(dbProduct);
        out.print("'>"
          + "<input type=submit value=' Login '>");
        out.endTD();
        out.endTR();

        out.endTable();
        settings.printGlobalForm(out);
        out.print("</form>");
        out.print("</td>\n");
      }
    } finally {
      out.endTR();
      out.print("</table>\n");
    }
  }

  /**
   * Prints the contents of the right-hand frame.
   */
  private void printRightFrame(HttpServletResponse response, JavatatorWriter out, Settings settings, String action) throws IOException {
    ServletContext servletContext = getServletContext();
    HttpServletRequest request = settings.getRequest();
    boolean isConnected = action != null && settings.getDatabaseProduct() != null && settings.getHostname() != null && settings.getPort() > 0 && settings.getUsername() != null && settings.getDatabase() != null;
    // TODO: Forward to a set of appropriate JSP views
    out.print("<html>\n"
      + "  <head>\n"
      + "    <script language=javascript src='");
    // TODO: response encodeURL
    out.print(request.getContextPath());
    out.print("/javatator.js'></script>\n"
      + "    ");
    Renderer.get(servletContext).renderStyles(
      request,
      response,
      new DocumentEE(servletContext, request, response, out),
      true,
      Collections.singletonMap(JavatatorStyles.RESOURCE_GROUP, true),
      RegistryEE.Request.get(servletContext, request),
      RegistryEE.Session.get(request.getSession(false)),
      RegistryEE.Page.get(request)
    );
    out.print("\n"
      + "  </head>\n"
      + "<body class='ALTBODY'>\n");
    if (isConnected) {
      out.print("<form method=post action='");
      // TODO: response encodeURL
      out.print(request.getContextPath());
      out.print("/' name=theform target='left_frame'>\n"
        + "<input type=hidden name=frame value=left>");
    }
    try {
      if ("show_info".equals(action)) {
        Info.printDatabaseInfo(out, settings);
      } else if ("show_options".equals(action)) {
        showOptions(out, settings);
      } else if (settings.getDatabaseProduct() != null && settings.getHostname() != null && settings.getPort() > 0 && settings.getUsername() != null && settings.getDatabase() != null) {
        settings = new Database(settings).processRequest(out);
      } else {
        printLoginForm(out, settings);
      }
    } catch (Exception e) {
      out.print("<br><span class='ERROR'>Error: ");
      out.print(e.toString());
      out.print("</span>\n");
      e.printStackTrace();
    } finally {
      if (isConnected) {
        settings.printForm(out);
        out.print("</form>\n");
      }
      out.endBody();
      out.print("</html>\n");
    }
  }

  /**
   * Prints the contents of the top frame.
   */
  private void printTopFrame(HttpServletResponse response, JavatatorWriter out, Settings settings, String action) throws IOException {
    ServletContext servletContext = getServletContext();
    HttpServletRequest request = settings.getRequest();
    boolean isConnected =
      settings.getDatabaseProduct() != null
      && settings.getHostname() != null
      && settings.getPort()>0
      && settings.getUsername() != null
      && settings.getDatabase() != null;
    // TODO: Forward to a set of appropriate JSP views
    out.print("<html>\n"
      + "<head>"
      + "    <script language=javascript src='");
    // TODO: response encodeURL
    out.print(request.getContextPath());
    out.print("/javatator.js'></script>\n"
      + "    ");
    Renderer.get(servletContext).renderStyles(
      request,
      response,
      new DocumentEE(servletContext, request, response, out),
      true,
      Collections.singletonMap(JavatatorStyles.RESOURCE_GROUP, true),
      RegistryEE.Request.get(servletContext, request),
      RegistryEE.Session.get(request.getSession(false)),
      RegistryEE.Page.get(request)
    );
    out.print('\n');
    if (isConnected) {
      try {
        JDBCConnector conn=settings.getJDBCConnector();
        out.print("<script language=javascript><!--\n"
          + "var databases=new Array();\n"
          + "var tables=new Array();\n"
          + "var parentDB=-1;\n");
        try {
          List<String> databases=conn.getDatabases();
          int dbIndex=databases.indexOf(settings.getDatabase());
          if (dbIndex >= 0) {
            int size=databases.size();
            for (int i=0;i<size;i++) {
              out.print("databases[");
              out.print(i);
              out.print("]='");
              out.print(databases.get(i));
              out.print("';\n");
            }
          } else {
            out.print("databases[0]='");
            out.print(settings.getDatabase());
            out.print("';\n");
          }
          out.print("parentDB=");
          out.print((dbIndex >= 0)?dbIndex:0);
          out.print(";\n");
          List<String> tables=conn.getTables();
          int tbSize=tables.size();
          for (int c=0;c<tbSize;c++) {
            String table=tables.get(c);
            out.print("tables[");
            out.print(c);
            out.print("]='");
            out.print(table);
            out.print("';\n");
          }
        } finally {
          out.print("//--></script>\n");
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    out.print("</head>\n"
      + "<body onLoad='");
    if (isConnected) {
      out.print("drawMenu(top.left_frame.window.document);");
    }
    if (action == null || "db_details".equals(action)) {
      out.print("document.theform.submit();");
    }
    out.print("'>\n"
      + "<form method=post name=theform target='right_frame' action='");
    // TODO: response encodeURL
    out.print(request.getContextPath());
    out.print("/'>\n"
      + "<input type=hidden name=frame value=right>\n");
    settings.printForm(out);
    out.print("</form>\n"
      + "<a href='");
    // TODO: response encodeURL
    out.print(request.getContextPath());
    out.print("/' target='_top'><img src='");
    // TODO: response encodeURL
    out.print(request.getContextPath());
    out.print("/images/2.gif' alt='Javatator Admin' border=0 align=left width=345 height=72></a>\n");

    out.startTable(null);
    out.startTR();
    out.startTD();
    out.print("<b>Javatator version ");
    out.print(Maven.properties.get("project.version"));
    out.print("</b> running on ");
    out.print(System.getProperty("os.name"));
    out.print(" (");
    out.print(System.getProperty("os.arch"));
    out.print(")<br>\n");

    if (settings.getError() != null) {
      out.print("<br>\n"
        +"<span class='ERROR'>");
      out.print(settings.getError());
      out.print("</span><br>\n");
    }

    if (isConnected) {
      try {
        JDBCConnector conn=settings.getJDBCConnector();

        out.print("<b>");
        out.print(conn.getDatabaseProductName());
        out.print("</b>");
        out.print(" running on ");
        out.print(conn.getURL());
        out.print(" <a href=\"javascript:showInfo()\">"
          + "More&nbsp;Info</a>"
          + "&nbsp;|&nbsp;"
          + "<a href=\"javascript:changeProduct()\">"
          + "Change&nbsp;Product</a><br>\n"
          + "<b>Driver: ");
        out.print(conn.getDriverName());
        out.print("</b> <a href=\"javascript:showOptions()\">Advanced Options</a>");
      } catch (Exception e) {
        out.print("<br><span class='ERROR'>Error: ");
        // TODO: Encode
        out.print(e.toString());
        out.print("</span>\n");
        e.printStackTrace();
      }
    }
    out.endTD();
    out.endTR();
    out.endTable();
    out.endBody();
    out.print("</html>\n");
  }

  private void showOptions(JavatatorWriter out, Settings settings) {
    out.print("<h2>Advanced Options</h2>");
    out.startTable(null);
    out.startTR();
    out.printTD("Maximum foreign key rows:");
    out.startTD();
    out.print("<input type=text name=newfkeyrows value='");
    out.print(settings.getForeignKeyRows());
    out.print("'>\n");
    out.endTD();
    out.endTR();
    out.startTR();
    out.startTD("colspan=2");
    out.print("<input type=checkbox name=newusemultiline value=true");
    if (settings.useMultiLine()) {
      out.print(" checked");
    }
    out.print("> Use multiline textareas as default for text data types.\n");
    out.endTD();
    out.endTR();
    out.endTable();
    out.print("<br><input type=submit value='Update Settings' "
      + "onClick=\"updateSettings(this.form)\">");
  }
}
