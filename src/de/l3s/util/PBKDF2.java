package de.l3s.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.log4j.Logger;

public class PBKDF2
{
    // The following constants may be changed without breaking existing hashes.
    private static final int ITERATIONS = 1000;
    private static final int HASH_BYTES = 128;
    private static final int SALT_BYTES = 24;

    /**
     * Returns a salted PBKDF2 hash of the password.
     *
     * @param password the password to hash
     * @return a salted PBKDF2 hash of the password
     */
    public static String hashPassword(String password)
    {
        try
        {
            byte[] salt = salt(SALT_BYTES);
            byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, HASH_BYTES);

            // format iterations:salt:hashPassword
            return ITERATIONS + ":" + StringHelper.toHex(salt) + ":" + StringHelper.toHex(hash);
        }
        catch(NoSuchAlgorithmException | InvalidKeySpecException e)
        {
            Logger.getLogger(PBKDF2.class).error("fatal hashing error", e);
            return "error while hashing";
        }
    }

    /**
     * Validates a password using a hashPassword.
     *
     * @param password     the password to check
     * @param passwordHash the hashPassword of the valid password
     * @return true if the password is correct, false if not
     */
    public static boolean validatePassword(String password, String passwordHash)
    {
        try
        {
            // format iterations:salt:hashPassword
            String[] params = passwordHash.split(":");

            int iterations = Integer.parseInt(params[0]);
            byte[] salt = StringHelper.fromHex(params[1]);
            byte[] hash = StringHelper.fromHex(params[2]);

            // use the same salt, iteration count and length
            byte[] testHash = pbkdf2(password.toCharArray(), salt, iterations, hash.length);
            return Arrays.equals(hash, testHash);
        }
        catch(NoSuchAlgorithmException | InvalidKeySpecException e)
        {
            Logger.getLogger(PBKDF2.class).error("fatal validating hash error", e);
            return false;
        }
    }

    private static byte[] salt(final int bytes)
    {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[bytes];
        random.nextBytes(salt);
        return salt;
    }

    private static byte[] pbkdf2(final char[] password, final byte[] salt, final int iterations, final int bytes) throws NoSuchAlgorithmException, InvalidKeySpecException
    {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, bytes * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
        return skf.generateSecret(spec).getEncoded();
    }
}
