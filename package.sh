#!/bin/bash

# HDFS Client Package Script
# This script packages all necessary files for distribution

set -e

# Define variables
PACKAGE_NAME="hdfs-client"
VERSION="1.0.0"
PACKAGE_DIR="${PACKAGE_NAME}-${VERSION}"
TARGET_DIR="target"
DIST_DIR="dist"

# Create directories if they don't exist
mkdir -p ${DIST_DIR}
mkdir -p ${TARGET_DIR}/${PACKAGE_DIR}

# Build the project if the JAR doesn't exist
if [ ! -f "${TARGET_DIR}/${PACKAGE_NAME}-${VERSION}.jar" ]; then
    echo "Building project..."
    ./build.sh
fi

# Copy necessary files to the package directory
echo "Copying files to package directory..."

# Find and copy the JAR file (more robust approach)
JAR_FILE="${TARGET_DIR}/${PACKAGE_NAME}-${VERSION}.jar"
if [ -f "${JAR_FILE}" ]; then
    echo "Using JAR file: ${JAR_FILE}"
    cp "${JAR_FILE}" "${TARGET_DIR}/${PACKAGE_DIR}/"
else
    echo "Looking for alternative JAR files..."
    # Try to find any JAR file in the target directory
    FOUND_JAR=$(find ${TARGET_DIR} -maxdepth 1 -name "*.jar" -not -name "*sources*" -not -name "*javadoc*" -not -name "*tests*" | head -1)
    
    if [ -n "${FOUND_JAR}" ]; then
        echo "Found JAR file: ${FOUND_JAR}"
        cp "${FOUND_JAR}" "${TARGET_DIR}/${PACKAGE_DIR}/${PACKAGE_NAME}-${VERSION}.jar"
    else
        echo "Error: No JAR file found in the target directory."
        exit 1
    fi
fi

# Copy other necessary files
cp run.sh "${TARGET_DIR}/${PACKAGE_DIR}/"

# Ensure conf directory exists
if [ -d "conf" ]; then
    cp -r conf "${TARGET_DIR}/${PACKAGE_DIR}/"
else
    echo "Warning: conf directory not found, creating an empty one"
    mkdir -p "${TARGET_DIR}/${PACKAGE_DIR}/conf"
fi

# Copy documentation
if [ -f "README.md" ]; then
    cp README.md "${TARGET_DIR}/${PACKAGE_DIR}/"
fi

if [ -f "LICENSE" ]; then
    cp LICENSE "${TARGET_DIR}/${PACKAGE_DIR}/"
fi

# Creating the distribution package
echo "Creating distribution package..."
cd ${TARGET_DIR}
tar -czf ../dist/${PACKAGE_NAME}-${VERSION}.tar.gz ${PACKAGE_DIR}
cd ..

# Verify the package
echo "Package created: dist/${PACKAGE_NAME}-${VERSION}.tar.gz"
echo "Package contents:"
tar -tf dist/${PACKAGE_NAME}-${VERSION}.tar.gz

echo "Packaging completed successfully!"
echo "To deploy, copy the package to the target server and extract it using:"
echo "  tar -xzf ${PACKAGE_NAME}-${VERSION}.tar.gz"
echo "Then use the run.sh script to execute the application." 