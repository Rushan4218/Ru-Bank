import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BankService {

    public int createAccount(String name, double initialBalance) throws SQLException {
        String insertAccountSql = "INSERT INTO accounts (name, balance) VALUES (?, ?)";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertAccountSql, Statement.RETURN_GENERATED_KEYS)) {

            preparedStatement.setString(1, name);
            preparedStatement.setDouble(2, initialBalance);
            preparedStatement.executeUpdate();

            try (ResultSet resultSet = preparedStatement.getGeneratedKeys()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }

        throw new SQLException("Account could not be created.");
    }

    public boolean depositMoney(int accountId, double amount) throws SQLException {
        // First confirm the account exists before updating the balance.
        if (!accountExists(accountId)) {
            return false;
        }

        String updateBalanceSql = "UPDATE accounts SET balance = balance + ? WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateBalanceSql)) {

            preparedStatement.setDouble(1, amount);
            preparedStatement.setInt(2, accountId);

            int rowsUpdated = preparedStatement.executeUpdate();
            if (rowsUpdated > 0) {
                addTransaction(accountId, "deposit", amount);
                return true;
            }
        }

        return false;
    }

    public boolean withdrawMoney(int accountId, double amount) throws SQLException {
        // Read the current balance so we can prevent over-withdrawal.
        double currentBalance = getBalance(accountId);

        if (currentBalance < 0) {
            return false;
        }

        if (amount > currentBalance) {
            return false;
        }

        String updateBalanceSql = "UPDATE accounts SET balance = balance - ? WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateBalanceSql)) {

            preparedStatement.setDouble(1, amount);
            preparedStatement.setInt(2, accountId);

            int rowsUpdated = preparedStatement.executeUpdate();
            if (rowsUpdated > 0) {
                addTransaction(accountId, "withdraw", amount);
                return true;
            }
        }

        return false;
    }

    public double getBalance(int accountId) throws SQLException {
        String selectSql = "SELECT balance FROM accounts WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {

            preparedStatement.setInt(1, accountId);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getDouble("balance");
                }
            }
        }

        return -1;
    }

    public List<TransactionRecord> getTransactionHistory(int accountId) throws SQLException {
        List<TransactionRecord> transactions = new ArrayList<>();
        String selectSql = "SELECT id, account_id, type, amount, date FROM transactions WHERE account_id = ? ORDER BY date DESC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {

            preparedStatement.setInt(1, accountId);

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

    private boolean accountExists(int accountId) throws SQLException {
        String selectSql = "SELECT id FROM accounts WHERE id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSql)) {

            preparedStatement.setInt(1, accountId);

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
}
