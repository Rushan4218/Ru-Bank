# Java Swing Banking Application

This is a simple beginner-friendly banking application built with:

- Java Swing for the GUI
- JDBC for database access
- PostgreSQL as the database

## Project Structure

- `src/DBConnection.java` - handles PostgreSQL connection
- `src/BankService.java` - contains account and transaction methods
- `src/TransactionRecord.java` - simple model class for transaction history
- `src/BankingAppUI.java` - main Swing user interface
- `sql/bankdb.sql` - SQL script to create the database and tables
- `scripts/run_migrations.sh` - runs the initial PostgreSQL setup
- `scripts/run_app.sh` - compiles and starts the app

## Database Setup

1. Make sure PostgreSQL is running locally.
2. Run the migration script:

```bash
./scripts/run_migrations.sh
```

3. This script creates the database and tables from `sql/bankdb.sql`.
4. The app connects using:

```text
jdbc:postgresql://localhost:5432/bankdb
```

5. It uses these credentials:

```text
Username: postgres
Password: password
```

## PostgreSQL JDBC Driver

You need the PostgreSQL JDBC driver JAR file before compiling and running.

Example download name:

```text
postgresql-42.7.3.jar
```

## Compile And Run With Script

Use the helper script:

```bash
./scripts/run_app.sh
```

This script:

- checks that the PostgreSQL JDBC JAR exists
- compiles all Java source files
- starts the Swing banking application

## Manual Compile

```bash
javac -cp ".:postgresql-42.7.9.jar" src/*.java
```

## Manual Run

```bash
java -cp ".:src:postgresql-42.7.9.jar" BankingAppUI
```

## Features

- Create account
- Deposit money
- Withdraw money
- Check balance
- View transaction history

## How To Use The App

When the window opens, you will see four text fields:

- `Account Holder Name`
- `Initial Balance`
- `Account ID`
- `Amount`

Use the fields based on the button you want to click.

### 1. Create Account

1. Enter the customer name in `Account Holder Name`.
2. Enter the starting balance in `Initial Balance`.
3. Click `Create Account`.
4. A message box will show the new account ID.
5. Save that account ID because you will need it for deposit, withdraw, balance check, and history.

Example:

```text
Name: Ross
Initial Balance: 5000
```

### 2. Deposit Money

1. Enter the saved account number in `Account ID`.
2. Enter the deposit amount in `Amount`.
3. Click `Deposit`.
4. A success message will appear if the deposit is completed.

Example:

```text
Account ID: 1
Amount: 1000
```

### 3. Withdraw Money

1. Enter the account number in `Account ID`.
2. Enter the withdrawal amount in `Amount`.
3. Click `Withdraw`.
4. If the amount is greater than the available balance, the app will show `Insufficient balance`.

Example:

```text
Account ID: 1
Amount: 300
```

### 4. Check Balance

1. Enter the account number in `Account ID`.
2. Click `Check Balance`.
3. A message box will show the current balance.

### 5. View Transaction History

1. Enter the account number in `Account ID`.
2. Click `View History`.
3. The table at the bottom of the window will show all deposits and withdrawals for that account.

## Notes For Users

- `Account ID` is generated automatically when a new account is created.
- `Amount` must be greater than `0`.
- `Initial Balance` cannot be negative.
- If an account does not exist, the app will show an error message.
- Every deposit and withdrawal is stored in the `transactions` table.
