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
