package de.l3s.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

/**
 * It is used to hash emails, never use it for passwords!
 * @author Astappiev
 */
public class SHA512
{
    public static String hash(String value)
    {
        try
        {
            if (value == null || value.isEmpty()) return null;
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return String.format("%0128x", new BigInteger(1, bytes));
        }
        catch(NoSuchAlgorithmException e)
        {
            Logger.getLogger(SHA512.class).error("fatal hashing error", e);

            return "error while hashing";
        }
    }
}
