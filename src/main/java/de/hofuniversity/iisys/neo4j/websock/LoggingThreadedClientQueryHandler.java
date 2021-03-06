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
package de.hofuniversity.iisys.neo4j.websock;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import de.hofuniversity.iisys.neo4j.websock.calls.LoggingCypherCallEngine;
import de.hofuniversity.iisys.neo4j.websock.procedures.LoggingStoredProcedureHandler;
import de.hofuniversity.iisys.neo4j.websock.query.EQueryType;
import de.hofuniversity.iisys.neo4j.websock.query.IMessageHandler;
import de.hofuniversity.iisys.neo4j.websock.query.WebsockQuery;
import de.hofuniversity.iisys.neo4j.websock.query.encoding.logging.LoggingBinaryTransferUtil;
import de.hofuniversity.iisys.neo4j.websock.query.encoding.logging.LoggingStringTransferUtil;
import de.hofuniversity.iisys.neo4j.websock.query.encoding.logging.LoggingTransferUtil;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockSession;

/**
 * Websocket handler for incoming client queries, relaying messages to the
 * appropriate components using threads from a thread pool.
 */
public class LoggingThreadedClientQueryHandler implements IMessageHandler
{
    public static final Map<String, List<Long>> PROCESSING_TIMES =
        new HashMap<String, List<Long>>();
    public static final Map<String, List<Long>> SENDING_TIMES =
        new HashMap<String, List<Long>>();
    public static final List<Long> WAIT_TIMES = new LinkedList<Long>();

    private static final long WAIT_MS = 1000;

    private final WebsockSession fWsSess;
    private final LoggingStoredProcedureHandler fProcHandler;
    private final LoggingCypherCallEngine fCypher;
    private final Session fSession;
    private final LoggingTransferUtil fTransfer;

    private final int fThreadCount;

    private final List<ResponderThread> fThreads;

    private final Logger fLogger;

    /**
     * Creates a query handler for the given session, using the given
     * stored procedure handler and cypher call engine to execute queries.
     * None of the parameters may be null.
     *
     * @param wsSess session information object
     * @param procHandler stored procedure handler to use
     * @param cypher cypher engine to use
     */
    public LoggingThreadedClientQueryHandler(WebsockSession wsSess,
        LoggingStoredProcedureHandler procHandler,
        LoggingCypherCallEngine cypher, int threads)
    {
        if(wsSess == null)
        {
            throw new RuntimeException("websocket session was null");
        }
        if(procHandler == null)
        {
            throw new RuntimeException("stored procedure handler was null");
        }
        if(cypher == null)
        {
            throw new RuntimeException("cypher engine was null");
        }

        fWsSess = wsSess;
        fProcHandler = procHandler;
        fCypher = cypher;
        fSession = fWsSess.getSession();
        fThreadCount = threads;
        Basic remote = fSession.getBasicRemote();

        LoggingStringTransferUtil stUtil = new LoggingStringTransferUtil(
            remote, this);
        LoggingBinaryTransferUtil btUtil = new LoggingBinaryTransferUtil(
            remote, this, false);
        fTransfer = new LoggingTransferUtil(stUtil, btUtil);

        //configure transfer utility
        WebsockContextHandler context = WebsockContextHandler.getInstance();
        GraphConfig config = context.getConfig();
        String format = config.getProperty(ServiceWebSocket.DEF_FORMAT_PROP);
        String comp = config.getProperty(
            ServiceWebSocket.DEF_COMPRESSION_PROP);
        fTransfer.setFormat(format, comp);

        fSession.addMessageHandler(btUtil);
        fSession.addMessageHandler(stUtil);

        fLogger = Logger.getLogger(this.getClass().getName());

        //create thread pool
        //TODO: deactivation method
        fThreads = new ArrayList<ResponderThread>();
        ResponderThread thread = null;
        for(int i = 0; i < fThreadCount; ++i)
        {
            thread = new ResponderThread(fTransfer.clone());
            fThreads.add(thread);
            new Thread(thread).start();
        }
    }

    private ResponderThread nextThread()
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

