package afarin.modules.gateway.model.dao;

import afarin.modules.gateway.model.entity.User;

import java.util.Optional;

public interface UserRepository extends BaseRepository<User> {

  Optional<User> findByUsername(String username);
  Optional<User> findByIpContaining(String ip);
}
