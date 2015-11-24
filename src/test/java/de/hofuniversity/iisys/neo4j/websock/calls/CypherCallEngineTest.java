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
package de.hofuniversity.iisys.neo4j.websock.calls;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;

import org.bson.BasicBSONObject;
import org.bson.types.BasicBSONList;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.query.EQueryType;
import de.hofuniversity.iisys.neo4j.websock.query.WebsockQuery;
import de.hofuniversity.iisys.neo4j.websock.result.AResultSet;
import de.hofuniversity.iisys.neo4j.websock.result.TableResult;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;
import de.hofuniversity.iisys.neo4j.websock.util.ResultSetConverter;

/**
 * Tests the Cypher call engine executing Cypher queries coming directly from
 * the client.
 */
public class CypherCallEngineTest
{
    private final static String INDEX_NAME = "things";

    private static final String ID_FIELD = "id";
    private static final String JOHN_ID = "john.doe", JANE_ID = "jane.doe",
        HORST_ID = "horst";

    private static final String USER_ID_FIELD = "userId";
    private static final String TITLE_FIELD = "title";
    private static final String BODY_FIELD = "body";
    private static final String TIME_FIELD = "time";

    private static final String ID_PARAM = "idParam";

    private GraphDatabaseService fDb;

    private CypherCallEngine setupService()
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

        createTestData(fDb);

