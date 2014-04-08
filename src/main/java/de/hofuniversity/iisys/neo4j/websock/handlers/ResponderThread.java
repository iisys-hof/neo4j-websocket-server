/*
 * Copyright (c) 2012-2014 Institute of Information Systems, Hof University
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.hofuniversity.iisys.neo4j.websock.calls.CypherCallEngine;
import de.hofuniversity.iisys.neo4j.websock.neo4j.security.SecurityInterceptor;
import de.hofuniversity.iisys.neo4j.websock.procedures.StoredProcedureHandler;
import de.hofuniversity.iisys.neo4j.websock.query.EQueryType;
import de.hofuniversity.iisys.neo4j.websock.query.WebsockQuery;
import de.hofuniversity.iisys.neo4j.websock.query.encoding.TransferUtil;

/**
 * Responder thread which asynchronously decodes incoming queries, handles
 * them, encodes and sends the response.
 */
public class ResponderThread implements Runnable
{
    private static final long WAIT_MS = 1000;

    private final Object fTrigger;
    private final List<QueryContainer> fQueue;

    private final Map<ClientSession, TransferUtil> fTransUitls;

    private final StoredProcedureHandler fProcHandler;
    private final CypherCallEngine fCypher;
    private final SecurityInterceptor fInteceptor;

    private final Logger fLogger;

    private int fWaiting = 0;
    private boolean fActive = false;

    /**
     * Creates a responder thread, using the given stored procedure handler and
     * Cypher query engine to handle queries and the optional security
     * interceptor for authentication.
     *
     * @param procHandler stored procedure handler to use
     * @param cypher Cypher query engine to use
     * @param interceptor security interceptor to use for authentication
     */
    public ResponderThread(StoredProcedureHandler procHandler,
        CypherCallEngine cypher, SecurityInterceptor interceptor)
    {
        if(procHandler == null)
        {
            throw new NullPointerException(
                "stored procedure handler was null");
        }
        if(cypher == null)
        {
            throw new NullPointerException("cypher query engine was null");
        }

        fTrigger = new Object();
        fLogger = Logger.getLogger(this.getClass().getName());
        fQueue = new LinkedList<QueryContainer>();
        fTransUitls = new HashMap<ClientSession, TransferUtil>();
        fInteceptor = interceptor;
        fProcHandler = procHandler;
        fCypher = cypher;
    }

    /**
     * Makes a client session known to the responder thread, cloning its
     * transfer utility for thread-safe further use.
     *
     * @param session new client session
     */
    public void addSession(ClientSession session)
    {
        TransferUtil util = session.getTransferUtil();
        fTransUitls.put(session, util.clone());
    }

    /**
     * Removes a known client session from the responder thread. Afterwards
     * the responder will not be able to respond to this session's queries.
     *
     * @param session session to remove from the responder
     */
    public void removeSession(ClientSession session)
    {
        fTransUitls.remove(session);
    }

    /**
     * Deactivates the responder, eventually shutting down its thread.
     */
    public void deactivate()
    {
        fActive = false;

        synchronized(fTrigger)
        {
            fTrigger.notify();
        }
    }

    /**
     * Enqueues a binary message from a certain client session for processing.
     *
     * @param buffer binary message received
     * @param session session to respond to
     */
    public void enqueue(ByteBuffer buffer, ClientSession session)
    {
        synchronized(fQueue)
        {
            fQueue.add(new QueryContainer(session, buffer));
            ++fWaiting;
        }

        synchronized(fTrigger)
        {
            fTrigger.notify();
        }
    }

    /**
     * Enqueues a text message from a certain client session for processing.
     *
     * @param message text message received
     * @param session session to respond to
     */
    public void enqueue(String message, ClientSession session)
    {
        synchronized(fQueue)
        {
            fQueue.add(new QueryContainer(session, message));
            ++fWaiting;
        }

        synchronized(fTrigger)
        {
            fTrigger.notify();
        }
    }

    /**
     * @return number of waiting queries
     */
    public int getWaiting()
    {
        return fWaiting;
    }

    /**
     * Swaps all pending queries to another responder, emptying the internal
     * queue. Should not be called while the responder is still active.
     *
     * @param repsonder responder to swap queries to
     */
    public void swapTo(ResponderThread responder)
    {
        //hand over all pending queries to other responder
        synchronized(fQueue)
        {
            responder.swapQueue(fQueue);
            fQueue.clear();
        }
    }

