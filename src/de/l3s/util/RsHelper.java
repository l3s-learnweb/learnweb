package de.l3s.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class RsHelper {

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

    public static Integer getInteger(ResultSet rs, String columnLabel) throws SQLException {
        Integer value = rs.getInt(columnLabel);
        if (rs.wasNull()) {
            return null;
        }
        return value;
    }
}
