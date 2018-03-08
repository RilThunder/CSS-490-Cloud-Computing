/**
 * @author Thuan Tran
 * @date February 7th, 2018
 * This program is used to back up a directory plus its subdirectories to a specific folder on S3
 */

import com.amazonaws.AmazonClientException;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Main {

    private static final String ACCESS_KEY = "AKIAI57RLEEWPZPFASLA";
    private static final String SECRET_KEY = "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";
    private static final String BUCKET_NAME = "css490lab3";

    private static AmazonS3 client;
    private static TransferManager manager;
    private static String directoryNumber;

    /**
     * Main entry point of the program. it will get a file path, upload all of them to a unique directory on S3
     * And print out the item in that unique directory
     * @param args a one argument command line argument
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Incorrect number of argument. Please only enter the full link to the directory");
            return;
        }
        // a unique directory number that is used to store the files in every backup
        directoryNumber = Long.toString(System.currentTimeMillis());
        try {
            // depth first search all files given the path
            Stream<Path> files = Files.walk(Paths.get(args[0]));
            List<Path> path = files.collect(Collectors.toList());
            setUpS3ClientAndTransferManager();
            System.out.println("Backing up to " + directoryNumber + " folder");
            for (Path item : path) {
                Upload upload = getUpload(item);
                if (upload == null) {
                    continue;
                }
                // Source https://aws.amazon.com/blogs/developer/amazon-s3-transfermanager/
                double previousPercentage = -1;
                while (!upload.isDone()) {
                    // For empty directory, the percentage will be shown as -1%. In addition, the thread
                    // display the same percentage sometime. So this is to prevent duplicate percentage and -1% be shown
                    double percentageTransferred = upload.getProgress().getPercentTransferred();
                    if (percentageTransferred > -1.0 && previousPercentage != percentageTransferred) {
                        System.out.println(upload.getProgress().getPercentTransferred() + "%");
                    }
                    previousPercentage = percentageTransferred;
                }
                upload.waitForCompletion();
                System.out.println("Uploaded " + item.toString() + " to directory " + directoryNumber + " complete");
                System.out.println();
            }
            Thread.sleep(200); // Make sure that S3 is updated so wait a while
            getBackUpFolderInfo();
        }
        catch (IOException exception) {
            System.out.println("Invalid Path encountered. Please provide correct path argument");
        }
        catch (InterruptedException exception) {
            System.out.println("Interrupted while uploading item. Canceled remaining upload");
        }
        catch (SecurityException exception) {
            System.out.println("Unable to access to the file because of denied access");
        }
        catch (AmazonClientException exception) {
            System.out.println("Something happened to the Amazon Web Service. Check if S3 is down or incorrect key");
        }
        finally {
            if (manager != null) {
                // close all remaining thread when finished uploading
                manager.shutdownNow();
            }
        }
        return;
    }

    /**
     * Helper method to set up S3 method and Transfer Manager tool to upload
     */
    private static void setUpS3ClientAndTransferManager() {
        AWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(credentials);
        client = AmazonS3ClientBuilder.standard().withCredentials(credentialsProvider).build();
        manager =  TransferManagerBuilder.standard().withS3Client(client).build();
    }

    /**
     * Helper method to create an upload to S3 based on the content type of the file
     * @param pathToItem the object that represent a path to an object we want to upload
     * @return an upload for a file so that we can query its progress later
     * @throws IOException when invalid file path is detected
     */
    private static Upload getUpload(Path pathToItem) throws IOException{
        Upload upload = null;
        File fileToUpload = pathToItem.toFile();
        // canonicalPath is a unique path that allows access to the file
        String keyForFileOnS3 = directoryNumber + fileToUpload.getCanonicalPath();
        System.out.println("This item is going to be uploaded " + fileToUpload.getCanonicalPath());
        if (fileToUpload.isDirectory() && fileToUpload.listFiles().length == 0) {
            ()      // in the case of empty directory
            System.out.println("Encountering empty directory. Will create empty directory on S3");
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("binary/octet-stream"); // S3 handles directories different
            metadata.setContentLength(0);
            InputStream emptyObject = new ByteArrayInputStream(new byte[0]);
            upload = manager.upload(BUCKET_NAME, keyForFileOnS3 + "/", emptyObject, metadata);
        } else if (fileToUpload.isDirectory()) {
            System.out.println("Encountering normal directories with files. No need to upload to overwrite");
            // In the case of regular directory, we don't need to worry since later on, the item in the directory will
            // be uploaded that also create the directory
            System.out.println();
        } else {
            System.out.println("Encountering normal files. This is going to be uploaded");
            upload = manager.upload(BUCKET_NAME, keyForFileOnS3, fileToUpload);
        }
        return upload;
    }

    /**
     * This is a helper method make sure the object was uploaded to S3 by printing the content
     * in the directory that we just uploaded the items to
     */
    private static void getBackUpFolderInfo() {
        System.out.println();
        System.out.println("Now printing the directory file contents");
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(BUCKET_NAME)
                                                                        .withPrefix(directoryNumber);
        ObjectListing objectListing = client.listObjects(listObjectsRequest);
        List<S3ObjectSummary> listOfObjectUploaded = objectListing.getObjectSummaries();
        for (S3ObjectSummary uploadedItem : listOfObjectUploaded) {
            System.out.println(uploadedItem.getKey());
        }
    }
}