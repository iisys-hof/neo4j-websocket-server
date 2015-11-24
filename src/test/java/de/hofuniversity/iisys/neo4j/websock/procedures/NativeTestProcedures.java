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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;

import com.google.inject.Inject;

import de.hofuniversity.iisys.neo4j.websock.GraphConfig;
import de.hofuniversity.iisys.neo4j.websock.calls.IStoredProcedure;
import de.hofuniversity.iisys.neo4j.websock.calls.NativeProcedure;
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.result.SingleResult;
import de.hofuniversity.iisys.neo4j.websock.result.TableResult;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Procedure class for testing the reflection based native procedure loader.
 */
public class NativeTestProcedures implements IProcedureProvider
{
    public static final String GET_CONFIG = "getConfig";
    public static final String GET_DATABASE = "getDatabase";
    public static final String GET_IMPL_UTIL = "getImplUtil";

    public static final String CONFIG = "config";
    public static final String DATABASE = "database";
    public static final String IMPL_UTIL = "implUtil";

    public static final String STRING_PARAM = "string";
    public static final String INTEGER_PARAM = "int";
    public static final String MISS_PARAM = "missing";
    public static final String LIST_PARAM = "list";

    private final GraphConfig fConfig;
    private final GraphDatabaseService fDb;
    private final ImplUtil fImpl;

    /**
     * Creates a new native test procedures provider, storing the injected
     * objects.
     *
     * @param config server configuration
     * @param database database service
     * @param impl implementation utility
     */
    @Inject
    public NativeTestProcedures(GraphConfig config,
        GraphDatabaseService database, ImplUtil impl)
    {
        fConfig = config;
        fDb = database;
        fImpl = impl;
    }

    @Override
    public Map<String, IStoredProcedure> getProcedures()
    {
        Map<String, IStoredProcedure> procedures =
            new HashMap<String, IStoredProcedure>();
        try
        {
            //getConfig
            Method method = NativeTestProcedures.class.getMethod(
                GET_CONFIG, Object.class);

            List<String> paramNames = new ArrayList<String>();
            paramNames.add(MISS_PARAM);

            NativeProcedure proc = new NativeProcedure(GET_CONFIG, this, method,
                paramNames);
            procedures.put(GET_CONFIG, proc);

            //getDatabase
            method = NativeTestProcedures.class.getMethod(
                GET_DATABASE, String.class, Integer.TYPE);

            paramNames = new ArrayList<String>();
            paramNames.add(STRING_PARAM);
            paramNames.add(INTEGER_PARAM);

            proc = new NativeProcedure(GET_DATABASE, this, method,
                paramNames);
            procedures.put(GET_DATABASE, proc);

            //getImplUtil
            method = NativeTestProcedures.class.getMethod(
                GET_IMPL_UTIL, List.class, Map.class);

            paramNames = new ArrayList<String>();
            paramNames.add(LIST_PARAM);
            paramNames.add(WebsockConstants.OPTIONS_MAP);

            proc = new NativeProcedure(GET_IMPL_UTIL, this, method,
                paramNames);
            procedures.put(GET_IMPL_UTIL, proc);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return procedures;
    }

    /**
     * @param missing parameter for testing missing parameter values
     * @return single map result with the configuration object
     */
    public SingleResult getConfig(Object missing)
    {
        Map<String, Object> resMap = new HashMap<String, Object>();
        resMap.put(CONFIG, fConfig);
        resMap.put(MISS_PARAM, missing);

        return new SingleResult(resMap);
    }

    /**
     * @param param1 String parameter
     * @param param2 integer parameter
     * @return
     *  list result with the database, the string and the integer parameter
     */
    public ListResult getDatabase(String param1, int param2)
    {
        List<Object> list = new ArrayList<Object>();
        list.add(fDb);
        list.add(param1);
        list.add(param2);

        return new ListResult(list);
    }

    /**
     * @param param3 list parameter
     * @param options the options map
     * @return
     *  table result with the implementation utility and the parameters in one
     *  row
     */
    public TableResult getImplUtil(List<Object> param3,
        Map<String, Object> options)
    {
        List<String> columns = new ArrayList<String>();
        columns.add(IMPL_UTIL);
        columns.add(LIST_PARAM);
        columns.add(WebsockConstants.OPTIONS_MAP);

        List<List<Object>> rows = new ArrayList<List<Object>>();
        List<Object> row = new ArrayList<Object>();
        row.add(fImpl);
        row.add(param3);
        row.add(options);
        rows.add(row);

        return new TableResult(columns, rows);
    }
}
