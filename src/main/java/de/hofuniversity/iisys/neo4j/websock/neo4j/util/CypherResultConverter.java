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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.neo4j.cypher.javacompat.ExecutionResult;
import org.neo4j.graphdb.PropertyContainer;

import de.hofuniversity.iisys.neo4j.websock.neo4j.convert.SimpleGraphObject;
import de.hofuniversity.iisys.neo4j.websock.result.TableResult;

/**
 * Converter class transforming the output of local Cypher queries into a
 * transferable result table.
 */
public class CypherResultConverter
{
    /**
     * Converts the given execution result into a result table, only including
     * the entries specified for pagination support.
     *
     * @param result result returned by Cypher execution engine
     * @param first index of first result to retrieve
     * @param last index of last result to retrieve (exclusive)
     * @return results converted to a table
     */
    public static TableResult toTableResult(final ExecutionResult result,
        final int first, final int last)
    {
        //TODO: specify list and map implementations

        final List<String> columnNames = result.columns();
        final int columnCount = columnNames.size();

        int total = 0;

        /*
         * create result list and individual entry lists based on how many rows
         * there are
         * add all columns' values to the individual lists for each entry
         * attention: columns can only be iterated over once
         */
        Iterator<Map<String, Object>> rows = result.iterator();
        final List<List<Object>> entries = new LinkedList<List<Object>>();
        List<Object> entry = null;
        Map<String, Object> map = null;
        Object value = null;
        while(rows.hasNext())
        {
            map = rows.next();
            ++total;

            //counter one too ahead already
            //only count rows that don't need to be returned
            if(total <= first
                || (last > 0 && total > last))
            {
                continue;
            }

            entry = new ArrayList<Object>(columnCount);

            for(String colName : columnNames)
            {
                value = map.get(colName);

                //convert if necessary
                value = getConvertedObject(value);

                entry.add(value);
            }

            entries.add(entry);
        }

        TableResult table = new TableResult(columnNames, entries);
        table.setFirst(first);
        if(last - first > 0)
        {
            table.setMax(last - first);
        }
        table.setTotal(total);

        return table;
    }

    @SuppressWarnings("unchecked")
    private static Object getConvertedObject(Object value)
    {
        //convert nodes and relationships
        if(value == null)
        {
            return null;
        }
        else if(value instanceof PropertyContainer)
        {
            value = new SimpleGraphObject(
                (PropertyContainer)value).toMap(null);
        }
        //convert lists of nodes and relationships
        else if(value instanceof List<?>
            && ((List<?>)value).size() > 0)
        {
            if(((List<?>)value).get(0) instanceof PropertyContainer)
            {
                final List<Map<String, Object>> mapList =
                    new ArrayList<Map<String, Object>>(
                    ((List<PropertyContainer>)value).size());

                for(PropertyContainer con : (List<PropertyContainer>)value)
                {
                    mapList.add(new SimpleGraphObject(con).toMap(null));
                }

                value = mapList;
            }
            else
            {
                final List<Object> newList = new ArrayList<Object>(
                    ((List<?>) value).size());

                for(Object o : (List<?>)value)
                {
                    newList.add(getConvertedObject(o));
                }

                value = newList;
            }
        }

        //TODO: other possible collections and values?

        return value;
    }
}
