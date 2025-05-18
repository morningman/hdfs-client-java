package com.example.hdfs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.hdfs.DFSInputStream;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.*;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;

/**
 * Utility class for HDFS file operations
 */
public class HdfsClient implements AutoCloseable {
    private final FileSystem fileSystem;
    private final String hdfsUri;
    
    // Kerberos configuration property names
    private static final String KERBEROS_KEYTAB_PROPERTY = "hadoop.kerberos.keytab";
    private static final String KERBEROS_PRINCIPAL_PROPERTY = "hadoop.kerberos.principal";
    private static final String HADOOP_SECURITY_AUTHENTICATION = "hadoop.security.authentication";
    private static final String KERBEROS_KRB5_CONF_PROPERTY = "hadoop.kerberos.krb5.conf";
    private static final String DEFAULT_KRB5_CONF = "/etc/krb5.conf";
    
    // Socket timeout configuration properties
    private static final String SOCKET_TIMEOUT = "dfs.client.socket-timeout";
    private static final String SOCKET_TIMEOUT_READ = "dfs.client.socket.timeout";
    private static final int DEFAULT_TIMEOUT_MS = 10000; // Default 10 seconds
    
    /**
     * Constructs a new HDFS client with the specified HDFS URI
     * 
     * @param hdfsUri the HDFS URI (e.g., "hdfs://localhost:9000")
     * @throws IOException if an error occurs during initialization
     */
    public HdfsClient(String hdfsUri) throws IOException {
        this.hdfsUri = hdfsUri;
        
        // Create configuration object and load configuration files
        Configuration configuration = new Configuration();
        
        // Set configuration file path
        String confDir = System.getenv("HADOOP_CONF_DIR");
        boolean kerberosConfigured = false;
        
        if (confDir != null && !confDir.isEmpty()) {
            System.out.println("Loading configuration from: " + confDir);
            
            // Add XML configuration files
            configuration.addResource(new Path(confDir, "core-site.xml"));
            configuration.addResource(new Path(confDir, "hdfs-site.xml"));
            
            // Load client.conf file (if exists)
            Properties clientProperties = loadClientConf(configuration, confDir);
            
            // Check if Kerberos authentication is configured
            kerberosConfigured = setupKerberosAuthentication(configuration, clientProperties);
        } else {
            System.out.println("HADOOP_CONF_DIR not set, using default configuration.");
        }
        
        // Set default IO timeout
        configuration.setInt(SOCKET_TIMEOUT, DEFAULT_TIMEOUT_MS);
        configuration.setInt(SOCKET_TIMEOUT_READ, DEFAULT_TIMEOUT_MS);
        System.out.println("Setting default socket timeout to " + DEFAULT_TIMEOUT_MS + " ms");
        
        // Print all configuration properties
        System.out.println("Configuration properties:");
        for (Map.Entry<String, String> entry : configuration) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
        
        // Set HDFS URI (if the URI specified from command line is different from the one in config files, override it)
        configuration.set("fs.defaultFS", hdfsUri);
        System.out.println("Setting fs.defaultFS to: " + hdfsUri);
        
        // Get file system
        this.fileSystem = FileSystem.get(configuration);
        System.out.println("HDFS client initialized with URI: " + hdfsUri + 
                (kerberosConfigured ? " (with Kerberos authentication)" : ""));
    }
    
    /**
     * Set up Kerberos authentication
     * 
     * @param configuration Hadoop configuration object
     * @param clientProperties properties from client.conf
     * @return whether Kerberos authentication was configured
     * @throws IOException if an error occurs during authentication
     */
    private boolean setupKerberosAuthentication(Configuration configuration, Properties clientProperties) throws IOException {
        if (clientProperties == null) {
            return false;
        }
        
        String keytabPath = clientProperties.getProperty(KERBEROS_KEYTAB_PROPERTY);
        String principal = clientProperties.getProperty(KERBEROS_PRINCIPAL_PROPERTY);
        
        if (keytabPath != null && !keytabPath.isEmpty() && principal != null && !principal.isEmpty()) {
            try {
                System.out.println("Setting up Kerberos authentication:");
                System.out.println("  Principal: " + principal);
                System.out.println("  Keytab: " + keytabPath);
                
                // Check for custom krb5.conf path
                String krb5ConfPath = clientProperties.getProperty(KERBEROS_KRB5_CONF_PROPERTY, DEFAULT_KRB5_CONF);
                System.out.println("  Using krb5.conf: " + krb5ConfPath);
                
                // Set system property for custom krb5.conf
                File krb5ConfFile = new File(krb5ConfPath);
                if (krb5ConfFile.exists() && krb5ConfFile.isFile()) {
                    System.setProperty("java.security.krb5.conf", krb5ConfPath);
                    System.out.println("  Set java.security.krb5.conf system property to: " + krb5ConfPath);
                } else {
                    System.out.println("  Warning: krb5.conf file not found at: " + krb5ConfPath + ", using default path");
                }
                
                // Ensure keytab file exists
                File keytabFile = new File(keytabPath);
                if (!keytabFile.exists() || !keytabFile.isFile()) {
                    System.out.println("Error: Keytab file not found: " + keytabPath);
                    return false;
                }
                
                // Set security authentication method to Kerberos
                configuration.set(HADOOP_SECURITY_AUTHENTICATION, "kerberos");
                
                // Apply security settings
                UserGroupInformation.setConfiguration(configuration);
                
                // Login using keytab file
                UserGroupInformation.loginUserFromKeytab(principal, keytabPath);
                
                System.out.println("Kerberos authentication setup successfully.");
                return true;
            } catch (Exception e) {
                System.out.println("Error setting up Kerberos authentication: " + e.getMessage());
                e.printStackTrace();
                throw new IOException("Failed to set up Kerberos authentication", e);
            }
        }
        
        return false;
    }
    
