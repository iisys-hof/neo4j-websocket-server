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
import java.util.logging.Logger;

import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import de.hofuniversity.iisys.neo4j.websock.GraphConfig;
import de.hofuniversity.iisys.neo4j.websock.ServiceWebSocket;
import de.hofuniversity.iisys.neo4j.websock.WebsockContextHandler;
import de.hofuniversity.iisys.neo4j.websock.calls.CypherCallEngine;
import de.hofuniversity.iisys.neo4j.websock.neo4j.security.SecurityInterceptor;
import de.hofuniversity.iisys.neo4j.websock.procedures.StoredProcedureHandler;
import de.hofuniversity.iisys.neo4j.websock.query.IMessageHandler;
import de.hofuniversity.iisys.neo4j.websock.query.encoding.BinaryTransferUtil;
import de.hofuniversity.iisys.neo4j.websock.query.encoding.StringTransferUtil;
import de.hofuniversity.iisys.neo4j.websock.query.encoding.TransferUtil;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockSession;

/**
 * Websocket handler for incoming client queries, relaying messages to the
 * appropriate components using threads from a thread pool.
 */
public class ThreadedClientQueryHandler implements IMessageHandler
{
    private final HandlerThreadPool fPool;
    private final ClientSession fClientSess;

    private final Logger fLogger;

    /**
     * Creates a threaded query handler for the given session, using the given
     * stored procedure handler and Cypher call engine to execute queries with
     * the given number of threads.
     * None of the parameters may be null.
     * The number of threads must be a positive number.
     *
     * @param wsSess session information object
     * @param procHandler stored procedure handler to use
     * @param cypher Cypher engine to use
     * @param interceptor security query interceptor (optional)
     * @param threadCount number of threads to use
     */
    public ThreadedClientQueryHandler(final WebsockSession wsSess,
        StoredProcedureHandler procHandler, CypherCallEngine cypher,
        final SecurityInterceptor interceptor, final int threadCount)
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

        fLogger = Logger.getLogger(this.getClass().getName());

        final Session session = wsSess.getSession();
        Basic remote = session.getBasicRemote();
        StringTransferUtil stUtil = new StringTransferUtil(remote, this);
        BinaryTransferUtil btUtil = new BinaryTransferUtil(remote, this,
            false);
        final TransferUtil transfer = new TransferUtil(stUtil, btUtil);

        //configure transfer utility
        WebsockContextHandler context = WebsockContextHandler.getInstance();
        GraphConfig config = context.getConfig();
        String format = config.getProperty(ServiceWebSocket.DEF_FORMAT_PROP);
        String comp = config.getProperty(
            ServiceWebSocket.DEF_COMPRESSION_PROP);
        transfer.setFormat(format, comp);

        session.addMessageHandler(btUtil);
        session.addMessageHandler(stUtil);

        //create thread pool if not yet initialized
        fPool = HandlerThreadPool.getInstance();
        fPool.setProcudureHandler(procHandler);
        fPool.setCypherEngine(cypher);
        fPool.setSecurityInterceptor(interceptor);

        fPool.activate(threadCount);

        //create and pass session
        fClientSess = new ClientSession(transfer);
        fPool.addSession(fClientSess);

        if(interceptor == null)
        {
            //no authentication, dummy user
            fClientSess.setAuthenticatedUser("noauth");
        }
        else
        {
            //send initial authentication challenge
            interceptor.onConnect(transfer);
        }
    }

    @Override
    public void dispose()
    {
        //cleanup session's links
        fPool.removeSession(fClientSess);
    }

    @Override
    public void onMessage(ByteBuffer buffer)
    {
        ResponderThread responder = fPool.getThread();

        if(responder != null)
        {
            responder.enqueue(buffer, fClientSess);
        }
        else
        {
            throw new RuntimeException(
                "no threads available, thread pool already terminated?");
        }
    }

    @Override
    public void onMessage(String message)
    {
        ResponderThread responder = fPool.getThread();

        if(responder != null)
        {
            responder.enqueue(message, fClientSess);
        }
        else
        {
            throw new RuntimeException(
                "no threads available, thread pool already terminated?");
        }
    }
}
