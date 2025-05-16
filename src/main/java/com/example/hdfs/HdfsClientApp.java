package com.example.hdfs;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;
import java.util.List;

public class HdfsClientApp {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: HdfsClientApp <hdfs-uri> [operation] [params...]");
            System.err.println("Operations:");
            System.err.println("  list <path>                - List files in directory");
            System.err.println("  read <file-path>           - Read file content");
            System.err.println("  write <file-path> <content> [overwrite] - Write content to file");
            System.err.println("  mkdir <dir-path>           - Create directory");
            System.err.println("  delete <path> [recursive]  - Delete file or directory");
            System.err.println("  exists <path>              - Check if path exists");
            System.err.println("  whoami                     - Show current authenticated user");
            System.exit(1);
        }

        String hdfsUri = args[0];
        String operation = args.length > 1 ? args[1] : "list";
        
        try (HdfsClient hdfsClient = new HdfsClient(hdfsUri)) {
            switch (operation) {
                case "list":
                    listOperation(hdfsClient, args);
                    break;
                case "read":
                    readOperation(hdfsClient, args);
                    break;
                case "write":
                    writeOperation(hdfsClient, args);
                    break;
                case "mkdir":
                    mkdirOperation(hdfsClient, args);
                    break;
                case "delete":
                    deleteOperation(hdfsClient, args);
                    break;
                case "exists":
                    existsOperation(hdfsClient, args);
                    break;
                case "whoami":
                    whoamiOperation();
                    break;
                default:
                    System.err.println("Unknown operation: " + operation);
                    System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error executing operation: " + e.getMessage());
            e.printStackTrace();
            
            // Specifically handle Kerberos authentication errors
            if (e.getMessage() != null && e.getMessage().contains("Kerberos")) {
                System.err.println("\nKerberos authentication error. Please check:");
                System.err.println("1. The keytab file exists and is readable");
                System.err.println("2. The principal is correct");
                System.err.println("3. The keytab file contains keys for the specified principal");
                System.err.println("4. The client has the correct time (time skew can cause authentication failures)");
            }
            
            System.exit(1);
        }
    }

    private static void listOperation(HdfsClient hdfsClient, String[] args) throws IOException {
        String path = args.length > 2 ? args[2] : "/";
        System.out.println("Listing files in " + path + ":");
        
        List<FileStatus> fileStatuses = hdfsClient.listFiles(path);
        for (FileStatus status : fileStatuses) {
            String fileType = status.isDirectory() ? "d" : "-";
            System.out.printf("%s %12d %s\n", 
                    fileType, 
                    status.getLen(), 
                    status.getPath().getName());
        }
    }

    private static void readOperation(HdfsClient hdfsClient, String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Missing file path for read operation");
            System.exit(1);
        }
        
        String filePath = args[2];
        hdfsClient.readFile(filePath);
    }

    private static void writeOperation(HdfsClient hdfsClient, String[] args) throws IOException {
        if (args.length < 4) {
            System.err.println("Missing parameters for write operation");
            System.exit(1);
        }
        
        String filePath = args[2];
        String content = args[3];
        boolean overwrite = args.length > 4 && Boolean.parseBoolean(args[4]);
        
        hdfsClient.writeFile(filePath, content, overwrite);
        System.out.println("Successfully wrote to " + filePath);
    }

    private static void mkdirOperation(HdfsClient hdfsClient, String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Missing directory path for mkdir operation");
            System.exit(1);
        }
        
        String dirPath = args[2];
        boolean success = hdfsClient.createDirectory(dirPath);
        System.out.println("Directory creation " + (success ? "successful" : "failed") + ": " + dirPath);
    }

    private static void deleteOperation(HdfsClient hdfsClient, String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Missing path for delete operation");
            System.exit(1);
        }
        
        String path = args[2];
        boolean recursive = args.length > 3 && Boolean.parseBoolean(args[3]);
        
        boolean success = hdfsClient.delete(path, recursive);
        System.out.println("Deletion " + (success ? "successful" : "failed") + ": " + path);
    }

    private static void existsOperation(HdfsClient hdfsClient, String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("Missing path for exists operation");
            System.exit(1);
        }
        
        String path = args[2];
        boolean exists = hdfsClient.exists(path);
        System.out.println("Path " + path + " " + (exists ? "exists" : "does not exist"));
    }
    
    private static void whoamiOperation() throws IOException {
        UserGroupInformation currentUser = UserGroupInformation.getCurrentUser();
        System.out.println("Current authenticated user: " + currentUser.getUserName());
        System.out.println("Authentication method: " + currentUser.getAuthenticationMethod());
        System.out.println("Is using Kerberos: " + currentUser.hasKerberosCredentials());
        
        if (currentUser.hasKerberosCredentials()) {
            System.out.println("Kerberos principal: " + currentUser.getUserName());
            String[] groups = currentUser.getGroupNames();
            System.out.println("Groups: " + String.join(", ", groups));
        }
    }
} 