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
package de.hofuniversity.iisys.neo4j.websock.neo4j.convert;

import java.util.Map;
import java.util.Set;

/**
 * Interface for a generic converter for objects in the graph.
 */
public interface IGraphObject
{
    /**
     * Converts the object to a transferable map, only containing the fields
     * specified. If no fields are specified, all fields are copied.
     *
     * @param fields set of fields or null
     * @return object converted to map
     */
    public Map<String, Object> toMap(Set<String> fields);

    /**
     * Sets the data for this entity in the graph, taken from the given map.
     * The given map must not be null.
     *
     * @param map map containing the data to store
     */
    public void setData(Map<String, ?> map);
}
