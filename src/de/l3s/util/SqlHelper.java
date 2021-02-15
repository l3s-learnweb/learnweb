package de.l3s.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;
import java.util.LinkedHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.statement.Update;

public final class SqlHelper {
    private static final Logger log = LogManager.getLogger(SqlHelper.class);

    public static void setSerializedObject(PreparedStatement stmt, int parameterIndex, Serializable obj) throws SQLException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (ObjectOutputStream oos = new ObjectOutputStream(outputStream)) {
            oos.writeObject(obj);

            byte[] employeeAsBytes = outputStream.toByteArray();
            stmt.setBinaryStream(parameterIndex, new ByteArrayInputStream(employeeAsBytes), employeeAsBytes.length);
        } catch (Exception e) {
            log.error("Couldn't serialize object: {}", obj, e);
            stmt.setNull(parameterIndex, Types.BLOB);
        }
    }

    public static Object getSerializedObject(ResultSet rs, String field) throws SQLException {
        byte[] columnBytes = rs.getBytes(field);

        if (columnBytes != null && columnBytes.length > 0) {
            ByteArrayInputStream columnBAIS = new ByteArrayInputStream(columnBytes);

            try (ObjectInputStream ois = new ObjectInputStream(columnBAIS)) {
                // re-create the object
                Object column = ois.readObject();

                return column;
            } catch (Exception e) {
                log.error("Couldn't load column {}", field, e);
            }
        }
        return null;
    }

    public static Timestamp convertDateTime(Date date) {
        if (date == null) {
            return null;
        }

        return new Timestamp(date.getTime());
    }

    /**
     * Creates {@link Update} statement with `INSERT INTO tableName ON DUPLICATE KEY UPDATE ...` query and bound given parameters.
     * Assumes that the first column is the primary key of this table.
     */
    public static Update generateInsertQuery(final Handle handle, final String tableName, final LinkedHashMap<String, Object> params) {
        String[] keys = params.keySet().toArray(new String[0]);
        Update update = handle.createUpdate(generateInsertQuery(tableName, keys));

        for (int i = 0, len = keys.length; i < len; ++i) {
            update.bind(i, params.get(keys[i]));
        }

        return update;
    }

    /**
     * Creates INSERT INTO tableName ON DUPLICATE KEY UPDATE statement.
     * Assumes that the first column is the primary key of this table.
     */
    public static String generateInsertQuery(final String tableName, final String[] columns) {
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
}
