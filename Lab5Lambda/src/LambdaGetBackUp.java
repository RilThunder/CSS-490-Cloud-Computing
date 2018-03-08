/**
 * This Lambda function is invoked through API Gateway
 * It will receive a Java POJO object that contains the user's id and password
 * It will then send an email to the email address of the user stored in DynamoDB
 */


import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LambdaGetBackUp implements RequestHandler<GetBackUpRequest, String> {

    /**
     * This method will call a helper method to send an email containing all links to uploaded files to user's email
     * @param getBackUpRequest A Java POJO object that contains the user's id and password
     * @param context A Context object that contains the Lambda's execution info
     * @return a String that represent the result of the request
     */
    public String handleRequest(GetBackUpRequest getBackUpRequest, Context context) {
        LambdaLogger logger = context.getLogger();
        RegisterAndGetBackUpHelper helper = RegisterAndGetBackUpHelper.getHelper();
        return helper.getBackUp(getBackUpRequest, logger);

    }
}
