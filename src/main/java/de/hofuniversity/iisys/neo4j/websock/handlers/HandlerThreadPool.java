/*
 * Copyright (c) 2012-2015 Institute of Information Systems, Hof University
 *
 * This file is part of "Neo4j WebSocket Server".
 *
 * "Neo4j WebSocket Server" is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.hofuniversity.iisys.neo4j.websock.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.hofuniversity.iisys.neo4j.websock.calls.CypherCallEngine;
import de.hofuniversity.iisys.neo4j.websock.neo4j.security.SecurityInterceptor;
import de.hofuniversity.iisys.neo4j.websock.procedures.StoredProcedureHandler;

/**
 * Class providing the control over a shared thread pool.
 */
public class HandlerThreadPool
{
    private static final int INITIAL_SIZE = 2;
    private static final Object INSTANCE_LOCK = new Object();

    private static HandlerThreadPool fInstance;

    private final Object fLock;
    private final Logger fLogger;

    private final List<ResponderThread> fThreads;

    private SecurityInterceptor fInterceptor;
    private StoredProcedureHandler fProcHandler;
    private CypherCallEngine fCypher;

    private boolean fActive;
    private int fTargetSize;

    /**
     * @return existing or newly created ThreadPool
     */
    public static HandlerThreadPool getInstance()
    {
        synchronized(INSTANCE_LOCK)
        {
            if(fInstance == null)
            {
                fInstance = new HandlerThreadPool();
            }
        }

        return fInstance;
    }

    /**
     * Creates a handler thread pool with a default size of 2. To use a shared
     * thread pool, use the class' getInstance method.
     */
    public HandlerThreadPool()
    {
        fLock = new Object();
        fLogger = Logger.getLogger(this.getClass().getName());
        fThreads = new ArrayList<ResponderThread>();

        fActive = false;
        fTargetSize = INITIAL_SIZE;
    }

    /**
     * Sets the handler to use for handling calls to stored procedures. This
     * should be passed before the pool is activated.
     *
     * @param handler stored procedure handler to use
     */
    public void setProcudureHandler(StoredProcedureHandler handler)
    {
        fProcHandler = handler;
    }

    /**
     * Sets the Cypher query engine to use for handling Cypher queries. This
     * should be passed before the pool is activated.
     *
     * @param engine engine to use for Cypher queries
     */
    public void setCypherEngine(CypherCallEngine engine)
    {
        fCypher = engine;
    }

    /**
     * Sets the interceptor to use for authentication. This should be passed
     * before the pool is activated.
     *
     * @param interceptor security interceptor to use
     */
    public void setSecurityInterceptor(SecurityInterceptor interceptor)
    {
        fInterceptor = interceptor;
    }

    /**
     * @return currently used security interceptor
     */
    public SecurityInterceptor getSecurityInterceptor()
    {
        return fInterceptor;
    }

    /**
     * Activates the thread pool, starting the configured number of threads.
     */
    public void activate()
    {
        activate(fTargetSize);
    }

    /**
     * Activates the thread pool, starting the given number of threads.
     * Sizes smaller than 1 are ignored and 2 threads will be started.
     *
     * @param size number of threads to start
     */
    public void activate(int size)
    {
        fActive = true;

        setSize(size);
    }

    /**
     * @return whether the pool has already been activated
     */
    public boolean isActive()
    {
        return fActive;
    }

    /**
     * Sets the thread pool's size at runtime. Threads will be terminated or
     * created accordingly.
     * Sizes smaller than 1 are ignored.
     *
     * @param size new size for the thread pool
     */
    public void setSize(int size)
    {
        if(size > 0)
        {
            fTargetSize = size;

            //break if not yet activated
            if(!fActive)
            {
                return;
            }

            synchronized(fLock)
            {
                int diff = size - fThreads.size();

                if(diff > 0)
                {
                    fLogger.log(Level.INFO, "starting " + diff
                        + " additional threads, starting with "
                        + fThreads.size());

                    //add additional threads
                    while(--diff >= 0)
                    {
                        addThread();
                    }
                }
                else if(diff < 0)
                {
                    fLogger.log(Level.INFO, "stopping " + Math.abs(diff)
                        + " threads, starting with " + fThreads.size());

                    //terminate running threads
                    while(++diff <= 0)
                    {
                        removeThread();
                    }
                }
            }
        }
    }

    /**
     * @return current number of threads in the pool
     */
    public int getSize()
    {
        return fThreads.size();
    }

    /**
     * Terminates all threads in the thread pool. A new thread pool will be
     * created when getInstance is called.
     */
    public void terminate()
    {
        synchronized(fLock)
        {
            for(ResponderThread thread : fThreads)
            {
                thread.deactivate();
            }

            fThreads.clear();
        }
    }

    /**
     * Makes a client session available to the thread pool, cloning the
     * necessary transfer utilities for thread-safe encoding.
     * The session must not be null.
     *
     * @param session newly available session
     */
    public void addSession(ClientSession session)
    {
        //distribute to all threads so they can clone the transfer utility
        synchronized(fLock)
        {
            for(ResponderThread thread : fThreads)
            {
                thread.addSession(session);
            }
        }
    }

    /**
     * Removes a client session from the thread pool. Afterwards the handlers
     * will not be able to respond to any queries from these sessions.
     *
     * @param session
     */
    public void removeSession(ClientSession session)
    {
        //remove session from all threads
        synchronized(fLock)
        {
            for(ResponderThread thread : fThreads)
            {
                thread.removeSession(session);
            }
        }
    }

    /**
     * Tries to return a thread from the pool with a short queue or no queries
     * in the queue.
     *
     * @return a thread from the pool
     */
    public ResponderThread getThread()
    {
        //try finding the thread with the shortest queue
        ResponderThread responder = null;
        int waiting = Integer.MAX_VALUE;
        int tmpWaiting = 0;

        for(ResponderThread thread : fThreads)
        {
            tmpWaiting = thread.getWaiting();
            if(tmpWaiting < waiting)
            {
                responder = thread;

                if(tmpWaiting == 0)
                {
                    break;
                }
                else
                {
                    waiting = tmpWaiting;
                }
            }
        }

        return responder;
    }

    private void addThread()
    {
        ResponderThread thread = new ResponderThread(fProcHandler, fCypher,
            fInterceptor);
        fThreads.add(thread);
        new Thread(thread).start();
    }

    private void removeThread()
    {
        //get thread with shortest queue and deactivate it
        final ResponderThread thread = getThread();
        fThreads.remove(thread);

        thread.deactivate();

        //swap pending queries
        thread.swapTo(getThread());
    }
}
