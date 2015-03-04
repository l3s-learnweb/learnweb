package de.l3s.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import de.l3s.learnweb.Learnweb;

public class Sql {

	public static Object getSingleResult(String query) throws SQLException
	{
		Connection connection = Learnweb.getInstance().getConnection();
		ResultSet rs = connection.createStatement().executeQuery(query);
		if(!rs.next())
			throw new IllegalArgumentException("Query doesn't return a result");
		return rs.getObject(1);
	}
}
