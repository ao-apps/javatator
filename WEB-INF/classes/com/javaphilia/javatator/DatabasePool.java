package com.javaphilia.javatator;

/**
 * Javatator - multi-database admin tool
 * 
 * Copyright (C) 2001  Jason Davies.
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
 * 
 * If you want to help or want to report any bugs, please email me:
 * jason@javaphilia.com
 * 
 */

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Database connection pool.
 *
 * @author Jason Davies
 */
public class DatabasePool {

	/**
	 * The maximum idle time for a connection.
	 */
	public static final long MAX_IDLE_TIME=300000;

	/**
	 * The delay between connection polls.
	 */
	public static final long CLEANUP_POLL_DELAY=180000;

	/**
	 * Stores all the pools that have been created.  Pools are never deleted because that
	 * would remove their statistics from the overall results.  Idle connections within
	 * pools are closed by <code>DatabasePoolCleanup</code>.
	 */
	private static final List<DatabasePool> pools=new ArrayList<DatabasePool>();

	/**
	 * The database product this pool is for.
	 */
	private final String databaseProduct;

	/**
	 * The hostname this pool is for.
	 */
	private final String hostname;

	/**
	 * The port this pool is for.
	 */
	private final int port;

	/**
	 * The username this pool is for.
	 */
	private final String username;

	/**
	 * The password this pool is for.
	 */
	private final String password;

	/**
	 * The database this pool is for.
	 */
	private final String database;

	/**
	 * The URL this pool is using.
	 */
	private final String url;

	/**
	 * The number of connections is taken from the properties file at construction time.
	 */
	private final int numConnections;

	/**
	 * Instances of <code>Connection</code> to reuse i.e. the <code>Connection</code>
	 * pool.
	 *
	 * @see  #getConnection
	 */
	private final Connection[] connections;

	/**
	 * Flags used to keep track of which connections are busy
	 */
	private final boolean[] busyConnections;

	/**
	 * Total time using each connection
	 */
	private final long[] totalTimes;

	/**
	 * The time getting each DB connection from the pool
	 */
	private final long[] startTimes;

	/**
	 * The time returning each DB connection to the pool
	 */
	private final long[] releaseTimes;

	/**
	 * Counts the number of times each connection is connected
	 */
	private final long[] connectCount;

	/**
	 * Counts the number of times each connection is used
	 */
	private final long[] connectionUses;

	private final Object connectLock = new Object();

	// Only load the driver the first time
	private boolean driverLoaded = false;

	static {
		DatabasePoolCleanup.startThread();
	}

	/**
	 * The constructor is used internally only.
	 */
	private DatabasePool(
		String databaseProduct,
		String hostname,
		int port,
		String username,
		String password,
		String database,
		String url
	) throws IOException {
		this.databaseProduct = databaseProduct;
		this.hostname = hostname;
		this.port = port;
		this.username = username;
		this.password = password;
		this.database = database;
		this.url = url;
		numConnections = Integer.parseInt(DatabaseConfiguration.getProperty("connections", databaseProduct));
		connections = new Connection[numConnections];
		busyConnections = new boolean[numConnections];
		totalTimes = new long[numConnections];
		startTimes = new long[numConnections];
		releaseTimes = new long[numConnections];
		connectCount = new long[numConnections];
		connectionUses = new long[numConnections];
	}

	public static void cleanup() throws SQLException {
		synchronized(pools) {
			int size = pools.size();
			for(int c = 0; c < size; c++) {
				pools.get(c).cleanup0();
			}
		}
	}

	private void cleanup0() throws SQLException {
		long time = System.currentTimeMillis();
		int size = connections.length;
		synchronized(connectLock) {
			for(int c = 0; c < size; c++) {
				if(
				   connections[c] != null &&
				   !busyConnections[c] &&
				   (time - releaseTimes[c]) >= MAX_IDLE_TIME
				) {
					connections[c].close();
					connections[c]=null;
				}
			}
		}
	}

	public static void closeDatabase(Settings settings) throws SQLException {
		synchronized(pools) {
			int size = pools.size();
			for(int c = 0; c < size; c++) {
				DatabasePool temp = pools.get(c);
				if(
				   temp.database.equals(settings.getDatabase())
				   && temp.hostname.equals(settings.getHostname())
				   && temp.port == settings.getPort()
				   && temp.databaseProduct.equals(settings.getDatabaseProduct())
				) {
					while(!temp.closeDatabase0()) {
						// Try until closed
					}
					//break;
				}
			}
		}
	}

