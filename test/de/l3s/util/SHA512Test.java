package de.l3s.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;


class SHA512Test
{

    @Test
    void hash()
    {
        String email = "randomemail@gmail.com";
        String hash = SHA512.hash(email);
        assertNull(SHA512.hash(null));
        assertNull(SHA512.hash(""));
        assertEquals("7ca8783b55fcd7845176ee7075a38faee3a9a97590a8fb1a39be5132f0008ccd1f810478cbe1bc1ee1df781c6e3cd6c28142bef551052499c43f325821cf5215", hash);
    }
}