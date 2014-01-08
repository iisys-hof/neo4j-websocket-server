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
package de.hofuniversity.iisys.neo4j.websock.neo4j.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;

/**
 * Test routine to check whether the node filtering utility is working
 * correctly.
 */
public class NodeFilterTest
{
    private static final String STRING_ATT = "string_attribute";
    private static final String INTEGER_ATT = "integer_attribute";
    private static final String INCOMP_ATT = "incomplete_attribute";
    private static final String ARRAY_ATT = "array_attribute";
    private static final String OTHER_ATT = "other_attribute";

    private final Node[] fNodes = new Node[10];
    private GraphDatabaseService fDb;

    @Before
    public void setupService()
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

        createTestData();
    }

    @After
    public void stopDatabase()
    {
        fDb.shutdown();
    }

    private void createTestData()
    {
        Transaction tx = fDb.beginTx();

        for(int i = 0; i < fNodes.length; ++i)
        {
            fNodes[i] = fDb.createNode();
        }

        fNodes[0].setProperty(STRING_ATT, "DAAD");
        fNodes[1].setProperty(STRING_ATT, "BBBB");
        fNodes[2].setProperty(STRING_ATT, "FFFF");
        fNodes[3].setProperty(STRING_ATT, "JJJJ");
        fNodes[4].setProperty(STRING_ATT, "AAAA");
        fNodes[5].setProperty(STRING_ATT, "CCCC");
        fNodes[6].setProperty(STRING_ATT, "HAAH");
        fNodes[7].setProperty(STRING_ATT, "EEEE");
        fNodes[8].setProperty(STRING_ATT, "IIII");
        fNodes[9].setProperty(STRING_ATT, "AGGG");

        fNodes[0].setProperty(INTEGER_ATT, 42);
        fNodes[1].setProperty(INTEGER_ATT, 27);
        fNodes[2].setProperty(INTEGER_ATT, 4);
        fNodes[3].setProperty(INTEGER_ATT, 17);
        fNodes[4].setProperty(INTEGER_ATT, 42);
        fNodes[5].setProperty(INTEGER_ATT, 1);
        fNodes[6].setProperty(INTEGER_ATT, 8);
        fNodes[7].setProperty(INTEGER_ATT, 72);
        fNodes[8].setProperty(INTEGER_ATT, 23);
        fNodes[9].setProperty(INTEGER_ATT, 42);

        fNodes[0].setProperty(INCOMP_ATT, "Zeug");
        fNodes[1].setProperty(INCOMP_ATT, "Dings");
        fNodes[4].setProperty(INCOMP_ATT, "stuff");
        fNodes[5].setProperty(INCOMP_ATT, "trucs");
        fNodes[6].setProperty(INCOMP_ATT, "things");
        fNodes[8].setProperty(INCOMP_ATT, "bidules");
        fNodes[9].setProperty(INCOMP_ATT, "clutter");

        fNodes[0].setProperty(ARRAY_ATT, new String[]{"1", "2"});
        fNodes[1].setProperty(ARRAY_ATT, new String[]{"3", "4"});
        fNodes[2].setProperty(ARRAY_ATT, new String[]{"5", "6"});
        fNodes[3].setProperty(ARRAY_ATT, new String[]{"7", "8"});
        fNodes[4].setProperty(ARRAY_ATT, new String[]{"9", "10"});
        fNodes[5].setProperty(ARRAY_ATT, new String[]{"11", "12"});
        fNodes[6].setProperty(ARRAY_ATT, new String[]{"13", "14"});
        fNodes[7].setProperty(ARRAY_ATT, new String[]{"15", "16"});
        fNodes[8].setProperty(ARRAY_ATT, new String[]{"17", "18"});
        fNodes[9].setProperty(ARRAY_ATT, new String[]{"19", "20"});

        fNodes[0].setProperty(OTHER_ATT, new int[]{1, 2});
        fNodes[1].setProperty(OTHER_ATT, new byte[]{3, 4});
        fNodes[2].setProperty(OTHER_ATT, new long[]{5, 6});
        fNodes[3].setProperty(OTHER_ATT, new boolean[]{true, false});
        fNodes[4].setProperty(OTHER_ATT, new float[]{9, 10});
        fNodes[5].setProperty(OTHER_ATT, new double[]{11, 12});
        fNodes[6].setProperty(OTHER_ATT, new char[]{13, 14});
        fNodes[7].setProperty(OTHER_ATT, new short[]{15, 16});

        tx.success();
        tx.finish();
    }

    /**
     * Test routine for the "contains" filter operation.
     */
    @Test
    public void containsTest()
    {
        List<Node> list = null;
        Map<String, Object> collOpts = new HashMap<String, Object>();
        collOpts.put(WebsockConstants.FILTER_OPERATION,
            WebsockConstants.CONTAINS_FILTER);

        //strings
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, STRING_ATT);
        collOpts.put(WebsockConstants.FILTER_VALUE, "AA");
        NodeFilter.filterNodes(list, collOpts);

        Assert.assertEquals(3, list.size());
        Assert.assertTrue(list.contains(fNodes[0]));
        Assert.assertTrue(list.contains(fNodes[4]));
        Assert.assertTrue(list.contains(fNodes[6]));

        //numbers
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, INTEGER_ATT);
        collOpts.put(WebsockConstants.FILTER_VALUE, "2");
        NodeFilter.filterNodes(list, collOpts);

        Assert.assertEquals(6, list.size());
        Assert.assertTrue(list.contains(fNodes[0]));
        Assert.assertTrue(list.contains(fNodes[1]));
        Assert.assertTrue(list.contains(fNodes[4]));
        Assert.assertTrue(list.contains(fNodes[7]));
        Assert.assertTrue(list.contains(fNodes[8]));
        Assert.assertTrue(list.contains(fNodes[9]));

        //incomplete
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, INCOMP_ATT);
        collOpts.put(WebsockConstants.FILTER_VALUE, "u");
        NodeFilter.filterNodes(list, collOpts);

        Assert.assertEquals(5, list.size());
        Assert.assertTrue(list.contains(fNodes[0]));
        Assert.assertTrue(list.contains(fNodes[4]));
        Assert.assertTrue(list.contains(fNodes[5]));
        Assert.assertTrue(list.contains(fNodes[8]));
        Assert.assertTrue(list.contains(fNodes[9]));

        //array of strings
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, ARRAY_ATT);
        collOpts.put(WebsockConstants.FILTER_VALUE, "2");
        NodeFilter.filterNodes(list, collOpts);

        Assert.assertEquals(3, list.size());
        Assert.assertTrue(list.contains(fNodes[0]));
        Assert.assertTrue(list.contains(fNodes[5]));
        Assert.assertTrue(list.contains(fNodes[9]));

        //other types
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, OTHER_ATT);
        collOpts.put(WebsockConstants.FILTER_VALUE, "2");
        NodeFilter.filterNodes(list, collOpts);

        //should not throw an exception
    }

    /**
     * Test routine for the "equals" filter operation.
     */
    @Test
    public void equalsTest()
    {
        List<Node> list = null;
        Map<String, Object> collOpts = new HashMap<String, Object>();
        collOpts.put(WebsockConstants.FILTER_OPERATION,
            WebsockConstants.EQUALS_FILTER);

        //strings
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, STRING_ATT);
        collOpts.put(WebsockConstants.FILTER_VALUE, "AAAA");
        NodeFilter.filterNodes(list, collOpts);

        Assert.assertEquals(1, list.size());
        Assert.assertEquals(list.get(0), fNodes[4]);

        //numbers
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, INTEGER_ATT);
        collOpts.put(WebsockConstants.FILTER_VALUE, "42");
        NodeFilter.filterNodes(list, collOpts);

        Assert.assertEquals(3, list.size());
        Assert.assertTrue(list.contains(fNodes[0]));
        Assert.assertTrue(list.contains(fNodes[4]));
        Assert.assertTrue(list.contains(fNodes[9]));

        //incomplete
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, INCOMP_ATT);
        collOpts.put(WebsockConstants.FILTER_VALUE, "stuff");
        NodeFilter.filterNodes(list, collOpts);

        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.contains(fNodes[4]));

        //arrays only work with contains
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, ARRAY_ATT);
        collOpts.put(WebsockConstants.FILTER_VALUE, "bogus");
        NodeFilter.filterNodes(list, collOpts);

        Assert.assertEquals(0, list.size());

        //other types
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, OTHER_ATT);
        collOpts.put(WebsockConstants.FILTER_VALUE, "2");
        NodeFilter.filterNodes(list, collOpts);

        Assert.assertEquals(0, list.size());
    }

    /**
     * Test routine for the "present" filter operation.
     */
    @Test
    public void presentTest()
    {
        List<Node> list = null;
        Map<String, Object> collOpts = new HashMap<String, Object>();
        collOpts.put(WebsockConstants.FILTER_OPERATION,
            WebsockConstants.HAS_PROPERTY_FILTER);

        //incomplete only
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, INCOMP_ATT);
        NodeFilter.filterNodes(list, collOpts);

        Assert.assertEquals(7, list.size());
        Assert.assertTrue(list.contains(fNodes[0]));
        Assert.assertTrue(list.contains(fNodes[1]));
        Assert.assertTrue(list.contains(fNodes[4]));
        Assert.assertTrue(list.contains(fNodes[5]));
        Assert.assertTrue(list.contains(fNodes[6]));
        Assert.assertTrue(list.contains(fNodes[8]));
        Assert.assertTrue(list.contains(fNodes[9]));
    }

    /**
     * Test routine for the "starts with" filter operation.
     */
    @Test
    public void startsWithTest()
    {
        List<Node> list = null;
        Map<String, Object> collOpts = new HashMap<String, Object>();
        collOpts.put(WebsockConstants.FILTER_OPERATION,
            WebsockConstants.STARTS_WITH_FILTER);

        //strings
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, STRING_ATT);
        collOpts.put(WebsockConstants.FILTER_VALUE, "A");
        NodeFilter.filterNodes(list, collOpts);

        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.contains(fNodes[4]));
        Assert.assertTrue(list.contains(fNodes[9]));

        //numbers
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, INTEGER_ATT);
        collOpts.put(WebsockConstants.FILTER_VALUE, "2");
        NodeFilter.filterNodes(list, collOpts);

        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.contains(fNodes[1]));
        Assert.assertTrue(list.contains(fNodes[8]));

        //incomplete
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, INCOMP_ATT);
        collOpts.put(WebsockConstants.FILTER_VALUE, "t");
        NodeFilter.filterNodes(list, collOpts);

        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.contains(fNodes[5]));
        Assert.assertTrue(list.contains(fNodes[6]));

        //arrays only work with contains
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, INCOMP_ATT);
        collOpts.put(WebsockConstants.FILTER_VALUE, "bogus");
        NodeFilter.filterNodes(list, collOpts);

        Assert.assertEquals(0, list.size());

        //other types
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }
        collOpts.put(WebsockConstants.FILTER_FIELD, OTHER_ATT);
        collOpts.put(WebsockConstants.FILTER_VALUE, "2");
        NodeFilter.filterNodes(list, collOpts);

        Assert.assertEquals(0, list.size());
    }
}
