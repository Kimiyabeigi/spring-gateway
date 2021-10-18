package afarin.modules.gateway.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class JsonWebToken {
    private String token;
    private Date expiration;
}
