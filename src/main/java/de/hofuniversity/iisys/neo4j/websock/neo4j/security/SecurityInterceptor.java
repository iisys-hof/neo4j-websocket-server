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
package de.hofuniversity.iisys.neo4j.websock.neo4j.security;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.hofuniversity.iisys.neo4j.websock.query.EQueryType;
import de.hofuniversity.iisys.neo4j.websock.query.WebsockQuery;
import de.hofuniversity.iisys.neo4j.websock.query.encoding.TransferUtil;

/**
 * Class capable of intercepting requests before there was a successful
 * authentication which deactivates itself after successful authentication.
 */
public class SecurityInterceptor
{
    private final Logger fLogger;

    private final IAuthHandler fAuthHandler;

    /**
     * Creates a security interceptor using the given authentication handler.
     * The authentication handler must not be null.
     *
     * @param authHandler authentication handler to use
     */
    public SecurityInterceptor(IAuthHandler authHandler)
    {
        if(authHandler == null)
        {
            throw new NullPointerException("no authentication handler given");
        }

        fLogger = Logger.getLogger(this.getClass().getName());
        fAuthHandler = authHandler;
    }

    /**
     * Handles an authentication attempt from the client, i.e. a websocket
     * query with the type AUTHENTICATION and returns the authenticated user
     * if authentication was successful and null if not.
     *
     * @param query authentication request received
     * @param transfer utility to respond with
     * @return authenticated user or null
     */
    public String handle(WebsockQuery query, TransferUtil transfer)
    {
        String username =  fAuthHandler.handleAuthentication(query);
        int queryId = query.getId();

        if(username == null)
        {
            //send error message
            query = new WebsockQuery(queryId, EQueryType.ERROR);
            query.setPayload("authentication failure");
            send(query, transfer);

            //send new challenge
            query = fAuthHandler.getChallenge();
            if(query != null)
            {
                send(query, transfer);
            }
        }
        else
        {
            //send success message
            query = new WebsockQuery(queryId, EQueryType.SUCCESS);
            send(query, transfer);
        }

        return username;
    }

    private boolean send(WebsockQuery query, TransferUtil transfer)
    {
        boolean success = false;

        try
        {
            transfer.sendMessage(query);
            success = true;
        }
        catch(Exception e)
        {
            fLogger.log(Level.SEVERE,
                "failed to send authentication message", e);
        }

        return success;
    }

    /**
     * Method that is called then the client first connects. If available,
     * sends an initial authentication challenge message.
     *
     * @param transfer utility to send messages with
     */
    public void onConnect(TransferUtil transfer)
    {
        WebsockQuery query = fAuthHandler.getChallenge();

        if(transfer != null && query != null)
        {
            try
            {
                transfer.sendMessage(query);
            }
            catch(Exception e)
            {
                fLogger.log(Level.SEVERE,
                    "failed to send initial challenge message", e);
            }
        }
        else if(query != null)
        {
            fLogger.log(Level.SEVERE, "onConnect called but no transfer "
                + "utility available to send challenge");
        }
    }
}
