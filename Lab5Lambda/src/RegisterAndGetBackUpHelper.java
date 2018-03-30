import com.amazonaws.HttpMethod;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class RegisterAndGetBackUpHelper {
    public static final String SUBJECT = "Download link for your data";
    public static final String PRIMARY_KEY = "user_id";
    public static final String PASSWORD = "password";
    public static final String EMAIL = "email";

    private static final String RESOURCE_NAME = "css490finalproject";
    private static final String FROM = "css490finalproject@gmail.com";
    private static final String SHORTENER_API_KEY = "AIzaSyAASFjuGxkpK2NoEqyxYYLF9IxsEJhLazU";
    private static final String GOOGLE_SHORTEN_API = "https://www.googleapis.com/urlshortener/v1/url";


    private LambdaLogger logger;
    private Table table;
    private AmazonS3 s3Client;
    private AmazonSimpleEmailService sesClient;

    private static RegisterAndGetBackUpHelper helper;


    private RegisterAndGetBackUpHelper() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
        table = new Table(client, RESOURCE_NAME);
        s3Client = AmazonS3ClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain()).build();
        sesClient = AmazonSimpleEmailServiceClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .withRegion(Regions.US_WEST_2)
                .build();
    }

    /**
     * Helper method to get a singleton of the class
     *
     * @return an instance of RegisterAndGetBackUpHelper
     */
    public static RegisterAndGetBackUpHelper getHelper() {
        if (helper == null) {
            helper = new RegisterAndGetBackUpHelper();
        }
        return helper;
    }

    /**
     * This method is used to register user to use the backup application
     * It will save the user's data into DynamoDB
     *
     * @param request      a Java POJO class that contains id, password and email address
     * @param lambdaLogger used to log for new user + existing user
     * @return a String that represent the condition of the request
     */
    public String registerUser(RegisterUserRequest request, LambdaLogger lambdaLogger) {
        if (request == null)
        {
            return "Incorrect data recevied. Can't be null";
        }
        logger = lambdaLogger;
        String id = request.getUser_id();
        String password = request.getPassword();
        String email = request.getEmail();
        if (id == null || password == null || email == null) {
            return "The data received is null";
        }
        if (id.isEmpty() || password.isEmpty() || password.isEmpty()) {
            return "Does not accept emtpy data";
        }
        Item result = table.getItem(PRIMARY_KEY, id);
        if (result != null) {
            logger.log("Received a request with an existing user: " + id);
            return "User already registered in database. Please create a new unique id";
        }
        Item itemToPut = new Item();
        itemToPut.withString(PRIMARY_KEY, id);
        itemToPut.withString(PASSWORD, password);
        itemToPut.withString(EMAIL, email);
        table.putItem(itemToPut);
        logger.log("Created new user: " + id);
        return "User created";
    }

    /**
     * This method is invoked when a user want to get the link to all of their files on S3
     *
     * @param request      The Java POJO object that contains the id and password of user
     * @param lambdaLogger the logging object
     * @return a String that represent the result (Unable to sent email, sent email, incorrect authentication)
     */
    public String getBackUp(GetBackUpRequest request, LambdaLogger lambdaLogger) {
        logger = lambdaLogger;
        if (request == null) {
            return "Invalid request";
        }
        String id = request.getUser_id();
        String password = request.getPassword();
        if (id == null || id.isEmpty() || password == null || password.isEmpty()) {
            return "Invalid data received";
        }
        Item user = table.getItem(PRIMARY_KEY, id);
        if (user == null) {
            logger.log("Incorrect id: " + id + "tried to use the application");
            return "You do not have an account associated with this application. ";
        }
        String storedPassword = user.getString(PASSWORD);
        if (!storedPassword.equals(password)) {
            logger.log("Incorrect password was provided with this id " + id);
            return "You have entered an incorrect password for this id";
        }

        return sendEmail(user);


    }

    /**
     * This helper method is used to send an email that contains pre-signed url of the files on S3 to the user
     *
     * @param user The user's data that we got from DynamoDB
     * @return a string that represent the result of the sending email action
     */
    private String sendEmail(Item user) {
        String emailDestination = user.getString(EMAIL);
        Date expiration = new Date();
        long milliseconds = expiration.getTime();
        milliseconds += 1000 * 60 * 60; // expires in 1 hour
        expiration.setTime(milliseconds);
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(RESOURCE_NAME)
                .withPrefix(user.getString(PRIMARY_KEY));
        ObjectListing objectListing = s3Client.listObjects(listObjectsRequest); // get everything the user uploaded
        List<S3ObjectSummary> listOfObjectUploaded = objectListing.getObjectSummaries();
        StringBuilder textBody = new StringBuilder();
        // Go through every file(key) that the user uploaded and create a presigned url
        // Add those to the would be message
        try {
            for (S3ObjectSummary uploadedItem : listOfObjectUploaded) {
                String key = uploadedItem.getKey();
                GeneratePresignedUrlRequest presignedUrlRequest = new GeneratePresignedUrlRequest(RESOURCE_NAME, key);
                presignedUrlRequest.setMethod(HttpMethod.GET);
                presignedUrlRequest.setExpiration(expiration);
                URL url = s3Client.generatePresignedUrl(presignedUrlRequest);
                String shortURL = getShortenURL(url.toString());
                textBody.append(key);
                textBody.append("\n");
                textBody.append(shortURL);
                textBody.append("\n"); // add some white space to look nice
                textBody.append("\n");
            }
            // Prep the email to sent. This include specify TO address, FROM address, the message and the subject
            Destination destination = new Destination().withToAddresses(emailDestination);
            Body messageBody = new Body().withText(new Content().withCharset("UTF-8").withData(textBody.toString()));
            Message messageToSend = new Message().withBody(messageBody)
                    .withSubject(new Content().withCharset("UTF-8").withData(SUBJECT));
            SendEmailRequest sendRequest = new SendEmailRequest().withDestination(destination)
                    .withMessage(messageToSend)
                    .withSource(FROM);
            sesClient.sendEmail(sendRequest);
            logger.log("Sent an email to user: " + user.getString(PRIMARY_KEY));
        } catch (Exception exception) {
            logger.log("Unable to send email because of this error");
            logger.log(exception.toString());
            return "Amazon SES encountered an error while sending email to this address:" + emailDestination;
        }
        return "Email was sent";

    }

    /**
     * This is a helper method to shorten the long presigned URL to a shorter one
     * @param longURL The original long URL
     * @return the shorten url in String
     * @throws Exception when failed to make the request
     */
    private String getShortenURL(String longURL) throws Exception {
        String json = "{\"longUrl\": \"" + longURL + "\"}";
        String shortenerAPI = GOOGLE_SHORTEN_API + "?key=" + SHORTENER_API_KEY;
        HttpPost postRequest = new HttpPost(shortenerAPI);
        postRequest.setHeader("Content-Type", "application/json");
        postRequest.setEntity(new StringEntity(json, "UTF-8"));
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(postRequest);
        String responseText = EntityUtils.toString(response.getEntity());
        Gson gson = new Gson();
        HashMap<String, String> res = gson.fromJson(responseText, HashMap.class);
        return res.get("id");
    }


}




