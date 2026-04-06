import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    // PostgreSQL connection details
    private static final String URL = "jdbc:postgresql://localhost:5432/bankdb";
    private static final String USERNAME = "postgres";
    private static final String PASSWORD = "password";

    // Returns a database connection using DriverManager
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }
}