        CypherCallEngine engine = new CypherCallEngine(fDb,
            new ImplUtil(BasicBSONList.class, BasicBSONObject.class));
        return engine;
    }

    @After
    public void stopDatabase()
    {
        fDb.shutdown();
    }

    private void createTestData(final GraphDatabaseService db)
    {
        Index<Node> testNodes = db.index().forNodes(INDEX_NAME);

        Transaction trans = db.beginTx();

        //some people
        Node johndoe = db.createNode();
        johndoe.setProperty(ID_FIELD, JOHN_ID);
        testNodes.add(johndoe, ID_FIELD, JOHN_ID);

        Node janedoe = db.createNode();
        janedoe.setProperty(ID_FIELD, JANE_ID);
        testNodes.add(janedoe, ID_FIELD, JANE_ID);

        Node horst = db.createNode();
        horst.setProperty(ID_FIELD, HORST_ID);
        testNodes.add(horst, ID_FIELD, HORST_ID);

        //some activities
        Node foundAct = db.createNode();
        foundAct.setProperty(TIME_FIELD, new Long(0));
        foundAct.setProperty(USER_ID_FIELD, JOHN_ID);
        foundAct.setProperty(ID_FIELD,
            "1");
        foundAct.setProperty(TITLE_FIELD, "GmbH gegründet");
        foundAct.setProperty(BODY_FIELD,
            "Habe die Pfusch und Bastel GmbH gegründet");

        johndoe.createRelationshipTo(foundAct, Neo4jRelTypes.ACTED);


        Node hireJaneAct = db.createNode();
        hireJaneAct.setProperty(TIME_FIELD, new Long(24*3600000L));
        hireJaneAct.setProperty(USER_ID_FIELD, JOHN_ID);
        hireJaneAct.setProperty(ID_FIELD, "2");
        hireJaneAct.setProperty(TITLE_FIELD,
            "Jane als Sektretärin eingestellt");

        johndoe.createRelationshipTo(hireJaneAct, Neo4jRelTypes.ACTED);


        Node janeHiredAct = db.createNode();
        janeHiredAct.setProperty(TIME_FIELD, new Long(32*3600000L));
        janeHiredAct.setProperty(USER_ID_FIELD, JANE_ID);
        janeHiredAct.setProperty(ID_FIELD, "3");
        janeHiredAct.setProperty(TITLE_FIELD,
            "wurde bei der PuB GmbH eingestellt");

        janedoe.createRelationshipTo(janeHiredAct, Neo4jRelTypes.ACTED);


        Node hireHorstAct = db.createNode();
        hireHorstAct.setProperty(TIME_FIELD, new Long(144*3600000L));
        hireHorstAct.setProperty(USER_ID_FIELD, JANE_ID);
        hireHorstAct.setProperty(ID_FIELD, "4");
        hireHorstAct.setProperty(TITLE_FIELD,
            "Horst als Hausmeister eingestellt");

        janedoe.createRelationshipTo(hireHorstAct, Neo4jRelTypes.ACTED);


        Node horstHiredAct = db.createNode();
        horstHiredAct.setProperty(TIME_FIELD, new Long(156*3600000L));
        horstHiredAct.setProperty(USER_ID_FIELD, HORST_ID);
        horstHiredAct.setProperty(ID_FIELD, "5");
        horstHiredAct.setProperty(TITLE_FIELD,
            "Bei Pfusch&Bastel GmbH eingestellt");

        horst.createRelationshipTo(horstHiredAct, Neo4jRelTypes.ACTED);


        Node cleanedAct = db.createNode();
        cleanedAct.setProperty(TIME_FIELD, new Long(160*3600000L));
        cleanedAct.setProperty(USER_ID_FIELD, HORST_ID);
        cleanedAct.setProperty(ID_FIELD, "6");
        cleanedAct.setProperty(TITLE_FIELD, "Firma sauber gemacht");

        horst.createRelationshipTo(cleanedAct, Neo4jRelTypes.ACTED);


        Node surpriseAct = db.createNode();
        surpriseAct.setProperty(TIME_FIELD, new Long(164*3600000L));
        surpriseAct.setProperty(USER_ID_FIELD, JOHN_ID);
        surpriseAct.setProperty(ID_FIELD, "7");
        surpriseAct.setProperty(TITLE_FIELD, "Firma plötzlich sauber");

        johndoe.createRelationshipTo(surpriseAct, Neo4jRelTypes.ACTED);

        trans.success();
        trans.finish();
    }

    /**
     * Tests a normal Cypher query and checks its results.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testCall()
    {
        CypherCallEngine engine = setupService();

        //build query string
        String query = "START n=node:" + INDEX_NAME + "(" + ID_FIELD + "={"
            + ID_PARAM + "})\n"
            + "MATCH n-[:" + Neo4jRelTypes.ACTED.toString() + "]->a\n"
            + "RETURN a, a." + ID_FIELD + ", a." + TITLE_FIELD + ";";

        //build websocket query with parameters
        WebsockQuery wsQuery = new WebsockQuery(EQueryType.DIRECT_CYPHER);
        wsQuery.setPayload(query);
        wsQuery.setParameter(ID_PARAM, JOHN_ID);
        wsQuery.setParameter(WebsockConstants.SUBSET_START, 1);
        wsQuery.setParameter(WebsockConstants.SUBSET_SIZE, 1);

        //execute and check result
        WebsockQuery response = engine.call(wsQuery);
        Assert.assertEquals(EQueryType.RESULT, response.getType());

        //check pagination
        AResultSet<?> set = ResultSetConverter.toResultSet(
            (Map<String, Object>) response.getPayload());
        Assert.assertEquals(1, set.getFirst());
        Assert.assertEquals(1, set.getMax());
        Assert.assertEquals(1, set.getSize());
        Assert.assertEquals(3, set.getTotal());

        //check returned row
        TableResult result = (TableResult) set;
        List<String> columns = result.getColumns();
        Assert.assertTrue(columns.contains("a"));
        Assert.assertTrue(columns.contains("a." + ID_FIELD));
        Assert.assertTrue(columns.contains("a." + TITLE_FIELD));

        List<Object> row = result.getResults().get(0);
        Assert.assertEquals("2", row.get(result.getColumnIndex(
            "a." + ID_FIELD)));
        Assert.assertEquals("Jane als Sektretärin eingestellt",
            row.get(result.getColumnIndex("a." + TITLE_FIELD)));

        //check converted node
        Map<String, Object> node = (Map<String, Object>)
            row.get(result.getColumnIndex("a"));
        Assert.assertEquals(JOHN_ID, node.get(USER_ID_FIELD));
    }

    /**
     * Tests the handler's reaction to a faulty Cypher query.
     */
    @Test
    public void testFaultyQuery()
    {
        //ready engine
        CypherCallEngine engine = setupService();

        //build query string
        String query = "START n=node:" + INDEX_NAME + "(" + ID_FIELD + "={"
            + ID_PARAM + "})\n"
            + "MATCH n-[:" + Neo4jRelTypes.ACTED.toString() + "]->a\n"
            + "RETURN a, a." + ID_FIELD + ", a." + TITLE_FIELD + ";";

        //build websocket query with missing parameters
        WebsockQuery wsQuery = new WebsockQuery(EQueryType.DIRECT_CYPHER);
        wsQuery.setPayload(query);

        //execute and check result
        WebsockQuery result = engine.call(wsQuery);
        Assert.assertEquals(EQueryType.ERROR, result.getType());

        //build faulty query String
        query = "START n=node:" + INDEX_NAME + "(" + ID_FIELD + "={"
            + ID_PARAM + "})\n"
            + "MATCH n-" + Neo4jRelTypes.ACTED.toString() + "->a\n"
            + "RETURN a, a." + ID_FIELD + ", a." + TITLE_FIELD + ";";

        //build websocket query with parameters
        wsQuery = new WebsockQuery(EQueryType.DIRECT_CYPHER);
        wsQuery.setPayload(query);
        wsQuery.setParameter(ID_PARAM, JOHN_ID);
        wsQuery.setParameter(WebsockConstants.SUBSET_START, 1);
        wsQuery.setParameter(WebsockConstants.SUBSET_SIZE, 1);

        //execute and check result
        result = engine.call(wsQuery);
        Assert.assertEquals(EQueryType.ERROR, result.getType());
    }
}
