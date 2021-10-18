package afarin.modules.gateway.service;

import afarin.modules.gateway.model.dao.UserRepository;
import afarin.modules.gateway.model.entity.Authority;
import afarin.modules.gateway.model.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Component
@Transactional
public class CustomUserDetailsService implements UserDetailsService {

  private UserRepository userRepository;

  public CustomUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * loadUserByUsername
   *
   * @param username
   * @return UserDetails
   * @throws UsernameNotFoundException
   */
  @Override
  public UserDetails loadUserByUsername(String username) {
    User user = userRepository.findByUsername(username).orElse(null);
    if (user == null)
      throw new UsernameNotFoundException("No user found with username ".concat(username));

    Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

    for (Authority authority : user.getAuthorities()) {
      grantedAuthorities.add(
          new SimpleGrantedAuthority(
              authority.getUri().concat("@").concat(authority.getMethod().name())));
    }

    return new org.springframework.security.core.userdetails.User(
        user.getUsername(),
        user.getPassword(),
        user.getIsActive(),
        true,
        true,
        true,
        grantedAuthorities);
  }
}
