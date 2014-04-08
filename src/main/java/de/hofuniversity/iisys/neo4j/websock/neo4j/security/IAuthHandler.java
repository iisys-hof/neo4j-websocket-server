/*
 * Copyright (c) 2012-2014 Institute of Information Systems, Hof University
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
package de.hofuniversity.iisys.neo4j.websock.neo4j.security;

import de.hofuniversity.iisys.neo4j.websock.query.WebsockQuery;

/**
 * Interface for a handler that can analyze incoming authentication queries
 * and determine whether the client was properly authenticated. Can also send
 * challenges to the client to which it has to respond. These will be re-sent
 * after unsuccessful authentication.
 */
public interface IAuthHandler
{
    /**
     * Checks whether the given query contains valid authentication
     * information and the returns the name of the authenticated user or null
     * if authentication was not successful.
     *
     * @param query query sent by the client
     * @return user name if authentication was successful
     */
    public String handleAuthentication(WebsockQuery query);

    /**
     * Returns a query which can be sent to the client as an authentication
     * challenge.
     *
     * @return challenge query or null
     */
    public WebsockQuery getChallenge();
}
