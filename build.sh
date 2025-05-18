#!/bin/bash

# HDFS Client Build Script
# This script builds the HDFS client Java project

set -e

# Define variables
PROJECT_NAME="hdfs-client"
VERSION="1.0.0"
JAR_NAME="${PROJECT_NAME}-${VERSION}.jar"
TARGET_DIR="target"
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

# Create target directory if it doesn't exist
mkdir -p ${TARGET_DIR}

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "Maven is not installed. Please install Maven to build the project."
    exit 1
fi

echo "Building ${PROJECT_NAME} ${VERSION}..."

# Run Maven build with optional hadoop version
if [[ -n "${HADOOP_VERSION}" ]]; then
    echo "Using Hadoop version: ${HADOOP_VERSION}"
    mvn clean package -DskipTests -Dhadoop.version=${HADOOP_VERSION}
else
    echo "Using default Hadoop version from pom.xml"
    mvn clean package -DskipTests
fi

# Check if the build was successful
if [ $? -eq 0 ]; then
    # Verify fat jar has been created
    FAT_JAR="${TARGET_DIR}/${PROJECT_NAME}-${VERSION}.jar"
    if [ -f "${FAT_JAR}" ]; then
        JAR_SIZE=$(du -k "${FAT_JAR}" | cut -f1)
        echo "Fat JAR size: ${JAR_SIZE}KB"
        
        if [ "${JAR_SIZE}" -lt 1000 ]; then
            echo "Warning: JAR file seems too small for a fat JAR (${JAR_SIZE}KB)"
            echo "It might not include all dependencies. Please check pom.xml configuration."
        else
            echo "Fat JAR was created successfully."
        fi
    fi
    
    # Copy the JAR to the target directory with the right name
    echo "Renaming JAR file to ${JAR_NAME}..."
    find target -name "*.jar" -not -name "*sources*" -not -name "*javadoc*" -not -name "*tests*" -not -name "original-*.jar" \
        -exec cp {} ${TARGET_DIR}/${JAR_NAME} \;
    
    echo "Build completed successfully!"
    echo "JAR file created: ${TARGET_DIR}/${JAR_NAME}"
else
    echo "Build failed!"
    exit 1
fi 