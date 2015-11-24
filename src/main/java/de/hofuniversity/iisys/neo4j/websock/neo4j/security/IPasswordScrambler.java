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

/**
 * Utility interface using cryptographic methods to securely store a password
 * and check incoming clear text and hashed passwords.
 */
public interface IPasswordScrambler
{
    /**
     * Scrambles a clear text password and returns the more secure version. In
     * this version the returned String may include a salt.
     *
     * @param user name of the user
     * @param clearText password to scramble
     * @return more secure version, optionally with salt
     */
    public String scramble(String user, String clearText);

    /**
     * Scrambles a clear text password using a provided salt and returns
     * the more secure version.
     *
     * @param user name of the user
     * @param clearText password to scramble
     * @param salt salt to use when scrambling the password
     * @return more secure version, without the salt
     */
    public String scramble(String user, String clearText, String salt);

    /**
     * Checks whether the given clear text password matches the given scrambled
     * password which may contain a salt.
     *
     * @param user name of the user
     * @param clearPass clear text password to check
     * @param scrambledOriginal scrambled password, optionally with salt
     * @return whether the passwords match
     */
    public boolean checkClearTextPass(String user, String clearPass,
        String scrambledOriginal);

    /**
     * Checks whether the given hashed password matches the given scrambled
     * password which may contain a salt.
     * The hashed password needs to be hashed in the scrambler's expected
     * hash format.
     *
     * @param user name of the user
     * @param hashedPass hashed password to check
     * @param scrambledOriginal scrambled password, optionally with salt
     * @return whether the passwords match
     */
    public boolean checkHashedPass(String user, String hashedPass,
        String scrambledOriginal);

    /**
     * Checks whether the given clear text password matches the given scrambled
     * password which uses the given salt.
     *
     * @param user name of the user
     * @param clearPass clear text password to check
     * @param scrambledOriginal scrambled password without salt
     * @param salt salt used to scramble the password
     * @return whether the passwords match
     */
    public boolean checkClearTextPass(String user, String clearPass,
        String scrambledOriginal, String salt);

    /**
     * Checks whether the given hashed password matches the given scrambled
     * password which uses the given salt.
     * The hashed password needs to be hashed in the scrambler's expected
     * hash format.
     *
     * @param user name of the user
     * @param hashedPass hashed password to check
     * @param scrambledOriginal scrambled password without salt
     * @param salt salt used to scramble the password
     * @return whether the passwords match
     */
    public boolean checkHashedPass(String user, String hashedPass,
        String scrambledOriginal, String salt);
}
