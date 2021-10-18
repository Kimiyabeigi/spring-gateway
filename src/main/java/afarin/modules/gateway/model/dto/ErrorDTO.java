package afarin.modules.gateway.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author k.kimiyabeigi
 * @version 1.0.0
 * @implNote The above class has been implemented for Project gateway.
 * @since 12/30/2020 11:11 AM
 */
@Data
@AllArgsConstructor
public class ErrorDTO {

  @NotNull private Integer errorCode;
  private String errorDesc;
  private List<String> errorDetails;

  public ErrorDTO(@NotNull Integer errorCode, String errorDesc) {
    this.errorCode = errorCode;
    this.errorDesc = errorDesc;
  }
}