    /**
     * Load client.conf configuration file
     * 
     * @param configuration Hadoop configuration object
     * @param confDir configuration directory
     * @return Properties object containing properties from client.conf, or null if file doesn't exist
     */
    private Properties loadClientConf(Configuration configuration, String confDir) {
        File clientConfFile = new File(confDir, "client.conf");
        if (clientConfFile.exists() && clientConfFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(clientConfFile))) {
                Properties properties = new Properties();
                properties.load(reader);
                
                System.out.println("Loading client.conf from: " + clientConfFile.getAbsolutePath());
                int count = 0;
                
                // Add properties to configuration
                for (String key : properties.stringPropertyNames()) {
                    String value = properties.getProperty(key);
                    if (value != null && !value.isEmpty()) {
                        // Set configuration, will override previous XML configuration
                        configuration.set(key, value);
                        System.out.println("  Override: " + key + " = " + value);
                        count++;
                    }
                }
                
                System.out.println("Loaded " + count + " properties from client.conf");
                return properties;
            } catch (IOException e) {
                System.out.println("Error loading client.conf: " + e.getMessage());
            }
        } else {
            System.out.println("client.conf not found at: " + clientConfFile.getAbsolutePath());
        }
        
        return null;
    }
    
    /**
     * Lists files and directories in the specified HDFS path
     * 
     * @param hdfsPath the path to list
     * @return a list of file status objects
     * @throws IOException if an error occurs during the operation
     */
    public List<FileStatus> listFiles(String hdfsPath) throws IOException {
        System.out.println("Starting listFiles operation for path: " + hdfsPath);
        long startTime = System.currentTimeMillis();
        
        Path path = new Path(hdfsPath);
        List<FileStatus> fileStatuses = new ArrayList<>();
        
        if (!fileSystem.exists(path)) {
            System.out.println("Path does not exist: " + hdfsPath);
            return fileStatuses;
        }
        
        FileStatus[] statuses = fileSystem.listStatus(path);
        for (FileStatus status : statuses) {
            fileStatuses.add(status);
            System.out.println(status.getPath().getName() + ": " + 
                    status.getLen() + " bytes, isDirectory: " + 
                    status.isDirectory());
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("listFiles operation completed in " + (endTime - startTime) + " ms");
        
        return fileStatuses;
    }
    
    /**
     * Reads the content of a file from HDFS
     * 
     * @param hdfsFilePath the path to the file in HDFS
     * @return the content of the file as a string
     * @throws IOException if an error occurs during the operation
     */
    public String readFile(String hdfsFilePath) throws IOException {
        System.out.println("Starting readFile operation for path: " + hdfsFilePath);
        long startTime = System.currentTimeMillis();
        
        Path path = new Path(hdfsFilePath);
        
        if (!fileSystem.exists(path)) {
            System.out.println("readFile operation failed: File not found");
            throw new FileNotFoundException("File not found: " + hdfsFilePath);
        }
        
        if (fileSystem.getFileStatus(path).isDirectory()) {
            System.out.println("readFile operation failed: Cannot read a directory");
            throw new IOException("Cannot read a directory: " + hdfsFilePath);
        }
        
        // Reconfigure timeout settings - key parameters
        Configuration conf = fileSystem.getConf();
        
        // Record old settings
        System.out.println("Current timeout settings:");
        for (String param : new String[]{
                SOCKET_TIMEOUT, SOCKET_TIMEOUT_READ, "ipc.client.connect.timeout",
                "dfs.client.read.shortcircuit.timeout", "dfs.client.socket.send.buffer.size",
                "dfs.client.socket.receive.buffer.size", "dfs.datanode.socket.write.timeout", 
                "dfs.datanode.socket.read.timeout", "dfs.client.block.read.timeout"}) {
            System.out.println("  " + param + " = " + conf.get(param, "(not set)"));
        }
        
        // Set key timeout parameters
        conf.setInt(SOCKET_TIMEOUT, DEFAULT_TIMEOUT_MS);                        // Socket timeout
        conf.setInt(SOCKET_TIMEOUT_READ, DEFAULT_TIMEOUT_MS);                   // Socket read timeout
        conf.setInt("dfs.client.socket.timeout", DEFAULT_TIMEOUT_MS);           // Client socket timeout
        conf.setInt("dfs.datanode.socket.write.timeout", DEFAULT_TIMEOUT_MS);   // DataNode write timeout
        conf.setInt("dfs.datanode.socket.read.timeout", DEFAULT_TIMEOUT_MS);    // DataNode read timeout
        conf.setInt("dfs.client.block.read.timeout", DEFAULT_TIMEOUT_MS);       // Block read timeout
        conf.setInt("ipc.client.connect.timeout", DEFAULT_TIMEOUT_MS);          // RPC connect timeout
        conf.setInt("ipc.client.connection.maxidletime", DEFAULT_TIMEOUT_MS);   // Maximum idle time
        conf.setInt("ipc.client.connect.max.retries.on.timeouts", 1);           // Retries after timeout
        
        System.out.println("Applied all timeout settings to " + DEFAULT_TIMEOUT_MS + " ms");
        
        // Set maximum bytes to read
        final int MAX_BYTES_TO_READ = 4096;
        
        String content;
        try {
            System.out.println("Step 1: Opening HDFS file...");
            long openStartTime = System.currentTimeMillis();
            FSDataInputStream inputStream = fileSystem.open(path);
            
            // Set readahead buffer size
            try {
                long readaheadLength = conf.getLong("dfs.client.read.readahead", 64*1024);
                inputStream.setReadahead(readaheadLength);
                System.out.println("Set readahead buffer to " + readaheadLength + " bytes");
            } catch (Exception e) {
                System.out.println("Warning: Could not set readahead buffer: " + e.getMessage());
            }
            
            System.out.println("Step 1: File opened successfully in " + (System.currentTimeMillis() - openStartTime) + " ms");
            
            try {
                System.out.println("Step 2: Reading file content (max " + MAX_BYTES_TO_READ + " bytes)...");
                long readStartTime = System.currentTimeMillis();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream(MAX_BYTES_TO_READ);
                
                // Read at most MAX_BYTES_TO_READ bytes of data
                byte[] buffer = new byte[4096];
                int bytesRead;
                int totalBytesRead = 0;
                long lastProgressTime = System.currentTimeMillis();
                
                // Read up to 4096 bytes at once
                bytesRead = inputStream.read(buffer, 0, MAX_BYTES_TO_READ);
                if (bytesRead > 0) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead = bytesRead;
                    System.out.println("Read " + bytesRead + " bytes from file");
                }
                
                content = outputStream.toString();
                // Don't print the read content
                System.out.println("Step 2: Content read successfully in " + (System.currentTimeMillis() - readStartTime) + 
                                  " ms, total bytes: " + totalBytesRead);
                outputStream.close();
            } finally {
                System.out.println("Step 3: Closing input stream...");
                long closeStartTime = System.currentTimeMillis();
                inputStream.close();
                System.out.println("Step 3: Stream closed in " + (System.currentTimeMillis() - closeStartTime) + " ms");
            }
        } catch (Exception e) {
            long errorTime = System.currentTimeMillis();
            System.out.println("Error occurred after " + (errorTime - startTime) + " ms: " + e.getClass().getName());
            System.out.println("Error message: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Error reading file (after " + (errorTime - startTime) + " ms): " + hdfsFilePath, e);
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("readFile operation completed in " + (endTime - startTime) + " ms");
        
        return content;
    }
    
    /**
     * Writes content to a file in HDFS
     * 
     * @param hdfsFilePath the path to the file in HDFS
     * @param content the content to write
     * @param overwrite whether to overwrite an existing file
     * @throws IOException if an error occurs during the operation
     */
    public void writeFile(String hdfsFilePath, String content, boolean overwrite) throws IOException {
        System.out.println("Starting writeFile operation for path: " + hdfsFilePath);
        long startTime = System.currentTimeMillis();
        
        Path path = new Path(hdfsFilePath);
        
        if (fileSystem.exists(path) && !overwrite) {
            System.out.println("writeFile operation failed: File exists and overwrite not allowed");
            throw new IOException("File already exists and overwrite is not allowed: " + hdfsFilePath);
        }
        
        try (FSDataOutputStream outputStream = fileSystem.create(path, overwrite);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            
            writer.write(content);
            writer.flush();
            System.out.println("Successfully wrote to file: " + hdfsFilePath);
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("writeFile operation completed in " + (endTime - startTime) + " ms");
    }
    
    /**
     * Creates a directory in HDFS
     * 
     * @param hdfsDirectoryPath the path to create
     * @return true if the directory was created, false otherwise
     * @throws IOException if an error occurs during the operation
     */
    public boolean createDirectory(String hdfsDirectoryPath) throws IOException {
        Path path = new Path(hdfsDirectoryPath);
        boolean success = fileSystem.mkdirs(path);
        if (success) {
            System.out.println("Successfully created directory: " + hdfsDirectoryPath);
        } else {
            System.out.println("Failed to create directory: " + hdfsDirectoryPath);
        }
        return success;
    }
    
    /**
     * Deletes a file or directory in HDFS
     * 
     * @param hdfsPath the path to delete
     * @param recursive whether to recursively delete a directory
     * @return true if the path was deleted, false otherwise
     * @throws IOException if an error occurs during the operation
     */
    public boolean delete(String hdfsPath, boolean recursive) throws IOException {
        Path path = new Path(hdfsPath);
        boolean success = fileSystem.delete(path, recursive);
        if (success) {
            System.out.println("Successfully deleted: " + hdfsPath);
        } else {
            System.out.println("Failed to delete: " + hdfsPath);
        }
        return success;
    }
    
    /**
     * Checks if a file or directory exists in HDFS
     * 
     * @param hdfsPath the path to check
     * @return true if the path exists, false otherwise
     * @throws IOException if an error occurs during the operation
     */
    public boolean exists(String hdfsPath) throws IOException {
        Path path = new Path(hdfsPath);
        boolean exists = fileSystem.exists(path);
        System.out.println("Path " + hdfsPath + " exists: " + exists);
        return exists;
    }
    
    /**
     * Set HDFS operation timeout
     * 
     * @param timeoutMs timeout in milliseconds
     */
    public void setTimeout(int timeoutMs) {
        Configuration conf = fileSystem.getConf();
        conf.setInt(SOCKET_TIMEOUT, timeoutMs);
        conf.setInt(SOCKET_TIMEOUT_READ, timeoutMs);
        System.out.println("Socket timeout has been set to " + timeoutMs + " ms");
    }
    
    /**
     * Closes the HDFS client and releases resources
     * 
     * @throws IOException if an error occurs during the operation
     */
    public void close() throws IOException {
        if (fileSystem != null) {
            fileSystem.close();
            System.out.println("HDFS client closed");
        }
    }
    
    /**
     * Get the first N bytes of a file
     * 
     * @param hdfsFilePath HDFS file path
     * @param maxBytes maximum number of bytes to read
     * @return file content as byte array
     * @throws IOException if an error occurs during reading
     */
    public byte[] readFileBytes(String hdfsFilePath, int maxBytes) throws IOException {
        System.out.println("Starting readFileBytes operation for path: " + hdfsFilePath + ", max bytes: " + maxBytes);
        long startTime = System.currentTimeMillis();
        
        Path path = new Path(hdfsFilePath);
        
        if (!fileSystem.exists(path)) {
            System.out.println("readFileBytes operation failed: File not found");
            throw new FileNotFoundException("File not found: " + hdfsFilePath);
        }
        
        if (fileSystem.getFileStatus(path).isDirectory()) {
            System.out.println("readFileBytes operation failed: Cannot read a directory");
            throw new IOException("Cannot read a directory: " + hdfsFilePath);
        }
        
        // Apply timeout settings
        Configuration conf = fileSystem.getConf();
        conf.setInt(SOCKET_TIMEOUT, DEFAULT_TIMEOUT_MS);
        conf.setInt(SOCKET_TIMEOUT_READ, DEFAULT_TIMEOUT_MS);
        
        byte[] content = null;
        try {
            System.out.println("Opening file and reading content...");
            FSDataInputStream inputStream = fileSystem.open(path);
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream(maxBytes);
                byte[] buffer = new byte[Math.min(4096, maxBytes)];
                int bytesRead;
                int totalBytesRead = 0;
                
                while (totalBytesRead < maxBytes && (bytesRead = inputStream.read(buffer, 0, Math.min(buffer.length, maxBytes - totalBytesRead))) > 0) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                }
                
                content = outputStream.toByteArray();
                System.out.println("Successfully read " + totalBytesRead + " bytes");
                outputStream.close();
            } finally {
                inputStream.close();
            }
        } catch (Exception e) {
            System.out.println("Error reading file: " + e.getMessage());
            throw new IOException("Failed to read file: " + hdfsFilePath, e);
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("readFileBytes operation completed in " + (endTime - startTime) + " ms");
        
        return content;
    }
} 