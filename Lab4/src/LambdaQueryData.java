/**
 * This class will be attached to a serverless Lambda environment that serve the web app through API Gateway
 * This class handle the querying of data from DynamoDB
 *
 * @date Feb 15th,2018
 */

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class LambdaQueryData implements RequestHandler<NameQueryRequest, String> {

    /**
     * This method is invoked whenever a user of a web app want to query a first name or last name
     * @param queryRequest a Java POJO object that was automatically parsed from the API Gateway connection
     * @param context the context of the request
     * @return a JSON String that represent the result of the query that was taken from DynamoDB
     */
    public String handleRequest(NameQueryRequest queryRequest, Context context) {
        LoadAndQueryHelper helper = LoadAndQueryHelper.getHelper();
        return helper.queryData(queryRequest, context.getLogger());
    }
}
