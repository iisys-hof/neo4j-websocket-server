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

import java.util.Map;

import org.junit.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

import de.hofuniversity.iisys.neo4j.websock.GraphConfig;
import de.hofuniversity.iisys.neo4j.websock.WebsockContextHandler;
import de.hofuniversity.iisys.neo4j.websock.calls.CypherProcedure;
import de.hofuniversity.iisys.neo4j.websock.calls.IStoredProcedure;

/**
 * Tests for the Cypher stored procedure loader.
 */
public class CypherProcedureLoaderTest
{
    private static final String TEST_FILE =
        "src/test/resources/cypher_procedures.ini";

    private static final String QUERY_1_NAME = "cFriends";
    private static final String QUERY_1 = "START person=node:persons("
        + "{idLookup})\n"
        + "MATCH person-[:FRIEND_OF]->friend\n"
        + "RETURN friend as person\n";

    private static final String QUERY_2_NAME = "cFriendsActivities";
    private static final String QUERY_2 = "START person=node:persons("
        + "{idLookup})\n"
        + "MATCH person-[:FRIEND_OF]->()-[:ACTED]->activity\n"
        + "RETURN extract(p in activity-[:GENERATOR]->() : last(p)) "
        + "as generator, extract(p in activity-[:PROVIDER]->() : last(p)) "
        + "as provider, extract(p in activity-[:OBJECT]->() : last(p)) "
        + "as object, extract(p in activity-[:TARGET]->() : last(p)) "
        + "as target, extract(p in activity-[:ACTOR]->() : last(p)) as actor, "
        + "activity\n";

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
     * Tests loading stored Cypher procedures.
     */
    @Test
    public void procLoadTest()
    {
        //setup test configuration
        GraphConfig config = new GraphConfig(true);
        config.setProperty(CypherProcedureLoader.CYPHER_FILES, TEST_FILE);

        //load procedures
        new WebsockContextHandler().initTestContext(config);
        CypherProcedureLoader loader = new CypherProcedureLoader(fDb);
        Map<String, IStoredProcedure> procs = loader.getProcedures();

        //verify
        CypherProcedure procedure = (CypherProcedure) procs.get(QUERY_1_NAME);
        Assert.assertFalse(procedure.isNative());
        Assert.assertEquals(QUERY_1_NAME, procedure.getName());
        Assert.assertEquals(QUERY_1, procedure.getQuery());

        procedure = (CypherProcedure) procs.get(QUERY_2_NAME);
        Assert.assertFalse(procedure.isNative());
        Assert.assertEquals(QUERY_2_NAME, procedure.getName());
        Assert.assertEquals(QUERY_2, procedure.getQuery());
    }
}
