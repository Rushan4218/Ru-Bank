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
