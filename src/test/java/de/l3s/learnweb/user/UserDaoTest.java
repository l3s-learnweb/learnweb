
package de.l3s.learnweb.user;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.test.LearnwebExtension;
import de.l3s.util.HashHelper;
import de.l3s.util.SqlHelper;

class UserDaoTest {

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();
    private final UserDao userDao = learnwebExt.attach(UserDao.class);
    private final TokenDao tokenDao = learnwebExt.attach(TokenDao.class);

    @BeforeEach
    void setUp() {
        UserDao.cache.clear();
    }

    @Test
    void findById() {
        Optional<User> user = userDao.findById(1);
        assertTrue(user.isPresent());

        assertEquals(1, user.get().getId());
        assertFalse(user.get().isDeleted());
        assertEquals(0, user.get().getImageFileId());
        assertEquals(1, user.get().getOrganisationId());
        assertNull(user.get().getFullName());
        assertNull(user.get().getAffiliation());
        assertEquals("admin", user.get().getUsername());
        assertEquals("lwadmin@maildrop.cc", user.get().getEmail());
        assertTrue(user.get().isEmailConfirmed());
        assertEquals(310, user.get().getPassword().length());
        assertEquals(User.PasswordHashing.PBKDF2, user.get().getHashing());
        assertEquals(User.NotificationFrequency.NEVER, user.get().getPreferredNotificationFrequency());
        assertEquals(Locale.of("en", "UK"), user.get().getLocale());
        assertEquals(User.Gender.OTHER, user.get().getGender());
        assertNull(user.get().getDateOfBirth());
        assertNull(user.get().getAddress());
        assertNull(user.get().getProfession());
        assertNull(user.get().getInterest());
        assertNull(user.get().getStudentId());
        assertNull(user.get().getCredits());
        assertTrue(user.get().isAcceptTermsAndConditions());
        assertEquals(ZoneId.of("Europe/Berlin"), user.get().getTimeZone());
        assertTrue(user.get().isAdmin());
        assertTrue(user.get().isModerator());
        assertEquals(LocalDateTime.of(2021, 2, 18, 11, 27, 19), user.get().getCreatedAt());
    }

    @Test
    void findByUsername() {
        Optional<User> username = userDao.findByUsername("moderator");
        assertTrue(username.isPresent());
        assertEquals(2, username.get().getId());
    }

    @Test
    void findByUsernameAndPassword() {
        Optional<User> userByInfo = userDao.findByUsernameAndPassword("admin", "admin");

        assertTrue(userByInfo.isPresent());
        assertEquals("admin", userByInfo.get().getUsername());
        assertEquals(1, userByInfo.get().getId());
    }

    @Test
    void findByEmail() {
        List<User> users = userDao.findByEmail("lwadmin@maildrop.cc");
        assertFalse(users.isEmpty());
    }

    @Test
    void findAll() {
        List<User> users = userDao.findAll();
        assertFalse(users.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}, users.stream().map(User::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findWithEnabledForumNotifications() {
        List<User> users = userDao.findWithEnabledForumNotifications();
        assertFalse(users.isEmpty());
        assertArrayEquals(new Integer[] {3, 7, 9}, users.stream().map(User::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByOrganisationId() {
        List<User> users = userDao.findByOrganisationId(1);
        assertFalse(users.isEmpty());
        assertArrayEquals(new Integer[] {1, 2, 3, 7, 9}, users.stream().map(User::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByCourseId() {
        List<User> users = userDao.findByCourseId(2);
        assertFalse(users.isEmpty());
        assertArrayEquals(new Integer[] {4, 6, 8, 10}, users.stream().map(User::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByGroupId() {
        List<User> users = userDao.findByGroupId(1);
        assertFalse(users.isEmpty());
        assertArrayEquals(new Integer[] {1, 4, 6}, users.stream().map(User::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findByGroupIdLastJoined() {
        List<User> users = userDao.findByGroupIdLastJoined(1, 10);
        assertFalse(users.isEmpty());
        assertArrayEquals(new Integer[] {1, 4, 6}, users.stream().map(User::getId).sorted().toArray(Integer[]::new));
    }

    @Test
    void findLastLoginDate() {
        Optional<LocalDateTime> loginDate = userDao.findLastLoginDate(1);
        assertTrue(loginDate.isPresent());
        assertEquals(LocalDateTime.of(2021, 2, 19, 18, 34, 10), loginDate.get());
    }

    @Test
    void countByCourseId() {
        assertEquals(5, userDao.countByCourseId(1));
    }

    @Test
    void countCoursesByUserId() {
        assertEquals(1, userDao.countCoursesByUserId(1));
    }

    @Test
    void authToken() {
        String token = RandomStringUtils.secure().nextAlphanumeric(128);

        int tokenId = tokenDao.insert(2, Token.TokenType.AUTH, HashHelper.sha512(token), SqlHelper.now().plusDays(31));

        Optional<User> userByAuthToken = tokenDao.findUserByToken(tokenId, HashHelper.sha512(token));
        assertTrue(userByAuthToken.isPresent());

        tokenDao.delete(tokenId);

        Optional<User> userByDeletedAuthToken = tokenDao.findUserByToken(tokenId, HashHelper.sha512(token));
        assertTrue(userByDeletedAuthToken.isEmpty());
    }

    @Test
    void grantToken() {
        String token = RandomStringUtils.secure().nextAlphanumeric(128);

        int tokenId = tokenDao.insert(1, Token.TokenType.GRANT, token, SqlHelper.now().plusDays(31));

        // should create a new token and store in database
        String grantToken = tokenDao.findOrCreate(Token.TokenType.GRANT, 1);
        assertEquals(token, grantToken);

        Optional<User> user = tokenDao.findUserByToken(tokenId, token);
        assertTrue(user.isPresent());
        assertEquals(1, user.get().getId());
    }

    @Test
    void deleteSoft() {
        Optional<User> user = userDao.findById(7);

        assertTrue(user.isPresent());

        userDao.deleteSoft(user.get());

        Optional<User> retrieved = userDao.findById(7);
        assertFalse(retrieved.isEmpty());
    }

    @Test
    void deleteHard() {
        Optional<User> user = userDao.findById(9);
        assertTrue(user.isPresent());

        userDao.deleteHard(user.get());

        Optional<User> retrieved = userDao.findById(9);
        assertTrue(retrieved.isEmpty());
    }

    @Test
    void save() {
        User user = new User();
        user.setUsername("thecapac");
        user.setOrganisationId(2);
        user.setHashing("PBKDF2");
        user.setTimeZone(ZoneId.of("Europe/Berlin"));
        userDao.save(user);

        Optional<User> retrieved = userDao.findByUsername("thecapac");
        assertTrue(retrieved.isPresent());
        assertEquals(ZoneId.of("Europe/Berlin"), retrieved.get().getTimeZone());
    }

    @Test
    void anonymize() {
        Optional<User> user = userDao.findById(2);
        userDao.anonymize(user.orElseThrow());
        assertEquals("", user.orElseThrow().getAddress());
    }
}
