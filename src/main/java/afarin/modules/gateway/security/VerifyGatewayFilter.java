package afarin.modules.gateway.security;

import org.bouncycastle.util.Strings;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;


@Component
public class VerifyGatewayFilter implements GlobalFilter, Ordered {
  public static final String CACHE_REQUEST_BODY_OBJECT_KEY = "cachedRequestBodyObject";
  private static final Logger LOGGER = LoggerFactory.getLogger(VerifyGatewayFilter.class);
  private static final String START_TIME = "startTime";
  private static final String HTTP_SCHEME = "http";
  private static final String HTTPS_SCHEME = "https";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    URI requestURI = request.getURI();
    String scheme = requestURI.getScheme();

    if ((!HTTP_SCHEME.equalsIgnoreCase(scheme) && !HTTPS_SCHEME.equals(scheme))) {
      return chain.filter(exchange);
    }
    long startTime = System.currentTimeMillis();
    exchange.getAttributes().put(START_TIME, startTime);
    Flux<DataBuffer> cachedBody = exchange.getAttribute(CACHE_REQUEST_BODY_OBJECT_KEY);
    logRequest(request, toRaw(cachedBody));

    return chain.filter(exchange.mutate().response(logResponse(exchange)).build());
  }

  private void logRequest(ServerHttpRequest request, String body) {
    URI requestURI = request.getURI();
    String scheme = requestURI.getScheme();
    HttpHeaders headers = request.getHeaders();
    LOGGER.info(
        "Request -> schema:{}, path:{}, method:{}, ip:{}, host:{}, contentType:{}, content length:{}",
        scheme,
        requestURI.getPath(),
        request.getMethod(),
        request.getRemoteAddress(),
        requestURI.getHost(),
        headers.getContentType(),
        headers.getContentLength());

    headers.forEach(
        (key, value) -> LOGGER.debug("Request -> headers: key->{}, value->{}", key, value));
    MultiValueMap<String, String> queryParams = request.getQueryParams();
    if (!queryParams.isEmpty()) {
      queryParams.forEach(
          (key, value) -> LOGGER.info("Request -> query param: key->{}, value->{}", key, value));
    }
    if (body != null) {
      LOGGER.info("Request -> body:{}", body);
    }
  }

  private ServerHttpResponseDecorator logResponse(ServerWebExchange exchange) {
    ServerHttpResponse origResponse = exchange.getResponse();
    Long startTime = exchange.getAttribute(START_TIME);
    HttpHeaders headers = origResponse.getHeaders();
    headers.forEach(
        (key, value) -> LOGGER.debug("Response -> headers: key->{}, value->{}", key, value));
    Long executeTime = (System.currentTimeMillis() - startTime);
    LOGGER.info(
        "Response -> contentType:{}, content length:{}, original path:{}, cost:{} ms",
        headers.getContentType(),
        headers.getContentLength(),
        exchange.getRequest().getURI().getPath(),
        executeTime);
    DataBufferFactory bufferFactory = origResponse.bufferFactory();

    return new ServerHttpResponseDecorator(origResponse) {
      @Override
      public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        if (body instanceof Flux) {
          Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;

          return super.writeWith(
              fluxBody.map(
                  dataBuffer -> {
                    try {
                      byte[] content = new byte[dataBuffer.readableByteCount()];
                      dataBuffer.read(content);
                      String bodyContent = new String(content, StandardCharsets.UTF_8);
                      LOGGER.info("Response -> body:{}", bodyContent);
                      return bufferFactory.wrap(content);
                    } finally {
                      DataBufferUtils.release(dataBuffer);
                    }
                  }));
        }
        return super.writeWith(body);
      }
    };
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 100;
  }

  private static String toRaw(Flux<DataBuffer> body) {
    if (body != null) {
      AtomicReference<String> rawRef = new AtomicReference<>();
      body.subscribe(
          buffer -> {
            byte[] bytes = new byte[buffer.readableByteCount()];
            buffer.read(bytes);
            DataBufferUtils.release(buffer);
            rawRef.set(Strings.fromUTF8ByteArray(bytes));
          });
      return rawRef.get();
    }

    return null;
  }
}
