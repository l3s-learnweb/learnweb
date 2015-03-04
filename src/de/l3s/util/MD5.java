package de.l3s.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 
{
	public static String hash(String hashString) {
		try {
			MessageDigest md5;

			md5 = MessageDigest.getInstance("MD5");

			md5.reset();
			md5.update(hashString.getBytes());
			byte[] result = md5.digest();

			StringBuffer hexString = new StringBuffer();

			for (int i = 0; i < result.length; i++) {
				if(result[i] <= 15 && result[i] >= 0){
					hexString.append("0");
				}

				hexString.append(Integer.toHexString(0xFF & result[i]));
			}

			String hashCodeString = hexString.toString();

			return hashCodeString;
		} 
		catch (NoSuchAlgorithmException e) 
		{			
			e.printStackTrace();
			return "error while hashing";
		}
	}
}
