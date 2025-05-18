#!/bin/bash

# HDFS Client Package Script
# This script packages all necessary files for distribution

set -e

# Define variables
PACKAGE_NAME="hdfs-client-java"
VERSION="1.0.0"
HADOOP_VERSION=""

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    key="$1"
    case $key in
        --hadoop-version)
        HADOOP_VERSION="$2"
        shift
        shift
        ;;
        *)
        shift
        ;;
    esac
done

# Set default Hadoop version if not specified
if [[ -z "${HADOOP_VERSION}" ]]; then
    # Extract default Hadoop version from pom.xml
    DEFAULT_HADOOP_VERSION=$(grep -o '<hadoop\.version>[^<]*</hadoop\.version>' pom.xml | sed 's/<hadoop\.version>\(.*\)<\/hadoop\.version>/\1/')
    HADOOP_VERSION="${DEFAULT_HADOOP_VERSION}"
fi

# Create package directory name with Hadoop version
PACKAGE_DIR="${PACKAGE_NAME}-${VERSION}-hadoop-${HADOOP_VERSION}"
TARGET_DIR="target"
DIST_DIR="dist"

# Create directories if they don't exist
mkdir -p ${DIST_DIR}
mkdir -p "${TARGET_DIR}/${PACKAGE_DIR}"

echo "Created package directory: ${TARGET_DIR}/${PACKAGE_DIR}"
ls -la "${TARGET_DIR}"

# Always build the project
echo "Building project..."
if [[ -n "${HADOOP_VERSION}" ]]; then
    echo "Using Hadoop version: ${HADOOP_VERSION}"
    ./build.sh --hadoop-version "${HADOOP_VERSION}"
else
    ./build.sh
fi

# Copy necessary files to the package directory
echo "Copying files to package directory..."

# Find and copy the JAR file (more robust approach)
JAR_FILE="${TARGET_DIR}/${PACKAGE_NAME}-${VERSION}.jar"
if [ -f "${JAR_FILE}" ]; then
    echo "Using JAR file: ${JAR_FILE}"
    # Ensure the target directory exists
    mkdir -p "${TARGET_DIR}/${PACKAGE_DIR}"
    cp "${JAR_FILE}" "${TARGET_DIR}/${PACKAGE_DIR}/"
else
    echo "Looking for alternative JAR files..."
    # Try to find any JAR file in the target directory
    FOUND_JAR=$(find ${TARGET_DIR} -maxdepth 1 -name "*.jar" -not -name "*sources*" -not -name "*javadoc*" -not -name "*tests*" | head -1)
    
    if [ -n "${FOUND_JAR}" ]; then
        echo "Found JAR file: ${FOUND_JAR}"
        # Ensure the target directory exists
        mkdir -p "${TARGET_DIR}/${PACKAGE_DIR}"
        cp "${FOUND_JAR}" "${TARGET_DIR}/${PACKAGE_DIR}/${PACKAGE_NAME}-${VERSION}.jar"
    else
        echo "Error: No JAR file found in the target directory."
        exit 1
    fi
fi

# Ensure the package directory exists before copying other files
mkdir -p "${TARGET_DIR}/${PACKAGE_DIR}"

# Copy and update run.sh to include Hadoop version information
cp run.sh "${TARGET_DIR}/${PACKAGE_DIR}/run.sh"
sed -i.bak "s/^# HDFS Client Run Script/# HDFS Client Run Script (Hadoop ${HADOOP_VERSION})/" "${TARGET_DIR}/${PACKAGE_DIR}/run.sh" && rm "${TARGET_DIR}/${PACKAGE_DIR}/run.sh.bak" || true

# Ensure conf directory exists
if [ -d "conf" ]; then
    mkdir -p "${TARGET_DIR}/${PACKAGE_DIR}/conf"
    cp -r conf/* "${TARGET_DIR}/${PACKAGE_DIR}/conf/"
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

# List the contents of the package directory to verify
echo "Contents of ${TARGET_DIR}/${PACKAGE_DIR}:"
ls -la "${TARGET_DIR}/${PACKAGE_DIR}"

# Creating the distribution package
echo "Creating distribution package..."
cd ${TARGET_DIR}
tar -czf "../${DIST_DIR}/${PACKAGE_DIR}.tar.gz" ${PACKAGE_DIR}
cd ..

# Verify the package
echo "Package created: ${DIST_DIR}/${PACKAGE_DIR}.tar.gz"
echo "Package contents:"
tar -tf "${DIST_DIR}/${PACKAGE_DIR}.tar.gz"

echo "Packaging completed successfully!"
echo "To deploy, copy the package to the target server and extract it using:"
echo "  tar -xzf ${PACKAGE_DIR}.tar.gz"
echo "Then use the run.sh script to execute the application." 