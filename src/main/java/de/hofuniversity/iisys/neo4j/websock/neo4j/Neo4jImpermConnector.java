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
package de.hofuniversity.iisys.neo4j.websock.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;

/**
 * Utility class for connecting to an impermanent Neo4j-Database, getting
 * an interface and shutting it down again.
 */
public class Neo4jImpermConnector implements INeo4jConnector
{
    private GraphDatabaseService fDbService;

    @Override
    public void start() throws Exception
    {
        TestGraphDatabaseFactory fact = new TestGraphDatabaseFactory();
        fDbService = fact.newImpermanentDatabase();

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                if (fDbService != null)
                {
                    fDbService.shutdown();
                }
            }
        });
    }

    @Override
    public void shutdown() throws Exception
    {
        fDbService.shutdown();
    }

    @Override
    public GraphDatabaseService getService()
    {
        return fDbService;
    }

}
