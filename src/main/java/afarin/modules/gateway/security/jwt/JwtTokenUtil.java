package afarin.modules.gateway.security.jwt;

import afarin.modules.gateway.constant.Constants;
import afarin.modules.gateway.model.JsonWebToken;
import afarin.modules.gateway.util.NetworkUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtTokenUtil implements Serializable {

  private static final long serialVersionUID = -2550185165626007488L;

  // retrieve username from jwt token
  public String getUsernameFromToken(String token) {

    return getClaimFromToken(token, Claims::getSubject);
  }

  // retrieve expiration date from jwt token
  public Date getExpirationDateFromToken(String token) {

    return getClaimFromToken(token, Claims::getExpiration);
  }

  public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = getAllClaimsFromToken(token);

    return claimsResolver.apply(claims);
  }

  // for retrieving any information from token we will need the secret key
  private Claims getAllClaimsFromToken(String token) {

    return Jwts.parser()
        .setSigningKey(Constants.security.jwtToken.SIGNING_KEY)
        .parseClaimsJws(token)
        .getBody();
  }

  // check if the token has expired
  private Boolean isTokenExpired(String token) {
    final Date expiration = getExpirationDateFromToken(token);

    return expiration.before(new Date());
  }

  // generate token for user
  public JsonWebToken generateToken(UserDetails userDetails, String ip) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("ip", ip);

    return doGenerateToken(claims, userDetails.getUsername());
  }

  // while creating the token -
  // 1. Define  claims of the token, like Issuer, Expiration, Subject, and the ID
  // 2. Sign the JWT using the HS512 algorithm and secret key.
  // 3. According to JWS Compact
  // Serialization(https://tools.ietf.org/html/draft-ietf-jose-json-web-signature-41#section-3.1)
  //   compaction of the JWT to a URI_SEND-safe string
  private JsonWebToken doGenerateToken(Map<String, Object> claims, String subject) {
    JsonWebToken jsonWebToken = new JsonWebToken();
    jsonWebToken.setExpiration(
        new Date(
            System.currentTimeMillis() + Constants.security.jwtToken.ACCESS_TOKEN_VALIDITY * 1000));
    jsonWebToken.setToken(
        Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(jsonWebToken.getExpiration())
            .signWith(SignatureAlgorithm.HS512, Constants.security.jwtToken.SIGNING_KEY)
            .compact());
    return jsonWebToken;
  }

  // validate token
  public Boolean validateToken(String token, UserDetails userDetails, ServerHttpRequest request) {
    final String username = getUsernameFromToken(token);
    Claims claims = getAllClaimsFromToken(token);
    String ip = (String) claims.get("ip");

    return (username.equals(userDetails.getUsername())
        && !isTokenExpired(token)
        && ip.contains(NetworkUtil.getIpFromRequest(request)));
  }

}