    @Override
    public void onMessage(ByteBuffer buffer)
    {
        nextThread().enqueue(buffer);
    }

    @Override
    public void onMessage(String message)
    {
        nextThread().enqueue(message);
    }

    //TODO: security
    private WebsockQuery handleCall(final WebsockQuery msg)
    {
        return fProcHandler.handleCall(msg);
    }

    private WebsockQuery handleQuery(final WebsockQuery msg)
    {
        return fCypher.call(msg);
    }

    private WebsockQuery handlePing(final WebsockQuery msg)
    {
        return new WebsockQuery(msg.getId(), EQueryType.PONG);
    }

    private void handlePong(final WebsockQuery msg)
    {
        //TODO: measure time taken or refresh watchdog
    }

    public WebsockQuery storeProcedure(final WebsockQuery msg)
    {
        return fProcHandler.storeProcedure(msg);
    }

    public WebsockQuery deleteProcedure(final WebsockQuery msg)
    {
        return fProcHandler.deleteProcedure(msg);
    }

    public WebsockQuery handleAuthentication(final WebsockQuery msg)
    {
        //TODO
        return null;
    }

    public WebsockQuery handleConfiguration(final WebsockQuery msg)
    {
        //TODO
        WebsockQuery response = null;

        if(msg.getPayload().equals("enableLogging"))
        {
            LoggingServiceWebSocket.LOGGING_ENABLED = true;
            LoggingBinaryTransferUtil.LOGGING_ENABLED = true;
            LoggingStringTransferUtil.LOGGING_ENABLED = true;

            response = new WebsockQuery(msg.getId(), EQueryType.SUCCESS);
        }
        if(msg.getPayload().equals("disableLogging"))
        {
            LoggingServiceWebSocket.LOGGING_ENABLED = false;
            LoggingBinaryTransferUtil.LOGGING_ENABLED = false;
            LoggingStringTransferUtil.LOGGING_ENABLED = false;

            response = new WebsockQuery(msg.getId(), EQueryType.SUCCESS);
        }

        return response;
    }

    private WebsockQuery handleError(final WebsockQuery msg)
    {
        String message = "query " + msg.getId() + ": " + msg.getPayload();
        System.err.println(message);
        fLogger.log(Level.SEVERE, message);
        return null;
    }

    private class ResponderThread implements Runnable
    {
        private final LinkedList<Long> fQueueTimes = new LinkedList<Long>();
        private final LinkedList<Long> fStringQueueTimes =
            new LinkedList<Long>();

        private final Object fTrigger;
        private final List<ByteBuffer> fQueue;
        private final List<String> fStringQueue;

        private final LoggingTransferUtil fTransfer;

        private int fWaiting = 0;
        private boolean fActive = false;

        public ResponderThread(LoggingTransferUtil tu)
        {
            fTrigger = new Object();
            fQueue = new LinkedList<ByteBuffer>();
            fStringQueue = new LinkedList<String>();
            fTransfer = tu;
        }

        public void setFormat(String format, String compression)
        {
            fTransfer.setFormat(format, compression);
        }

        public void deactivate()
        {
            fActive = false;

            synchronized(fTrigger)
            {
                fTrigger.notify();
            }
        }

        public void enqueue(ByteBuffer buffer)
        {
            synchronized(fQueue)
            {
                fQueue.add(buffer);

                ++fWaiting;
            }

            if(LoggingServiceWebSocket.LOGGING_ENABLED)
            {
                synchronized(fQueueTimes)
                {
                    fQueueTimes.add(System.nanoTime());
                }
            }

            synchronized(fTrigger)
            {
                fTrigger.notify();
            }
        }

        public void enqueue(String message)
        {
            synchronized(fStringQueue)
            {
                fStringQueue.add(message);

                ++fWaiting;
            }

            if(LoggingServiceWebSocket.LOGGING_ENABLED)
            {
                synchronized(fStringQueueTimes)
                {
                    fStringQueueTimes.add(System.nanoTime());
                }
            }

            synchronized(fTrigger)
            {
                fTrigger.notify();
            }
        }

        public int getWaiting()
        {
            return fWaiting;
        }

