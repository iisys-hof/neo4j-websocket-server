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
package de.hofuniversity.iisys.neo4j.websock.handlers;

import de.hofuniversity.iisys.neo4j.websock.query.encoding.TransferUtil;

/**
 * Session object containing a transfer utility and the currently authenticated
 * user.
 */
public class ClientSession
{
    private final TransferUtil fTransfer;
    private String fAuthenticated;

    /**
     * Creates a client session around the given transfer utility.
     * The given transfer utility must not be null.
     *
     * @param util transfer utility to use
     */
    public ClientSession(TransferUtil util)
    {
        if(util == null)
        {
            throw new NullPointerException("transfer utility was null");
        }

        fTransfer = util;
    }

    /**
     * @return the session's transfer utility
     */
    public TransferUtil getTransferUtil()
    {
        return fTransfer;
    }

    /**
     * @return authenticated user or null
     */
    public String getAuthenticatedUser()
    {
        return fAuthenticated;
    }

    /**
     * Sets the authenticated user for this session.
     *
     * @param user authenticated user
     */
    public void setAuthenticatedUser(String user)
    {
        fAuthenticated = user;
    }
}
