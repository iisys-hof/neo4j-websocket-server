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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for the conversion process from database nodes to transferable
 * objects. It contains a map of JSON fields that were split up into multiple
 * properties and sets of fields that are mapped to relations or are context
 * sensitive.
 */
public class ConvHelper
{
    private final Map<String, List<String>> fSplitFields;
    private final Set<String> fRelMapped, fContextSensitive;

    /**
     * Creates a Conversion helper, returning the given map and lists. Accepts
     * null for all parameters.
     *
     * @param splitFields
     * @param relMapped
     * @param contextSensitive
     */
    public ConvHelper(Map<String, List<String>> splitFields,
        Set<String> relMapped, Set<String> contextSensitive)
    {
        fSplitFields = splitFields;
        fRelMapped = relMapped;
        fContextSensitive = contextSensitive;
    }

    /**
     * Delivers a map of an objects's attributes that have been split into
     * several node properties or null if there are none. Selective copy
     * operations will copy the
     *
     * @return map of split attributes
     */
    public Map<String, List<String>> getSplitFields()
    {
        return fSplitFields;
    }

    /**
     * Returns a set of properties which are mapped to relationships instead of
     * node properties or null if there are none.
     *
     * @return list of properties mapped to relationships
     */
    public Set<String> getRelationshipMapped()
    {
        return fRelMapped;
    }

    /**
     * Returns a set of properties which are context sensitive and as such
     * can't be retrieved from the database directly or null if there are none.
     *
     * @return list of context sensitive properties
     */
    public Set<String> getContextSensitive()
    {
        return fContextSensitive;
    }
}
