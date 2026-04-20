# Ru-Bank – Full Source Code

## Folder Structure

```
Ru-Bank/
├── postgresql-42.7.9.jar          # PostgreSQL JDBC driver (not committed to source)
├── README.md
├── sourcecode.md
├── .gitignore
├── scripts/
│   ├── run_app.sh                 # Compiles and runs the application
│   └── run_migrations.sh          # Runs the database migration SQL
├── sql/
│   └── bankdb.sql                 # Full database schema (migration SQL)
└── src/
    ├── AccountInfo.java           # Model – bank account data
    ├── BankService.java           # Business logic & SQL operations
    ├── BankingAppUI.java          # Swing UI (main entry point)
    ├── DBConnection.java          # PostgreSQL connection factory
    └── TransactionRecord.java     # Model – transaction history row
```

---

## Source Files

### `src/AccountInfo.java`

```java
public class AccountInfo {
    private final int id;
    private final String name;
    private final double balance;

    public AccountInfo(int id, String name, double balance) {
        this.id = id;
        this.name = name;
        this.balance = balance;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    @Override
    public String toString() {
        return name + " (ID: " + id + ") - Balance: $" + String.format("%.2f", balance);
    }
}
```

---

### `src/TransactionRecord.java`

```java
public class TransactionRecord {
    private final int id;
    private final int accountId;
    private final String type;
    private final double amount;
    private final String date;

    public TransactionRecord(int id, int accountId, String type, double amount, String date) {
        this.id = id;
        this.accountId = accountId;
        this.type = type;
        this.amount = amount;
        this.date = date;
    }

    public int getId() {
        return id;
    }

    public int getAccountId() {
        return accountId;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public String getDate() {
        return date;
    }
}
```

---

### `src/DBConnection.java`

```java
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
```

---

### `src/BankService.java`

```java
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
```

---

### `src/BankingAppUI.java`

