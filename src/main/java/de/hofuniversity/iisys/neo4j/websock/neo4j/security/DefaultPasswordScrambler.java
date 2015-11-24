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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Default password scrambler which uses UUIDs as salt and by default SHA-512
 * for hashing. Resulting hashes will be formed over the concatenation of
 * user name and password, if no salt is specified preceded by the salt,
 * separated by ':'.
 */
public class DefaultPasswordScrambler implements IPasswordScrambler
{
    //TODO: unit test

    private static final String DEF_HASH_ALGORITHM = "SHA-512";
    private static final String ENCODING = "UTF-8";

    private final String fHashAlgo;

    /**
     * Creates a password scrambler using the SHA-512 hashing algorithm.
     */
    public DefaultPasswordScrambler()
    {
        this(DEF_HASH_ALGORITHM);
    }

    /**
     * Creates a password scrambler using the given hashing algorithm.
     * The algorithm must not be null and must be known to
     * MessageDigest.getInstance().
     *
     * @param algorithm algorithm to use for hashing
     */
    public DefaultPasswordScrambler(String algorithm)
    {
        if(algorithm == null || algorithm.isEmpty())
        {
            throw new NullPointerException("no algorithm specified");
        }

        fHashAlgo = algorithm;
    }

    /**
     * Directly hashes the given String in UTF-8 using the configured hashing
     * algorithm.
     *
     * @param text text to hash
     * @return hashed text
     */
    public String hash(String text)
    {
        byte[] hash = null;

        try
        {
            MessageDigest md = MessageDigest.getInstance(fHashAlgo);
            hash = md.digest(text.getBytes(ENCODING));
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        //convert to hex string
        StringBuilder hexString = new StringBuilder();
        for(byte b : hash)
        {
            hexString.append(Integer.toString(
                (b & 0xff) + 0x100, 16).substring(1));
        }

        return hexString.toString();
    }

    @Override
    public String scramble(String user, String clearText)
    {
        String salt = UUID.randomUUID().toString();

        return salt + ":" + scramble(user, clearText, salt);
    }

    @Override
    public String scramble(String user, String clearText, String salt)
    {
        //hash user name and password, add salt
        clearText = salt + hash(user + clearText);

        //hash password with salt
        return hash(clearText);
    }

    @Override
    public boolean checkClearTextPass(String user, String clearPass,
        String scrambledOriginal)
    {
        String[] parts = scrambledOriginal.split(":");
        return checkClearTextPass(user, clearPass, parts[1], parts[0]);
    }

    @Override
    public boolean checkHashedPass(String user, String hashedPass,
        String scrambledOriginal)
    {
        String[] parts = scrambledOriginal.split(":");
        return checkHashedPass(user, hashedPass, parts[1], parts[0]);
    }

    @Override
    public boolean checkClearTextPass(String user, String clearPass,
        String scrambledOriginal, String salt)
    {
        String newHash = scramble(user, clearPass, salt);
        return scrambledOriginal.equals(newHash);
    }

    @Override
    public boolean checkHashedPass(String user, String hashedPass,
        String scrambledOriginal, String salt)
    {
        String newHash = hash(salt + hashedPass);
        return scrambledOriginal.equals(newHash);
    }

    /**
     * Hashes the given combination of user name and password, optionally with
     * a specific hash function and a specified salt.
     * Parameters:
     *      -u user name
     *      -p password
     *      -h hash algorithm (optional)
     *      -s salt (optional)
     *
     * @param args argument vector
     */
    public static void main(String[] args)
    {
        final String userParam = "-u";
        final String passParam = "-p";
        final String hashParam = "-h";
        final String saltParam = "-s";

        String username = null;
        String password = null;
        String hashFunction = DEF_HASH_ALGORITHM;
        String salt = null;

        String value = null;
        for(int i = 0; i < args.length; ++i)
        {
            value = args[i];

            if(userParam.equals(value)
                && args.length > i + 1)
            {
                username = args[++i];
            }
            else if(passParam.equals(value)
                && args.length > i + 1)
            {
                password = args[++i];
            }
            else if(hashParam.equals(value)
                && args.length > i + 1)
            {
                hashFunction = args[++i];
            }
            else if(saltParam.equals(value)
                && args.length > i + 1)
            {
                salt = args[++i];
            }
        }

        if(username == null)
        {
            System.out.println("incomplete parameters, usage:");
            System.out.println("$COMMAND -u username [-p password]"
                + " [-h hash_function] [-s salt]");

            System.exit(1);
        }

        //read password from command line if needed
        if(password == null)
        {
            System.out.print("please enter a password: ");

            char[] passwordArr = System.console().readPassword();
            if(passwordArr != null && passwordArr.length > 0)
            {
                password = new String(passwordArr);
            }
        }

        //create hash if password was entered
        if(password != null && !password.isEmpty())
        {
            DefaultPasswordScrambler scrambler =
                new DefaultPasswordScrambler(hashFunction);

            String output = null;

            if(salt == null)
            {
                output = scrambler.scramble(username, password);
            }
            else
            {
                output = salt + ":" + scrambler.scramble(username, password,
                    salt);
            }

            System.out.println(output);
        }
        else
        {
            System.out.println("no password given");
            System.exit(1);
        }
    }
}
