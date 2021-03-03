package de.l3s.util;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;

import org.apache.commons.lang3.StringUtils;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Update;

public final class SqlHelper {

    public static Integer toNullable(int value) {
        return value == 0 ? null : value;
    }

    public static Long toNullable(long value) {
        return value == 0 ? null : value;
    }

    public static String toNullable(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    public static Instant getInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public static LocalDateTime getLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    public static LocalDateTime getLocalDateTime(final int lastVisit) {
        return LocalDateTime.ofEpochSecond(lastVisit, 0, ZoneOffset.UTC);
    }

    public static LocalDate getLocalDate(java.sql.Date date) {
        return date == null ? null : date.toLocalDate();
    }

    /**
     * Creates {@link Update} statement with `INSERT INTO tableName ON DUPLICATE KEY UPDATE ...` query and bound given parameters.
     * Assumes that the first column is the primary key of this table.
     */
    public static Update handleSave(final Handle handle, final String tableName, final LinkedHashMap<String, Object> params) {
        String[] keys = params.keySet().toArray(new String[0]);
        Update update = handle.createUpdate(generateInsertReplaceQuery(tableName, keys));

        for (int i = 0, len = keys.length; i < len; ++i) {
            update.bind(i, params.get(keys[i]));
        }

        return update;
    }

    /**
     * Creates `INSERT INTO tableName ON DUPLICATE KEY UPDATE` query.
     * Assumes that the first column is the primary key of this table.
     */
    public static String generateInsertReplaceQuery(final String tableName, final String[] columns) {
        StringBuilder sb = new StringBuilder("INSERT INTO ");
        sb.append(tableName).append(" (");

        for (String column : columns) {
            sb.append(column).append(',');
        }
        sb.setLength(sb.length() - 1); // remove last comma

        String questionMarks = StringUtils.repeat(",?", columns.length).substring(1);
        sb.append(") VALUES (").append(questionMarks).append(") ON DUPLICATE KEY UPDATE ");

        for (int i = 1, len = columns.length; i < len; i++) { // skip first column
            String column = columns[i];
            sb.append(column).append("=VALUES(").append(column).append("),");
        }
        sb.setLength(sb.length() - 1); // remove last comma

        return sb.toString();
    }

    /**
     * Creates `INSERT INTO tableName (column1,column2,column3) VALUES (?,?,?)` query.
     */
    public static String generateInsertQuery(final String tableName, final String[] columns) {
        StringBuilder sb = new StringBuilder("INSERT INTO ");
        sb.append(tableName).append(" (");

        for (String column : columns) {
            sb.append(column).append(',');
        }
        sb.setLength(sb.length() - 1); // remove last comma

        String questionMarks = StringUtils.repeat(",?", columns.length).substring(1);
        sb.append(") VALUES (").append(questionMarks).append(")");
        return sb.toString();
    }

    /**
     * Creates `UPDATE tableName SET column1=?,column2=?,column3=? WHERE primaryKey = ?` query.
     */
    public static String generateUpdateQuery(final String tableName, final String primaryKey, final String[] columns) {
        StringBuilder sb = new StringBuilder("UPDATE ");
        sb.append(tableName).append(" SET ");

        for (final String column : columns) {
            sb.append(column).append("=?,");
        }
        sb.setLength(sb.length() - 1); // remove last comma

        sb.append(" WHERE ").append(primaryKey).append("=?");
        return sb.toString();
    }
}
