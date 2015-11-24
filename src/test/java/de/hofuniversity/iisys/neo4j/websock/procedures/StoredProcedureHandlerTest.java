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

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.junit.After;
import org.junit.Assert;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.calls.IStoredProcedure;
import de.hofuniversity.iisys.neo4j.websock.procedures.StoredProcedureHandler;
import de.hofuniversity.iisys.neo4j.websock.query.EQueryType;
import de.hofuniversity.iisys.neo4j.websock.query.WebsockQuery;
import de.hofuniversity.iisys.neo4j.websock.result.AResultSet;
import de.hofuniversity.iisys.neo4j.websock.result.SingleResult;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Test for the handler for stored procedures.
 */
public class StoredProcedureHandlerTest
{
    private static final String NAME_FIELD = "name";

    private static final String PROCEDURE_1 = "proc1";
    private static final String PROCEDURE_2 = "proc2";
    private static final String PROCEDURE_3 = "proc3";
    private static final String PROCEDURE_4 = "proc4";

    private GraphDatabaseService fDb;

    private StoredProcedureHandler setupHandler()
    {
        TestGraphDatabaseFactory fact = new TestGraphDatabaseFactory();
        fDb = fact.newImpermanentDatabase();

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                if(fDb != null)
                {
                    fDb.shutdown();
                }
            }
        });

        Map<String, IStoredProcedure> procedures =
            new HashMap<String,IStoredProcedure>();
        TestProcedure proc = new TestProcedure(PROCEDURE_1, false, false);
        procedures.put(PROCEDURE_1, proc);
        proc = new TestProcedure(PROCEDURE_2, false, true);
        procedures.put(PROCEDURE_2, proc);
        proc = new TestProcedure(PROCEDURE_3, true, false);
        procedures.put(PROCEDURE_3, proc);

        return new StoredProcedureHandler(fDb, procedures, new ImplUtil(
            BasicBSONList.class, BasicBSONObject.class));
    }

    @After
    public void stopDatabase()
    {
        fDb.shutdown();
    }

    /**
     * Tests valid calls to valid procedures.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCall()
    {
        StoredProcedureHandler handler = setupHandler();

        //create and execute call, no return value
        WebsockQuery query = new WebsockQuery(EQueryType.PROCEDURE_CALL);
        query.setPayload(PROCEDURE_1);

        WebsockQuery response = handler.handleCall(query);
        Assert.assertEquals(EQueryType.SUCCESS, response.getType());
        Assert.assertNull(response.getPayload());

        //create and execute call, with return value
        query = new WebsockQuery(EQueryType.PROCEDURE_CALL);
        query.setPayload(PROCEDURE_2);

        response = handler.handleCall(query);
        Assert.assertEquals(EQueryType.RESULT, response.getType());
        Map<String, Object> set = (Map<String, Object>) response.getPayload();
        Map<String, ?> map = (Map<String, ?>) set.get(WebsockConstants.RESULT);
        Assert.assertEquals(PROCEDURE_2, map.get(NAME_FIELD));
    }

    /**
     * Tests calls to missing procedures and calls with occurring exceptions.
     */
    @Test
    public void testFaultyCalls()
    {
        StoredProcedureHandler handler = setupHandler();

        //call to missing procedure
        WebsockQuery query = new WebsockQuery(EQueryType.PROCEDURE_CALL);
        query.setPayload(PROCEDURE_4);
        WebsockQuery response = handler.handleCall(query);
        Assert.assertEquals(EQueryType.ERROR, response.getType());

        //call causing an exception
        query = new WebsockQuery(EQueryType.PROCEDURE_CALL);
        query.setPayload(PROCEDURE_3);
        response = handler.handleCall(query);
        Assert.assertEquals(EQueryType.ERROR, response.getType());
    }

    /**
     * Tests the storing of procedures.
     */
    @Test
    public void testStoring()
    {
        StoredProcedureHandler handler = setupHandler();

        //store new procedure
        String cypher = "START n=node(*) RETURN n";
        WebsockQuery query = new WebsockQuery(EQueryType.STORE_PROCEDURE);
        query.setPayload(cypher);
        query.setParameter(WebsockConstants.PROCEDURE_NAME, PROCEDURE_4);
        handler.storeProcedure(query);

        //execute and check
        query = new WebsockQuery(EQueryType.PROCEDURE_CALL);
        query.setPayload(PROCEDURE_2);

        WebsockQuery response = handler.handleCall(query);
        Assert.assertEquals(EQueryType.RESULT, response.getType());
        Assert.assertNotNull(response.getPayload());

        //overwrite existing procedure
        query.setParameter(WebsockConstants.PROCEDURE_NAME, PROCEDURE_1);
        handler.storeProcedure(query);

        //execute and check
        response = handler.handleCall(query);
        Assert.assertEquals(EQueryType.RESULT, response.getType());
        Assert.assertNotNull(response.getPayload());
    }

    /**
     * Tests the deletion of stored procedures.
     */
    @Test
    public void testDeletion()
    {
        StoredProcedureHandler handler = setupHandler();

        //delete existing procedure
        WebsockQuery query = new WebsockQuery(EQueryType.DELETE_PROCEDURE);
        query.setPayload(PROCEDURE_1);
        handler.deleteProcedure(query);

        //call and check
        query = new WebsockQuery(EQueryType.PROCEDURE_CALL);
        query.setPayload(PROCEDURE_1);
        WebsockQuery response = handler.handleCall(query);
        Assert.assertEquals(EQueryType.ERROR, response.getType());

        //delete non-existent procedure - should not throw an exception
        query = new WebsockQuery(EQueryType.DELETE_PROCEDURE);
        query.setPayload(PROCEDURE_4);
        handler.handleCall(query);
    }

    private class TestProcedure implements IStoredProcedure
    {
        private final String fName;
        private final boolean fFail;
        private final boolean fRetVal;

        public TestProcedure(String name, boolean fail, boolean retVal)
        {
            fName = name;
            fFail = fail;
            fRetVal = retVal;
        }

        @Override
        public String getName()
        {
            return fName;
        }

        @Override
        public AResultSet<?> call(Map<String, Object> parameters)
        {
            if(fFail)
            {
                throw new IllegalArgumentException("bogus error message");
            }

            if(fRetVal)
            {
                Map<String, Object> resMap = new HashMap<String, Object>();
                resMap.put(NAME_FIELD, fName);

                return new SingleResult(resMap);
            }

            return null;
        }

        @Override
        public boolean isNative()
        {
            return true;
        }
    }
}
