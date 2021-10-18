package afarin.modules.gateway.model.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = {"password"})
public class JwtRequestDTO {

  private String username;
  private String password;

}
