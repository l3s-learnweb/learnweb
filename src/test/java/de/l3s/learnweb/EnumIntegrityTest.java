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
        assertArrayEquals(values, getDatabaseColumnEnumValues("lw_search_history", "service"));
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
        assertArrayEquals(Arrays.stream(SearchHistoryDao.SearchAction.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_search_history_action", "action"));
        assertArrayEquals(Arrays.stream(SearchMode.values()).map(Enum::name).toArray(), getDatabaseColumnEnumValues("lw_search_history", "mode"));
    }

    private String[] getDatabaseColumnEnumValues(String tableName, final String columnName) throws SQLException {
        String databaseType = learnwebExt.getHandle().getConnection().getMetaData().getDatabaseProductName();
        String catalog = learnwebExt.getHandle().getConnection().getCatalog(); // random UUID for H2, default database name provided in database url for MariaDB
        String schema = learnwebExt.getHandle().getConnection().getSchema(); // `PUBLIC` for H2, null for MariaDB

        if (tableName.contains(".")) {
            String[] parts = tableName.split("\\.");
            schema = parts[0];
            tableName = parts[1];
        }

        if ("H2".equals(databaseType)) {
            return learnwebExt.getHandle().select("SELECT v.value_name FROM information_schema.enum_values v INNER JOIN information_schema.columns c "
                    + "ON v.object_schema = c.table_schema AND v.object_name = c.table_name AND v.enum_identifier = c.dtd_identifier "
                    + "WHERE object_schema = ? AND object_name = ? AND column_name = ?", ObjectUtils.firstNonNull(schema, catalog), tableName, columnName)
                .mapTo(String.class).list().toArray(new String[0]);
        } else {
            String columnType = learnwebExt.getHandle().select("SELECT column_type FROM information_schema.columns WHERE table_schema = ? AND "
                + "table_name = ? AND column_name = ?", ObjectUtils.firstNonNull(schema, catalog), tableName, columnName).mapTo(String.class).one();

            String types = columnType.substring(columnType.indexOf('(') + 1, columnType.indexOf(')')).replace("'", "");
            return SPLIT_PATTERN.split(types);
        }
    }
}
