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
package de.hofuniversity.iisys.neo4j.websock.neo4j.service;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;

import de.hofuniversity.iisys.neo4j.websock.result.SingleResult;

/**
 * Class that manages the creation of unique IDs per type of node.
 */
public class IDManager
{
    private static final String ID_NODE = "id";

    private final GraphDatabaseService fDatabase;
    private final Node fIdNode;

    /**
     * Creates an ID manager using data from the given Neo4j database service.
     * Throws a NullPointerException if the given service is null.
     *
     * @param database graph database to use
     */
    public IDManager(GraphDatabaseService database)
    {
        if(database == null)
        {
            throw new NullPointerException("database service was null");
        }

        fDatabase = database;

        //retrieve ID management node
        Index<Node> idNodes = fDatabase.index().forNodes(ID_NODE);
        Node idNode = idNodes.get(ID_NODE, ID_NODE)
            .getSingle();

        //create an ID management node if there is none
        if(idNode == null)
        {
            Transaction tx = fDatabase.beginTx();

            idNode = fDatabase.createNode();
            idNode.setProperty(ID_NODE, ID_NODE);
            idNodes.add(idNode, ID_NODE, ID_NODE);

            tx.success();
            tx.finish();
        }
        fIdNode = idNode;
    }

    /**
     * Generates a unique ID for the given type.
     * Throws a NullPointerException if the given type is null.
     *
     * @param type type of the object to create an ID for
     * @return newly generated ID unique for the object's type
     */
    public String genID(final String type)
    {
        Long nextId = null;
        String id = type + ':';

        if(type == null)
        {
            throw new NullPointerException("type was null");
        }

        Transaction tx = fDatabase.beginTx();

        if(fIdNode.hasProperty(type))
        {
            //get ID and store next
            nextId = (Long)fIdNode.getProperty(type);
            id += nextId;
            fIdNode.setProperty(type, ++nextId);
        }
        else
        {
            //start counting at 0
            id += '0';
            fIdNode.setProperty(type, 1L);
        }

        tx.success();
        tx.finish();

        return id;
    }

    /**
     * Generates a unique ID for the given type (for procedure calls).
     * Throws a NullPointerException if the given type is null.
     *
     * @param type type of the object to create an ID for
     * @return result with newly generated ID unique for the object's type
     */
    public SingleResult requestId(String type)
    {
        String id = genID(type);

        //TODO: specify map implementation
        Map<String, Object> resMap = new HashMap<String, Object>();
        resMap.put("id", id);
        return new SingleResult(resMap);
    }

    //TODO: free IDs again?
}
