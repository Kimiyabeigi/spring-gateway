package afarin.modules.gateway.service;

import afarin.modules.gateway.config.AESConfig;
import afarin.modules.gateway.model.JsonWebToken;
import afarin.modules.gateway.model.dto.JwtRequestDTO;
import afarin.modules.gateway.model.dto.JwtResponseDTO;
import afarin.modules.gateway.model.dto.UserDTO;
import afarin.modules.gateway.security.jwt.JwtTokenUtil;
import ir.karafarin.AESUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;

@Service
public class JwtAuthenticationService {

  private final Logger log = LoggerFactory.getLogger(getClass().getName());
  private final JwtTokenUtil jwtTokenUtil;
  private final CustomUserDetailsService customUserDetailsService;
  private final UserService userService;
  private final AESConfig aesConfig;

  public JwtAuthenticationService(
      JwtTokenUtil jwtTokenUtil,
      CustomUserDetailsService customUserDetailsService,
      UserService userService,
      AESConfig aesConfig) {
    this.jwtTokenUtil = jwtTokenUtil;
    this.customUserDetailsService = customUserDetailsService;
    this.userService = userService;
    this.aesConfig = aesConfig;
  }

  /**
   * createAuthenticationToken
   *
   * @param jwtRequestDTO
   * @return JwtResponseDTO
   * @throws Exception
   */
  public JwtResponseDTO createAuthenticationToken(
      JwtRequestDTO jwtRequestDTO, boolean encrypted, String ip)
      throws BadCredentialsException, DisabledException {

    String username = jwtRequestDTO.getUsername();
    String password = jwtRequestDTO.getPassword();
    if (encrypted) {
      AESConfig.Client client = getCurrentClient(ip);
      username = AESUtil.decrypt(username, client.getKey());
      password = AESUtil.decrypt(password, client.getKey());
    }

    log.info(
        "Request to createAuthenticationToken by Username: {}, ip: {}, encrypted: {}",
        username,
        ip,
        encrypted);

    authenticate(username, password);
    final UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
    UserDTO userDTO = userService.findByUserName(username);
    JsonWebToken jsonWebToken = jwtTokenUtil.generateToken(userDetails, userDTO.getIp());
    JwtResponseDTO jwtResponseDTO = new JwtResponseDTO();
    jwtResponseDTO.setJwtToken(jsonWebToken.getToken());
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    jwtResponseDTO.setExpirationStr(dateFormat.format(jsonWebToken.getExpiration()));
    jwtResponseDTO.setExpiration(jsonWebToken.getExpiration().getTime());

    log.info("Response of calling createAuthenticationToken {}", jwtResponseDTO);

    return jwtResponseDTO;
  }

  /**
   * authenticate
   *
   * @param username
   * @param password
   * @throws Exception
   */
  private void authenticate(String username, String password)
      throws BadCredentialsException, DisabledException {
    UserDTO user = userService.findByUserNameAndPassword(username, password);
    if (user == null) throw new BadCredentialsException("INVALID_CREDENTIALS");
    else if (!user.getIsActive()) throw new DisabledException("USER_DISABLED");
  }

  /**
   * @param ip
   * @return
   */
  private AESConfig.Client getCurrentClient(String ip) {
    return aesConfig.getClients().stream()
        .filter(c -> c.getIp().equals(ip))
        .findAny()
        .orElseGet(null);
  }
}