```java
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

public class BankingAppUI extends JFrame {
  private final BankService bankService;
  private static final Font APP_FONT = new Font("SansSerif", Font.PLAIN, 16);
  private static final Font TITLE_FONT = new Font("SansSerif", Font.BOLD, 24);
  private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.##");

  private JTextField amountField;
  private JCheckBox withdrawCheckbox;
  private JLabel currentBalanceLabel;
  private DefaultTableModel tableModel;
  private JLabel sessionLabel;
  private JLabel userInfoLabel;
  private JLabel accountInfoLabel;

  private Integer currentUserId;
  private String currentUsername = "Not logged in";
  private String currentDisplayName = "";
  private AccountInfo userAccount;
  private boolean historyAscending;

  public BankingAppUI() {
    bankService = new BankService();
    initializeUI();
    enforceLoginOnStartup();
  }

  private void initializeUI() {
    setApplicationFont();

    setTitle("RuBank");
    setSize(900, 540);
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLocationRelativeTo(null);
    ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    setJMenuBar(createMenuBar());

    JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
    inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    JLabel titleLabel = new JLabel("RuBank", JLabel.CENTER);
    titleLabel.setFont(TITLE_FONT);
    titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    sessionLabel = new JLabel();
    sessionLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    updateSessionLabel();

    userInfoLabel = new JLabel();
    userInfoLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    userInfoLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
    updateUserInfoLabel();

    accountInfoLabel = new JLabel();
    accountInfoLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    accountInfoLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

    amountField = new JTextField();
    withdrawCheckbox = new JCheckBox("Withdraw (unchecked = Deposit)");
    currentBalanceLabel = new JLabel();
    currentBalanceLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
    updateCurrentBalanceLabel();

    inputPanel.add(currentBalanceLabel);
    inputPanel.add(new JLabel(""));
    inputPanel.add(new JLabel("Amount:"));
    inputPanel.add(amountField);
    inputPanel.add(withdrawCheckbox);
    inputPanel.add(new JLabel(""));

    JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 10));
    JButton transactionButton = new JButton("Process Transaction");
    JButton historyButton = new JButton("View History");

    buttonPanel.add(transactionButton);
    buttonPanel.add(historyButton);

    String[] columnNames = { "Transaction ID", "Account ID", "Type", "Amount", "Date" };
    tableModel = new DefaultTableModel(columnNames, 0);
    JTable historyTable = new JTable(tableModel);
    JScrollPane scrollPane = new JScrollPane(historyTable);
    scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

    JPanel topPanel = new JPanel(new BorderLayout(10, 10));
    topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    JPanel titlePanel = new JPanel(new BorderLayout());
    titlePanel.add(titleLabel, BorderLayout.CENTER);
    titlePanel.add(sessionLabel, BorderLayout.EAST);
    topPanel.add(titlePanel, BorderLayout.NORTH);
    JPanel infoPanel = new JPanel(new GridLayout(2, 1, 5, 5));
    infoPanel.add(userInfoLabel);
    infoPanel.add(accountInfoLabel);
    topPanel.add(infoPanel, BorderLayout.CENTER);

    JPanel formPanel = new JPanel(new BorderLayout(10, 10));
    formPanel.add(inputPanel, BorderLayout.NORTH);
    formPanel.add(buttonPanel, BorderLayout.SOUTH);
    topPanel.add(formPanel, BorderLayout.CENTER);

    add(topPanel, BorderLayout.NORTH);
    add(scrollPane, BorderLayout.CENTER);

    transactionButton.addActionListener(e -> processTransaction());
    historyButton.addActionListener(e -> viewTransactionHistory());

    // Initialize labels after all components are created
    updateAccountInfoLabel();
    updateCurrentBalanceLabel();
  }

  private JMenuBar createMenuBar() {
    JMenuBar menuBar = new JMenuBar();

    JMenu accountMenu = new JMenu("Account");
    JMenuItem loginItem = new JMenuItem("Login");
    JMenuItem registerItem = new JMenuItem("Register");
    JMenuItem logoutItem = new JMenuItem("Logout");
    JMenuItem exitItem = new JMenuItem("Exit");

    loginItem.addActionListener(e -> loginUser());
    registerItem.addActionListener(e -> registerUser());
    logoutItem.addActionListener(e -> logoutUser());
    exitItem.addActionListener(e -> System.exit(0));

    accountMenu.add(loginItem);
    accountMenu.add(registerItem);
    accountMenu.add(logoutItem);
    accountMenu.add(exitItem);

    JMenu historyMenu = new JMenu("History");
    JMenuItem sortNewestItem = new JMenuItem("Sort: Newest to Oldest");
    JMenuItem sortOldestItem = new JMenuItem("Sort: Oldest to Newest");
    JMenuItem refreshHistoryItem = new JMenuItem("Refresh History");

    sortNewestItem.addActionListener(e -> {
      historyAscending = false;
      updateSessionLabel();
      refreshHistoryIfAccountPresent();
    });
    sortOldestItem.addActionListener(e -> {
      historyAscending = true;
      updateSessionLabel();
      refreshHistoryIfAccountPresent();
    });
    refreshHistoryItem.addActionListener(e -> viewTransactionHistory());

    historyMenu.add(sortNewestItem);
    historyMenu.add(sortOldestItem);
    historyMenu.add(refreshHistoryItem);

    menuBar.add(accountMenu);
    menuBar.add(historyMenu);
    return menuBar;
  }

  private void setApplicationFont() {
    UIManager.put("Label.font", APP_FONT);
    UIManager.put("Button.font", APP_FONT);
    UIManager.put("TextField.font", APP_FONT);
    UIManager.put("Table.font", APP_FONT);
    UIManager.put("TableHeader.font", APP_FONT);
    UIManager.put("OptionPane.messageFont", APP_FONT);
    UIManager.put("OptionPane.buttonFont", APP_FONT);
  }

  private void createAccount() {
    if (!requireLogin()) {
      return;
    }

    if (userAccount != null) {
      JOptionPane.showMessageDialog(this, "You already have an account.");
      return;
    }

    JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
    JTextField nameField = new JTextField();
    JTextField balanceField = new JTextField();

    panel.add(new JLabel("Account Holder Name:"));
    panel.add(nameField);
    panel.add(new JLabel("Initial Balance:"));
    panel.add(balanceField);

    int result = JOptionPane.showConfirmDialog(
        this,
        panel,
        "Create Account",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }

    String name = nameField.getText().trim();
    String balanceText = balanceField.getText().trim();
    if (name.isEmpty() || balanceText.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Please enter name and initial balance.");
      return;
    }

    try {
      double initialBalance = Double.parseDouble(balanceText);
      if (initialBalance < 0) {
        JOptionPane.showMessageDialog(this, "Initial balance cannot be negative.");
        return;
      }

      int accountId = bankService.createAccount(currentUserId, name, initialBalance);
      JOptionPane.showMessageDialog(this, "Account created successfully. Account ID: " + accountId);
      refreshUserAccount();
      updateUserInfoLabel();
    } catch (NumberFormatException ex) {
      JOptionPane.showMessageDialog(this, "Please enter a valid number for balance.");
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(this, "Error creating account: " + ex.getMessage());
    }
  }

  private void processTransaction() {
    if (!requireLogin()) {
      return;
    }

    if (userAccount == null) {
      int result = JOptionPane.showConfirmDialog(
          this,
          "No account found. Would you like to create one now?",
          "Account Required",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE);
      if (result == JOptionPane.YES_OPTION) {
        createAccount();
      }
      return;
    }

    Double amount = parseAmount();
    if (amount == null) {
      return;
    }

    if (amount <= 0) {
      JOptionPane.showMessageDialog(this, "Amount must be greater than zero.");
      return;
    }

    boolean isWithdraw = withdrawCheckbox.isSelected();
    String transactionType = isWithdraw ? "withdraw" : "deposit";

    try {
      boolean success;
      if (isWithdraw) {
        if (amount > userAccount.getBalance()) {
          JOptionPane.showMessageDialog(this, "Insufficient balance.");
          return;
        }
        success = bankService.withdrawMoney(currentUserId, userAccount.getId(), amount);
      } else {
        success = bankService.depositMoney(currentUserId, userAccount.getId(), amount);
      }

      if (success) {
        JOptionPane.showMessageDialog(this, "Money " + transactionType + "ed successfully.");
        clearTransactionFields();
        refreshUserAccount();
      } else {
        JOptionPane.showMessageDialog(this,
            transactionType.substring(0, 1).toUpperCase() + transactionType.substring(1) + " failed.");
      }
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(this, "Error processing transaction: " + ex.getMessage());
    }
  }

  private void viewTransactionHistory() {
    if (!requireLogin()) {
      return;
    }

    if (userAccount == null) {
      JOptionPane.showMessageDialog(this, "No account found. Please create an account first.");
      return;
    }

    try {
      List<TransactionRecord> transactions = bankService.getTransactionHistory(currentUserId, userAccount.getId(),
          historyAscending);
      tableModel.setRowCount(0);

      for (TransactionRecord record : transactions) {
        Object[] row = {
            record.getId(),
            record.getAccountId(),
            record.getType(),
            formatAmount(record.getAmount()),
            record.getDate()
        };
        tableModel.addRow(row);
      }

      if (transactions.isEmpty()) {
        JOptionPane.showMessageDialog(this, "No transactions found for " + userAccount.getName() + ".");
      }
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(this, "Error loading history: " + ex.getMessage());
    }
  }

  private Double parseAmount() {
    String amountText = amountField.getText().trim();
    if (amountText.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Please enter an amount.");
      return null;
    }

    try {
      return Double.parseDouble(amountText);
    } catch (NumberFormatException ex) {
      JOptionPane.showMessageDialog(this, "Please enter a valid amount.");
      return null;
    }
  }

  private void clearCreateAccountFields() {
    // No fields to clear as account info is now displayed as labels
  }

  private void clearTransactionFields() {
    amountField.setText("");
  }

  private String formatAmount(double amount) {
    return MONEY_FORMAT.format(amount);
  }

  private boolean requireLogin() {
    if (currentUserId == null) {
      JOptionPane.showMessageDialog(this, "Please login first from Account menu.");
      return false;
    }
    return true;
  }

  private void refreshHistoryIfAccountPresent() {
    if (userAccount != null && currentUserId != null) {
      viewTransactionHistory();
    }
  }

  private void refreshUserAccount() {
    if (currentUserId != null) {
      try {
        userAccount = bankService.getUserAccount(currentUserId);
        updateAccountInfoLabel();
        updateCurrentBalanceLabel();
      } catch (SQLException ex) {
        JOptionPane.showMessageDialog(this, "Error refreshing account: " + ex.getMessage());
      }
    }
  }

  private void updateUserInfoLabel() {
    if (currentUserId != null && !currentDisplayName.isEmpty()) {
      if (userAccount != null) {
        userInfoLabel.setText("Welcome, " + currentDisplayName + "! (Account ready)");
      } else {
        userInfoLabel.setText("Welcome, " + currentDisplayName + "! (Setup required)");
      }
    } else {
      userInfoLabel.setText("Please login to view your information");
    }
  }

  private void updateAccountInfoLabel() {
    if (userAccount != null) {
      accountInfoLabel.setText("Account Holder: " + userAccount.getName());
    } else {
      accountInfoLabel.setText("Click 'Account' menu > 'Create Account' to setup your banking account");
    }
    updateCurrentBalanceLabel();
  }

  private void updateCurrentBalanceLabel() {
    if (userAccount != null) {
      currentBalanceLabel.setText("Current Balance: " + formatAmount(userAccount.getBalance()));
    } else {
      currentBalanceLabel.setText("Current Balance: No account");
    }
  }

  private void updateSessionLabel() {
    String orderText = historyAscending ? "Oldest -> Newest" : "Newest -> Oldest";
    sessionLabel.setText("User: " + currentUsername + " | History: " + orderText);
  }

  private void enforceLoginOnStartup() {
    while (currentUserId == null) {
      String[] options = { "Login", "Register", "Exit" };
      int choice = JOptionPane.showOptionDialog(
          this,
          "Please login or register to use RuBank.",
          "Authentication Required",
          JOptionPane.DEFAULT_OPTION,
          JOptionPane.INFORMATION_MESSAGE,
          null,
          options,
          options[0]);

      if (choice == 0) {
        loginUser();
      } else if (choice == 1) {
        registerUser();
      } else {
        dispose();
        System.exit(0);
      }
    }
  }

  private void loginUser() {
    JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
    JTextField usernameField = new JTextField();
    JPasswordField passwordField = new JPasswordField();

    panel.add(new JLabel("Username:"));
    panel.add(usernameField);
    panel.add(new JLabel("Password:"));
    panel.add(passwordField);

    int result = JOptionPane.showConfirmDialog(
        this,
        panel,
        "Login",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }

    String username = usernameField.getText().trim();
    String password = new String(passwordField.getPassword());
    if (username.isEmpty() || password.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Username and password are required.");
      return;
    }

    try {
      Integer userId = bankService.authenticateUser(username, password);
      if (userId == null) {
        JOptionPane.showMessageDialog(this, "Invalid username or password.");
        return;
      }

      currentUserId = userId;
      currentUsername = username;
      currentDisplayName = bankService.getUserDisplayName(userId);
      updateSessionLabel();
      refreshUserAccount();
      updateUserInfoLabel();

      if (userAccount == null) {
        int createAccountResult = JOptionPane.showConfirmDialog(
            this,
            "Login successful! Welcome, " + currentDisplayName
                + "!\n\nYou don't have an account yet. Would you like to create one now?",
            "Account Setup Required",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        if (createAccountResult == JOptionPane.YES_OPTION) {
          createAccount();
        }
      } else {
        JOptionPane.showMessageDialog(this, "Login successful! Welcome back, " + currentDisplayName + "!");
      }
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(this, "Error logging in: " + ex.getMessage());
    }
  }

  private void registerUser() {
    JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
    JTextField displayNameField = new JTextField();
    JTextField usernameField = new JTextField();
    JPasswordField passwordField = new JPasswordField();
    JTextField accountNameField = new JTextField();

    panel.add(new JLabel("Display Name:"));
    panel.add(displayNameField);
    panel.add(new JLabel("Username:"));
    panel.add(usernameField);
    panel.add(new JLabel("Password:"));
    panel.add(passwordField);
    panel.add(new JLabel("Account Name:"));
    panel.add(accountNameField);

    int result = JOptionPane.showConfirmDialog(
        this,
        panel,
        "Register User",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }

    String displayName = displayNameField.getText().trim();
    String username = usernameField.getText().trim();
    String password = new String(passwordField.getPassword());
    String accountName = accountNameField.getText().trim();
    if (displayName.isEmpty() || username.isEmpty() || password.isEmpty() || accountName.isEmpty()) {
      JOptionPane.showMessageDialog(this, "All fields are required.");
      return;
    }

    try {
      int userId = bankService.registerUserWithAccount(username, password, displayName, accountName);
      JOptionPane.showMessageDialog(this,
          "Registration successful! Your account has been created with $0 balance.\n\nPlease login to continue.");
    } catch (SQLException ex) {
      JOptionPane.showMessageDialog(this, "Error registering user: " + ex.getMessage());
    }
  }

  private void logoutUser() {
    if (currentUserId == null) {
      JOptionPane.showMessageDialog(this, "No user is currently logged in.");
      return;
    }

    currentUserId = null;
    currentUsername = "Not logged in";
    currentDisplayName = "";
    userAccount = null;
    tableModel.setRowCount(0);
    clearTransactionFields();
    updateSessionLabel();
    updateUserInfoLabel();
    updateAccountInfoLabel();
    updateCurrentBalanceLabel();
    JOptionPane.showMessageDialog(this, "You have been logged out.");
    enforceLoginOnStartup();
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      BankingAppUI bankingAppUI = new BankingAppUI();
      bankingAppUI.setVisible(true);
    });
  }
}
```

