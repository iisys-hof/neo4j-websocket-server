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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.hofuniversity.iisys.neo4j.websock.query.EQueryType;
import de.hofuniversity.iisys.neo4j.websock.query.WebsockQuery;
import de.hofuniversity.iisys.neo4j.websock.session.WebsockConstants;

/**
 * Default authentication handler using the default parameters and a
 * configurable password scrambler.
 */
public class DefaultAuthHandler implements IAuthHandler
{
    private static final String SEPARATOR = "=";

    private final Map<String, String> fPasswordHashes;
    private final IPasswordScrambler fScrambler;

    private final Logger fLogger;

    /**
     * Creates a default authentication handler, reading user accounts from the
     * given password file, using the given password scrambler to verify
     * passwords.
     * The scrambler must not be null and the user file String must point to a
     * readable file containing user information.
     *
     * @param userFile file containing user accounts
     * @param scrambler password scrambler to use
     * @throws Exception
     */
    public DefaultAuthHandler(String userFile, IPasswordScrambler scrambler)
        throws Exception
    {
        if(scrambler == null)
        {
            throw new NullPointerException("password scrambler was null");
        }

        fLogger = Logger.getLogger(this.getClass().getName());
        fPasswordHashes = new HashMap<String, String>();
        fScrambler = scrambler;

        final BufferedReader br = new BufferedReader(new FileReader(userFile));
        String line = br.readLine();
        int split = 0;
        String username = null;
        String password = null;

        while(line != null)
        {
            if(!line.isEmpty() && !line.startsWith("#"))
            {
                split = line.indexOf(SEPARATOR);
                username = line.substring(0, split);
                password = line.substring(split + 1, line.length());

                fPasswordHashes.put(username, password);
            }

            line = br.readLine();
        }
    }

    @Override
    public String handleAuthentication(WebsockQuery query)
    {
        boolean success = false;
        String username = null;

        try
        {
            if(query.getType() == EQueryType.AUTHENTICATION)
            {
                //data from request
                username = query.getParameter(
                    WebsockConstants.USERNAME).toString();
                String password = query.getParameter(
                    WebsockConstants.PASSWORD).toString();

                //compare to stored hashes
                String hash = fPasswordHashes.get(username);

                success = fScrambler.checkHashedPass(username, password, hash);
                if(!success)
                {
                    fScrambler.checkClearTextPass(username, password, hash);
                }
            }
        }
        catch(Exception e)
        {
            success = false;
        }

        if(success)
        {
            fLogger.log(Level.INFO, "Authentication successful for user '"
                + username + "'");
        }
        else
        {
            fLogger.log(Level.WARNING,
                "Authentication not successful for user '" + username + "'");
            username = null;
        }

        return username;
    }

    @Override
    public WebsockQuery getChallenge()
    {
        //no challenge
        return null;
    }
}
