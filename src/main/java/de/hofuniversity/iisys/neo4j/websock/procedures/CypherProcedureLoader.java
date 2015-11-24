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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.graphdb.GraphDatabaseService;

import de.hofuniversity.iisys.neo4j.websock.GraphConfig;
import de.hofuniversity.iisys.neo4j.websock.WebsockContextHandler;
import de.hofuniversity.iisys.neo4j.websock.calls.CypherProcedure;
import de.hofuniversity.iisys.neo4j.websock.calls.IStoredProcedure;

/**
 * Class loading named Cypher queries from text files specified in the
 * configuration.
 *
 * Definitions are read in the format:
 * <query name>
 * <query line 1>
 * ...
 * <query line n>
 * <empty line>
 * <next name>
 */
public class CypherProcedureLoader implements IProcedureProvider
{
    public static final String CYPHER_FILES = "websocket.stored.cypher";

    private final GraphDatabaseService fDb;
    private final Logger fLogger;

    private String[] fFiles;

    /**
     * Creates a Cypher procedure loader creating procedures using the given
     * graph database.
     * The given database must not be null.
     *
     * @param database database to execute queries on
     */
    public CypherProcedureLoader(GraphDatabaseService database)
    {
        if(database == null)
        {
            throw new NullPointerException("database service was null");
        }
        fDb = database;

        GraphConfig config = WebsockContextHandler.getInstance().getConfig();
        String filesString = config.getProperty(CYPHER_FILES);

        if(filesString != null && !filesString.isEmpty())
        {
            fFiles = filesString.split(";");
        }

        fLogger = Logger.getLogger(this.getClass().getName());
    }

    @Override
    public Map<String, IStoredProcedure> getProcedures()
    {
        final Map<String, IStoredProcedure> procedures =
            new HashMap<String, IStoredProcedure>();

        if(fFiles != null && fFiles.length > 0)
        {
            for(String file : fFiles)
            {
                try
                {
                    addProcedures(file, procedures);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                    fLogger.log(Level.SEVERE, "could not load cypher " +
                    		"procedure definition file: " + file, e);
                }
            }
        }

        return procedures;
    }

    private void addProcedures(String file,
        final Map<String, IStoredProcedure> procedures) throws Exception
    {
        final BufferedReader reader = new BufferedReader(
            new FileReader(new File(file)));

        String name = null;
        String query = "";

        String line = reader.readLine();

        CypherProcedure proc = null;
        while(line != null)
        {
            //skip comments
            if(line.startsWith("#"))
            {
                line = reader.readLine();
                continue;
            }
            else if(line.isEmpty())
            {
                if(name != null
                    && !query.isEmpty())
                {
                    //end of statement, store
                    proc = new CypherProcedure(name, fDb, query);
                    procedures.put(name, proc);

                    name = null;
                    query = "";
                }
            }
            else if(name != null)
            {
                //statement (continued)
                query += line + "\n";
            }
            else
            {
                //beginning of new statement
                name = line;
            }

            line = reader.readLine();
        }

        //include last query with no trailing empty line
        if(name != null
            && !query.isEmpty())
        {
            //end of statement, store
            proc = new CypherProcedure(name, fDb, query);
            procedures.put(name, proc);

            name = null;
            query = "";
        }
    }
}
