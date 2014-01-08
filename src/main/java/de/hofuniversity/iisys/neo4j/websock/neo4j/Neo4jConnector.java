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
package de.hofuniversity.iisys.neo4j.websock.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

/**
 * Utility class for connecting to a Neo4j-Database, getting an interface
 * and shutting it down again.
 */
public class Neo4jConnector implements INeo4jConnector
{
    private final String fDbPath, fProperties;

    private GraphDatabaseService fDbService;

    /**
     * When started, creates an instance of neo4j at the given location.
     * Throws a NullPointerException if the given path is null.
     *
     * @param dbPath path to the database
     */
    public Neo4jConnector(String dbPath)
    {
        this(dbPath, null);
    }

    /**
     * When started, creates an instance of neo4j at the given location with
     * the properties specified in the file, pointed to by the properties
     * parameter.
     * Throws a NullPointerException if the given path is null.
     *
     * @param dbPath path to the database
     * @param properties path to properties file
     */
    public Neo4jConnector(String dbPath, String properties)
    {
        if(dbPath == null)
        {
            throw new NullPointerException("database path was null");
        }

        fDbPath = dbPath;
        fProperties = properties;
    }

    /**
     * Starts the database, if it hasn't already been started.
     * The interface is available through the getService()-method.
     *
     * @throws Exception if startup fails
     */
    @Override
    public void start() throws Exception
    {
        if(fDbService != null)
        {
            //already started
        }
        //start without properties
        else if(fProperties == null || fProperties.isEmpty())
        {
            fDbService = new GraphDatabaseFactory()
                .newEmbeddedDatabase(fDbPath);
        }
        //start with properties
        else
        {
            fDbService = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(
                fDbPath).loadPropertiesFromFile(fProperties)
                .newGraphDatabase();
        }

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                if(fDbService != null)
                {
                    fDbService.shutdown();
                }
            }
        });
    }

    /**
     * Tries to shut down the database.
     *
     * @throws Exception if it fails
     */
    @Override
    public void shutdown() throws Exception
    {
        fDbService.shutdown();
        fDbService = null;
    }

    @Override
    public GraphDatabaseService getService()
    {
        return fDbService;
    }
}
