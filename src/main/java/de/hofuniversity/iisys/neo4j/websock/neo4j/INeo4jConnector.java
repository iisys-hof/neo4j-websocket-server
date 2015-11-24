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

/**
 * Interface for a connector to a Neo4j database instance that can start and
 * stop the connection and deliver the basic service interface.
 */
public interface INeo4jConnector
{
    /**
     * Starts the database connection, if it hasn't already been started.
     * The interface is available through the getService()-method.
     *
     * @throws Exception if startup fails
     */
    public void start() throws Exception;

    /**
     * Tries to shut down the database connection.
     *
     * @throws Exception if it fails
     */
    public void shutdown() throws Exception;

    /**
     * @return interface of the current database or null if not connected
     */
    public GraphDatabaseService getService();
}
