import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.DecimalFormat;
import java.sql.SQLException;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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

    private JTextField nameField;
    private JTextField initialBalanceField;
    private JTextField accountIdField;
    private JTextField amountField;
    private DefaultTableModel tableModel;

    public BankingAppUI() {
        bankService = new BankService();
        initializeUI();
    }

    private void initializeUI() {
        // Apply one font to common Swing components so the whole window feels consistent.
        setApplicationFont();

        setTitle("RuBank");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("RuBank", JLabel.CENTER);
        titleLabel.setFont(TITLE_FONT);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        nameField = new JTextField();
        initialBalanceField = new JTextField();
        accountIdField = new JTextField();
        amountField = new JTextField();

        inputPanel.add(new JLabel("Account Holder Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Initial Balance:"));
        inputPanel.add(initialBalanceField);
        inputPanel.add(new JLabel("Account ID:"));
        inputPanel.add(accountIdField);
        inputPanel.add(new JLabel("Amount:"));
        inputPanel.add(amountField);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 5, 10, 10));

        JButton createAccountButton = new JButton("Create Account");
        JButton depositButton = new JButton("Deposit");
        JButton withdrawButton = new JButton("Withdraw");
        JButton checkBalanceButton = new JButton("Check Balance");
        JButton historyButton = new JButton("View History");

        buttonPanel.add(createAccountButton);
        buttonPanel.add(depositButton);
        buttonPanel.add(withdrawButton);
        buttonPanel.add(checkBalanceButton);
        buttonPanel.add(historyButton);

        String[] columnNames = {"Transaction ID", "Account ID", "Type", "Amount", "Date"};
        tableModel = new DefaultTableModel(columnNames, 0);
        JTable historyTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        topPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new BorderLayout(10, 10));
        formPanel.add(inputPanel, BorderLayout.NORTH);
        formPanel.add(buttonPanel, BorderLayout.SOUTH);

        topPanel.add(formPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        createAccountButton.addActionListener(e -> createAccount());
        depositButton.addActionListener(e -> depositMoney());
        withdrawButton.addActionListener(e -> withdrawMoney());
        checkBalanceButton.addActionListener(e -> checkBalance());
        historyButton.addActionListener(e -> viewTransactionHistory());
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
        String name = nameField.getText().trim();
        String balanceText = initialBalanceField.getText().trim();

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

            int accountId = bankService.createAccount(name, initialBalance);
            JOptionPane.showMessageDialog(this, "Account created successfully. Account ID: " + accountId);
            clearCreateAccountFields();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for balance.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error creating account: " + ex.getMessage());
        }
    }

    private void depositMoney() {
        Integer accountId = parseAccountId();
        Double amount = parseAmount();

        if (accountId == null || amount == null) {
            return;
        }

        if (amount <= 0) {
            JOptionPane.showMessageDialog(this, "Deposit amount must be greater than zero.");
            return;
        }

        try {
            boolean success = bankService.depositMoney(accountId, amount);

            if (success) {
                JOptionPane.showMessageDialog(this, "Money deposited successfully.");
                clearTransactionFields();
            } else {
                JOptionPane.showMessageDialog(this, "Account not found.");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error depositing money: " + ex.getMessage());
        }
    }

    private void withdrawMoney() {
        Integer accountId = parseAccountId();
        Double amount = parseAmount();

        if (accountId == null || amount == null) {
            return;
        }

        if (amount <= 0) {
            JOptionPane.showMessageDialog(this, "Withdrawal amount must be greater than zero.");
            return;
        }

        try {
            double currentBalance = bankService.getBalance(accountId);

            if (currentBalance < 0) {
                JOptionPane.showMessageDialog(this, "Account not found.");
                return;
            }

            if (amount > currentBalance) {
                JOptionPane.showMessageDialog(this, "Insufficient balance.");
                return;
            }

            boolean success = bankService.withdrawMoney(accountId, amount);

            if (success) {
                JOptionPane.showMessageDialog(this, "Money withdrawn successfully.");
                clearTransactionFields();
            } else {
                JOptionPane.showMessageDialog(this, "Withdrawal failed.");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error withdrawing money: " + ex.getMessage());
        }
    }

    private void checkBalance() {
        Integer accountId = parseAccountId();

        if (accountId == null) {
            return;
        }

        try {
            double balance = bankService.getBalance(accountId);

            if (balance >= 0) {
                JOptionPane.showMessageDialog(this, "Current balance: " + formatAmount(balance));
            } else {
                JOptionPane.showMessageDialog(this, "Account not found.");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error checking balance: " + ex.getMessage());
        }
    }

    private void viewTransactionHistory() {
        Integer accountId = parseAccountId();

        if (accountId == null) {
            return;
        }

        try {
            List<TransactionRecord> transactions = bankService.getTransactionHistory(accountId);
            tableModel.setRowCount(0);

            // Refresh the table every time so the user sees the latest transactions only.
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
                JOptionPane.showMessageDialog(this, "No transactions found for this account.");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading history: " + ex.getMessage());
        }
    }

    private Integer parseAccountId() {
        String accountIdText = accountIdField.getText().trim();

        if (accountIdText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter an account ID.");
            return null;
        }

        try {
            return Integer.parseInt(accountIdText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid account ID.");
            return null;
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
        nameField.setText("");
        initialBalanceField.setText("");
    }

    private void clearTransactionFields() {
        accountIdField.setText("");
        amountField.setText("");
    }

    private String formatAmount(double amount) {
        return MONEY_FORMAT.format(amount);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BankingAppUI bankingAppUI = new BankingAppUI();
            bankingAppUI.setVisible(true);
        });
    }
}
