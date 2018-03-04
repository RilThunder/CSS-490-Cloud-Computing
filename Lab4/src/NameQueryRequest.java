/**
 * Java POJO for the request to query DynamoDB
 * This will be automatically mapped by API Gateway
 * @date Feb 15th,2018
 */

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NameQueryRequest {
    private String firstName;
    private String lastName;
}
