/**
 * Java POJO for requesting back up info to be sent to user's email address
 * Automatically mapped by API Gateway
 */

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class GetBackUpRequest {
    private String user_id;
    private String password;
}
