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
import de.hofuniversity.iisys.neo4j.websock.result.AResultSet;
import de.hofuniversity.iisys.neo4j.websock.result.TableResult;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;

/**
 * Wrapper for a stored, named Cypher statement that can be executed with
 * different parameters.
 */
public class LoggingCypherProcedure implements IStoredProcedure
{
    public static final Map<String, List<Long>> CONV_TIMES =
        new HashMap<String, List<Long>>();

    private final String fName;
    private final String fQuery;

    private final GraphDatabaseService fDb;
    private final ExecutionEngine fEngine;
    private final Logger fLogger;

    /**
     * Creates a stored cypher procedure that executes a prepared statement
     * on the given database when called by its name.
     * None of the parameters may be null.
     *
     * @param name name of the query
     * @param database database service to use
     * @param query Cypher query to execute
     */
    public LoggingCypherProcedure(String name, GraphDatabaseService database,
        String query)
    {
        if(name == null || name.isEmpty())
        {
            throw new RuntimeException("procedure name was null or empty");
        }
        if(database == null)
        {
            throw new RuntimeException("database service was null");
        }
        if(query == null || query.isEmpty())
        {
            throw new RuntimeException("query was null or empty");
        }

        fName = name;
        fQuery = query;

        fDb = database;
        fEngine = new ExecutionEngine(fDb);
        fLogger = Logger.getLogger(this.getClass().getName());
    }

    @Override
    public String getName()
    {
        return fName;
    }

    @Override
    public AResultSet<?> call(Map<String, Object> parameters)
    {
        ExecutionResult result = null;

        try
        {
            //TODO: parameter map conversion (list -> array)

            result = fEngine.execute(fQuery, parameters);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fLogger.log(Level.SEVERE, "failed to execute Cypher query", e);
            throw new RuntimeException(e);
        }

        //TODO: check error handling

        //TODO: caching


        long time = System.nanoTime();

        //pagination
        Integer first =
            (Integer) parameters.get(WebsockConstants.SUBSET_START);
        Integer max =
            (Integer) parameters.get(WebsockConstants.SUBSET_SIZE);

        if(first == null)
        {
            first = 0;
        }
        if(max == null)
        {
            max = 0;
        }
        max += first;

        TableResult r = CypherResultConverter.toTableResult(result, first, max);

        time = System.nanoTime() - time;

        if(LoggingServiceWebSocket.LOGGING_ENABLED)
        {
            List<Long> times = null;
            synchronized(CONV_TIMES)
            {
                times = CONV_TIMES.get(fName);

                if(times == null)
                {
                    times = new LinkedList<Long>();
                    CONV_TIMES.put(fName, times);
                }
            }

            synchronized(times)
            {
                times.add(time);
            }
        }

        return r;
    }

    @Override
    public boolean isNative()
    {
        return false;
    }
}
