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
package de.hofuniversity.iisys.neo4j.websock.procedures;

import java.util.Map;

import de.hofuniversity.iisys.neo4j.websock.calls.IStoredProcedure;

/**
 * Interface for a source of stored procedures.
 *
 * @author fholzschuher2
 *
 */
public interface IProcedureProvider
{
    /**
     * @return stored procedures mapped by their name
     */
    public Map<String, IStoredProcedure> getProcedures();
}
