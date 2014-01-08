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

import java.util.Iterator;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

/**
 * Class managing index entries in a Neo4j database.
 */
public class IndexManager
{
    private final GraphDatabaseService fDatabase;

    public IndexManager(GraphDatabaseService database)
    {
        if(database == null)
        {
            throw new NullPointerException("Neo4j database service was null");
        }

        fDatabase = database;
    }

    /**
     * Creates an index entry in the index with the given name, for the node
     * with the given node ID under the specified key-value combination.
     * None of the parameters may be null or empty.
     *
     * @param index name of the index to manipulate
     * @param nodeId node ID of the node to create an entry for
     * @param key property key of the entry
     * @param value property value of the entry
     */
    public void createIndexEntry(final String index, final Long nodeId,
        final String key, final String value)
    {
        if(index == null || index.isEmpty())
        {
            throw new NullPointerException("no index name given");
        }
        if(nodeId == null)
        {
            throw new NullPointerException("no node ID given");
        }
        if(key == null || key.isEmpty())
        {
            throw new NullPointerException("no property key given");
        }
        if(value == null || value.isEmpty())
        {
            throw new NullPointerException("no property value given");
        }

        Transaction tx = fDatabase.beginTx();

        try
        {
            Index<Node> nodeIdx = fDatabase.index().forNodes(index);
            Node node = fDatabase.getNodeById(nodeId);
            nodeIdx.add(node, key, value);

            tx.success();
            tx.finish();
        }
        catch(Exception e)
        {
            tx.failure();
            tx.finish();

            throw e;
        }
    }

    /**
     * Deletes one or more entries from the index with the specified name.
     * To specify which entries to remove, at least a node ID or a key-value
     * combination are required, but both can be used together. If only a
     * key-value combination is specified, all entries with that combination
     * will be deleted.
     *
     * @param index name of the index to manipulate
     * @param nodeIdnode ID of the node to remove an entry for
     * @param key property key of the entry
     * @param value property value of the entry
     */
    public void deleteIndexEntry(final String index, Long nodeId,
        final String key, final String value)
    {
        if(index == null || index.isEmpty())
        {
            throw new NullPointerException("no index name given");
        }

        if(nodeId == null
            && (key == null || key.isEmpty()
                || value == null || value.isEmpty()))
        {
            throw new NullPointerException("insufficient parameters");
        }

        Transaction tx = fDatabase.beginTx();

        try
        {
            final Index<Node> nodeIdx = fDatabase.index().forNodes(index);

            //either directly request a node or delete all if none is specified
            if(nodeId != null)
            {
                Node node = fDatabase.getNodeById(nodeId);
                removeEntry(nodeIdx, node, key, value);
            }
            else
            {
                IndexHits<Node> hits = nodeIdx.get(key, value);
                final Iterator<Node> iter = hits.iterator();
                while(iter.hasNext())
                {
                    removeEntry(nodeIdx, iter.next(), key, value);
                }
            }

            tx.success();
            tx.finish();
        }
        catch(Exception e)
        {
            tx.failure();
            tx.finish();

            throw e;
        }
    }

    private void removeEntry(final Index<Node> nodeIdx, final Node node,
        final String key, final String value)
    {
        if(key == null)
        {
            nodeIdx.remove(node);
        }
        else if(value == null)
        {
            nodeIdx.remove(node, key);
        }
        else
        {
            nodeIdx.remove(node, key, value);
        }
    }

    /**
     * Deletes the index with the given name.
     * The given name must not be null or empty.
     *
     * @param index name of the index to delete
     */
    public void clearIndex(String index)
    {
        if(index == null || index.isEmpty())
        {
            throw new NullPointerException("no index name given");
        }

        Transaction tx = fDatabase.beginTx();

        try
        {
            final Index<Node> nodeIdx = fDatabase.index().forNodes(index);
            nodeIdx.delete();

            tx.success();
            tx.finish();
        }
        catch(Exception e)
        {
            tx.failure();
            tx.finish();

            throw e;
        }
    }
}
