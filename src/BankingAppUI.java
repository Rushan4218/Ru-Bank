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
