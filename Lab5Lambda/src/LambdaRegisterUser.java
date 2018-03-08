/**
 * This lambda function is invoked through API Gateway
 * It will receive a Java POJO object that contains user_id, password and email to register the user
 * It will then return the result of the registering action
 */

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LambdaRegisterUser implements RequestHandler<RegisterUserRequest,String> {

    /**
     * This method will call the helper method to register a user to DynamoDB
     * @param registerUserRequest a Java POJO object that contains user's information
     * @param context Context object of the lambda's execution
     * @return a String result of the registering action
     */
    @Override
    public String handleRequest(RegisterUserRequest registerUserRequest, Context context) {
        LambdaLogger logger = context.getLogger();
        RegisterAndGetBackUpHelper helper = RegisterAndGetBackUpHelper.getHelper();
        return helper.registerUser(registerUserRequest,logger);

    }
}
