import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BankService {

    public int registerUser(String username, String password, String displayName) throws SQLException {
        String insertUserSql = "INSERT INTO users (username, password, display_name) VALUES (?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);
            preparedStatement.setString(3, displayName);
            preparedStatement.executeUpdate();

            try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }

        throw new SQLException("User could not be registered.");
    }

    public int registerUserWithAccount(String username, String password, String displayName, String accountName) throws SQLException {
        Connection connection = null;
        try {
            connection = DBConnection.getConnection();
            connection.setAutoCommit(false);

            // Insert user
            String insertUserSql = "INSERT INTO users (username, password, display_name) VALUES (?, ?, ?)";
            int userId;
            try (PreparedStatement userStmt = connection.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                userStmt.setString(1, username);
                userStmt.setString(2, password);
                userStmt.setString(3, displayName);
                userStmt.executeUpdate();

                try (ResultSet resultSet = userStmt.getGeneratedKeys()) {
                    if (resultSet.next()) {
                        userId = resultSet.getInt(1);
                    } else {
                        throw new SQLException("User could not be registered.");
                    }
                }
            }

            // Create account with 0 balance
            String insertAccountSql = "INSERT INTO accounts (user_id, name, balance) VALUES (?, ?, ?)";
            try (PreparedStatement accountStmt = connection.prepareStatement(insertAccountSql)) {
                accountStmt.setInt(1, userId);
                accountStmt.setString(2, accountName);
                accountStmt.setDouble(3, 0.0);
                accountStmt.executeUpdate();
            }

            connection.commit();
            return userId;
        } catch (SQLException ex) {
            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    throw new SQLException("Error during transaction rollback: " + rollbackEx.getMessage());
                }
            }
            throw new SQLException("Error registering user with account: " + ex.getMessage());
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException closeEx) {
                    // Ignore close errors
                }
            }
        }
    }

    public Integer authenticateUser(String username, String password) throws SQLException {
        String selectSql = "SELECT id FROM users WHERE username = ? AND password = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {

            preparedStatement.setString(1, username);
            preparedStatement.setString(2, password);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }

        return null;
    }

    public int createAccount(int userId, String name, double initialBalance) throws SQLException {
        String insertAccountSql = "INSERT INTO accounts (user_id, name, balance) VALUES (?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertAccountSql, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setInt(1, userId);
            preparedStatement.setString(2, name);
            preparedStatement.setDouble(3, initialBalance);
            preparedStatement.executeUpdate();

            try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }

        throw new SQLException("Account could not be created.");
    }

    public boolean depositMoney(int userId, int accountId, double amount) throws SQLException {
        // First confirm the account exists before updating the balance.
        if (!accountExists(userId, accountId)) {
            return false;
        }

        String updateBalanceSql = "UPDATE accounts SET balance = balance + ? WHERE id = ? AND user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateBalanceSql)) {

            preparedStatement.setDouble(1, amount);
            preparedStatement.setInt(2, accountId);
            preparedStatement.setInt(3, userId);

            int rowsUpdated = preparedStatement.executeUpdate();
            if (rowsUpdated > 0) {
                addTransaction(accountId, "deposit", amount);
                return true;
            }
        }

        return false;
    }

    public boolean withdrawMoney(int userId, int accountId, double amount) throws SQLException {
        // Read the current balance so we can prevent over-withdrawal.
        double currentBalance = getBalance(userId, accountId);

        if (currentBalance < 0) {
            return false;
        }

        if (amount > currentBalance) {
            return false;
        }

        String updateBalanceSql = "UPDATE accounts SET balance = balance - ? WHERE id = ? AND user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateBalanceSql)) {

            preparedStatement.setDouble(1, amount);
            preparedStatement.setInt(2, accountId);
            preparedStatement.setInt(3, userId);

            int rowsUpdated = preparedStatement.executeUpdate();
            if (rowsUpdated > 0) {
                addTransaction(accountId, "withdraw", amount);
                return true;
            }
        }

        return false;
    }

    public double getBalance(int userId, int accountId) throws SQLException {
        String selectSql = "SELECT balance FROM accounts WHERE id = ? AND user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {

            preparedStatement.setInt(1, accountId);
            preparedStatement.setInt(2, userId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("balance");
                }
            }
        }

        return -1;
    }

    public List<TransactionRecord> getTransactionHistory(int userId, int accountId, boolean ascending) throws SQLException {
        List<TransactionRecord> transactions = new ArrayList<>();
        String sortDirection = ascending ? "ASC" : "DESC";
        String selectSql = "SELECT t.id, t.account_id, t.type, t.amount, t.date " +
                "FROM transactions t JOIN accounts a ON t.account_id = a.id " +
                "WHERE t.account_id = ? AND a.user_id = ? ORDER BY t.date " + sortDirection;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {

            preparedStatement.setInt(1, accountId);
            preparedStatement.setInt(2, userId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    TransactionRecord record = new TransactionRecord(
                            resultSet.getInt("id"),
                            resultSet.getInt("account_id"),
                            resultSet.getString("type"),
                            resultSet.getDouble("amount"),
                            resultSet.getTimestamp("date").toString()
                    );
                    transactions.add(record);
                }
            }
        }

        return transactions;
    }

    private boolean accountExists(int userId, int accountId) throws SQLException {
        String selectSql = "SELECT id FROM accounts WHERE id = ? AND user_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {

            preparedStatement.setInt(1, accountId);
            preparedStatement.setInt(2, userId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void addTransaction(int accountId, String type, double amount) throws SQLException {
        // Store every deposit and withdrawal so it can be shown in the history table.
        String insertTransactionSql = "INSERT INTO transactions (account_id, type, amount) VALUES (?, ?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertTransactionSql)) {

            preparedStatement.setInt(1, accountId);
            preparedStatement.setString(2, type);
            preparedStatement.setDouble(3, amount);
            preparedStatement.executeUpdate();
        }
    }

    public List<AccountInfo> getUserAccounts(int userId) throws SQLException {
        List<AccountInfo> accounts = new ArrayList<>();
        String selectSql = "SELECT id, name, balance FROM accounts WHERE user_id = ? ORDER BY id";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {

            preparedStatement.setInt(1, userId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    AccountInfo account = new AccountInfo(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getDouble("balance")
                    );
                    accounts.add(account);
                }
            }
        }

        return accounts;
    }

    public String getUserDisplayName(int userId) throws SQLException {
        String selectSql = "SELECT display_name FROM users WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {

            preparedStatement.setInt(1, userId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("display_name");
                }
            }
        }

        return null;
    }

    public AccountInfo getUserAccount(int userId) throws SQLException {
        String selectSql = "SELECT id, name, balance FROM accounts WHERE user_id = ? LIMIT 1";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {

            preparedStatement.setInt(1, userId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return new AccountInfo(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getDouble("balance")
                    );
                }
            }
        }

        return null;
    }
}
