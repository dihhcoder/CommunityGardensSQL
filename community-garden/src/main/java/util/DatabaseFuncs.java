package util;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseFuncs {
    private static final String URL = "jdbc:mysql://localhost:3306/G04_V01?serverTimezone=Asia/Manila";
    private static final String USER = "root"; //username mysql connections (replace if necessary)
    private static final String PASSWORD = "Ph@nt0m01"; //server password (replace if necessary)
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static String generateNextCode(Connection connection, String table, String column, String prefix) throws SQLException {
        String statement = "SELECT MAX(" + column + ") FROM " + table + " WHERE " + column + " LIKE ?";
        try (PreparedStatement prepState = connection.prepareStatement(statement)) {
            prepState.setString(1, prefix + "%");
            try (ResultSet result = prepState.executeQuery()) {
                if (result.next() && result.getString(1) != null) {
                    String maxCode = result.getString(1);
                    int num = Integer.parseInt(maxCode.substring(prefix.length()));
                    return String.format("%s%03d", prefix, num + 1);
                }
            }
        }
        return prefix + "001";
    }
}
