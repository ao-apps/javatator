/*
 * javatator - Multi-database admin tool.
 *
 * Copyright (C) 2001  Jason Davies.
 *     If you want to help or want to report any bugs, please email me:
 *     jason@javaphilia.com
 *
 * Copyright (C) 2016, 2018, 2019, 2021, 2022  AO Industries, Inc.
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

/**
 * The DatabasePoolCleanup periodically calls the clean up functions to disconnect
 * any database connection that has been idle too long.
 */
// TODO: Make a servlet context listener and close all on context destroyed
public class DatabasePoolCleanup extends Thread {

  /**
   * A reference to the thread is used to only allow one thread at a time.
   */
  private static DatabasePoolCleanup thread;

  /**
   * A {@link DatabasePoolCleanup} starts itself as a daemon thread upon instantiation
   * and sets its priority to normal.
   */
  private DatabasePoolCleanup() {
    super("Cleanup JDBC Connections");
    setPriority(Thread.NORM_PRIORITY);
    setDaemon(true);
    start();
  }

  /**
   * Periodically polls every connection in the connection pool. If it finds a
   * connection is idle for more than the {@link DatabasePool#MAX_IDLE_TIME},
   * it closes the connection.
   */
  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        sleep(DatabasePool.CLEANUP_POLL_DELAY);
        DatabasePool.cleanup();
      } catch (InterruptedException e) {
        e.printStackTrace();
        // Restore the interrupted status
        Thread.currentThread().interrupt();
      } catch (ThreadDeath td) {
        throw td;
      } catch (Throwable t) {
        t.printStackTrace();
        try {
          sleep(DatabasePool.CLEANUP_POLL_DELAY);
        } catch (InterruptedException err) {
          err.printStackTrace();
          // Restore the interrupted status
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  /**
   * Starts the RefreshConnection thread.  Does nothing if already started.
   */
  public static synchronized void startThread() {
    if (thread == null) {
      thread = new DatabasePoolCleanup();
    }
  }
}
