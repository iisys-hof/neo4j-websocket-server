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
package de.hofuniversity.iisys.neo4j.websock.neo4j.convert;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.neo4j.graphdb.PropertyContainer;

import de.hofuniversity.iisys.neo4j.websock.util.ImplUtil;

/**
 * Basic implementation for a graph object, copying and storing only local
 * node or relationship properties.
 */
public class SimpleGraphObject implements IGraphObject
{
    private final PropertyContainer fContainer;

    private final ImplUtil fImpl;

    /**
     * Creates a simple graph object, retrieving data from and writing data to
     * the given container.
     * The container may not be null.
     *
     * @param container container to convert
     */
    public SimpleGraphObject(PropertyContainer container)
    {
        this(container, new ImplUtil(LinkedList.class, HashMap.class));
    }

    /**
     * Creates a simple graph object, retrieving data from and writing data to
     * the given container, using the given map implementation for conversion.
     * None of the arguments may be null.
     *
     * @param container container to convert
     */
    public SimpleGraphObject(PropertyContainer container, ImplUtil impl)
    {
        if(container == null)
        {
            throw new NullPointerException("property container was null");
        }
        if(impl == null)
        {
            throw new NullPointerException("implementation utility was null");
        }

        fContainer = container;
        fImpl = impl;
    }

    @Override
    public Map<String, Object> toMap(final Set<String> fields)
    {
        final Map<String, Object> map = fImpl.newMap();

        if(fields == null || fields.isEmpty())
        {
            for(String key : fContainer.getPropertyKeys())
            {
                map.put(key, fContainer.getProperty(key));
            }
        }
        else
        {
            for(String key : fields)
            {
                if(fContainer.hasProperty(key))
                {
                    map.put(key, fContainer.getProperty(key));
                }
            }
        }

        return map;
    }

    @Override
    public void setData(Map<String, ?> map)
    {
        Object value = null;
        for(Entry<String, ?> fieldE : map.entrySet())
        {
            value = fieldE.getValue();

            if(value == null)
            {
                fContainer.removeProperty(fieldE.getKey());
            }
            else if(value instanceof List<?>)
            {
                //convert lists to arrays
                Object[] array = ((List<?>)value).toArray();
                fContainer.setProperty(fieldE.getKey(), array);
            }
            else
            {
                fContainer.setProperty(fieldE.getKey(), value);
            }
        }
    }
}
