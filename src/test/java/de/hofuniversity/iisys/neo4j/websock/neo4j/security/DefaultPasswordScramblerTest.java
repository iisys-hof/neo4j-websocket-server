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

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test routines for the server's default password scrambler, verifying
 * password hashing and verification.
 */
public class DefaultPasswordScramblerTest
{
    //default: 512bit SHA -> hex String length
    private static final int DEF_HASH_LENGHT = 512/4;

    /**
     * Tests the hashing and verification process using automatically generated
     * salts.
     */
    @Test
    public void autoSaltTest()
    {
        String user = "demo";
        String password = "demonstration";

        final DefaultPasswordScrambler scrambler =
            new DefaultPasswordScrambler();

        //check general structure
        String saltedHash1 = scrambler.scramble(user, password);
        String saltedHash2 = scrambler.scramble(user, password);
        String saltedHash3 = scrambler.scramble(user, password);
        Assert.assertTrue(saltedHash1.contains(":"));

        int sepIndex = saltedHash1.indexOf(':');
        Assert.assertTrue(sepIndex > 0);
        //appropriate position in a SHA-512 hex hash String
        Assert.assertEquals(saltedHash1.length() - DEF_HASH_LENGHT - 1,
            sepIndex);

        sepIndex = saltedHash2.indexOf(':');
        Assert.assertTrue(sepIndex > 0);
        Assert.assertEquals(saltedHash2.length() - DEF_HASH_LENGHT - 1,
            sepIndex);

        sepIndex = saltedHash3.indexOf(':');
        Assert.assertTrue(sepIndex > 0);
        Assert.assertEquals(saltedHash3.length() - DEF_HASH_LENGHT - 1,
            sepIndex);

        //make sure hashes will differ because of salt
        String unsaltedHash1 = saltedHash1.substring(sepIndex + 1);
        String unsaltedHash2 = saltedHash2.substring(sepIndex + 1);
        String unsaltedHash3 = saltedHash3.substring(sepIndex + 1);

        Assert.assertNotEquals(unsaltedHash1, unsaltedHash2);
        Assert.assertNotEquals(unsaltedHash1, unsaltedHash3);
        Assert.assertNotEquals(unsaltedHash2, unsaltedHash3);

        //check password hashing and verification
        String passHash = scrambler.hash(user + password);
        Assert.assertTrue(scrambler.checkHashedPass(user, passHash,
            saltedHash1));
        Assert.assertTrue(scrambler.checkHashedPass(user, passHash,
            saltedHash2));
        Assert.assertTrue(scrambler.checkHashedPass(user, passHash,
            saltedHash3));

        //check clear text verification
        Assert.assertTrue(scrambler.checkClearTextPass(user, password,
            saltedHash1));
        Assert.assertTrue(scrambler.checkClearTextPass(user, password,
            saltedHash2));
        Assert.assertTrue(scrambler.checkClearTextPass(user, password,
            saltedHash3));

        //negative checks
        String wrongUser = "demu";
        String wrongPass = "showcase";

        //hashed
        String wrongHash1 = scrambler.hash(wrongUser + password);
        String wrongHash2 = scrambler.hash(user + wrongPass);

        Assert.assertFalse(scrambler.checkHashedPass(user, wrongHash1,
            saltedHash1));
        Assert.assertFalse(scrambler.checkHashedPass(user, wrongHash1,
            saltedHash2));
        Assert.assertFalse(scrambler.checkHashedPass(user, wrongHash1,
            saltedHash3));

        Assert.assertFalse(scrambler.checkHashedPass(wrongUser, wrongHash1,
            saltedHash1));
        Assert.assertFalse(scrambler.checkHashedPass(wrongUser, wrongHash1,
            saltedHash2));
        Assert.assertFalse(scrambler.checkHashedPass(wrongUser, wrongHash1,
            saltedHash3));

        Assert.assertFalse(scrambler.checkHashedPass(user, wrongHash2,
            saltedHash1));
        Assert.assertFalse(scrambler.checkHashedPass(user, wrongHash2,
            saltedHash2));
        Assert.assertFalse(scrambler.checkHashedPass(user, wrongHash2,
            saltedHash3));

        Assert.assertFalse(scrambler.checkHashedPass(wrongUser, wrongHash2,
            saltedHash1));
        Assert.assertFalse(scrambler.checkHashedPass(wrongUser, wrongHash2,
            saltedHash2));
        Assert.assertFalse(scrambler.checkHashedPass(wrongUser, wrongHash2,
            saltedHash3));

        //clear text
        Assert.assertFalse(scrambler.checkClearTextPass(wrongUser, password,
            saltedHash1));
        Assert.assertFalse(scrambler.checkClearTextPass(wrongUser, password,
            saltedHash2));
        Assert.assertFalse(scrambler.checkClearTextPass(wrongUser, password,
            saltedHash3));

        Assert.assertFalse(scrambler.checkClearTextPass(user, wrongPass,
            saltedHash1));
        Assert.assertFalse(scrambler.checkClearTextPass(user, wrongPass,
            saltedHash2));
        Assert.assertFalse(scrambler.checkClearTextPass(user, wrongPass,
            saltedHash3));
    }

