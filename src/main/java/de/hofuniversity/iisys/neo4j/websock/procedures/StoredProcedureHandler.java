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
package de.hofuniversity.iisys.neo4j.websock.procedures;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.graphdb.GraphDatabaseService;

import de.hofuniversity.iisys.neo4j.websock.calls.CypherProcedure;
import de.hofuniversity.iisys.neo4j.websock.calls.IStoredProcedure;
import de.hofuniversity.iisys.neo4j.websock.query.EQueryType;
import de.hofuniversity.iisys.neo4j.websock.query.WebsockQuery;
import de.hofuniversity.iisys.neo4j.websock.result.AResultSet;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;
import de.hofuniversity.iisys.neo4j.websock.util.ResultSetConverter;

/**
 * Handler class for stored procedures, relaying calls to the appropriate
 * stored procedures, storing and deleting procedures and returning responses.
 */
public class StoredProcedureHandler
{
    private final GraphDatabaseService fDb;
    private final Map<String, IStoredProcedure> fProcedures;

    private final ImplUtil fImpl;

    private final Logger fLogger;
    private final boolean fDebug;

    /**
     * Creates a stored procedure handler using the given database and creating
     * results based on the given map implementation with no predefined stored
     * procedures.
     * None of the parameters may be null.
     *
     * @param database database service to use
     * @param impl implementation utility to use
     */
    public StoredProcedureHandler(GraphDatabaseService database, ImplUtil impl)
    {
        this(database, new HashMap<String, IStoredProcedure>(), impl);
    }

    /**
     * Creates a stored procedure handler using the given database and creating
     * results based on the given map implementation with the stored procedures
     * form the given map.
     * None of the parameters may be null.
     *
     * @param database database service to use
     * @param procedures initial map of stored procedures by name
     * @param impl implementation utility to use
     */
    public StoredProcedureHandler(GraphDatabaseService database,
        Map<String, IStoredProcedure> procedures, ImplUtil impl)
    {
        if(database == null)
        {
            throw new NullPointerException("database service was null");
        }
        if(procedures == null)
        {
            throw new NullPointerException(
                "map of stored procedures was null");
        }
        if(impl == null)
        {
            throw new NullPointerException("implementation utility was null");
        }

        fDb = database;
        fProcedures = procedures;
        fImpl = impl;
        fLogger = Logger.getLogger(this.getClass().getName());
        fDebug = (fLogger.getLevel() == Level.FINEST);
    }

    /**
     * Calls a stored procedure if there is one with the name specified.
     * The given query must be of the right type and contain a valid procedure
     * name.
     *
     * @param query message containing a procedure call
     * @return response for caller
     */
    public WebsockQuery handleCall(final WebsockQuery query)
    {
        WebsockQuery response = null;
        IStoredProcedure proc = fProcedures.get(query.getPayload());

        if(proc == null)
        {
            fLogger.log(Level.WARNING, "call to missing procedure '"
                + query.getPayload() + "'");
            response = new WebsockQuery(query.getId(), EQueryType.ERROR);
            response.setPayload("stored procedure '" + query.getPayload()
                + "' not found");
            return response;
        }

        try
        {
            if(fDebug)
            {
                fLogger.log(Level.FINEST, "call to procedure '"
                    + query.getPayload() + "' (" + query.getParameters()
                    + ")");
            }

            AResultSet<?> result = proc.call(query.getParameters());

            if(result != null)
            {
                response = new WebsockQuery(query.getId(), EQueryType.RESULT);

                Map<String, Object> map = fImpl.newMap();
                response.setPayload(ResultSetConverter.toMap(result, map));
            }
            else
            {
                response = new WebsockQuery(query.getId(), EQueryType.SUCCESS);
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fLogger.log(Level.SEVERE, "error during query execution", e);
            response = new WebsockQuery(query.getId(), EQueryType.ERROR);
            response.setPayload(e.toString());
        }

        return response;
    }

    /**
     * Deletes the procedure named in the given request.
     * The given request must not be null and must contain a valid procedure
     * name.
     *
     * @param query deletion request
     * @return response for caller
     */
    public WebsockQuery deleteProcedure(WebsockQuery query)
    {
        String name = query.getPayload().toString();

        if(fDebug)
        {
            fLogger.log(Level.FINEST, "deleting procedure '"
                + query.getPayload() + "'");
        }

        fProcedures.remove(name);

        WebsockQuery respsonse = new WebsockQuery(query.getId(),
            EQueryType.SUCCESS);
        return respsonse;
    }

    /**
     * Creates a new stored procedure based on the contained Cypher statement
     * under the name specified.
     * Query must not be null and must contain a valid name and Cyoher query.
     *
     * @param query new procedure request
     * @return response for caller
     */
    public WebsockQuery storeProcedure(WebsockQuery query)
    {
        String name = query.getParameters().get(
            WebsockConstants.PROCEDURE_NAME).toString();
        String statement = query.getPayload().toString();

        //TODO: synchronization?

        //TODO: regulate overwriting?

        IStoredProcedure procedure = new CypherProcedure(name, fDb, statement);
        fProcedures.put(name, procedure);

        WebsockQuery respsonse = new WebsockQuery(query.getId(),
            EQueryType.SUCCESS);
        return respsonse;
    }
}