	private boolean closeDatabase0() throws SQLException {
		long time = System.currentTimeMillis();
		boolean isSuccess = true;
		int size = connections.length;
		synchronized(connectLock) {
			for(int c = 0; c < size; c++) {
				if(connections[c] != null) {
					if(
					   !busyConnections[c] ||
					   (busyConnections[c] && (time - releaseTimes[c]) >= MAX_IDLE_TIME)
					) {
						connections[c].close();
						connections[c] = null;
						busyConnections[c] = false;
						System.out.println("a connection was killed successfully");
					} else {
						isSuccess = false;
					}
				}
			}
		}
		return isSuccess;
	}

	/**
	 * Locates or creates the proper <code>DatabasePool</code> for a <code>Settings</code> and
	 * retrieves a <code>Connection</code> from it.
	 */
	public static Connection getConnection(Settings settings) throws SQLException, IOException {
		String databaseProduct = settings.getDatabaseProduct();
		if(databaseProduct == null || (databaseProduct = databaseProduct.trim()).length() == 0) throw new SQLException("databaseProduct not set");
		String hostname = settings.getHostname();
		if(hostname == null || (hostname = hostname.trim()).length() == 0) throw new SQLException("hostname not set");
		int port = settings.getPort();
		if(port < 1 || port > 65535) throw new SQLException("Invalid port: " + port);
		String username = settings.getUsername();
		if(username == null || (username = username.trim()).length() == 0) throw new SQLException("username not set");
		String password = settings.getPassword();
		if(password == null) password = "";
		String database = settings.getDatabase();
		if(database == null || (database = database.trim()).length() == 0) throw new SQLException("database not set");

		// Look for an existing pool
		DatabasePool pool = null;
		synchronized(pools) {
			int size = pools.size();
			for(int c = 0; c < size; c++) {
				DatabasePool temp = pools.get(c);
				if(
				   databaseProduct.equals(temp.databaseProduct)
				   && hostname.equals(temp.hostname)
				   && port == temp.port
				   && username.equals(temp.username)
				   && password.equals(temp.password)
				   && database.equals(temp.database)
				) {
					pool = temp;
					break;
				}
			}

			// Create if not found
			if(pool == null) {
				pool = new DatabasePool(
					databaseProduct,
					hostname,
					port,
					username,
					password,
					database,
					settings.getURL()
				);
				pools.add(pool);
			}
		}

		// Get an available connection from the pool
		return pool.getConnection0();
	}

	/**
	 * Gets a connection to the database.  Multiple <code>Connection</code>s to the database
	 * may exist at any moment. It checks the <code>Connection</code> pool for a not busy
	 * <code>Connection</code> sequentially. If found, it returns that <code>Connection</code>
	 * object, otherwise creates a new <code>connection</code>, adds it to the pool and also
	 * returns the <code>Connection</code> object.  If all the connections in the pool are
	 * busy, it waits till a connection becomes available.
	 */
	private Connection getConnection0() throws SQLException, IOException {
		synchronized(connectLock) {
			while(true) {
				for(int c = 0; c < numConnections; c++) {
					if(!busyConnections[c]) {
						startTimes[c] = System.currentTimeMillis();
						Connection conn = connections[c];
						if(conn == null || conn.isClosed()) {
							if(!driverLoaded) {
								try {
									Class.forName(DatabaseConfiguration.getProperty("driver", databaseProduct)).newInstance();
									driverLoaded = true;
								} catch(ClassNotFoundException err) {
									SQLException sqlErr = new SQLException();
									sqlErr.initCause(err);
									throw sqlErr;
								} catch(InstantiationException err) {
									SQLException sqlErr = new SQLException();
									sqlErr.initCause(err);
									throw sqlErr;
								} catch(IllegalAccessException err) {
									SQLException sqlErr = new SQLException();
									sqlErr.initCause(err);
									throw sqlErr;
								}
							}
							conn = connections[c] = DriverManager.getConnection(
								url,
								username,
								password
							);
							connectCount[c]++;
						}
						busyConnections[c] = true;
						releaseTimes[c] = 0;
						connectionUses[c]++;
						return conn;
					}
				}
				try {
					connectLock.wait();
				} catch(InterruptedException err) {
					err.printStackTrace();
				}
			}
		}
	}

	/**
	 * Releases a <code>Connection</code> by calling release connection on
	 * all the pools until the correct pool is found.
	 */
	public static void releaseConnection(Connection conn) {
		synchronized(pools) {
			int size = pools.size();
			for(int c = 0; c < size; c++) {
				if(pools.get(c).releaseConnection0(conn)) break;
			}
		}
	}

	private boolean releaseConnection0(Connection conn) {
		synchronized(connectLock) {
			for(int c = 0; c < numConnections; c++) {
				if(conn == connections[c]) {
					busyConnections[c] = false;
					long time = System.currentTimeMillis();
					releaseTimes[c] = time;
					totalTimes[c] += time - startTimes[c];
					connectLock.notify();
					return true;
				}
			}
		}
		return false;
	}
}
