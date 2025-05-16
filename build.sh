#!/bin/bash

# HDFS Client Build Script
# This script builds the HDFS client Java project

set -e

# Define variables
PROJECT_NAME="hdfs-client"
VERSION="1.0.0"
JAR_NAME="${PROJECT_NAME}-${VERSION}.jar"
TARGET_DIR="target"

# Create target directory if it doesn't exist
mkdir -p ${TARGET_DIR}

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Maven is not installed. Please install Maven to build the project."
    exit 1
fi

echo "Building ${PROJECT_NAME} ${VERSION}..."

# Run Maven build
mvn clean package -DskipTests

# Check if the build was successful
if [ $? -eq 0 ]; then
    # Copy the JAR to the target directory with the right name
    echo "Renaming JAR file to ${JAR_NAME}..."
    find target -name "*.jar" -not -name "*sources*" -not -name "*javadoc*" -not -name "*tests*" \
        -exec cp {} ${TARGET_DIR}/${JAR_NAME} \;
    
    echo "Build completed successfully!"
    echo "JAR file created: ${TARGET_DIR}/${JAR_NAME}"
else
    echo "Build failed!"
    exit 1
fi 