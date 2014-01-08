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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.glassfish.tyrus.server.Server;
import org.neo4j.graphdb.GraphDatabaseService;

import de.hofuniversity.iisys.neo4j.websock.calls.CypherCallEngine;
import de.hofuniversity.iisys.neo4j.websock.calls.IStoredProcedure;
import de.hofuniversity.iisys.neo4j.websock.neo4j.service.Neo4jServiceProcedures;
import de.hofuniversity.iisys.neo4j.websock.procedures.CypherProcedureLoader;
import de.hofuniversity.iisys.neo4j.websock.procedures.GuiceProcedureLoader;
import de.hofuniversity.iisys.neo4j.websock.procedures.StoredProcedureHandler;
import de.hofuniversity.iisys.neo4j.websock.query.IMessageHandler;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockSession;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;
import de.hofuniversity.iisys.neo4j.websock.util.JSONList;
import de.hofuniversity.iisys.neo4j.websock.util.JSONMap;

/**
 * Annotated server endpoint implementation, logging connections and errors and
 * directing incoming queries to the appropriate client query handler.
 * Loads initial data and persistent stored procedures.
 */
@ServerEndpoint(value = "/websocket")
public class ServiceWebSocket
{
    public static final String HOST_PROP = "websocket.host";
    public static final String DEF_HOST = "127.0.0.1";

    public static final String PORT_PROP = "websocket.port";
    public static final String DEF_PORT = "8080";

    public static final String PATH_PROP = "websocket.path";
    public static final String DEF_PATH = "/neo4j-websocket-server";

    public static final String DEF_FORMAT_PROP = "websocket.default.format";
    public static final String DEF_FORMAT = WebsockConstants.JSON_FORMAT;

    public static final String DEF_COMPRESSION_PROP =
        "websocket.default.compression";
    public static final String DEF_COMPRESSION =
        WebsockConstants.NO_COMPRESSION;

    public static final String THREADING_PROP = "websocket.default.threading";
    public static final String DEF_THREADING = "false";

    public static final String THREADS_PROP = "websocket.default.threads";
    public static final String DEF_THREADS = "4";

    private final Logger fLogger = Logger.getLogger(this.getClass().getName());

    private GraphConfig fConfig;
    private GraphDatabaseService fDb;

    private String fDefFormat;
    private boolean fThreading;
    private int fDefThreads;

    private Map<Session, WebsockSession> fSessions;
    private Map<Session, IMessageHandler> fHandlers;
    private StoredProcedureHandler fStoredProcs;
    private CypherCallEngine fCypher;

    /**
     * Creates a service websocket, activating the database in necessary,
     * loading initial data and setting up persistent stored procedures.
     */
    public ServiceWebSocket()
    {
        try
        {
            WebsockContextHandler context = WebsockContextHandler.getInstance();

            fConfig = context.getConfig();
            fDb = context.getDatabase();

            final ImplUtil impl = configure();

            //TODO: load initial data?

            final Map<String, IStoredProcedure> procs =
                new HashMap<String, IStoredProcedure>();

            //load stored Cypher procedures
            CypherProcedureLoader cpl = new CypherProcedureLoader(fDb);
            procs.putAll(cpl.getProcedures());

            //load native stored procedures
            GuiceProcedureLoader gpl = new GuiceProcedureLoader(fDb, impl);
            procs.putAll(gpl.getProcedures());

            //load Neo4j utility service procedures
            Neo4jServiceProcedures neo4jSvc = new Neo4jServiceProcedures(fDb);
            procs.putAll(neo4jSvc.getProcedures());

            fStoredProcs = new StoredProcedureHandler(fDb, procs, impl);
            fCypher = new CypherCallEngine(fDb, impl);

            fSessions = new HashMap<Session, WebsockSession>();
            fHandlers = new HashMap<Session, IMessageHandler>();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fLogger.log(Level.SEVERE,
                "could not initialize service websocket", e);
        }
    }

