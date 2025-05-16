# HDFS Client

A simple Java client application for interacting with HDFS (Hadoop Distributed File System).

## Features

- List files in HDFS directories
- Read content from HDFS files
- Write content to HDFS files
- Create HDFS directories
- Delete HDFS files or directories
- Check if HDFS paths exist

## Technical Requirements

- Java 8
- Maven 3.x
- Hadoop 3.3.6

## Building the Project

Build the project using the following command:

```bash
./build.sh
```

This will create an executable JAR file with all dependencies included.

## Usage

Run the application using the following command:

```bash
./run.sh <hdfs-uri> [operation] [params...]
```

### Examples

1. List files in the root directory:

```bash
./run.sh hdfs://localhost:9000 list /
```

2. Read file content:

```bash
./run.sh hdfs://localhost:9000 read /path/to/file.txt
```

3. Write content to a file:

```bash
./run.sh hdfs://localhost:9000 write /path/to/file.txt "Hello, HDFS!" true
```
The last parameter `true` indicates whether to overwrite existing files.

4. Create a directory:

```bash
./run.sh hdfs://localhost:9000 mkdir /path/to/new/directory
```

5. Delete a file or directory:

```bash
./run.sh hdfs://localhost:9000 delete /path/to/delete true
```
The last parameter `true` indicates whether to recursively delete directories.

6. Check if a path exists:

```bash
./run.sh hdfs://localhost:9000 exists /path/to/check
```

## Project Structure

- `src/main/java/com/example/hdfs/HdfsClient.java` - Utility class for HDFS operations
- `src/main/java/com/example/hdfs/HdfsClientApp.java` - Main application class
- `build.sh` - Build script
- `run.sh` - Run script

## Configuring HDFS Connection

The application requires a valid HDFS URI as the first parameter. This is typically in the form of `hdfs://hostname:port`. For local development environments, it's usually `hdfs://localhost:9000`.

## Note

Ensure your Hadoop cluster is running and accessible from the machine where you're running this application. 

## Example

- put `core-site.xml` and `hdfs-site.xml` to `conf/`
- add `hadoop.kerberos.keytab` and `hadoop.kerberos.principal` in `conf/client.conf`
- `sh run.sh hdfs://hdfs-cluster list hdfs://hdfs-cluster/tmp/`
- `sh run.sh hdfs://hdfs-cluster write hdfs://hdfs-cluster/tmp/test1.txt "test123"`
- `sh run.sh hdfs://hdfs-cluster read hdfs://hdfs-cluster/tmp/test1.txt`