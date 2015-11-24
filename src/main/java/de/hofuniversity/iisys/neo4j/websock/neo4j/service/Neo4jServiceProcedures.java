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
package de.hofuniversity.iisys.neo4j.websock.neo4j.service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.graphdb.GraphDatabaseService;

import de.hofuniversity.iisys.neo4j.websock.calls.IStoredProcedure;
import de.hofuniversity.iisys.neo4j.websock.calls.NativeProcedure;
import de.hofuniversity.iisys.neo4j.websock.procedures.IProcedureProvider;
import de.hofuniversity.iisys.neo4j.websock.service.Neo4jServiceQueries;

/**
 * Class creating stored procedures for Neo4j utility services using
 * reflection.
 */
public class Neo4jServiceProcedures implements IProcedureProvider
{
    private final GraphDatabaseService fDb;

    private final Logger fLogger;

    /**
     * Creates an instance of the Neo4j service method procedure generator
     * using the given database service.
     * The given service must not be null.
     *
     * @param database database service to use
     */
    public Neo4jServiceProcedures(GraphDatabaseService database)
    {
        if(database == null)
        {
            throw new NullPointerException("Neo4j database service was null");
        }

        fDb = database;

        fLogger = Logger.getLogger(this.getClass().getName());
    }

    @Override
    public Map<String, IStoredProcedure> getProcedures()
    {
        final Map<String, IStoredProcedure> procedures =
            new HashMap<String, IStoredProcedure>();

        try
        {
            addIdService(procedures);
            addIndexService(procedures);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fLogger.log(Level.SEVERE,
                "could not create Neo4j utility procedures", e);
        }

        return procedures;
    }

    private void addIdService(
        final Map<String, IStoredProcedure> procedures) throws Exception
    {
        final IDManager idMan = new IDManager(fDb);

        //requestId
        Method genId = IDManager.class.getMethod(
            Neo4jServiceQueries.GET_UID_METHOD, String.class);

        List<String> paramNames = new ArrayList<String>();
        paramNames.add(Neo4jServiceQueries.TYPE);

        IStoredProcedure proc = new NativeProcedure(
            Neo4jServiceQueries.GET_UID_METHOD, idMan, genId, paramNames);
        procedures.put(Neo4jServiceQueries.GET_UID_QUERY, proc);
    }

    private void addIndexService(
        final Map<String, IStoredProcedure> procedures) throws Exception
    {
        final IndexManager idxMan = new IndexManager(fDb);

        //createIndexEntry
        Method createIdxEntry = IndexManager.class.getMethod(
            Neo4jServiceQueries.CREATE_INDEX_ENTRY_METHOD, String.class,
            Long.class, String.class, String.class);

        List<String> paramNames = new ArrayList<String>();
        paramNames.add(Neo4jServiceQueries.INDEX);
        paramNames.add(Neo4jServiceQueries.NODE_ID);
        paramNames.add(Neo4jServiceQueries.KEY);
        paramNames.add(Neo4jServiceQueries.VALUE);

        IStoredProcedure proc = new NativeProcedure(
            Neo4jServiceQueries.CREATE_INDEX_ENTRY_METHOD, idxMan,
            createIdxEntry, paramNames);
        procedures.put(Neo4jServiceQueries.CREATE_INDEX_ENTRY_QUERY, proc);

        //deleteIndexEntry
        Method deleteIdxEntry = IndexManager.class.getMethod(
            Neo4jServiceQueries.DELETE_INDEX_ENTRY_METHOD, String.class,
            Long.class, String.class, String.class);

        paramNames = new ArrayList<String>();
        paramNames.add(Neo4jServiceQueries.INDEX);
        paramNames.add(Neo4jServiceQueries.NODE_ID);
        paramNames.add(Neo4jServiceQueries.KEY);
        paramNames.add(Neo4jServiceQueries.VALUE);

        proc = new NativeProcedure(
            Neo4jServiceQueries.DELETE_INDEX_ENTRY_METHOD, idxMan,
            deleteIdxEntry, paramNames);
        procedures.put(Neo4jServiceQueries.DELETE_INDEX_ENTRY_QUERY, proc);

        //clearIndex
        Method clearIdx = IndexManager.class.getMethod(
            Neo4jServiceQueries.CLEAR_INDEX_METHOD, String.class);

        paramNames = new ArrayList<String>();
        paramNames.add(Neo4jServiceQueries.INDEX);

        proc = new NativeProcedure(
            Neo4jServiceQueries.CLEAR_INDEX_METHOD, idxMan, clearIdx,
            paramNames);
        procedures.put(Neo4jServiceQueries.CLEAR_INDEX_QUERY, proc);
    }
}
