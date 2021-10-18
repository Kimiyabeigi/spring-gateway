package afarin.modules.gateway.model.dto;

import lombok.Data;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @author k.kimiyabeigi
 * @version 1.0.0
 * @implNote The above class has been implemented for Project gateway.
 * @since 1/2/2021 2:18 PM
 **/
@Data
public class TokenValidateDTO {
    private UserDetails userDetails;
    private String message;
}
