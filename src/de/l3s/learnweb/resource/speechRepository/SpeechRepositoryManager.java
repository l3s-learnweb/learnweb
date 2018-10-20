package de.l3s.learnweb.resource.speechRepository;

import java.sql.*;

public class SpeechRepositoryManager {
    private final String host;
    private final String database;
    private final String username;
    private final String password;
    private Connection connection;

    public SpeechRepositoryManager(String host, String database, String username, String password) {
        this.host = host;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null) {
            connection = DriverManager.getConnection("jdbc:mysql://" + host + "/"+ database + "?user=" + username + "&password=" + password + "&useAffectedRows=true");
        }

        return connection;
    }

    public Statement createStatement() throws SQLException {
        return getConnection().createStatement();
    }

    public ResultSet executeQuery(String sql) throws SQLException {
        return createStatement().executeQuery(sql);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return getConnection().prepareStatement(sql);
    }

    public void close() throws SQLException {
        connection.close();
    }
}
