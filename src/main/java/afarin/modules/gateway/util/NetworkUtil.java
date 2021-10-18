package afarin.modules.gateway.util;

import afarin.modules.gateway.constant.Constants;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;

import java.net.InetAddress;

public class NetworkUtil {

    public static String getIpFromRequest(ServerHttpRequest request) {
        try {
            String ip = null;
            if (request != null) {
                String xff = request.getHeaders().getFirst(Constants.security.header.XFF);
                ip =  xff != null ? xff.split(",")[0].trim() : null;
                if (StringUtils.isEmpty(ip)) {
                    ip = request.getRemoteAddress().getAddress().getHostAddress();
                }
            }

            if (ip.equalsIgnoreCase("0:0:0:0:0:0:0:1")) {
                InetAddress inetAddress = InetAddress.getLocalHost();
                String ipAddress = inetAddress.getHostAddress();
                ip = ipAddress;
            }
            return ip;
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