    /**
     * Tests the hashing and verification process using manually generated
     * salts.
     */
    @Test
    public void extSaltTest()
    {
        String user = "demo";
        String password = "demonstration";

        Random random = new Random();
        String salt1 = Long.toHexString(random.nextLong());
        String salt2 = Long.toHexString(random.nextLong());
        String salt3 = Long.toHexString(random.nextLong());

        final DefaultPasswordScrambler scrambler =
            new DefaultPasswordScrambler();

        //no hash should be included
        String unsaltedHash1 = scrambler.scramble(user, password, salt1);
        String unsaltedHash2 = scrambler.scramble(user, password, salt2);
        String unsaltedHash3 = scrambler.scramble(user, password, salt3);

        Assert.assertEquals(-1, unsaltedHash1.indexOf(':'));
        Assert.assertEquals(DEF_HASH_LENGHT, unsaltedHash1.length());
        Assert.assertEquals(-1, unsaltedHash2.indexOf(':'));
        Assert.assertEquals(DEF_HASH_LENGHT, unsaltedHash2.length());
        Assert.assertEquals(-1, unsaltedHash3.indexOf(':'));
        Assert.assertEquals(DEF_HASH_LENGHT, unsaltedHash3.length());

        //make sure hashes will differ because of salt
        Assert.assertNotEquals(unsaltedHash1, unsaltedHash2);
        Assert.assertNotEquals(unsaltedHash1, unsaltedHash3);
        Assert.assertNotEquals(unsaltedHash2, unsaltedHash3);

        //check password hashing and verification
        String passHash = scrambler.hash(user + password);
        Assert.assertTrue(scrambler.checkHashedPass(user, passHash,
            unsaltedHash1, salt1));
        Assert.assertTrue(scrambler.checkHashedPass(user, passHash,
            unsaltedHash2, salt2));
        Assert.assertTrue(scrambler.checkHashedPass(user, passHash,
            unsaltedHash3, salt3));

        //check clear text verification
        Assert.assertTrue(scrambler.checkClearTextPass(user, password,
            unsaltedHash1, salt1));
        Assert.assertTrue(scrambler.checkClearTextPass(user, password,
            unsaltedHash2, salt2));
        Assert.assertTrue(scrambler.checkClearTextPass(user, password,
            unsaltedHash3, salt3));


        //negative checks
        String wrongUser = "demu";
        String wrongPass = "showcase";

        //hashed
        String wrongHash1 = scrambler.hash(wrongUser + password);
        String wrongHash2 = scrambler.hash(user + wrongPass);

        Assert.assertFalse(scrambler.checkHashedPass(user, wrongHash1,
            unsaltedHash1, salt1));
        Assert.assertFalse(scrambler.checkHashedPass(user, wrongHash1,
            unsaltedHash2, salt2));
        Assert.assertFalse(scrambler.checkHashedPass(user, wrongHash1,
            unsaltedHash3, salt3));

        Assert.assertFalse(scrambler.checkHashedPass(wrongUser, wrongHash1,
            unsaltedHash1, salt1));
        Assert.assertFalse(scrambler.checkHashedPass(wrongUser, wrongHash1,
            unsaltedHash2, salt2));
        Assert.assertFalse(scrambler.checkHashedPass(wrongUser, wrongHash1,
            unsaltedHash3, salt3));

        Assert.assertFalse(scrambler.checkHashedPass(user, wrongHash2,
            unsaltedHash1, salt1));
        Assert.assertFalse(scrambler.checkHashedPass(user, wrongHash2,
            unsaltedHash2, salt2));
        Assert.assertFalse(scrambler.checkHashedPass(user, wrongHash2,
            unsaltedHash3, salt3));

        Assert.assertFalse(scrambler.checkHashedPass(wrongUser, wrongHash2,
            unsaltedHash1, salt1));
        Assert.assertFalse(scrambler.checkHashedPass(wrongUser, wrongHash2,
            unsaltedHash2, salt2));
        Assert.assertFalse(scrambler.checkHashedPass(wrongUser, wrongHash2,
            unsaltedHash3, salt3));

        //clear text
        Assert.assertFalse(scrambler.checkClearTextPass(wrongUser, password,
            unsaltedHash1, salt1));
        Assert.assertFalse(scrambler.checkClearTextPass(wrongUser, password,
            unsaltedHash2, salt2));
        Assert.assertFalse(scrambler.checkClearTextPass(wrongUser, password,
            unsaltedHash3, salt3));

        Assert.assertFalse(scrambler.checkClearTextPass(user, wrongPass,
            unsaltedHash1, salt1));
        Assert.assertFalse(scrambler.checkClearTextPass(user, wrongPass,
            unsaltedHash2, salt2));
        Assert.assertFalse(scrambler.checkClearTextPass(user, wrongPass,
            unsaltedHash3, salt3));
    }

