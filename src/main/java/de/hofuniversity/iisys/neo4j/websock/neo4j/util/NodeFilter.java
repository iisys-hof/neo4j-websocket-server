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

import java.util.List;
import java.util.Map;

import org.neo4j.graphdb.Node;

import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;
import de.hofuniversity.iisys.neo4j.websock.util.EFilterOperation;

/**
 * Utility class for filtering a list of database nodes based on shindig's
 * collection options.
 */
public class NodeFilter
{
    /**
     * Filters a list of graph database nodes by the values stored under the
     * property key defined by the given collection options. Only works for
     * simple value based filters. Returns without an error if parameters are
     * incomplete.
     * Known problem: objects that don't specify their own toString-method are
     * filtered based on their memory address
     *
     * @param nodes list of nodes to filter
     * @param options filter options as defined by websocket constants
     */
    public static void filterNodes(final List<Node> nodes,
        Map<String, Object> options)
    {
        final String filterKey =
            (String)options.get(WebsockConstants.FILTER_FIELD);

        final String filterVal =
            (String)options.get(WebsockConstants.FILTER_VALUE);

        String opVal = (String)options.get(WebsockConstants.FILTER_OPERATION);
        EFilterOperation filterOp = null;
        if(opVal != null)
        {
            filterOp = EFilterOperation.getTypeFor(opVal);
        }

        //check if the filter or filter operation are missing
        if(filterKey == null || filterKey.isEmpty())
        {
            return;
        }

        //only the "has property" filter requires no value
        if((filterVal == null || filterVal.isEmpty())
            && filterOp != EFilterOperation.HAS_PROPERTY)
        {
            return;
        }

        //filter lists, monitor changing size
        switch(filterOp)
        {
            case CONTAINS:
                containsFilter(nodes, filterKey, filterVal.toLowerCase());
                break;

            case EQUALS:
                equalsFilter(nodes, filterKey, filterVal.toLowerCase());
                break;

            case HAS_PROPERTY:
                presentFilter(nodes, filterKey);
                break;

            case STARTS_WITH:
                startsWithFilter(nodes, filterKey, filterVal.toLowerCase());
                break;

            case ENDS_WITH:
                endsWithFilter(nodes, filterKey, filterVal.toLowerCase());
                break;
        }
    }

    private static void containsFilter(final List<Node> nodes,
        final String filter, final String filterVal)
    {
        Node node = null;
        int size = nodes.size();
        Object value = null;

        for(int i = 0; i < size;)
        {
            node = nodes.get(i);
            if(node.hasProperty(filter))
            {
                value = node.getProperty(filter);

                /*
                 * technically only strings can contain others, but partial
                 * number matching might also be requested
                 */
                if(value instanceof String[])
                {
                    boolean remove = true;

                    for(String s : (String[])value)
                    {
                        if(s.toLowerCase().contains(filterVal))
                        {
                            ++i;
                            remove = false;
                            break;
                        }
                    }

                    if(remove)
                    {
                        nodes.remove(i);
                        --size;
                    }
                }
                else if(value.toString().toLowerCase().contains(filterVal))
                {
                    ++i;
                }
                else
                {
                    nodes.remove(i);
                    --size;
                }
            }
            else
            {
                nodes.remove(i);
                --size;
            }
        }
    }

    private static void equalsFilter(final List<Node> nodes,
        final String filter, final String filterVal)
    {
        Node node = null;
        int size = nodes.size();
        Object value = null;

        for(int i = 0; i < size;)
        {
            node = nodes.get(i);
            if(node.hasProperty(filter))
            {
                value = node.getProperty(filter);

                //check for equality via string representation
                if(value.toString().toLowerCase().equals(filterVal))
                {
                    ++i;
                }
                else
                {
                    nodes.remove(i);
                    --size;
                }
            }
            else
            {
                nodes.remove(i);
                --size;
            }
        }
    }

    private static void presentFilter(final List<Node> nodes,
        final String filter)
    {
        Node node = null;
        int size = nodes.size();

        for(int i = 0; i < size;)
        {
            node = nodes.get(i);
            if(node.hasProperty(filter))
            {
                ++i;
            }
            else
            {
                nodes.remove(i);
                --size;
            }
        }
    }

    private static void startsWithFilter(final List<Node> nodes,
        final String filter, final String filterVal)
    {
        Node node = null;
        int size = nodes.size();
        Object value = null;

        for(int i = 0; i < size;)
        {
            node = nodes.get(i);
            if(node.hasProperty(filter))
            {
                value = node.getProperty(filter);

                //check for start via string representation
                if(value.toString().toLowerCase().startsWith(filterVal))
                {
                    ++i;
                }
                else
                {
                    nodes.remove(i);
                    --size;
                }
            }
            else
            {
                nodes.remove(i);
                --size;
            }
        }
    }

    private static void endsWithFilter(final List<Node> nodes,
        final String filter, final String filterVal)
    {
        Node node = null;
        int size = nodes.size();
        Object value = null;

        for(int i = 0; i < size;)
        {
            node = nodes.get(i);
            if(node.hasProperty(filter))
            {
                value = node.getProperty(filter);

                //check for start via string representation
                if(value.toString().toLowerCase().endsWith(filterVal))
                {
                    ++i;
                }
                else
                {
                    nodes.remove(i);
                    --size;
                }
            }
            else
            {
                nodes.remove(i);
                --size;
            }
        }
    }
}
