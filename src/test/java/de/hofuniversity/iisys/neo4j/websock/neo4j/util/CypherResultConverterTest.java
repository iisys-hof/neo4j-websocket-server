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
package de.hofuniversity.iisys.neo4j.websock.neo4j.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;

import org.junit.Test;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jRelTypes;
import de.hofuniversity.iisys.neo4j.websock.result.TableResult;

/**
 * Tests the conversion of Cypher execution results to standard table results.
 */
public class CypherResultConverterTest
{
    private final static String INDEX_NAME = "things";

    private static final String ID_FIELD = "id";
    private static final String JOHN_ID = "john.doe";

    private static final String USER_ID_FIELD = "userId";
    private static final String TITLE_FIELD = "title";
    private static final String BODY_FIELD = "body";
    private static final String TIME_FIELD = "time";
    private static final String OPTIONAL_FIELD = "optional";

    private static final String STRING_ARRAY_FIELD = "stringArr";
    private static final String BYTE_ARRAY_FIELD = "byteArr";

    private static final String ID_PARAM = "idParam";

    private static final long TIME_1 = 0;
    private static final long TIME_2 = 24*3600000L;
    private static final long TIME_3 = 72*3600000L;
    private static final long TIME_4 = 164*3600000L;

    private static final String TITLE_1 = "GmbH gegründet";
    private static final String TITLE_2 = "Jane als Sektretärin eingestellt";
    private static final String TITLE_3 = "Horst als Hausmeister eingestellt";
    private static final String TITLE_4 = "Firma plötzlich sauber";

    private static final String[] STRING_ARR_1 =
        {"string", "another", "third"};
    private static final String[] STRING_ARR_2 =
        {"characters", "more", "additional"};
    private static final String[] STRING_ARR_3 =
        {"letters", "words", "texts"};
    private static final String[] STRING_ARR_4 =
        {"arrays", "lists", "collections"};

    private static final byte[] BYTE_ARR_1 = {123, 45, 68};
    private static final byte[] BYTE_ARR_2 = {91, 111, 21};
    private static final byte[] BYTE_ARR_3 = {31, 41, 51};
    private static final byte[] BYTE_ARR_4 = {61, 71, 81};

    private static final String AUX_1 = "aux1";
    private static final String AUX_2 = "aux2";
    private static final String AUX_3 = "aux3";

    private GraphDatabaseService fDb;

    private ExecutionEngine getEngine(boolean withData)
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

        if(withData)
        {
            createTestData(fDb);
        }

