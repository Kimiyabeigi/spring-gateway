package afarin.modules.gateway.service;

import afarin.modules.gateway.model.dao.UserRepository;
import afarin.modules.gateway.model.dto.UserDTO;
import afarin.modules.gateway.model.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final Logger log = LoggerFactory.getLogger(getClass().getName());
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  public UserService(
      UserRepository userRepository, PasswordEncoder passwordEncoder, UserMapper userMapper) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.userMapper = userMapper;
  }

  /**
   * findByUserName
   *
   * @param username
   * @return UserDTO
   */
  @Transactional(readOnly = true)
  public UserDTO findByUserName(String username) {
    return userRepository.findByUsername(username).map(userMapper::toDto).orElse(null);
  }

  @Transactional(readOnly = true)
  public UserDTO findByIp(String ip) {
    return userRepository.findByIpContaining(ip).map(userMapper::toDto).orElse(null);
  }

  public UserDTO findByUserNameAndPassword(String username, String password) {
    BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
    UserDTO user = findByUserName(username);
    if (user != null
        && user.getIsActive()
        && bCryptPasswordEncoder.matches(password, user.getPassword())) {
      return user;
    }

    return null;
  }
}
