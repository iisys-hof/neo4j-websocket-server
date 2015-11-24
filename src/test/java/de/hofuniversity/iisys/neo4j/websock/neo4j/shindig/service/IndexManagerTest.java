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
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.neo4j.service.IndexManager;

/**
 * Tests the server's index management functionality.
 */
public class IndexManagerTest
{
    private static final String INDEX_1_NAME = "index1";
    private static final String INDEX_2_NAME = "index2";
    private static final String INDEX_3_NAME = "index3";

    private static final String INDEX_1_KEY = "key1";
    private static final String INDEX_2_KEY = "key2";
    private static final String INDEX_3_KEY = "key3";

    private static final String NODE_1_VALUE = "node1val";
    private static final String NODE_2_VALUE = "node2val";
    private static final String NODE_3_VALUE = "node3val";
    private static final String NODE_4_VALUE = "node4val";

    private GraphDatabaseService fDb;
    private IndexManager fIndexMan;

    private Node fNode1, fNode2, fNode3, fNode4;

    private Index<Node> fIndex1, fIndex2;

    /**
     * Sets up an impermanent test database with some data.
     */
    @Before
    public void setupDb()
    {
        //start database
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

        //create index manager
        fIndexMan = new IndexManager(fDb);

        //create sample data
        Transaction tx = fDb.beginTx();

        fNode1 = fDb.createNode();
        fNode2 = fDb.createNode();
        fNode3 = fDb.createNode();
        fNode4 = fDb.createNode();

        fIndex1 = fDb.index().forNodes(INDEX_1_NAME);
        fIndex2 = fDb.index().forNodes(INDEX_2_NAME);

        fIndex1.add(fNode1, INDEX_1_KEY, NODE_1_VALUE);
        fIndex1.add(fNode2, INDEX_1_KEY, NODE_2_VALUE);

        fIndex2.add(fNode2, INDEX_2_KEY, NODE_2_VALUE);
        fIndex2.add(fNode3, INDEX_2_KEY, NODE_3_VALUE);

        tx.success();
        tx.finish();
    }

    /**
     * Shuts down the impermanent test database.
     */
    @After
    public void shutdownDb()
    {
        fDb.shutdown();
    }

    /**
     * Creates a new index and adds an entry.
     */
    @Test
    public void creationTest()
    {
        //create new index by adding a node
        fIndexMan.createIndexEntry(INDEX_3_NAME, fNode4.getId(), INDEX_3_KEY,
            NODE_4_VALUE);

        //check
        Index<Node> index3 = fDb.index().forNodes(INDEX_3_NAME);
        IndexHits<Node> hits = index3.get(INDEX_3_KEY, NODE_4_VALUE);
        Node node = hits.getSingle();

        Assert.assertEquals(fNode4, node);
    }

    /**
     * Tests the modification of existing indexes.
     */
    @Test
    public void modificationTest()
    {
        //add a node to an existing index
        fIndexMan.createIndexEntry(INDEX_1_NAME, fNode4.getId(), INDEX_1_KEY,
            NODE_4_VALUE);

        //check
        IndexHits<Node> hits = fIndex1.get(INDEX_1_KEY, NODE_4_VALUE);
        Node node = hits.getSingle();

        Assert.assertEquals(fNode4, node);
    }

    /**
     * Tests the deletion of entries.
     */
    @Test
    public void deletionTest()
    {
        //remove one entry from an existing index
        fIndexMan.deleteIndexEntry(INDEX_1_NAME, fNode1.getId(), INDEX_1_KEY,
            NODE_1_VALUE);

        //check
        IndexHits<Node> hits = fIndex1.get(INDEX_1_KEY, NODE_1_VALUE);
        Node node = hits.getSingle();
        Assert.assertNull(node);

        //other entry should still be there
        hits = fIndex1.get(INDEX_1_KEY, NODE_2_VALUE);
        node = hits.getSingle();
        Assert.assertEquals(fNode2, node);
    }

    /**
     * Tests the clearing of an index.
     */
    @Test
    public void clearingTest()
    {
        //completely deletes the index
        fIndexMan.clearIndex(INDEX_2_NAME);

        boolean fail = false;
        try
        {
            IndexHits<Node> hits = fIndex2.query(INDEX_2_KEY, "*");
            Assert.assertEquals(0, hits.size());
            fail = true;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fail = true;
        }
        Assert.assertTrue(fail);
    }
}
