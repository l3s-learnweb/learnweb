package de.l3s.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Oleh Astappiev
 */
public final class HashHelper {

    /**
     * Don't use it! :/
     *
     * @return 32 characters string
     */
    public static String md5(String value) {
        try {
            if (StringUtils.isNotEmpty(value)) {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
                return String.format("%032x", new BigInteger(1, bytes));
            }
        } catch (NoSuchAlgorithmException ignore) {
        }
        return null;
    }

    /**
     * @return 64 characters string
     */
    public static String sha256(String value) {
        try {
            if (StringUtils.isNotEmpty(value)) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
                return String.format("%064x", new BigInteger(1, bytes));
            }
        } catch (NoSuchAlgorithmException ignore) {
        }
        return null;
    }

    /**
     * A utility method which indicates whether a value is valid according to the stored hash.
     */
    public static boolean isValidSha256(String value, String hash) {
        return hash.equals(sha256(value));
    }

    /**
     * @return 128 characters string
     */
    public static String sha512(String value) {
        try {
            if (StringUtils.isNotEmpty(value)) {
                MessageDigest md = MessageDigest.getInstance("SHA-512");
                byte[] bytes = md.digest(value.getBytes(StandardCharsets.UTF_8));
                return String.format("%0128x", new BigInteger(1, bytes));
            }
        } catch (NoSuchAlgorithmException ignore) {
        }
        return null;
    }

    /**
     * A utility method which indicates whether a value is valid according to the stored hash.
     */
    public static boolean isValidSha512(String value, String hash) {
        return hash.equals(sha512(value));
    }
}
