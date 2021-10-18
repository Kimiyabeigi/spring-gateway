package afarin.modules.gateway.model.dto;

import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Data
@ToString(exclude = "password")
public class UserDTO {

  private Long id;
  @NotNull
  private String username;
  @NotNull
  private String ip;
  @NotNull
  private String password;
  @NotNull
  private String authType;
  private Boolean isActive = true;
  private String applicationId;
  private String moduleId;

}
