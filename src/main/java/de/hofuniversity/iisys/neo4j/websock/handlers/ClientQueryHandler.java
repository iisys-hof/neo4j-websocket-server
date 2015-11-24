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

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import de.hofuniversity.iisys.neo4j.websock.GraphConfig;
import de.hofuniversity.iisys.neo4j.websock.ServiceWebSocket;
import de.hofuniversity.iisys.neo4j.websock.WebsockContextHandler;
import de.hofuniversity.iisys.neo4j.websock.calls.CypherCallEngine;
import de.hofuniversity.iisys.neo4j.websock.neo4j.security.SecurityInterceptor;
import de.hofuniversity.iisys.neo4j.websock.procedures.StoredProcedureHandler;
import de.hofuniversity.iisys.neo4j.websock.query.EQueryType;
import de.hofuniversity.iisys.neo4j.websock.query.IMessageHandler;
import de.hofuniversity.iisys.neo4j.websock.query.WebsockQuery;
import de.hofuniversity.iisys.neo4j.websock.query.encoding.BinaryTransferUtil;
import de.hofuniversity.iisys.neo4j.websock.query.encoding.StringTransferUtil;
import de.hofuniversity.iisys.neo4j.websock.query.encoding.TransferUtil;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockSession;

/**
 * Websocket handler for incoming client queries, relaying messages to the
 * appropriate components.
 */
public class ClientQueryHandler implements IMessageHandler
{
    private final WebsockSession fWsSess;
    private final StoredProcedureHandler fProcHandler;
    private final CypherCallEngine fCypher;
    private final Session fSession;
    private final Logger fLogger;
    private final TransferUtil fTransfer;

    private final SecurityInterceptor fInteceptor;
    private String fAuthenticated;

    /**
     * Creates a query handler for the given session, using the given
     * stored procedure handler and cypher call engine to execute queries.
     * None of the parameters may be null.
     *
     * @param wsSess session information object
     * @param procHandler stored procedure handler to use
     * @param cypher cypher engine to use
     * @param interceptor security query interceptor (optional)
     */
    public ClientQueryHandler(WebsockSession wsSess,
        StoredProcedureHandler procHandler, CypherCallEngine cypher,
        SecurityInterceptor interceptor)
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
        fInteceptor = interceptor;
        fSession = fWsSess.getSession();
        fLogger = Logger.getLogger(this.getClass().getName());

        Basic remote = fSession.getBasicRemote();
        StringTransferUtil stUtil = new StringTransferUtil(remote, this);
        BinaryTransferUtil btUtil = new BinaryTransferUtil(remote, this,
            false);
        fTransfer = new TransferUtil(stUtil, btUtil);

        //configure transfer utility
        WebsockContextHandler context = WebsockContextHandler.getInstance();
        GraphConfig config = context.getConfig();
        String format = config.getProperty(ServiceWebSocket.DEF_FORMAT_PROP);
        String comp = config.getProperty(
            ServiceWebSocket.DEF_COMPRESSION_PROP);
        fTransfer.setFormat(format, comp);

        fSession.addMessageHandler(btUtil);
        fSession.addMessageHandler(stUtil);

        if(fInteceptor == null)
        {
            fAuthenticated = "noauth";
        }
        else
        {
            fInteceptor.onConnect(fTransfer);
        }
    }

    @Override
    public void dispose()
    {
        //not needed
    }

    private void handle(final WebsockQuery msg)
    {
        WebsockQuery response = null;

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

            default:
                response = handleUnknownType(msg);
                break;
        }

        if(response != null)
        {
            try
            {
                fTransfer.sendMessage(response);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fLogger.log(Level.SEVERE,
                    "failed to send response to client", e);
            }
        }
    }

    private void handleUnauth(final WebsockQuery msg)
    {
        //query while no user is authenticated
        WebsockQuery response = null;

        switch(msg.getType())
        {
            case PING:
                response = handlePing(msg);
                break;

            case PONG:
                handlePong(msg);
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

            default:
                response = handleUnauthenticatedQuery(msg);
                break;
        }

        if(response != null)
        {
            try
            {
                fTransfer.sendMessage(response);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fLogger.log(Level.SEVERE,
                    "failed to send response to client", e);
            }
        }
    }

    @Override
    public void onMessage(ByteBuffer buffer)
    {
        try
        {
            WebsockQuery query = fTransfer.convert(buffer);

            if(fAuthenticated != null)
            {
                handle(query);
            }
            else
            {
                handleUnauth(query);
            }
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

            if(fAuthenticated != null)
            {
                handle(query);
            }
            else
            {
                handleUnauth(query);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            //TODO: logging
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
        WebsockQuery response = null;

        if(fAuthenticated != null)
        {
            //already authenticated
            response = new WebsockQuery(msg.getId(), EQueryType.ERROR);
            response.setPayload("already authenticated");
        }
        else
        {
            //handle authentication
            //the interceptor sends its own messages
            fAuthenticated = fInteceptor.handle(msg, fTransfer);
        }

        return response;
    }

    public WebsockQuery handleConfiguration(final WebsockQuery msg)
    {
        //TODO
        return null;
    }

    private WebsockQuery handleError(final WebsockQuery msg)
    {
        String message = "query " + msg.getId() + ": " + msg.getPayload();
        System.err.println(message);
        fLogger.log(Level.SEVERE, message);
        return null;
    }

    private WebsockQuery handleUnauthenticatedQuery(final WebsockQuery msg)
    {
        WebsockQuery response = new WebsockQuery(msg.getId(),
            EQueryType.ERROR);
        response.setPayload("authentication required");

        return response;
    }

    private WebsockQuery handleUnknownType(final WebsockQuery msg)
    {
        WebsockQuery response = new WebsockQuery(msg.getId(),
            EQueryType.ERROR);
        response.setPayload("unknown query type: " + msg.getType());

        return response;
    }
}
