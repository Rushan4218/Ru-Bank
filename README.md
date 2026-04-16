# Java Swing Banking Application (V2)

This is a beginner-friendly banking desktop app built with:

- Java Swing for the UI
- JDBC for database access
- PostgreSQL as the backend database

Version 2 adds:

- login and registration
- menu bar actions
- user-scoped accounts
- transaction history sorting (oldest/newest)

## Project Structure

- `src/DBConnection.java` - PostgreSQL connection config and factory
- `src/BankService.java` - business logic + SQL operations
- `src/TransactionRecord.java` - model for transaction history rows
- `src/BankingAppUI.java` - Swing app window, menu bar, dialogs, table
- `sql/bankdb.sql` - full schema setup for users/accounts/transactions
- `scripts/run_migrations.sh` - runs schema migration
- `scripts/run_app.sh` - compiles and launches the app

## Database Schema (V2)

The migration now creates:

- `users` - login users (`username`, `password`, `display_name`)
- `accounts` - bank accounts owned by users (`user_id` foreign key)
- `transactions` - deposit/withdraw records for accounts

Notes:

- each account belongs to one user
- deleting a user cascades to their accounts and transactions
- amount/type constraints and indexes are included for safety/performance

## Database Setup

1. Make sure PostgreSQL is running locally.
2. Run:

```bash
./scripts/run_migrations.sh
```

3. The script rebuilds `bankdb` from `sql/bankdb.sql`.

Connection used by the app:

```text
jdbc:postgresql://localhost:5432/bankdb
Username: postgres
Password: password
```

## PostgreSQL JDBC Driver

Place the PostgreSQL JDBC driver jar in the project root:

```text
postgresql-42.7.9.jar
```

## Run The App

Using script:

```bash
./scripts/run_app.sh
```

Manual compile:

```bash
javac -cp ".:postgresql-42.7.9.jar" src/*.java
```

Manual run:

```bash
java -cp ".:src:postgresql-42.7.9.jar" BankingAppUI
```

## Features

- user registration and login
- create account
- deposit money
- withdraw money
- check balance
- view transaction history in table
- sort history:
  - newest to oldest
  - oldest to newest

## How To Use

### 1) Login / Register

When the app starts, authentication is required.

- choose `Login` if you already have an account
- choose `Register` to create a new user

You can also use the menu:

- `Account -> Login`
- `Account -> Register`
- `Account -> Logout`

### 2) Create Bank Account

1. Enter `Account Holder Name`
2. Enter `Initial Balance`
3. Click `Create Account`
4. Save the generated `Account ID`

### 3) Deposit / Withdraw

1. Enter `Account ID`
2. Enter `Amount`
3. Click `Deposit` or `Withdraw`

### 4) Check Balance

1. Enter `Account ID`
2. Click `Check Balance`

### 5) Transaction History + Sorting

1. Enter `Account ID`
2. Click `View History`
3. Use menu options:
   - `History -> Sort: Newest to Oldest`
   - `History -> Sort: Oldest to Newest`
   - `History -> Refresh History`

## Important Notes

- account operations are scoped to the currently logged-in user
- `Amount` must be greater than `0`
- `Initial Balance` cannot be negative
- deposits/withdrawals are written to `transactions`
- passwords are currently stored as plain text (okay for learning/demo, not production)
