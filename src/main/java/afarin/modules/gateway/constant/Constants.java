package afarin.modules.gateway.constant;

public interface Constants {

  interface security {
    interface jwtToken {
      long ACCESS_TOKEN_VALIDITY = 30 * 60;
      String SIGNING_KEY = "$g7GpOt63ztX0tG7cP%T13lC0ydpPpuCKNS^tX3or!eGmv#fYw";
      String TOKEN_PREFIX_BEARER = "Bearer";
      String TOKEN_PREFIX_BASIC = "Basic";
      String HEADER_STRING = "Authorization";
    }

    interface header {
      String XFF = "x-forwarded-for";
    }
  }
}
