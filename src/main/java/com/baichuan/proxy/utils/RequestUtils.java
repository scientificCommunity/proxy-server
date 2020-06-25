package com.baichuan.proxy.utils;

import com.baichuan.proxy.domain.RequestBO;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author kun
 * @date 2020-06-10 10:04
 */
public class RequestUtils {

    private static final String HOST_PORT_REG = "^(?:https?://)?(?<host>[^:]*)(?::(?<port>\\d+))?(/.*)?$";

    public static RequestBO getRequestBO(HttpRequest request) {
        RequestBO requestBO = new RequestBO();

        String host = request.headers().get(HttpHeaderNames.HOST);
        String s;
        if (StringUtils.isBlank(host)) {
            List<String> info = RegUtils.matchOne(HOST_PORT_REG, request.uri(), "host", "port");
            host = info.get(0);
            s = info.get(1);
        }else {
            List<String> info = RegUtils.matchOne(HOST_PORT_REG, host, "host", "port");
            host = info.get(0);
            s = info.get(1);
        }
        requestBO.setHost(host);

        if (StringUtils.isBlank(s)) {
            if (requestBO.getHost().contains("https")) {
                s = "443";
            }else {
                s = "80";
            }
        }
        requestBO.setPort(Integer.parseInt(s));
        requestBO.setSupportSsl(false);
        return requestBO;
    }
}
