/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2019, 2020, 2022  AO Industries, Inc.
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

/**
 * Menu functions
 */

function setDatabases(arr) {
  databases=arr;
}

function setTables(arr) {
  tables=arr;
}

function setParentDB(num) {
  parentDB=num;
}

function deleteDatabase(db) {
  len=databases.length;
  tmp=new Array();
  for (var i=0;i<len;i++) {
    if (databases[i]!=db) tmp[tmp.length]=databases[i];
    else if (parentDB==i) parentDB=-1;
  }
  databases=tmp;
}

function deleteTable(tb) {
  len=tables.length;
  tmp=new Array();
  for (var i=0;i<len;i++) {
    if (tables[i]!=tb) tmp[tmp.length]=tables[i];
  }
  tables=tmp;
}

function drawAdminMenu(doc) {
  with(doc) {
    write("<html><head>\n");
    // TODO: RegistryEE
    write("<link rel=stylesheet type='text/css' href='javatator.css'>\n");
    write("</head>\n");
    write("<body class='ALTBODY'><center>\n");
    write("<b>Admin menu</b><br><br><A href='mailto:bugs@javatator.com'>Report Bugs</A>\n");
    write("</center></body></html>");
    close();
  }
}

function drawMenu(doc) {
  with(doc) {
    write("<html><head>\n");
    // TODO: RegistryEE
    write("<link rel=stylesheet type='text/css' href='javatator.css'>\n");
    write("</head>\n");
    write("<body class='ALTBODY'>\n");
    write("<a href='javascript:top.top_frame.goHome();'>Home</a><br>\n");
    if (parentDB>=0) {
      printDBSelect(databases[parentDB]);
      for (var i=0;i<tables.length;i++) {
        printTableSelect(tables[i]);
      }
    }
    for (i=0;i<databases.length;i++) {
      if (i!=parentDB) printDBSelect(databases[i]);
    }
    write("</body></html>");
    close();
  }
}

/**
 * Other stuff
 */

function setConstraint(c) {
  f=document.theform;
  if (f.constraint) f.constraint.value=c;
}

function updateSettings(f) {
  var t=top.top_frame.document.theform;
  t.fkeyrows.value=f.newfkeyrows.value;
  if (f.newusemultiline.checked) t.usemultiline.value="true";
  else t.usemultiline.value="false";
  history.go(-1);
}

function select(table, where) {
  var f=document.theform;
  f.selectcols.value=''
  f.startpos.value=0;
  f.selectwhere.value=where;
  selectTable(table, 'doselect');
}

function showInfo() {
  var f=document.theform;
  f.action.value="show_info";
  f.submit();
}

function showOptions() {
  var f=document.theform;
  f.action.value="show_options";
  f.submit();
}

function reloadMenu() {
  var f=document.theform;
  f.frame.value="top";
  f.action.value="reload";
  f.target="top_frame";
  f.submit();
}

function changeProduct() {
  var f=document.theform;
  f.dbproduct.value="";
  f.table.value="";
  f.column.value="";
  f.action.value="";
  f.frame.value="top";
  f.target="_self";
  f.submit();
}

function goHome() {
  var f=document.theform;
  f.table.value="";
  f.column.value="";
  f.frame.value="right";
  f.target="right_frame";
  f.action.value="";
  f.submit();
}

function setNextAction(n) {
  document.theform.nextaction.value=n;
}

function setPrimaryKeys(n,v) {
  var f=document.theform;
  f.primarykeys.value=n;
  f.values.value=v;
}

function setIndexName(n) {
  document.theform.indexname.value=n;
}

function setColumn(n) {
  document.theform.column.value=n;
}

function setSortColumn(n) {
  var f=document.theform;
  f.sortcolumn.value=n;
  if (f.startpos) f.startpos.value=0;
}

function setSortOrder(n) {
  document.theform.sortorder.value=n;
}

function setStartPos(num) {
  document.theform.startpos.value=num;
}

function setNumRows(num) {
  document.theform.numrows.value=num;
  window.top.top_frame.document.theform.numrows.value=num;
}

function selectAction(action) {
  var f=document.theform;
  f.target="right_frame";
  f.frame.value="right";
  f.action.value=action;
  f.submit();
  return false;
}

function selectTable(table, action) {
  var f=document.theform;
  if (checkDatabaseProduct(f) && checkDatabase(f)) {
    f.sortcolumn.value="";
    f.sortorder.value="";
    f.table.value=table;
    f.frame.value="right";
    f.target="right_frame";
    f.action.value=action;
    f.submit();
  }
}

function selectDatabase(database) {
  var f=document.theform;
  if (checkDatabaseProduct(f)) {
    if (f.database.value!=database) {
      f.database.value=database;
      f.frame.value="left";
      f.target="left_frame";
      f.action.value="db_details";
      f.submit();
    } else {
      f.frame.value="right";
      f.target="right_frame";
      f.action.value="db_details";
      f.submit();
    }
  }
}

function checkDatabaseProduct(form) {
  if (form.dbproduct.value=="") {
    window.alert("No database product selected!");
    return false;
  }
  return true;
}

function checkDatabase(form) {
  if (form.database.value=="") {
    window.alert("No database selected!");
    return false;
  }
  return true;
}

function printDBSelect(db) {
  with(top.left_frame.document) {
    write("&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"javascript:top.top_frame.selectDatabase('");
    // TODO: Encode
    write(db);
    write("');\">");
    // TODO: Encode
    write(db);
    write("</a><br>\n");
  }
}

function printTableSelect(table) {
  with(top.left_frame.document) {
    write("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<a href=\"javascript:top.top_frame.selectTable('");
    // TODO: Encode
    write(table);
    write("','properties')\" class='TABLELINK'>");
    write(table);
    write("</a><br>\n");
  }
}

