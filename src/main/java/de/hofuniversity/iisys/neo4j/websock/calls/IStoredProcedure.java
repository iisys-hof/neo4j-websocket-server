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
package de.hofuniversity.iisys.neo4j.websock.calls;

import java.util.Map;

import de.hofuniversity.iisys.neo4j.websock.result.AResultSet;

/**
 * Interface for a stored procedure that can be called by its name using
 * different sets of parameters.
 */
public interface IStoredProcedure
{
    /**
     * @return internal name of the procedure
     */
    public String getName();

    /**
     * Calls the procedure with the given map of parameters, returning a
     * result set or null.
     *
     * @param parameters parameters to pass
     * @return result set or null
     */
    public AResultSet<?> call(Map<String, Object> parameters);

    /**
     * @return whether the procedure is native (Java) or based on a query
     *  language
     */
    public boolean isNative();
}
