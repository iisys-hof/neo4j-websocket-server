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
package de.hofuniversity.iisys.neo4j.websock.calls;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;

import de.hofuniversity.iisys.neo4j.websock.LoggingServiceWebSocket;
import de.hofuniversity.iisys.neo4j.websock.neo4j.util.CypherResultConverter;
import de.hofuniversity.iisys.neo4j.websock.query.EQueryType;
import de.hofuniversity.iisys.neo4j.websock.query.WebsockQuery;
import de.hofuniversity.iisys.neo4j.websock.result.AResultSet;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;
import de.hofuniversity.iisys.neo4j.websock.util.ResultSetConverter;

/**
 * Engine, executing incoming cypher queries from the client and encoding the
 * results.
 */
public class LoggingCypherCallEngine
{
    public static final Map<String, List<Long>> CONV_TIMES =
        new HashMap<String, List<Long>>();

    private final GraphDatabaseService fDb;
    private final ExecutionEngine fEngine;

    private final ImplUtil fImpl;

    private final Logger fLogger;

    /**
     * Creates a Cypher call engine using the given database and encoding
     * responses in the given map implementation.
     * None of the parameters may be null.
     *
     * @param database database to use
     * @param impl implementation utility to use
     */
    public LoggingCypherCallEngine(GraphDatabaseService database, ImplUtil impl)
    {
        if(database == null)
        {
            throw new NullPointerException("database was null");
        }
        if(impl == null)
        {
            throw new NullPointerException("implementation utility was null");
        }

        fDb = database;
        fEngine = new ExecutionEngine(fDb);
        fImpl = impl;
        fLogger = Logger.getLogger(this.getClass().getName());
    }

    /**
     * Executes the Cypher query contained in the given message.
     * The given message must not be null or of an incompatible type.
     *
     * @param query message containing the query to execute
     * @return response
     */
    public WebsockQuery call(WebsockQuery query)
    {
        WebsockQuery response = null;
        ExecutionResult result = null;

        try
        {
            //TODO: parameter map conversion (list -> array)

            result = fEngine.execute(query.getPayload().toString(),
                query.getParameters());


            long time = System.nanoTime();

            response = handleResult(query, result);

            time = System.nanoTime() - time;

            if(LoggingServiceWebSocket.LOGGING_ENABLED)
            {
                List<Long> times = null;
                synchronized(CONV_TIMES)
                {
                    times = CONV_TIMES.get(query.getPayload().toString());

                    if(times == null)
                    {
                        times = new LinkedList<Long>();
                        CONV_TIMES.put(query.getPayload().toString(), times);
                    }
                }

                synchronized(times)
                {
                    times.add(time);
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fLogger.log(Level.SEVERE, "failed to execute Cypher query", e);

            response = new WebsockQuery(query.getId(), EQueryType.ERROR);
            response.setPayload("failed to execute Cypher query:\n"
                + e.getMessage());
        }

        //TODO: caching

        return response;
    }

    private WebsockQuery handleResult(final WebsockQuery query,
        final ExecutionResult result) throws Exception
    {
        //pagination
        Integer first =
            (Integer) query.getParameter(WebsockConstants.SUBSET_START);
        Integer max =
            (Integer) query.getParameter(WebsockConstants.SUBSET_SIZE);

        if(first == null)
        {
            first = 0;
        }
        if(max == null)
        {
            max = 0;
        }
        max += first;

        AResultSet<?> resultSet = CypherResultConverter.toTableResult(result,
            first, max);
        WebsockQuery response = new WebsockQuery(query.getId(),
            EQueryType.RESULT);

        final Map<String, Object> map = fImpl.newMap();
        response.setPayload(ResultSetConverter.toMap(resultSet, map));

        return response;
    }
}
