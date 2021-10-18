package afarin.modules.gateway.model.mapper;

import afarin.modules.gateway.model.dto.UserDTO;
import afarin.modules.gateway.model.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

  UserDTO toDto(User user);

  User toEntity(UserDTO userDTO);

  default User userFromId(Long id) {
    if (id == null) {
      return null;
    }
    User user = new User();
    user.setId(id);
    return user;
  }

}
