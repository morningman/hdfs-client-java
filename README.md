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
- Hadoop 3.3.6 (default, can be configured during build)

## Building the Project

Build the project using the following command:

```bash
./build.sh
```

This will create an executable fat JAR file with all dependencies included, allowing it to run independently without external libraries.

### Custom Hadoop Version

By default, the project uses Hadoop version 3.3.6. You can specify a different Hadoop version during build:

```bash
./build.sh --hadoop-version 3.4.1
```

You can also specify the Hadoop version when creating a package:

```bash
./package.sh --hadoop-version 3.4.1
```

This will create a package with the name format `hdfs-client-java-1.0.0-hadoop-3.4.1.tar.gz`.

## Usage

### Configuration
Put core-site.xml and hdfs-site.xml in conf/ (Optional).

Can add more config in conf/client.conf, it will overwrite config in xml file.

### Using the run script

Run the application using the following command:

```bash
./run.sh <hdfs-uri> [operation] [params...]
```

### Examples

#### 1. List files in the root directory:

```bash
./run.sh hdfs://localhost:9000 list /
```

#### 2. Read file content:

```bash
./run.sh hdfs://localhost:9000 read /path/to/file.txt
```

#### 3. Write content to a file:

```bash
./run.sh hdfs://localhost:9000 write /path/to/file.txt "Hello, HDFS!" true
```
The last parameter `true` indicates whether to overwrite existing files.

#### 4. Create a directory:

```bash
./run.sh hdfs://localhost:9000 mkdir /path/to/new/directory
```

#### 5. Delete a file or directory:

```bash
./run.sh hdfs://localhost:9000 delete /path/to/delete true
```
The last parameter `true` indicates whether to recursively delete directories.

#### 6. Check if a path exists:

```bash
./run.sh hdfs://localhost:9000 exists /path/to/check
```

#### 7. Benchmark read performance:
   You can use the `benchmarkRead` command to test the read performance of a file or directory using multiple threads.

Usage

```bash
./run.sh benchmarkRead <path> <threads>
```

Parameters

- **`<path>`**:  
  Path to the file or directory to read.  
  Supports both local and remote file systems (e.g., HDFS, S3).

- **`<threads>`**:  
  Number of threads to use for concurrent reading.  
  Each thread will read all the files independently.

> **Note**:
> - If `<path>` is a file, each thread will repeatedly read the same file.
> - If `<path>` is a directory, each thread will read all files in that directory.

 Example

Read the file `/path/to/file.orc` using 20 threads:

```bash
./run.sh benchmarkRead hdfs://localhost:9000/path/to/file.orc 20
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
- `run.sh hdfs://hdfs-cluster  benchmarkRead hdfs://hdfs-cluster/tmp/tmp.orc 20`

### Kerberos & HDFS HA

client.conf:

```
hadoop.kerberos.krb5.conf=/path/to/krb5.conf
hadoop.security.authentication=kerberos
hadoop.kerberos.keytab=/path/to/hdfs.keytab
hadoop.kerberos.principal=your_hdfs_principal

dfs.nameservices=hdfs-cluster
dfs.ha.namenodes.hdfs-cluster=nn1,nn2,nn3
dfs.namenode.rpc-address.hdfs-cluster.nn1=node1:8020
dfs.namenode.rpc-address.hdfs-cluster.nn2=node1:8020
dfs.namenode.rpc-address.hdfs-cluster.nn3=node1:8020
dfs.client.failover.proxy.provider.hdfs-cluster=org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider
```
