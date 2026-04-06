#!/usr/bin/env bash

set -e

echo "Running initial PostgreSQL migration..."

PGPASSWORD=password psql -h localhost -U postgres -d postgres -f sql/bankdb.sql

echo "Database setup completed."