    private ImplUtil configure() throws Exception
    {
        //determine whether to use threading
        String threading = fConfig.getProperty(THREADING_PROP);
        if(threading == null || threading.isEmpty())
        {
            threading = DEF_THREADING;
        }
        fThreading = Boolean.parseBoolean(threading);

        if(fThreading)
        {
            String threads = fConfig.getProperty(THREADS_PROP);
            if(threads == null || threads.isEmpty())
            {
                threads = DEF_THREADS;
            }

            fDefThreads = Integer.parseInt(threads);
        }

        //determine default transmission format
        fDefFormat = fConfig.getProperty(DEF_FORMAT_PROP);
        if(fDefFormat == null || fDefFormat.isEmpty())
        {
            fDefFormat = DEF_FORMAT;
        }
        fConfig.setProperty(DEF_FORMAT_PROP, fDefFormat);

        String defCompression = fConfig.getProperty(DEF_COMPRESSION_PROP);
        if(defCompression == null || defCompression.isEmpty())
        {
            defCompression = DEF_COMPRESSION;
        }
        fConfig.setProperty(DEF_COMPRESSION_PROP, defCompression);

        //set default list and map implementation
        //TODO: set per session
        @SuppressWarnings("rawtypes")
        Class<? extends List> listClass = null;
        @SuppressWarnings("rawtypes")
        Class<? extends Map> mapClass = null;

        switch(fDefFormat)
        {
            case WebsockConstants.JSON_FORMAT:
                listClass = JSONList.class;
                mapClass = JSONMap.class;
                break;

            case WebsockConstants.BSON_FORMAT:
                listClass = BasicBSONList.class;
                mapClass = BasicBSONObject.class;
                break;

            default:
                fLogger.log(Level.WARNING, "unknown format '" + fDefFormat
                    + "', switching to default");
                fDefFormat = DEF_FORMAT;
                listClass = LinkedList.class;
                mapClass = HashMap.class;
        }

        return new ImplUtil(listClass, mapClass);
    }

    /**
     * Opens a new session for a client, attaching a message handler.
     *
     * @param session session opened
     * @param config configuration received
     */
    @OnOpen
    public void onOpen(final Session session, EndpointConfig config)
    {
        fLogger.log(Level.FINE, "opened session");

        WebsockSession wsSess = new WebsockSession(session);
        fSessions.put(session, wsSess);

        //TODO: individualize per client
        IMessageHandler handler = null;

        if(!fThreading)
        {
            handler = new ClientQueryHandler(wsSess, fStoredProcs, fCypher);
        }
        else
        {
            handler = new ThreadedClientQueryHandler(wsSess, fStoredProcs,
                fCypher, fDefThreads);
        }

        fHandlers.put(session, handler);
    }

    /**
     * Closes a session for a client.
     *
     * @param session session closed
     * @param closeReason reason for closed connection
     */
    @OnClose
    public void onClose(Session session, CloseReason closeReason)
    {
        fLogger.log(Level.FINE, closeReason.getReasonPhrase());
        fSessions.remove(session);

        IMessageHandler handler = fHandlers.remove(session);
        if(handler != null)
        {
            handler.dispose();
        }
    }

    /**
     * Registers and logs an error for a session.
     *
     * @param session session in which the error occurred
     * @param t exception that was thrown
     */
    @OnError
    public void onError(Session session, Throwable t)
    {
        fLogger.log(Level.SEVERE, t.getMessage(), t);
        t.printStackTrace();
    }

    public static void main(String[] args) throws Exception
    {
        WebsockContextHandler conHan = new WebsockContextHandler();
        conHan.contextInitialized(null);

        //read host, port and path from configuration
        GraphConfig config = conHan.getConfig();

        String host = config.getProperty(HOST_PROP);
        if(host == null || host.isEmpty())
        {
            host = DEF_HOST;
        }

        String port = config.getProperty(PORT_PROP);
        if(port == null || port.isEmpty())
        {
            port = DEF_PORT;
        }
        int portNum = Integer.parseInt(port);

        String path = config.getProperty(PATH_PROP);
        if(path == null || path.isEmpty())
        {
            path = DEF_PATH;
        }

        //start server
        Server server = new Server(host, portNum, path,
            ServiceWebSocket.class);

        server.start();
    }
}
