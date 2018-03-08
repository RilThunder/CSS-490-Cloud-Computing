/**
 * Java POJO for the request to create a new account
 * Automatically mapped by API Gateway
 */


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RegisterUserRequest {
    private String user_id;
    private String password;
    private String email;
}
