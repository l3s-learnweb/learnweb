package de.l3s.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

public class MD5
{
    public static String hash(String hashString)
    {
        try
        {
            MessageDigest md5;

            md5 = MessageDigest.getInstance("MD5");

            md5.reset();
            md5.update(hashString.getBytes());
            byte[] result = md5.digest();

            StringBuilder hexString = new StringBuilder();
            for(final byte aResult : result)
            {
                if(aResult <= 15 && aResult >= 0)
                    hexString.append("0");

                hexString.append(Integer.toHexString(0xFF & aResult));
            }

            return hexString.toString();
        }
        catch(NoSuchAlgorithmException e)
        {
            Logger.getLogger(MD5.class).error("fatal hashing error", e);

            return "error while hashing";
        }
    }
}
