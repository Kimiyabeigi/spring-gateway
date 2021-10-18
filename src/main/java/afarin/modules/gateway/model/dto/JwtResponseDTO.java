package afarin.modules.gateway.model.dto;

import lombok.Data;

@Data
public class JwtResponseDTO {

  private String jwtToken;
  private String expirationStr;
  private Long expiration;
}
