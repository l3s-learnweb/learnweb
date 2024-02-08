package de.l3s.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ProfileImageHelperTest {

    @Test
    @Disabled("Remote HTTP requests should be disabled on CI")
    void getGravatarAvatar() {
        ImmutableTriple<String, String, InputStream> gravatar = ProfileImageHelper.getGravatarAvatar("205e460b479e2e5b48aec07710c08d50");

        assertNotNull(gravatar);
        assertEquals("205e460b479e2e5b48aec07710c08d50.png", gravatar.getLeft());
        assertEquals("image/png", gravatar.getMiddle());
        assertNotNull(gravatar.getRight());
    }

    @Test
    @Disabled("Remote HTTP requests should be disabled on CI")
    void getGravatarAvatarMissing() {
        ImmutableTriple<String, String, InputStream> gravatar = ProfileImageHelper.getGravatarAvatar("00000000000000000000000000000000");

        assertNull(gravatar);
    }

    @Test
    void getProfilePicture() {
        assertEquals("rgba(81,74,157,1.000)", ProfileImageHelper.getColorForProfilePicture("Hello"));
    }

    @Test
    void getColorForProfilePicture() {
        assertEquals("rgba(143,59,79,1.000)", ProfileImageHelper.getColorForProfilePicture("Hello World"));
        assertEquals("rgba(74,80,93,1.000)", ProfileImageHelper.getColorForProfilePicture("HW"));
    }

    @Test
    void getInitialsForProfilePicture() {
        assertEquals("HW", ProfileImageHelper.getInitialsForProfilePicture("HW"));
        assertEquals("hw", ProfileImageHelper.getInitialsForProfilePicture("hello.world"));
        assertEquals("hw", ProfileImageHelper.getInitialsForProfilePicture("hello world"));
        assertEquals("h6", ProfileImageHelper.getInitialsForProfilePicture("hello 156"));
        assertEquals("HW", ProfileImageHelper.getInitialsForProfilePicture("Hello World"));
        assertEquals("HW", ProfileImageHelper.getInitialsForProfilePicture("Hello blabla World"));
        assertEquals("HW", ProfileImageHelper.getInitialsForProfilePicture("Hello bla bla World"));
        assertEquals("HW", ProfileImageHelper.getInitialsForProfilePicture("HelloWorld"));
        assertEquals("H", ProfileImageHelper.getInitialsForProfilePicture("Hello"));
        assertEquals("46", ProfileImageHelper.getInitialsForProfilePicture("14584546"));
        assertEquals("hW", ProfileImageHelper.getInitialsForProfilePicture(" hello World"));
    }
}
