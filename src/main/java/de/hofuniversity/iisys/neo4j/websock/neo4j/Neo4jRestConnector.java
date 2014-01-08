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

import org.neo4j.rest.graphdb.RestGraphDatabase;

/**
 * Utility class for connecting to a remote Neo4j-Database using the REST API,
 * getting an interface and shutting it down again.
 */
public class Neo4jRestConnector implements INeo4jConnector
{
    private final String fUrl;
    private final String fUsername, fPassword;

    private RestGraphDatabase fDbService;

    /**
     * Creates a connector that will connect to a graph database at the
     * specified URL without authentication.
     * Throws a NullPointerException if the given URL is null or empty.
     *
     * @param url URL to connect to
     */
    public Neo4jRestConnector(String url)
    {
        this(url, null, null);
    }

    /**
     * Creates a connector that will connect to a graph database at the
     * specified URL, authenticating with the given user name and password.
     * Throws a NullPointerException if the given URL is null or empty.
     *
     * @param url URL to connect to
     * @param username user name to use
     * @param password password to use
     */
    public Neo4jRestConnector(String url, String username, String password)
    {
        if(url == null || url.isEmpty())
        {
            throw new NullPointerException("no database URL given");
        }

        fUrl = url;
        fUsername = username;
        fPassword = password;
    }

    /**
     * Connects to the database, if it hasn't already been done.
     * The interface is available through the getService()-method.
     *
     * @throws Exception if connecting fails
     */
    @Override
    public void start() throws Exception
    {
        System.setProperty("org.neo4j.rest.driver",
            "neo4j-rest-graphdb/1.9");
        System.setProperty("org.neo4j.rest.stream", "true");
        System.setProperty("org.neo4j.rest.read_timeout", "120");
        System.setProperty("org.neo4j.rest.connect_timeout", "120");

        if(fDbService != null)
        {
            //already started
        }
        else if(fUsername == null || fPassword == null)
        {
            fDbService = new RestGraphDatabase(fUrl);
        }
        else
        {
            fDbService = new RestGraphDatabase(fUrl, fUsername, fPassword);
        }
    }

    /**
     * Tries to shut down the database connection.
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
    public RestGraphDatabase getService()
    {
        return fDbService;
    }
}
