package de.l3s.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HashHelperTest {
    private static final String TEST_VALUE = "test value to hash";

    @Test
    void hash256() {
        assertNull(HashHelper.sha512(null));
        assertNull(HashHelper.sha512(""));

        String hash = HashHelper.sha256(TEST_VALUE);
        assertEquals("12b263a565322a9bc7ae12a50100cb759caf620575c97c3d05188921625a142c", hash);
    }

    @Test
    void hash512() {
        assertNull(HashHelper.sha512(null));
        assertNull(HashHelper.sha512(""));

        String hash = HashHelper.sha512(TEST_VALUE);
        assertEquals("2f6c8b7bcfd764b3ee54b4e85e545b198f2557b8d4f00c5abf2dc431d8c45a1d2b4af1ad2f9137f2a718874b69f4468d2f3645f345d2ccf939e543cb4659187d", hash);
    }
}