        return new ExecutionEngine(fDb);
    }

    @After
    public void stopDatabase()
    {
        fDb.shutdown();
    }

    private void createTestData(GraphDatabaseService db)
    {
        Index<Node> testNodes = db.index().forNodes(INDEX_NAME);

        Transaction trans = db.beginTx();

        //person node
        Node johndoe = db.createNode();
        johndoe.setProperty(ID_FIELD, JOHN_ID);
        testNodes.add(johndoe, ID_FIELD, JOHN_ID);

        //some activities with extra data
        Node foundAct = db.createNode();
        foundAct.setProperty(TIME_FIELD, TIME_1);
        foundAct.setProperty(USER_ID_FIELD, JOHN_ID);
        foundAct.setProperty(ID_FIELD,
            "1");
        foundAct.setProperty(TITLE_FIELD, TITLE_1);
        foundAct.setProperty(BODY_FIELD,
            "Habe die Pfusch und Bastel GmbH gegründet");

        foundAct.setProperty(STRING_ARRAY_FIELD, STRING_ARR_1);
        foundAct.setProperty(BYTE_ARRAY_FIELD, BYTE_ARR_1);

        johndoe.createRelationshipTo(foundAct, Neo4jRelTypes.ACTED);


        Node hireJaneAct = db.createNode();
        hireJaneAct.setProperty(TIME_FIELD, TIME_2);
        hireJaneAct.setProperty(USER_ID_FIELD, JOHN_ID);
        hireJaneAct.setProperty(ID_FIELD, "2");
        hireJaneAct.setProperty(TITLE_FIELD, TITLE_2);
        hireJaneAct.setProperty(OPTIONAL_FIELD, OPTIONAL_FIELD);

        hireJaneAct.setProperty(STRING_ARRAY_FIELD, STRING_ARR_2);
        hireJaneAct.setProperty(BYTE_ARRAY_FIELD, BYTE_ARR_2);

        johndoe.createRelationshipTo(hireJaneAct, Neo4jRelTypes.ACTED);


        Node hireHorstAct = db.createNode();
        hireHorstAct.setProperty(TIME_FIELD, TIME_3);
        hireHorstAct.setProperty(USER_ID_FIELD, JOHN_ID);
        hireHorstAct.setProperty(ID_FIELD, "3");
        hireHorstAct.setProperty(TITLE_FIELD, TITLE_3);

        hireHorstAct.setProperty(STRING_ARRAY_FIELD, STRING_ARR_3);
        hireHorstAct.setProperty(BYTE_ARRAY_FIELD, BYTE_ARR_3);

        johndoe.createRelationshipTo(hireHorstAct, Neo4jRelTypes.ACTED);


        Node surpriseAct = db.createNode();
        surpriseAct.setProperty(TIME_FIELD, TIME_4);
        surpriseAct.setProperty(USER_ID_FIELD, JOHN_ID);
        surpriseAct.setProperty(ID_FIELD, "4");
        surpriseAct.setProperty(TITLE_FIELD, TITLE_4);

        surpriseAct.setProperty(STRING_ARRAY_FIELD, STRING_ARR_4);
        surpriseAct.setProperty(BYTE_ARRAY_FIELD, BYTE_ARR_4);

        johndoe.createRelationshipTo(surpriseAct, Neo4jRelTypes.ACTED);

        //auxiliary nodes to be collected
        Node aux1 = db.createNode();
        aux1.setProperty(ID_FIELD, AUX_1);
        Node aux2 = db.createNode();
        aux2.setProperty(ID_FIELD, AUX_2);
        Node aux3 = db.createNode();
        aux3.setProperty(ID_FIELD, AUX_3);

        hireJaneAct.createRelationshipTo(aux1, Neo4jRelTypes.HAS_DATA);
        hireJaneAct.createRelationshipTo(aux2, Neo4jRelTypes.HAS_DATA);
        hireJaneAct.createRelationshipTo(aux3, Neo4jRelTypes.HAS_DATA);

        trans.success();
        trans.finish();
    }

    /**
     * Tests the conversion of filled execution results with pagination.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void tableResultTest()
    {
        ExecutionEngine engine = getEngine(true);

        //build query string
        String query = "START n=node:" + INDEX_NAME + "(" + ID_FIELD + "={"
            + ID_PARAM + "})\n"
            + "MATCH n-[:" + Neo4jRelTypes.ACTED + "]->a, "
            + "a-[?:" + Neo4jRelTypes.HAS_DATA + "]-aux\n"
            + "RETURN a, a." + TIME_FIELD
            + ", a." + TITLE_FIELD
            + ", a." + OPTIONAL_FIELD  + "?"
            + ", a." + STRING_ARRAY_FIELD
            + ", a." + BYTE_ARRAY_FIELD
            + ", COLLECT(aux.id?)"
            + ", COLLECT(aux)" + "\n"
            + "ORDER BY a." + ID_FIELD + ";";

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put(ID_PARAM, JOHN_ID);
        ExecutionResult result = engine.execute(query, parameters);

        TableResult table = CypherResultConverter.toTableResult(result, 1, 3);
        Assert.assertNotNull(table);
        Assert.assertEquals(1, table.getFirst());
        Assert.assertEquals(2, table.getMax());
        Assert.assertEquals(2, table.getSize());
        Assert.assertEquals(4, table.getTotal());

        Assert.assertEquals(0, table.getColumnIndex("a"));
        Assert.assertEquals(1, table.getColumnIndex("a." + TIME_FIELD));
        Assert.assertEquals(2, table.getColumnIndex("a." + TITLE_FIELD));
        Assert.assertEquals(3, table.getColumnIndex("a." + OPTIONAL_FIELD
            + "?"));
        Assert.assertEquals(4, table.getColumnIndex("a." + STRING_ARRAY_FIELD));
        Assert.assertEquals(5, table.getColumnIndex("a." + BYTE_ARRAY_FIELD));
        Assert.assertEquals(6, table.getColumnIndex("COLLECT(aux.id?)"));
        Assert.assertEquals(7, table.getColumnIndex("COLLECT(aux)"));

        List<List<Object>> results = table.getResults();

        //node converted to map
        Assert.assertTrue(results.get(0).get(0) instanceof Map<?, ?>);
        Map<String, Object> nodeMap =
            (Map<String, Object>) results.get(0).get(0);
        Assert.assertEquals(TIME_2, nodeMap.get(TIME_FIELD));
        Assert.assertEquals(TITLE_2, nodeMap.get(TITLE_FIELD));

        //simple properties
        Assert.assertEquals(TIME_2, results.get(0).get(1));
        Assert.assertEquals(TIME_3, results.get(1).get(1));

        Assert.assertEquals(TITLE_2, results.get(0).get(2));
        Assert.assertEquals(TITLE_3, results.get(1).get(2));

        //optional values
        Assert.assertEquals(OPTIONAL_FIELD, results.get(0).get(3));
        Assert.assertNull(results.get(1).get(3));

        //arrays
        Assert.assertArrayEquals(STRING_ARR_2,
            (String[]) results.get(0).get(4));
        Assert.assertArrayEquals(STRING_ARR_3,
            (String[]) results.get(1).get(4));

        Assert.assertArrayEquals(BYTE_ARR_2, (byte[]) results.get(0).get(5));
        Assert.assertArrayEquals(BYTE_ARR_3, (byte[]) results.get(1).get(5));

        //collections
        //values
        List<String> stringList = (List<String>) results.get(0).get(6);
        Assert.assertEquals(AUX_1, stringList.get(0));
        Assert.assertEquals(AUX_2, stringList.get(1));
        Assert.assertEquals(AUX_3, stringList.get(2));

        stringList = (List<String>) results.get(1).get(6);
        Assert.assertEquals(0, stringList.size());

        //nodes
        List<Map<String, Object>> nodeList =
            (List<Map<String, Object>>) results.get(0).get(7);
        Assert.assertEquals(3, nodeList.size());
        Assert.assertEquals(AUX_1, nodeList.get(0).get(ID_FIELD));
        Assert.assertEquals(AUX_2, nodeList.get(1).get(ID_FIELD));
        Assert.assertEquals(AUX_3, nodeList.get(2).get(ID_FIELD));

        nodeList = (List<Map<String, Object>>) results.get(1).get(7);
        Assert.assertEquals(0, nodeList.size());
    }

    /**
     * Tests the conversion of empty result sets.
     */
    @Test
    public void emptyResultTest()
    {
        ExecutionEngine engine = getEngine(false);

        String query = "START n=node(*) WHERE has(n.x) RETURN n";
        ExecutionResult result = engine.execute(query);

        TableResult table = CypherResultConverter.toTableResult(result, 0, 0);
        Assert.assertNotNull(table);
        Assert.assertEquals(0, table.getFirst());
        Assert.assertEquals(-1, table.getMax());
        Assert.assertEquals(0, table.getSize());
        Assert.assertEquals(0, table.getTotal());
        Assert.assertEquals(0, table.getColumnIndex("n"));
    }
}
