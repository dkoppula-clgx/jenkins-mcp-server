#!/bin/bash

echo ""
echo "Jenkins MCP Server Startup"
echo "============================="
echo ""

read -p "Enter Jenkins username: " username
if [ -z "$username" ]; then
    echo "[ERROR] Username cannot be empty"
    exit 1
fi

read -sp "Enter Jenkins password: " password
echo ""
if [ -z "$password" ]; then
    echo "[ERROR] Password cannot be empty"
    exit 1
fi

read -p "Enter server port (default: 8080): " port
port=${port:-8080}

echo ""
echo "Starting server with:"
echo "  Username: $username"
echo "  Port: $port"
echo ""

./gradlew bootRun --args="--jenkins.username=$username --jenkins.password=$password --server.port=$port"
