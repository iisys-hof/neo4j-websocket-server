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
package de.hofuniversity.iisys.neo4j.websock.neo4j.shindig.service;

import org.junit.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.neo4j.service.IDManager;

/**
 * Test for the ID generator.
 */
public class IDManagerTest
{
    private static final String TYPE1 = "type1", TYPE2 = "type2",
        TYPE3 = "type3";

    private GraphDatabaseService fDb;

    /**
     * Sets up an impermanent database.
     */
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
    }

    @After
    public void stopDatabase()
    {
        fDb.shutdown();
    }

    /**
     * Tests the ID manager on a blank database so it has to create a
     * management node.
     */
    @Test
    public void clearDBTest()
    {
        final IDManager manager = new IDManager(fDb);
        String id = null;

        //initial IDs
        for(int i = 0; i < 50; ++i)
        {
            id = manager.genID(TYPE1);
            Assert.assertEquals(TYPE1 + ':' + Integer.toString(i), id);
        }

        //IDs for different type
        for(int i = 0; i < 25; ++i)
        {
            id = manager.genID(TYPE2);
            Assert.assertEquals(TYPE2 + ':' + Integer.toString(i), id);
        }

        //continued generation
        for(int i = 50; i < 100; ++i)
        {
            id = manager.genID(TYPE1);
            Assert.assertEquals(TYPE1 + ':' + Integer.toString(i), id);
        }
    }

    /**
     * Tests the ID manager on a database with an existing management node.
     */
    @Test
    public void usedDBTest()
    {
        //create precondition
        clearDBTest();

        final IDManager manager = new IDManager(fDb);
        String id = null;

        //continued generation
        for(int i = 100; i < 150; ++i)
        {
            id = manager.genID(TYPE1);
            Assert.assertEquals(TYPE1 + ':' + Integer.toString(i), id);
        }

        //new type
        for(int i = 0; i < 50; ++i)
        {
            id = manager.genID(TYPE3);
            Assert.assertEquals(TYPE3 + ':' + Integer.toString(i), id);
        }

        //continued generation - different type
        for(int i = 25; i < 50; ++i)
        {
            id = manager.genID(TYPE2);
            Assert.assertEquals(TYPE2 + ':' + Integer.toString(i), id);
        }
    }
}
