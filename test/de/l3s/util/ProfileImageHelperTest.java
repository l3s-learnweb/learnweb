package de.l3s.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ProfileImageHelperTest {

    @Test
    @Disabled("Remote host terminated the handshake on GitLab :(")
    void getGravatarAvatar() {
        ImmutableTriple<String, String, InputStream> gravatar = ProfileImageHelper.getGravatarAvatar("205e460b479e2e5b48aec07710c08d50");

        assertNotNull(gravatar);
        assertEquals("205e460b479e2e5b48aec07710c08d50.png", gravatar.getLeft());
        assertEquals("image/png", gravatar.getMiddle());
        assertNotNull(gravatar.getRight());
    }

    @Test
    @Disabled("Remote host terminated the handshake on GitLab :(")
    void getGravatarAvatarMissing() {
        ImmutableTriple<String, String, InputStream> gravatar = ProfileImageHelper.getGravatarAvatar("00000000000000000000000000000000");

        assertNull(gravatar);
    }

    @Test
    void getProfilePicture() {
        assertEquals("#514A9D", ProfileImageHelper.getColorForProfilePicture("Hello"));
    }

    @Test
    void getColorForProfilePicture() {
        assertEquals("#8f3b4f", ProfileImageHelper.getColorForProfilePicture("Hello World"));
        assertEquals("#4a505d", ProfileImageHelper.getColorForProfilePicture("HW"));
    }

    @Test
    void getInitialsForProfilePicture() {
        assertEquals("HW", ProfileImageHelper.getInitialsForProfilePicture("HW"));
        assertEquals("hw", ProfileImageHelper.getInitialsForProfilePicture("hello world"));
        assertEquals("h6", ProfileImageHelper.getInitialsForProfilePicture("hello 156"));
        assertEquals("HW", ProfileImageHelper.getInitialsForProfilePicture("Hello World"));
        assertEquals("HbW", ProfileImageHelper.getInitialsForProfilePicture("Hello blabla World")); // FIXME
        assertEquals("HbbW", ProfileImageHelper.getInitialsForProfilePicture("Hello bla bla World")); // FIXME
        assertEquals("HW", ProfileImageHelper.getInitialsForProfilePicture("HelloWorld"));
        assertEquals("H", ProfileImageHelper.getInitialsForProfilePicture("Hello"));
        assertEquals("46", ProfileImageHelper.getInitialsForProfilePicture("14584546"));
    }
}
