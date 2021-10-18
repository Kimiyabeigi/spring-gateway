package afarin.modules.gateway.security;

import afarin.modules.gateway.constant.Constants;
import afarin.modules.gateway.enums.AuthenticationType;
import afarin.modules.gateway.enums.MethodType;
import afarin.modules.gateway.model.dto.ErrorDTO;
import afarin.modules.gateway.model.dto.TokenValidateDTO;
import afarin.modules.gateway.model.dto.UserDTO;
import afarin.modules.gateway.security.jwt.JwtTokenUtil;
import afarin.modules.gateway.service.CustomUserDetailsService;
import afarin.modules.gateway.service.UserService;
import afarin.modules.gateway.util.NetworkUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.jsonwebtoken.ExpiredJwtException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
// public class SCGWPreFilter extends AbstractGatewayFilterFactory<Object> {
public class SCGWPreFilter implements Ordered, GlobalFilter {
  private final Logger log = LoggerFactory.getLogger(getClass().getName());
  private final JwtTokenUtil jwtTokenUtil;
  private final CustomUserDetailsService customUserDetailsService;
  private final UserService userService;

  public SCGWPreFilter(
      JwtTokenUtil jwtTokenUtil,
      CustomUserDetailsService customUserDetailsService,
      UserService userService) {
    this.jwtTokenUtil = jwtTokenUtil;
    this.customUserDetailsService = customUserDetailsService;
    this.userService = userService;
  }

  private boolean isAuthorizationValid(ServerHttpRequest request, UserDetails userDetails) {
    AtomicBoolean allMethodPermission = new AtomicBoolean(false);
    AtomicBoolean isMethodPermission = new AtomicBoolean(false);

    userDetails.getAuthorities().stream()
        .forEach(
            authority -> {
              String[] authorities = authority.getAuthority().split("@");
              if (request.getURI().getPath().contains(authorities[0])
                  && authorities[1].equalsIgnoreCase(MethodType.ALL.name()))
                allMethodPermission.set(true);

              if (request.getURI().getPath().contains(authorities[0])
                  && authorities[1].equalsIgnoreCase(request.getMethod().name()))
                isMethodPermission.set(true);
            });

    return allMethodPermission.get() || isMethodPermission.get();
  }

  private Mono<Void> onError(ServerWebExchange exchange, String errorMessage) {

    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    try {
      ErrorDTO errorDTO = new ErrorDTO(HttpStatus.UNAUTHORIZED.value(), errorMessage);
      ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
      String errorJson = objectWriter.writeValueAsString(errorDTO);
      byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
      DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
      return exchange.getResponse().writeWith(Flux.just(buffer));
    } catch (JsonProcessingException e) {
      log.error("There is an error for making error response in filter", e);
      e.printStackTrace();
    }

    return response.setComplete();
  }

  private TokenValidateDTO jwtValidate(ServerHttpRequest request) {
    TokenValidateDTO result = new TokenValidateDTO();
    final String requestTokenHeader =
        request.getHeaders().getFirst(Constants.security.jwtToken.HEADER_STRING);
    if (requestTokenHeader == null)
      return setError("There is no Authorization value in header of request", result);

    StringTokenizer stringTokenizer = new StringTokenizer(requestTokenHeader);
    if (!stringTokenizer.hasMoreTokens())
      return setError("The value of JWT Token does not have valid format", result);

    String bearer = stringTokenizer.nextToken();
    if (!bearer.equalsIgnoreCase(Constants.security.jwtToken.TOKEN_PREFIX_BEARER))
      return setError("The value of JWT Token does not begin with Bearer String", result);

    String username = null;
    String jwtToken = stringTokenizer.nextToken();
    try {
      username = jwtTokenUtil.getUsernameFromToken(jwtToken);
    } catch (IllegalArgumentException e) {
      return setError("Unable to get JWT Token", result);
    } catch (ExpiredJwtException e) {
      return setError("JWT Token has expired", result);
    } catch (Exception e) {
      return setError("Your JWT token is not a valid token", result);
    }

    // Once we get the token validate it.
    if (username != null) {
      UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
      // if token is valid configure Spring Security to manually set
      // authentication
      if (jwtTokenUtil.validateToken(jwtToken, userDetails, request)) {
        result.setUserDetails(userDetails);
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        // After setting the Authentication in the context, we specify
        // that the current user is authenticated. So it passes the
        // Spring Security Configurations successfully.
        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
      } else return setError("The current token has not been issued to your system", result);
    }

    return result;
  }

