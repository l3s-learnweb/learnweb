package de.l3s.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.l3s.learnweb.Learnweb;

public class Sql {
    private static final Logger log = LogManager.getLogger(Sql.class);

    public static Object getSingleResult(String query) throws SQLException {
        Connection connection = Learnweb.getInstance().getConnection();
        ResultSet rs = connection.createStatement().executeQuery(query);
        if (!rs.next()) {
            throw new IllegalArgumentException("Query doesn't return a result");
        }
        return rs.getObject(1);
    }

    public static void setSerializedObject(PreparedStatement stmt, int parameterIndex, Object obj) throws SQLException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            ObjectOutputStream oos = new ObjectOutputStream(outputStream);
            oos.writeObject(obj);

            byte[] employeeAsBytes = outputStream.toByteArray();

            stmt.setBinaryStream(parameterIndex, new ByteArrayInputStream(employeeAsBytes), employeeAsBytes.length);

            return;
        } catch (Exception e) {
            log.error("Couldn't serialize preferences: " + obj, e);
        }

        stmt.setNull(parameterIndex, java.sql.Types.BLOB);
    }

    public static Object getSerializedObject(ResultSet rs, String field) throws SQLException {
        byte[] columnBytes = rs.getBytes(field);

        if (columnBytes != null && columnBytes.length > 0) {
            ByteArrayInputStream columnBAIS = new ByteArrayInputStream(columnBytes);

            try {
                ObjectInputStream columnOIS = new ObjectInputStream(columnBAIS);

                // re-create the object
                Object column = columnOIS.readObject();

                if (column != null) {
                    return column;
                }
            } catch (Exception e) {
                log.error("Couldn't load column " + field, e);
            }
        }
        return null;
    }

    public static Timestamp convertDateTime(Date date) {
        if (date == null) {
            return null;
        }

        return new java.sql.Timestamp(date.getTime());
    }

    /**
     * Creates INSERT INTO tableName ON DUPLICATE KEY UPDATE statement.
     * Assumes that the first column is the primary key of this table.
     */
    public static String getCreateStatement(final String tableName, final String[] columns) {
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
