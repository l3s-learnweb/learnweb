package de.l3s.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * It is used to hash emails, never use it for passwords!
 *
 * @author Astappiev
 */
public class MD5 {
    private static final Logger log = LogManager.getLogger(MD5.class);

    public static String hash(String hashString) {
        try {
            MessageDigest md5;

            md5 = MessageDigest.getInstance("MD5");

            md5.reset();
            md5.update(hashString.getBytes(StandardCharsets.UTF_8));
            byte[] result = md5.digest();

            StringBuilder hexString = new StringBuilder();
            for (final byte aResult : result) {
                if (aResult <= 15 && aResult >= 0) {
                    hexString.append("0");
                }

                hexString.append(Integer.toHexString(0xFF & aResult));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("fatal hashing error", e);

            return "error while hashing";
        }
    }
}
