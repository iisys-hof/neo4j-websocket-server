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
package de.hofuniversity.iisys.neo4j.websock.neo4j.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Node;

import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;

/**
 * Utility class for sorting a list of database nodes by a certain property.
 */
public class NodeSorter
{
    /**
     * Sorts a list of graph database nodes by the values stored under the
     * key specified in the given options as far as possible. Values have to
     * be available and Comparable for this method to work properly.
     *
     * @param nodes list of nodes to sort
     * @param options sorting options as defined by websocket constants
     */
    public static void sortNodes(final List<Node> nodes,
        Map<String, Object> options)
    {
        String order = (String)options.get(WebsockConstants.SORT_ORDER);

        String sortField = (String)options.get(WebsockConstants.SORT_FIELD);

        //get parameters and break if there is nothing to do
        if(nodes == null
            || order == null
            || sortField == null)
        {
            return;
        }

        boolean ascending = true;
        if(order.equals(WebsockConstants.DESCENDING))
        {
            ascending = false;
        }
        else if(order != null
            && !order.equals(WebsockConstants.ASCENDING))
        {
            //TODO: log
        }

        //extract values to sort by
        final Map<Node, Object> values = new HashMap<Node, Object>();
        for(Node node : nodes)
        {
            if(node.hasProperty(sortField))
            {
                values.put(node, node.getProperty(sortField));
            }
        }

        //use java sorting algorithm using cached values
        Collections.sort(nodes, new Comparator<Node>()
        {
            @SuppressWarnings("unchecked")
            public int compare(Node o1, Node o2)
            {
                int value = 0;
                Object val1 = values.get(o1);
                Object val2 = values.get(o2);

                if(val1 == null && val2 != null)
                {
                    value = 1;
                }
                else if(val2 == null)
                {
                    value = -1;
                }
                else if(val1 instanceof Comparable
                    && val2 instanceof Comparable)
                {
                    value = ((Comparable<Object>)val1).compareTo(val2);
                }

                return value;
            }
        });

        //reverse if necessary
        if(!ascending)
        {
            Collections.reverse(nodes);
        }
    }
}
