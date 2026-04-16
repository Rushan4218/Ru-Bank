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
