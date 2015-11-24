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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.GraphConfig;
import de.hofuniversity.iisys.neo4j.websock.WebsockContextHandler;
import de.hofuniversity.iisys.neo4j.websock.calls.IStoredProcedure;
import de.hofuniversity.iisys.neo4j.websock.result.ListResult;
import de.hofuniversity.iisys.neo4j.websock.result.SingleResult;
import de.hofuniversity.iisys.neo4j.websock.result.TableResult;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Tests for the Guice-based Java stored procedure loader.
 */
public class GuiceProcedureLoaderTest
{
    private static final String TEST_FILE =
        "src/test/resources/native_procedures.ini";

    private GraphDatabaseService fDb;

    /**
     * Sets up an impermanent graph database.
     */
    @Before
    public void setupDb()
    {
        TestGraphDatabaseFactory fact = new TestGraphDatabaseFactory();
        fDb = fact.newImpermanentDatabase();

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                if (fDb != null)
                {
                    fDb.shutdown();
                }
            }
        });
    }

    /**
     * Shuts down the impermanent graph database.
     */
    @After
    public void shutdownDb()
    {
        fDb.shutdown();
    }

    /**
     * Tests loading stored Java procedures.
     */
    @Test
    public void procLoadTest()
    {
        //setup test configuration
        GraphConfig config = new GraphConfig(true);
        config.setProperty(GuiceProcedureLoader.NATIVE_FILES, TEST_FILE);
        ImplUtil impl = new ImplUtil(ArrayList.class, HashMap.class);

        //load procedures
        new WebsockContextHandler().initTestContext(config);
        GuiceProcedureLoader loader = new GuiceProcedureLoader(fDb, impl);
        Map<String, IStoredProcedure> procedures = loader.getProcedures();

        //verify
        //getConfig
        IStoredProcedure proc =
            procedures.get(NativeTestProcedures.GET_CONFIG);
        Assert.assertEquals(NativeTestProcedures.GET_CONFIG, proc.getName());

        Map<String, Object> parameters = new HashMap<String, Object>();
        SingleResult sRes = (SingleResult) proc.call(parameters);

        Map<String, ?> resMap = sRes.getResults();
        Assert.assertNull(resMap.get(NativeTestProcedures.MISS_PARAM));
        Assert.assertEquals(config, resMap.get(NativeTestProcedures.CONFIG));

        //getDatabase
        proc = procedures.get(NativeTestProcedures.GET_DATABASE);
        Assert.assertEquals(NativeTestProcedures.GET_DATABASE, proc.getName());

        parameters.put(NativeTestProcedures.STRING_PARAM, "string");
        parameters.put(NativeTestProcedures.INTEGER_PARAM, 42);
        ListResult lRes = (ListResult) proc.call(parameters);

        List<?> resList = lRes.getResults();
        Assert.assertEquals(fDb, resList.get(0));
        Assert.assertEquals("string", resList.get(1));
        Assert.assertEquals(42, resList.get(2));

        //getImplUtil
        proc = procedures.get(NativeTestProcedures.GET_IMPL_UTIL);
        Assert.assertEquals(NativeTestProcedures.GET_IMPL_UTIL, proc.getName());

        parameters.clear();
        List<Object> listParam = new ArrayList<Object>();
        parameters.put(NativeTestProcedures.LIST_PARAM, listParam);
        TableResult tRes = (TableResult) proc.call(parameters);

        List<Object> row = tRes.getResults().get(0);

        int implIndex = tRes.getColumnIndex(NativeTestProcedures.IMPL_UTIL);
        int listIndex = tRes.getColumnIndex(NativeTestProcedures.LIST_PARAM);
        int optsIndex = tRes.getColumnIndex(WebsockConstants.OPTIONS_MAP);

        Assert.assertEquals(impl, row.get(implIndex));
        Assert.assertEquals(listParam, row.get(listIndex));
        Assert.assertEquals(parameters, row.get(optsIndex));
    }
}