    private void swapQueue(List<QueryContainer> containers)
    {
        //called on a responder, receiving
        synchronized(fQueue)
        {
            fQueue.addAll(containers);
            fWaiting += containers.size();
        }
    }

    @Override
    public void run()
    {
        final List<QueryContainer> handling = new LinkedList<QueryContainer>();
        fActive = true;

        WebsockQuery query = null;
        ClientSession session = null;
        String user = null;
        while(fActive)
        {
            synchronized(fQueue)
            {
                handling.addAll(fQueue);
                fQueue.clear();
            }

            //waiting queries
            for(QueryContainer container : handling)
            {
                try
                {
                    query = container.getQuery();
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    //TODO: logging
                }

                session = container.getSession();
                user = session.getAuthenticatedUser();
                if(user != null)
                {
                    handle(query, session);
                }
                else
                {
                    handleUnauth(query, session);
                }
                --fWaiting;
            }
            handling.clear();

            //if deactivated, directly terminate the loop
            //if there are already new queries in the queue, continue
            if(!fActive
                || !fQueue.isEmpty())
            {
                continue;
            }

            //otherwise, wait
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

    private void handle(WebsockQuery msg, ClientSession session)
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
                response = handleAuthentication(msg, session);
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
                fTransUitls.get(session).sendMessage(response);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fLogger.log(Level.SEVERE,
                    "failed to send response to client", e);
            }
        }
    }

    private void handleUnauth(final WebsockQuery msg, ClientSession session)
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
                response = handleAuthentication(msg, session);
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
                fTransUitls.get(session).sendMessage(response);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                fLogger.log(Level.SEVERE,
                    "failed to send response to client", e);
            }
        }
    }

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

    private WebsockQuery storeProcedure(final WebsockQuery msg)
    {
        return fProcHandler.storeProcedure(msg);
    }

    private WebsockQuery deleteProcedure(final WebsockQuery msg)
    {
        return fProcHandler.deleteProcedure(msg);
    }

    private WebsockQuery handleAuthentication(final WebsockQuery msg,
        ClientSession session)
    {
        String authenticated = session.getAuthenticatedUser();
        WebsockQuery response = null;

        if(fInteceptor == null)
        {
            //no authentication
            response = new WebsockQuery(msg.getId(), EQueryType.ERROR);
            response.setPayload("no authentication required");
        }
        else if(authenticated != null)
        {

        }
        else
        {
            //handle authentication
            //the interceptor sends its own messages
            TransferUtil util = fTransUitls.get(session);
            authenticated = fInteceptor.handle(msg, util);
            session.setAuthenticatedUser(authenticated);
        }

        return response;
    }

    private WebsockQuery handleConfiguration(final WebsockQuery msg)
    {
        //TODO
        return null;
    }


    private class QueryContainer
    {
        private final ClientSession fSession;
        private final ByteBuffer fBinary;
        private final String fText;

        /**
         * Creates a new query container for incoming binary data.
         *
         * @param session client session to respond to
         * @param binary binary data received
         */
        public QueryContainer(ClientSession session, ByteBuffer binary)
        {
            fSession = session;
            fBinary = binary;
            fText = null;
        }

        /**
         * Creates a new query container for incoming String data.
         *
         * @param session client session to respond to
         * @param text String data received
         */
        public QueryContainer(ClientSession session, String text)
        {
            fSession = session;
            fBinary = null;
            fText = text;
        }

        /**
         * @return session to respond to
         */
        public ClientSession getSession()
        {
            return fSession;
        }

        /**
         * Converts the stored raw data into a WebsockQuery using a
         * registered transfer utility.
         * Throws an Exception if decoding fails.
         *
         * @return converted WebsockQuery
         * @throws Exception if conversion fails
         */
        public WebsockQuery getQuery() throws Exception
        {
            final TransferUtil util = fTransUitls.get(fSession);

            WebsockQuery query = null;

            if(util != null)
            {
                if(fText == null)
                {
                    query = util.convert(fBinary);
                }
                else
                {
                    query = util.convert(fText);
                }
            }

            return query;
        }
    }
}
