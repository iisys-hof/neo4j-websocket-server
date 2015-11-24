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
 * Test routine to check whether the node sorting utility is working correctly.
 */
public class NodeSorterTest
{
    private static final String STRING_ATT = "string_attribute";
    private static final String INTEGER_ATT = "integer_attribute";
    private static final String INCOMP_ATT = "incomplete_attribute";
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

        fNodes[0].setProperty(STRING_ATT, "DDDD");
        fNodes[1].setProperty(STRING_ATT, "BBBB");
        fNodes[2].setProperty(STRING_ATT, "FFFF");
        fNodes[3].setProperty(STRING_ATT, "JJJJ");
        fNodes[4].setProperty(STRING_ATT, "AAAA");
        fNodes[5].setProperty(STRING_ATT, "CCCC");
        fNodes[6].setProperty(STRING_ATT, "HHHH");
        fNodes[7].setProperty(STRING_ATT, "EEEE");
        fNodes[8].setProperty(STRING_ATT, "IIII");
        fNodes[9].setProperty(STRING_ATT, "GGGG");

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

        fNodes[0].setProperty(OTHER_ATT, new String[]{"1", "2"});
        fNodes[1].setProperty(OTHER_ATT, new String[]{"3", "4"});
        fNodes[2].setProperty(OTHER_ATT, new String[]{"5", "6"});
        fNodes[3].setProperty(OTHER_ATT, new String[]{"7", "8"});
        fNodes[4].setProperty(OTHER_ATT, new String[]{"9", "10"});
        fNodes[5].setProperty(OTHER_ATT, new String[]{"11", "12"});
        fNodes[6].setProperty(OTHER_ATT, new String[]{"13", "14"});
        fNodes[7].setProperty(OTHER_ATT, new String[]{"15", "16"});
        fNodes[8].setProperty(OTHER_ATT, new String[]{"17", "18"});
        fNodes[9].setProperty(OTHER_ATT, new String[]{"19", "20"});

        tx.success();
        tx.finish();
    }

    //TODO: update

    /**
     * Test testing string sorting and inversion functionality.
     */
    @Test
    public void stringTest()
    {
        List<Node> list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }

        Map<String, Object> opts = new HashMap<String, Object>();
        opts.put(WebsockConstants.SORT_FIELD, STRING_ATT);
        opts.put(WebsockConstants.SORT_ORDER, WebsockConstants.ASCENDING);

        NodeSorter.sortNodes(list, opts);

        Assert.assertEquals(fNodes[4], list.get(0));
        Assert.assertEquals(fNodes[1], list.get(1));
        Assert.assertEquals(fNodes[5], list.get(2));
        Assert.assertEquals(fNodes[0], list.get(3));
        Assert.assertEquals(fNodes[7], list.get(4));
        Assert.assertEquals(fNodes[2], list.get(5));
        Assert.assertEquals(fNodes[9], list.get(6));
        Assert.assertEquals(fNodes[6], list.get(7));
        Assert.assertEquals(fNodes[8], list.get(8));
        Assert.assertEquals(fNodes[3], list.get(9));


        //inverted
        list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }

        opts.put(WebsockConstants.SORT_ORDER, WebsockConstants.DESCENDING);

        NodeSorter.sortNodes(list, opts);

        Assert.assertEquals(fNodes[4], list.get(9));
        Assert.assertEquals(fNodes[1], list.get(8));
        Assert.assertEquals(fNodes[5], list.get(7));
        Assert.assertEquals(fNodes[0], list.get(6));
        Assert.assertEquals(fNodes[7], list.get(5));
        Assert.assertEquals(fNodes[2], list.get(4));
        Assert.assertEquals(fNodes[9], list.get(3));
        Assert.assertEquals(fNodes[6], list.get(2));
        Assert.assertEquals(fNodes[8], list.get(1));
        Assert.assertEquals(fNodes[3], list.get(0));
    }

    /**
     * Test number sorting functionality.
     */
    @Test
    public void numberTest()
    {
        List<Node> list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }

        Map<String, Object> opts = new HashMap<String, Object>();
        opts.put(WebsockConstants.SORT_FIELD, INTEGER_ATT);
        opts.put(WebsockConstants.SORT_ORDER, WebsockConstants.ASCENDING);

        NodeSorter.sortNodes(list, opts);

        Assert.assertEquals(fNodes[5], list.get(0));
        Assert.assertEquals(fNodes[2], list.get(1));
        Assert.assertEquals(fNodes[6], list.get(2));
        Assert.assertEquals(fNodes[3], list.get(3));
        Assert.assertEquals(fNodes[8], list.get(4));
        Assert.assertEquals(fNodes[1], list.get(5));
        //3 * 42
        Assert.assertTrue(list.contains(fNodes[0]));
        Assert.assertTrue(list.contains(fNodes[4]));
        Assert.assertTrue(list.contains(fNodes[9]));
        Assert.assertEquals(fNodes[7], list.get(9));
    }

    /**
     * Test the sorter's reaction to missing values.
     */
    @Test
    public void missingValueTest()
    {
        List<Node> list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }

        Map<String, Object> opts = new HashMap<String, Object>();
        opts.put(WebsockConstants.SORT_FIELD, INCOMP_ATT);
        opts.put(WebsockConstants.SORT_ORDER, WebsockConstants.ASCENDING);

        NodeSorter.sortNodes(list, opts);

        Assert.assertEquals(fNodes[1], list.get(0));
        Assert.assertEquals(fNodes[0], list.get(1));
        Assert.assertEquals(fNodes[8], list.get(2));
        Assert.assertEquals(fNodes[9], list.get(3));
        Assert.assertEquals(fNodes[4], list.get(4));
        Assert.assertEquals(fNodes[6], list.get(5));
        Assert.assertEquals(fNodes[5], list.get(6));

        Assert.assertTrue(list.contains(fNodes[2]));
        Assert.assertTrue(list.contains(fNodes[3]));
        Assert.assertTrue(list.contains(fNodes[7]));
    }

    /**
     * Test the sorter's reaction to values that do not implement Comparable.
     */
    @Test
    public void notComparableTest()
    {
        List<Node> list = new ArrayList<Node>();
        for(Node node : fNodes)
        {
            list.add(node);
        }

        Map<String, Object> opts = new HashMap<String, Object>();
        opts.put(WebsockConstants.SORT_FIELD, OTHER_ATT);
        opts.put(WebsockConstants.SORT_ORDER, WebsockConstants.ASCENDING);

        NodeSorter.sortNodes(list, opts);

        //no exception - no problem
    }
}