  private TokenValidateDTO basicValidate(ServerHttpRequest request) {
    TokenValidateDTO result = new TokenValidateDTO();
    final String requestTokenHeader =
        request.getHeaders().getFirst(Constants.security.jwtToken.HEADER_STRING);
    if (requestTokenHeader == null)
      setError("There is no Authorization value in header of request", result);

    StringTokenizer stringTokenizer = new StringTokenizer(requestTokenHeader);
    if (!stringTokenizer.hasMoreTokens())
      setError("The value of Basic Auth does not have valid format", result);

    String basic = stringTokenizer.nextToken();
    if (!basic.equalsIgnoreCase(Constants.security.jwtToken.TOKEN_PREFIX_BASIC))
      setError("The value of Basic Auth does not begin with Basic String", result);

    String basicAuth = stringTokenizer.nextToken();
    String credentials = new String(Base64.getDecoder().decode(basicAuth));
    int indexOfDelimiter = credentials.indexOf(":");

    if (indexOfDelimiter == -1)
      setError("The value of Basic Auth does not have ':' as a delimiter", result);

    String username = credentials.substring(0, indexOfDelimiter).trim();
    String password = credentials.substring(indexOfDelimiter + 1).trim();
    UserDTO userDTO = userService.findByUserNameAndPassword(username, password);
    if (userDTO == null) setError("Bad credentials for basic auth", result);

    UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
    // if token is valid configure Spring Security to manually set
    // authentication
    result.setUserDetails(userDetails);
    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

    // After setting the Authentication in the context, we specify
    // that the current user is authenticated. So it passes the
    // Spring Security Configurations successfully.
    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

    return result;
  }

  private TokenValidateDTO setError(String message, TokenValidateDTO tokenValidateDTO) {
    log.error(message);
    tokenValidateDTO.setUserDetails(null);
    tokenValidateDTO.setMessage(message);
    return tokenValidateDTO;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String ipFromRequest = NetworkUtil.getIpFromRequest(exchange.getRequest());
    UserDTO userDTO = userService.findByIp(ipFromRequest);
    if (userDTO == null) {
      return this.onError(exchange, "Account not found for your ip: ".concat(ipFromRequest));
    }

    UserDetails userDetails = null;
    TokenValidateDTO tokenValidateDTO = null;
    if (userDTO.getAuthType().equalsIgnoreCase(AuthenticationType.NONE.toString()))
      userDetails = customUserDetailsService.loadUserByUsername(userDTO.getUsername());
    else if (userDTO.getAuthType().equalsIgnoreCase(AuthenticationType.TOKEN.toString())) {
      tokenValidateDTO = jwtValidate(exchange.getRequest());
      userDetails = tokenValidateDTO.getUserDetails();
    } else if (userDTO.getAuthType().equalsIgnoreCase(AuthenticationType.BASIC.toString())) {
      tokenValidateDTO = basicValidate(exchange.getRequest());
      userDetails = tokenValidateDTO.getUserDetails();
    }

    if (userDetails == null)
      return this.onError(
          exchange, tokenValidateDTO != null ? tokenValidateDTO.getMessage() : "Unauthorized user");

    if (!userDetails.isEnabled())
      return this.onError(exchange, "Your account has been deactivated");

    if (!isAuthorizationValid(exchange.getRequest(), userDetails))
      return this.onError(exchange, "You are not permitted to execute the current request");

    /*ServerHttpRequest modifiedRequest = exchange.getRequest().mutate().
            header("secret", RandomStringUtils.random(10)).
            build();

    return chain.filter(exchange.mutate().request(modifiedRequest).build());*/

    ServerHttpRequest modifiedRequest = setConsumerInfo(exchange, userDTO);
    if (modifiedRequest != null) {
      return chain.filter(exchange.mutate().request(modifiedRequest).build());
    } else return chain.filter(exchange);
  }

  private ServerHttpRequest setConsumerInfo(ServerWebExchange exchange, UserDTO userDTO) {
    ServerHttpRequest result = null;
    if (StringUtils.isEmpty(exchange.getRequest().getHeaders().getFirst("consumerinfo"))
        && StringUtils.isNotEmpty(userDTO.getApplicationId())
        && StringUtils.isNotEmpty(userDTO.getModuleId())) {
      result =
          exchange
              .getRequest()
              .mutate()
              .header(
                  "consumerinfo",
                  "{\"applicationId\":\"appId\",\"moduleId\":\"mlId\"}"
                      .replace("appId", userDTO.getApplicationId())
                      .replace("mlId", userDTO.getModuleId()))
              .build();
    }

    return result;
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 200;
  }
}
