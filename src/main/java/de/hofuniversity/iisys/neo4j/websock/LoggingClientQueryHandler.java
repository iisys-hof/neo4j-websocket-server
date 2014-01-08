/*
 * Copyright (c) 2012-2013 Institute of Information Systems, Hof University
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
 * appropriate components.
 */
public class LoggingClientQueryHandler implements IMessageHandler
{
    public static final Map<String, List<Long>> PROCESSING_TIMES =
        new HashMap<String, List<Long>>();
    public static final Map<String, List<Long>> SENDING_TIMES =
        new HashMap<String, List<Long>>();

    private final WebsockSession fWsSess;
    private final LoggingStoredProcedureHandler fProcHandler;
    private final LoggingCypherCallEngine fCypher;
    private final Session fSession;
    private final Logger fLogger;
    private final LoggingTransferUtil fTransfer;

    /**
     * Creates a query handler for the given session, using the given
     * stored procedure handler and cypher call engine to execute queries.
     * None of the parameters may be null.
     *
     * @param wsSess session information object
     * @param procHandler stored procedure handler to use
     * @param cypher cypher engine to use
     */
    public LoggingClientQueryHandler(WebsockSession wsSess,
        LoggingStoredProcedureHandler procHandler, LoggingCypherCallEngine cypher)
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
    }

    @Override
    public void onMessage(ByteBuffer buffer)
    {
        try
        {
            WebsockQuery query = fTransfer.convert(buffer);
            handle(query);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            //TODO: logging
        }
    }

    @Override
    public void onMessage(String message)
    {
        try
        {
            WebsockQuery query = fTransfer.convert(message);
            handle(query);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            //TODO: logging
        }
    }

    private void handle(final WebsockQuery msg)
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


        //log processing time
        final String type = msg.getPayload().toString();

        if(LoggingServiceWebSocket.LOGGING_ENABLED)
        {
            List<Long> times = null;
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
                    List<Long> times = null;

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

    @Override
    public void dispose()
    {
        //not needed
    }
}
