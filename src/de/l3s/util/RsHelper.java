package de.l3s.util;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

public final class RsHelper {
    public static Date getDate(Timestamp timestamp) {
        return timestamp == null ? null : new Date(timestamp.getTime());
    }

    public static Date getDate(java.sql.Date date) {
        return date == null ? null : new Date(date.getTime());
    }

    public static LocalDateTime getLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    public static LocalDate getLocalDate(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().toLocalDate();
    }

    public static LocalDate getLocalDate(java.sql.Date date) {
        return date == null ? null : date.toLocalDate();
    }
}
