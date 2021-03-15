package de.l3s.learnweb;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import de.l3s.learnweb.group.Group;
import de.l3s.learnweb.logging.ActionCategory;
import de.l3s.learnweb.logging.ActionTargetId;
import de.l3s.learnweb.resource.File;
import de.l3s.learnweb.resource.Resource;
import de.l3s.learnweb.resource.ResourceService;
import de.l3s.learnweb.resource.ResourceType;
import de.l3s.learnweb.resource.search.SearchMode;
import de.l3s.learnweb.resource.survey.SurveyQuestion;
import de.l3s.learnweb.resource.ted.TedManager;
import de.l3s.learnweb.searchhistory.SearchHistoryDao;
import de.l3s.learnweb.user.Token;
import de.l3s.learnweb.user.User;
import de.l3s.test.LearnwebExtension;

class EnumIntegrityTest {
    private static final Pattern SPLIT_PATTERN = Pattern.compile(",\\s?");

    @RegisterExtension
    static final LearnwebExtension learnwebExt = new LearnwebExtension();

    @Test
    void testFile() throws SQLException {
        assertArrayEquals(Arrays.stream(File.FileType.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_file", "type"));
    }

    @Test
    void testGroupPolicy() throws SQLException {
        assertArrayEquals(Arrays.stream(Group.PolicyAdd.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_group", "policy_add"));
        assertArrayEquals(Arrays.stream(Group.PolicyAnnotate.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_group", "policy_annotate"));
        assertArrayEquals(Arrays.stream(Group.PolicyEdit.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_group", "policy_edit"));
        assertArrayEquals(Arrays.stream(Group.PolicyJoin.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_group", "policy_join"));
        assertArrayEquals(Arrays.stream(Group.PolicyView.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_group", "policy_view"));
    }

    @Test
    void testResourcePolicy() throws SQLException {
        assertArrayEquals(Arrays.stream(Resource.StorageType.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_resource", "storage_type"));
        assertArrayEquals(Arrays.stream(Resource.PolicyView.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_resource", "policy_view"));
        assertArrayEquals(Arrays.stream(Resource.OnlineStatus.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_resource", "online_status"));
    }

    @Test
    void testResourceService() throws SQLException {
        String[] values = Arrays.stream(ResourceService.values()).map(Enum::name).toArray(String[]::new);
        assertArrayEquals(values, getDatabaseColumnEnumValues("lw_resource", "service"));
        assertArrayEquals(values, getDatabaseColumnEnumValues("learnweb_large.sl_query", "service"));
    }

    @Test
    void testResourceType() throws SQLException {
        String[] values = Arrays.stream(ResourceType.values()).map(Enum::name).toArray(String[]::new);
        assertArrayEquals(values, getDatabaseColumnEnumValues("lw_resource", "type"));
    }

    @Test
    void testSurvey() throws SQLException {
        assertArrayEquals(Arrays.stream(SurveyQuestion.QuestionType.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_survey_question", "question_type"));
    }

    @Test
    void testTranscript() throws SQLException {
        assertArrayEquals(Arrays.stream(TedManager.SummaryType.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_transcript_summary", "summary_type"));
    }

    @Test
    void testUser() throws SQLException {
        String[] notificationFrequencies = Arrays.stream(User.NotificationFrequency.values()).map(Enum::name).toArray(String[]::new);
        assertArrayEquals(notificationFrequencies, getDatabaseColumnEnumValues("lw_user", "preferred_notification_frequency"));
        assertArrayEquals(notificationFrequencies, getDatabaseColumnEnumValues("lw_group_user", "notification_frequency"));
        assertArrayEquals(Arrays.stream(User.PasswordHashing.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_user", "hashing"));
    }

    @Test
    void testUserLog() throws SQLException {
        assertArrayEquals(Arrays.stream(ActionTargetId.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_user_log_action", "target"));
        assertArrayEquals(Arrays.stream(ActionCategory.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_user_log_action", "category"));
    }

    @Test
    void testUserToken() throws SQLException {
        assertArrayEquals(Arrays.stream(Token.TokenType.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_user_token", "type"));
    }

    @Test
    void testSearchHistory() throws SQLException {
        assertArrayEquals(Arrays.stream(SearchHistoryDao.SearchAction.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("learnweb_large.sl_action", "action"));
        assertArrayEquals(Arrays.stream(SearchMode.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("learnweb_large.sl_query", "mode"));
    }

    private String getDatabaseColumnType(final String tableName, final String columnName) throws SQLException {
        // the default database name is the ones provided in database url, so we need to retrieve it
        String catalog = learnwebExt.getHandle().getConnection().getCatalog(); // random UUID for H2, database name for MySQL
        String schema = learnwebExt.getHandle().getConnection().getSchema(); // `PUBLIC` for H2, null for MySQL

        // IDK why, but mysql requires that TABLE_NAME values was lowercase and H2 requires uppercase, so this LOWER is necessary here!
        return learnwebExt.getHandle().select("SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND "
            + "LOWER(TABLE_NAME) = LOWER(?) AND LOWER(COLUMN_NAME) = LOWER(?)", ObjectUtils.firstNonNull(schema, catalog), tableName, columnName).mapTo(String.class).one();
    }

    private String[] getDatabaseColumnEnumValues(final String tableName, final String columnName) throws SQLException {
        final String columnType = getDatabaseColumnType(tableName, columnName);
        String types = columnType.substring(columnType.indexOf('(') + 1, columnType.indexOf(')')).replace("'", "");
        return SPLIT_PATTERN.split(types);
    }
}
