package de.l3s.searchHistoryTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

public class Sql
{
    private final static Logger log = Logger.getLogger(Sql.class);

    public static void setSerializedObject(PreparedStatement stmt, int parameterIndex, Object obj) throws SQLException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try
        {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);

            byte[] employeeAsBytes = baos.toByteArray();

            stmt.setBinaryStream(parameterIndex, new ByteArrayInputStream(employeeAsBytes), employeeAsBytes.length);

            return;
        }
        catch(Exception e)
        {
            log.error("Couldn't serialize preferences: " + obj.toString(), e);
        }

        stmt.setNull(parameterIndex, java.sql.Types.BLOB);
    }
    
    public static Object getSerializedObject(ResultSet rs, String field) throws SQLException
    {
        byte[] columnBytes = rs.getBytes(field);

        if(columnBytes != null && columnBytes.length > 0)
        {
            ByteArrayInputStream columnBAIS = new ByteArrayInputStream(columnBytes);

            try
            {
                ObjectInputStream columnOIS = new ObjectInputStream(columnBAIS);

                // re-create the object
                Object column = columnOIS.readObject();

                if(column != null)
                	return column;
            }
            catch(Exception e)
            {
                log.error("Couldn't load column " + field, e);
            }
        }
        return null;
    }
}

