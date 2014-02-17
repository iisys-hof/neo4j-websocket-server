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
package de.hofuniversity.iisys.neo4j.websock;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import de.hofuniversity.iisys.neo4j.websock.neo4j.INeo4jConnector;
import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jConnector;
import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jHAConnector;
import de.hofuniversity.iisys.neo4j.websock.neo4j.Neo4jImpermConnector;

/**
 * Base class that reads the configuration and chooses the right Neo4j
 * connector.
 */
public class GraphConfig
{
    private static final String PROPERTIES = "neo4j-websocket-server";
    private static final String MODE = "neo4j.mode";
    private static final String PATH = "neo4j.path";
    private static final String CONF_PATH = "neo4j.configpath";

    private static final String EMB_MODE = "embedded";
    private static final String HA_MODE = "enterprise";
    private static final String IMP_MODE = "impermanent";


    private final Map<String, String> fProperties;

    private final Map<String, String> fNeo4jParams;

    private final INeo4jConnector fConnector;

    /**
     * Initializes the class by reading the configuration properties file.
     *
     * @throws Exception if any errors occur
     */
    public GraphConfig()
    {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ResourceBundle rb = ResourceBundle.getBundle(PROPERTIES,
            Locale.getDefault(), loader);

        fProperties = new HashMap<String, String>();
        fNeo4jParams = new HashMap<String, String>();

        String key = null;
        String value = null;
        Enumeration<String> keys = rb.getKeys();
        while(keys.hasMoreElements())
        {
            key = keys.nextElement();
            value = rb.getString(key);

            fProperties.put(key, value);

            checkNeo4jParam(key, value);
        }

        fConnector = setConnector();
    }

    /**
     * Creates an empty configuration object, without reading properties,
     * for testing purposes, with an impermanent database connector.
     *
     * @param test redundant parameter
     */
    public GraphConfig(boolean test)
    {
        fProperties = new HashMap<String, String>();
        fNeo4jParams = new HashMap<String, String>();
        fConnector = new Neo4jImpermConnector();
    }

    private void checkNeo4jParam(String key, String value)
    {
        if(key.startsWith("neo4j.properties."))
        {
            key = key.replaceFirst("neo4j.properties.", "");

            fNeo4jParams.put(key, value);
        }
    }

    private INeo4jConnector setConnector()
    {
        INeo4jConnector conn = null;

        String mode = fProperties.get(MODE);
        String path = fProperties.get(PATH);
        String configpath = fProperties.get(CONF_PATH);

        if(EMB_MODE.equals(mode))
        {
            if(configpath != null)
            {
                conn = new Neo4jConnector(path, configpath);
            }
            else
            {
                conn = new Neo4jConnector(path);
            }
        }
        else if(HA_MODE.equals(mode))
        {
            if(configpath != null)
            {
                conn = new Neo4jHAConnector(path, configpath);
            }
            else
            {
                conn = new Neo4jHAConnector(path);
            }
        }
        else if(IMP_MODE.equals(mode))
        {
            conn = new Neo4jImpermConnector();
        }
        else
        {
            //unsupported mode
            throw new RuntimeException("Neo4j mode '" + mode + "' is unknown");
        }

        //TODO: pass properties

        return conn;
    }

    /**
     * @return configured neo4j implementation
     */
    public INeo4jConnector getConnector()
    {
        return fConnector;
    }

    /**
     * Sets the value for a property key.
     *
     * @param key key of the property
     * @param value value of the property
     */
    public void setProperty(String key, String value)
    {
        fProperties.put(key, value);
    }

    /**
     * Returns the value for a property key or null if it doesn't exist.
     *
     * @param key key of the property
     * @return value of the property
     */
    public String getProperty(String key)
    {
        return fProperties.get(key);
    }
}