        @Override
        public void run()
        {
            final List<ByteBuffer> handling = new LinkedList<ByteBuffer>();
            final List<String> handlingStrings = new LinkedList<String>();
            fActive = true;

            long time = 0;
            WebsockQuery query = null;
            while(fActive)
            {
                synchronized(fQueue)
                {
                    handling.addAll(fQueue);
                    fQueue.clear();
                }

                synchronized(fStringQueue)
                {
                    handlingStrings.addAll(fStringQueue);
                    fStringQueue.clear();
                }

                //waiting binary queries
                for(ByteBuffer buffer : handling)
                {
                    if(LoggingServiceWebSocket.LOGGING_ENABLED)
                    {
                        synchronized(fQueueTimes)
                        {
                            time = System.nanoTime() - fQueueTimes.pop();
                        }

                        synchronized(WAIT_TIMES)
                        {
                            WAIT_TIMES.add(time);
                        }
                    }

                    try
                    {
                        query = fTransfer.convert(buffer);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        //TODO: logging
                    }

                    handle(query);
                    --fWaiting;
                }
                handling.clear();

                //waiting string queries
                for(String s : handlingStrings)
                {
                    if(LoggingServiceWebSocket.LOGGING_ENABLED)
                    {
                        synchronized(fStringQueueTimes)
                        {
                            time = System.nanoTime() - fStringQueueTimes.pop();
                        }

                        synchronized(WAIT_TIMES)
                        {
                            WAIT_TIMES.add(time);
                        }
                    }

                    try
                    {
                        query = fTransfer.convert(s);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                        //TODO: logging
                    }

                    handle(query);
                    --fWaiting;
                }
                handlingStrings.clear();

                if(!fQueue.isEmpty()
                    || !fStringQueue.isEmpty())
                {
                    continue;
                }

                synchronized(fTrigger)
                {
                    try
                    {
                        fTrigger.wait(WAIT_MS);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void handle(WebsockQuery msg)
        {
            WebsockQuery response = null;

            long time = System.nanoTime();
            switch(msg.getType())
            {
                case PROCEDURE_CALL:
                    response = handleCall(msg);
                    break;

                case DIRECT_CYPHER:
                    response = handleQuery(msg);
                    break;

                case PING:
                    response = handlePing(msg);
                    break;

                case PONG:
                    handlePong(msg);
                    break;

                case STORE_PROCEDURE:
                    response = storeProcedure(msg);
                    break;

                case DELETE_PROCEDURE:
                    response = deleteProcedure(msg);
                    break;

                case AUTHENTICATION:
                    response = handleAuthentication(msg);
                    break;

                case CONFIGURATION:
                    response = handleConfiguration(msg);
                    break;

                case ERROR:
                    response = handleError(msg);
                    break;
            }
            time = System.nanoTime() - time;


            final String type = msg.getPayload().toString();
            List<Long> times = null;

            //log processing time
            if(LoggingServiceWebSocket.LOGGING_ENABLED)
            {
                synchronized(PROCESSING_TIMES)
                {
                    times = PROCESSING_TIMES.get(type);

                    if(times == null)
                    {
                        times = new LinkedList<Long>();
                        PROCESSING_TIMES.put(type, times);
                    }
                }

                synchronized(times)
                {
                    times.add(time);
                }
            }

            if(response != null)
            {
                try
                {
                    time = System.nanoTime();
                    fTransfer.sendMessage(response);
                    time = System.nanoTime() - time;


                    if(LoggingServiceWebSocket.LOGGING_ENABLED)
                    {
                        synchronized(SENDING_TIMES)
                        {
                            times = SENDING_TIMES.get(type);

                            if(times == null)
                            {
                                times = new LinkedList<Long>();
                                SENDING_TIMES.put(type, times);
                            }
                        }

                        synchronized(times)
                        {
                            times.add(time);
                        }
                    }
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    fLogger.log(Level.SEVERE,
                        "failed to send response to client", e);
                }
            }
        }
    }

    @Override
    public void dispose()
    {
        for(ResponderThread thread : fThreads)
        {
            thread.deactivate();
        }
    }
}
