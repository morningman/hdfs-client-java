#!/bin/bash

# HDFS Client Run Script
# This script runs the HDFS client application

set -e

# Define variables
PROJECT_NAME="hdfs-client"
VERSION="1.0-SNAPSHOT"
JAR_NAME="${PROJECT_NAME}-${VERSION}.jar"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONF_DIR="${SCRIPT_DIR}/conf"
HADOOP_CONF_DIR="${CONF_DIR}"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "Java is not installed. Please install Java to run the application."
    exit 1
fi

# Check for at least one argument
if [ $# -lt 1 ]; then
    echo "Usage: $0 <hdfs-uri> [operation] [params...]"
    echo "Operations:"
    echo "  list <path>                - List files in directory"
    echo "  read <file-path>           - Read file content"
    echo "  write <file-path> <content> [overwrite] - Write content to file"
    echo "  mkdir <dir-path>           - Create directory"
    echo "  delete <path> [recursive]  - Delete file or directory"
    echo "  exists <path>              - Check if path exists"
    echo "  whoami                     - Show current authenticated user"
    exit 1
fi

# Find the JAR file
if [ -f "${SCRIPT_DIR}/target/${JAR_NAME}" ]; then
    JAR_PATH="${SCRIPT_DIR}/target/${JAR_NAME}"
elif [ -f "${SCRIPT_DIR}/${JAR_NAME}" ]; then
    JAR_PATH="${SCRIPT_DIR}/${JAR_NAME}"
else
    echo "JAR file not found. Please build the project first using build.sh."
    exit 1
fi

# Set environment variables
export HADOOP_CONF_DIR

# Print the current configuration
echo "===== Runtime Configuration ====="
echo "JAR: ${JAR_PATH}"
echo "HADOOP_CONF_DIR: ${HADOOP_CONF_DIR}"
echo "============================="

# Run the application
echo "Starting HDFS Client..."
java -cp "${JAR_PATH}" com.example.hdfs.HdfsClientApp "$@" 
