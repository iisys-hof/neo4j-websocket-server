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
package de.hofuniversity.iisys.neo4j.websock;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.neo4j.graphdb.GraphDatabaseService;

import de.hofuniversity.iisys.neo4j.websock.neo4j.INeo4jConnector;

/**
 * Context handler, reading the configuration, setting up and shutting down the
 * database exactly once and providing it over a singleton object.
 */
public class WebsockContextHandler implements ServletContextListener
{
    private static WebsockContextHandler fInstance;

    private final Logger fLogger = Logger.getLogger(this.getClass().getName());

    private GraphConfig fConfig;
    private INeo4jConnector fConn;

    /**
     * @return single instance of this class
     */
    public static WebsockContextHandler getInstance()
    {
        return fInstance;
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0)
    {
        fInstance = this;

        fConfig = new GraphConfig();
        fConn = fConfig.getConnector();

        try
        {
            fLogger.log(Level.INFO, "starting Neo4j database");
            fConn.start();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fLogger.log(Level.SEVERE, "could not start database", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent arg0)
    {
        try
        {
            fLogger.log(Level.INFO, "stopping Neo4j database");
            fConn.shutdown();
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fLogger.log(Level.SEVERE, "could not stop database", e);
        }
    }

    /**
     * Initializes the context without reading external files using the given
     * configuration object.
     *
     * @param config configuration to use
     */
    public void initTestContext(GraphConfig config)
    {
        fInstance = this;

        fConfig = config;
        fConn = fConfig.getConnector();
    }

    /**
     * @return configuration object read
     */
    public GraphConfig getConfig()
    {
        return fConfig;
    }

    /**
     * @return database service interface
     */
    public GraphDatabaseService getDatabase()
    {
        return fConn.getService();
    }
}