---

## SQL Migration

### `sql/bankdb.sql`

```sql
DROP DATABASE IF EXISTS bankdb;
CREATE DATABASE bankdb;

\c bankdb;

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    balance DOUBLE PRECISION NOT NULL CHECK (balance >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE transactions (
    id SERIAL PRIMARY KEY,
    account_id INT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL CHECK (type IN ('deposit', 'withdraw')),
    amount DOUBLE PRECISION NOT NULL CHECK (amount > 0),
    date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
CREATE INDEX idx_transactions_account_id_date ON transactions(account_id, date);
```

---

## Scripts

### `scripts/run_migrations.sh`

```bash
#!/usr/bin/env bash

set -e

echo "Running initial PostgreSQL migration..."

PGPASSWORD=password psql -h localhost -U postgres -d postgres -f sql/bankdb.sql

echo "Database setup completed."
```

---

### `scripts/run_app.sh`

```bash
#!/usr/bin/env bash

set -e

JDBC_JAR="postgresql-42.7.9.jar"

if [ ! -f "$JDBC_JAR" ]; then
  echo "PostgreSQL JDBC driver not found: $JDBC_JAR"
  echo "Place the JAR file in the project root and try again."
  exit 1
fi

echo "Compiling Java files..."
javac -cp ".:$JDBC_JAR" src/*.java

echo "Starting banking application..."
java -cp ".:src:$JDBC_JAR" BankingAppUI
```

---

## `.gitignore`

```
.codex
```
