/**
 * This class is used to serve as the logic implemention of the web api
 * This class is responsible for loading the data into DynamoDB whenever a user hit load data
 * Or querying from DynamoDB when user
 */

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class LoadAndQueryHelper {


    public static final String TABLE_NAME = "css490lab4";
    public static final String FILE_LOCATION = "https://s3-us-west-2.amazonaws.com/css490/input.txt";

    public static final String INDEX = "firstName-lastName-index"; // Global Secondary index for cases querying first name

    public static final String PRIMARY_KEY_LAST_NAME = "lastName";
    public static final String SECONDARY_KEY_FIRST_NAME = "firstName";

    private static LoadAndQueryHelper helper;

    private LambdaLogger logger;


    private Table table;

    /**
     * Private constructor of the class so that it won't be called outside everytime the web api receive new request
     * Instantiate a connection to DynamoDB on AWS and get connection to the table we are handling data with
     */
    private LoadAndQueryHelper() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withCredentials(new DefaultAWSCredentialsProviderChain()) // already attached a role to the lambda environment
                .build();
        table = new Table(client, TABLE_NAME);
    }


    /**
     * Method to get a singleton instance of the class
     *
     * @return an instance LoadAndQueryHelper
     */
    public static LoadAndQueryHelper getHelper() {
        if (helper == null) {
            helper = new LoadAndQueryHelper();
        }
        return helper;
    }


    /**
     * This method is used to get the text data from S3 and load the items into DynamoDB
     *
     * @return a String that represent the result
     */
    public String loadData(LambdaLogger lambdaLogger) {
        logger = lambdaLogger;
        try {
            URL url = new URL(FILE_LOCATION);
            Scanner scanner = new Scanner(url.openStream());
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                // separate the data within the line by space
                List<String> unformattedData = new ArrayList<String>(Arrays.asList(line.trim().split("\\s+")));
                Item itemToPut = prepareDataToInsert(unformattedData);
                table.putItem(itemToPut);
            }
        } catch (MalformedURLException exception) {
            logger.log("The link to the S3 text file is broken. Needs update ");
            return "Could not load data to S3 due to malformed link to data";
        } catch (IOException exception) {
            logger.log("Connection to the S3 text file encountered error. Make sure the connection is stable");
            return "Could not load data to S3 due to connection error";

        }
        return "Completed loading the data";
    }


    /**
     * Private helper method to go over the line extracted and extract the key and value
     *
     * @param unformattedData a list of unformatted data
     *                        Example : Li Shen id=1234 phone=2065197887 office=none
     * @return a put item request so that DynamoDB can put the item
     * {["firstName","Li"], ["lastName","Shen"], ["phone","2065197887"], ["office","none"]}
     */
    private Item prepareDataToInsert(List<String> unformattedData) {
        Item itemToPut = new Item();
        for (int i = 0; i < unformattedData.size(); i++) {
            if (i == 0) {
                itemToPut.withString(PRIMARY_KEY_LAST_NAME, unformattedData.get(i));
                continue;
            }
            if (i == 1) {
                itemToPut.withString(SECONDARY_KEY_FIRST_NAME, unformattedData.get(i));
                continue;
            }
            // separate cases within the text file where attribute name and value are connected by "="
            String[] keyAndValue = unformattedData.get(i).trim().split("=");
            itemToPut.withString(keyAndValue[0], keyAndValue[1]);
        }
        return itemToPut;
    }

    /**
     * This method is used to query the info of a person from DynamoDB
     * It will query based on 3 cases: when only last name was provided, when only first name is provided
     * And when both first and last name are provided
     *
     * @param request      The Java POJO object that contains details of the request
     * @param lambdaLogger a logger to log the event
     * @return A Json String that represent the output
     */
    public String queryData(NameQueryRequest request, LambdaLogger lambdaLogger) {
        logger = lambdaLogger;
        String firstName = request.getFirstName();
        String lastName = request.getLastName();
        // query based on last name
        if (firstName == null || firstName.trim() == "") {
            logger.log("Received only the last name");
            return queryFirstOrLastName(lastName, 0);
        }
        // query based on first name
        if (lastName == null || lastName.trim() == "") {
            logger.log("Received only the first name");
            return queryFirstOrLastName(firstName, 1);
        }
        //get item based on both
        logger.log("Received both first name and last name. Will return only 1 result");
        return queryLastAndFirstName(lastName, firstName);
    }


    /**
     * Helper method to query based on last name
     *
     * @param firstNameOrLastName the name that we want to query
     * @param option              the option to query either first name or last name (0 is for last name, 1 is for first name)
     * @return A Json String that represent the output
     */
    private String queryFirstOrLastName(String firstNameOrLastName, int option) {
        JsonArray result = new JsonArray();
        PrimaryKey lastKeyEvaluated = null;
        String attributeCode;
        String valueCode;
        // query expression SQL Style. Search for anything that match
        // lastName = :lastName for example where :lastName will be replaced by the actual name
        if (option == 0) {
            attributeCode = PRIMARY_KEY_LAST_NAME;
            valueCode = ":" + PRIMARY_KEY_LAST_NAME;
        } else {
            attributeCode = SECONDARY_KEY_FIRST_NAME;
            valueCode = ":" + SECONDARY_KEY_FIRST_NAME;
        }
        do {
            Map<String, Object> attributeValues = new HashMap<>();
            attributeValues.put(valueCode, firstNameOrLastName);
            QuerySpec query = new QuerySpec().withKeyConditionExpression(attributeCode + " = " + valueCode)
                    .withValueMap(attributeValues)
                    .withExclusiveStartKey(lastKeyEvaluated);
            ItemCollection<QueryOutcome> queryResult;
            if (option == 0) {
                query = query.withConsistentRead(true);
                queryResult = table.query(query);
            } else {
                // sadly, secondary index does not support consistent read
                // Query from another table that has primary key as first name and secondary key as last name
                Index firstNameIndex = table.getIndex(INDEX);
                queryResult = firstNameIndex.query(query);
            }
            JsonArray tempResultInThisQuery = prepareResult(queryResult);
            result.addAll(tempResultInThisQuery);
            // DynamoDB can only query 1 MB of data per time. So we have to go through multiple pass
            // to get all the matching data
            // only when this is null that we have queried all the data in DynamoDB
            lastKeyEvaluated = prepareLastEvaluatedKey(queryResult, option);
        }
        while (lastKeyEvaluated != null);
        return result.toString();
    }

    /**
     * A method that is used to get a single result from DynamoDB given first and Last name
     *
     * @param lastName  the last name of the person
     * @param firstName the first name of the person
     * @return a Json string that represent the information about that person
     */
    private String queryLastAndFirstName(String lastName, String firstName) {
        Item result = table.getItem(PRIMARY_KEY_LAST_NAME, lastName, SECONDARY_KEY_FIRST_NAME, firstName);
        if (result == null) {
            logger.log("Searching for " + firstName + " " + lastName + ". No record of the person");
            return "No record was found for " + firstName + " " + lastName;
        }
        return result.toJSON();
    }

    /**
     * A method to add the record of the query result to a json array where each element is a json string of a record
     * in DynamoDB
     *
     * @param queryResult the query result given within this pass
     * @return a Json Array that contains the result queried within this pass
     */
    private JsonArray prepareResult(ItemCollection<QueryOutcome> queryResult) {
        JsonParser parser = new JsonParser();
        JsonArray parsedResult = new JsonArray();
        for (Item item : queryResult) {
            parsedResult.add(parser.parse(item.toJSONPretty()).getAsJsonObject());
        }
        return parsedResult;

    }

    /**
     * Helper method to get the last key that was found within a query result
     *
     * @param queryResult the query that DynamoDB returned within this pass
     * @return a Primary key so that the class know to query again from that point
     */
    private PrimaryKey prepareLastEvaluatedKey(ItemCollection<QueryOutcome> queryResult, int option) {
        Map<String, AttributeValue> lastEvaluatedKey = queryResult.getLastLowLevelResult()
                                                                  .getQueryResult()
                                                                  .getLastEvaluatedKey();
        if (lastEvaluatedKey == null) {
            return null;
        }
        PrimaryKey keyToStartNextSearch = null;
        if (option == 0) {
            // Using the original table where primary key is last name and secondary key is first name
            keyToStartNextSearch = new PrimaryKey(PRIMARY_KEY_LAST_NAME, lastEvaluatedKey.get(PRIMARY_KEY_LAST_NAME).getS(),
                    SECONDARY_KEY_FIRST_NAME, lastEvaluatedKey.get(SECONDARY_KEY_FIRST_NAME).getS());
        } else {
            // Using a secondary global index table where primary key is first name and secondary key is last name
            keyToStartNextSearch = new PrimaryKey(SECONDARY_KEY_FIRST_NAME, lastEvaluatedKey.get(SECONDARY_KEY_FIRST_NAME).getS(),
                    PRIMARY_KEY_LAST_NAME, lastEvaluatedKey.get(PRIMARY_KEY_LAST_NAME).getS());
        }
        return keyToStartNextSearch;
    }
}
