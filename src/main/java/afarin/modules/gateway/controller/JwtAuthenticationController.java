package afarin.modules.gateway.controller;

import afarin.modules.gateway.constant.Constants;
import afarin.modules.gateway.model.dto.ErrorDTO;
import afarin.modules.gateway.model.dto.JwtRequestDTO;
import afarin.modules.gateway.model.dto.JwtResponseDTO;
import afarin.modules.gateway.service.JwtAuthenticationService;
import afarin.modules.gateway.util.NetworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gateway")
public class JwtAuthenticationController {

  private final Logger log = LoggerFactory.getLogger(getClass().getName());
  private final JwtAuthenticationService jwtAuthenticationService;

  @Autowired
  public JwtAuthenticationController(JwtAuthenticationService jwtAuthenticationService) {
    this.jwtAuthenticationService = jwtAuthenticationService;
  }

  /**
   * @param jwtRequestDTO jwtRequestDTO
   * @return JwtResponseDTO
   * @throws BadCredentialsException createAuthenticationToken
   */
  @PostMapping("/login")
  public ResponseEntity<Object> createAuthenticationToken(
      @RequestHeader(value = Constants.security.header.XFF, required = false) String xff,
      @RequestBody JwtRequestDTO jwtRequestDTO,
      ServerHttpRequest request)
      throws BadCredentialsException {

    log.info("x-forwarded-for: {}", xff);

    try {
      JwtResponseDTO authenticationToken =
          jwtAuthenticationService.createAuthenticationToken(
              jwtRequestDTO, false, NetworkUtil.getIpFromRequest(request));
      return ResponseEntity.ok().body(authenticationToken);
    } catch (BadCredentialsException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new ErrorDTO(Integer.parseInt("401"), "Wrong username or password"));
    }
  }

  /**
   * @param jwtRequestDTO JwtRequestDTO
   * @return JwtResponseDTO
   * @throws BadCredentialsException createAuthenticationToken
   */
  @PostMapping("/crypto/login")
  public ResponseEntity<Object> createCryptoAuthenticationToken(
      @RequestHeader(value = Constants.security.header.XFF, required = false) String xff,
      @RequestBody JwtRequestDTO jwtRequestDTO,
      ServerHttpRequest request)
      throws BadCredentialsException {

    log.info("x-forwarded-for: {}", xff);

    try {
      JwtResponseDTO authenticationToken =
          jwtAuthenticationService.createAuthenticationToken(
              jwtRequestDTO, true, NetworkUtil.getIpFromRequest(request));
      return ResponseEntity.ok().body(authenticationToken);
    } catch (BadCredentialsException e) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(new ErrorDTO(Integer.parseInt("401"), "Wrong username or password"));
    }
  }
}
