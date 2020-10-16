package de.l3s.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HashHelperTest {
    private static final String TEST_VALUE = "randomemail@gmail.com";

    @Test
    void hashMd5() {
        assertNull(HashHelper.sha512(null));
        assertNull(HashHelper.sha512(""));

        String hash = HashHelper.md5(TEST_VALUE);
        assertEquals("eb2a7c5c436a9f861e510e8593875221", hash);
    }

    @Test
    void hash256() {
        assertNull(HashHelper.sha512(null));
        assertNull(HashHelper.sha512(""));

        String hash = HashHelper.sha256(TEST_VALUE);
        assertEquals("1581b7cd5f747ba382b4da5b9851b763596e1d5ba3fb6eca3831ed415db3aacb", hash);
    }

    @Test
    void hash512() {
        assertNull(HashHelper.sha512(null));
        assertNull(HashHelper.sha512(""));

        String hash = HashHelper.sha512(TEST_VALUE);
        assertEquals("7ca8783b55fcd7845176ee7075a38faee3a9a97590a8fb1a39be5132f0008ccd1f810478cbe1bc1ee1df781c6e3cd6c28142bef551052499c43f325821cf5215", hash);
    }
}