    /**
     * Tests the hashing and verification process using different hashing
     * algorithms.
     */
    @Test
    public void hashAlgoTest()
    {
        String user = "demo";
        String password = "demonstration";

        //MD5
        final DefaultPasswordScrambler md5scrambler =
            new DefaultPasswordScrambler("MD5");

        String md5Hash = md5scrambler.scramble(user, password);
        int sepIndex = md5Hash.indexOf(':');
        Assert.assertEquals(32, md5Hash.substring(sepIndex + 1).length());

        //hashed password
        String md5PassHash = md5scrambler.hash(user + password);
        Assert.assertTrue(md5scrambler.checkHashedPass(user, md5PassHash,
            md5Hash));

        //clear text
        Assert.assertTrue(md5scrambler.checkClearTextPass(user, password,
            md5Hash));

        //negative check
        String wrongUser = "demu";
        String wrongPass = "showcase";

        //hashed
        String wrongMd5Hash1 = md5scrambler.hash(wrongUser + password);
        String wrongMd5Hash2 = md5scrambler.hash(user + wrongPass);

        Assert.assertFalse(md5scrambler.checkHashedPass(user, wrongMd5Hash1,
            md5Hash));

        Assert.assertFalse(md5scrambler.checkHashedPass(wrongUser,
            wrongMd5Hash1, md5Hash));

        Assert.assertFalse(md5scrambler.checkHashedPass(user, wrongMd5Hash2,
            md5Hash));

        Assert.assertFalse(md5scrambler.checkHashedPass(wrongUser,
            wrongMd5Hash2, md5Hash));

        //clear text
        Assert.assertFalse(md5scrambler.checkClearTextPass(wrongUser, password,
            md5Hash));

        Assert.assertFalse(md5scrambler.checkClearTextPass(user, wrongPass,
            md5Hash));


        //SHA-1
        final DefaultPasswordScrambler shaScrambler =
            new DefaultPasswordScrambler("SHA-1");

        String shaHash = shaScrambler.scramble(user, password);
        sepIndex = shaHash.indexOf(':');
        Assert.assertEquals(40, shaHash.substring(sepIndex + 1).length());

        //hashed password
        String shaPassHash = shaScrambler.hash(user + password);
        Assert.assertTrue(shaScrambler.checkHashedPass(user, shaPassHash,
            shaHash));

        //clear text
        Assert.assertTrue(shaScrambler.checkClearTextPass(user, password,
            shaHash));

        //negative check
        //hashed
        String wrongShaHash1 = shaScrambler.hash(wrongUser + password);
        String wrongShaHash2 = shaScrambler.hash(user + wrongPass);

        Assert.assertFalse(shaScrambler.checkHashedPass(user, wrongShaHash1,
            shaHash));

        Assert.assertFalse(shaScrambler.checkHashedPass(wrongUser,
            wrongShaHash1, shaHash));

        Assert.assertFalse(shaScrambler.checkHashedPass(user, wrongShaHash2,
            shaHash));

        Assert.assertFalse(shaScrambler.checkHashedPass(wrongUser,
            wrongShaHash2, shaHash));

        //clear text
        Assert.assertFalse(shaScrambler.checkClearTextPass(wrongUser, password,
            shaHash));

        Assert.assertFalse(shaScrambler.checkClearTextPass(user, wrongPass,
            shaHash));
    }
}
