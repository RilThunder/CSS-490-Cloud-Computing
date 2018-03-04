/**
 * This class will be attached to a serverless Lambda environment that serve the web app through API Gateway
 * This class handle the loading of data into DynamoDB
 */

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LambdaLoadData implements RequestHandler<Object, String> {

    /**
     * This method is invoked through API Gateway whenever a user pressed Load Data
     * It will call a helper function from LoadAndQueryHelper that will load the data and return result of the request
     * @param nothing This function does not need input
     * @param context Context object of the lambda invocation
     * @return a String that represent the result of the request
     */
    public String handleRequest(Object nothing, Context context) {
        LoadAndQueryHelper helper = LoadAndQueryHelper.getHelper();
        return helper.loadData(context.getLogger());
    }
}
