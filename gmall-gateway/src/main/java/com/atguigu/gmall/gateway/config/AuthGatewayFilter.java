package com.atguigu.gmall.gateway.config;

import com.atguigu.core.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@EnableConfigurationProperties(JwtProperties.class)
@Component
public class AuthGatewayFilter implements GatewayFilter {

    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        //1.获取JWT类型的token信息
        //先求出Cookie
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        if(CollectionUtils.isEmpty(cookies)){
           //拦截
           response.setStatusCode(HttpStatus.UNAUTHORIZED);
           return response.setComplete();
        }

        //2.判断jwt类型的token是否为null
        HttpCookie cookie = cookies.getFirst(jwtProperties.getCookieName());
        if(cookie == null){
            //拦截
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        //3.解析JWT，如果正常解析放行
        try {
            JwtUtils.getInfoFromToken(cookie.getValue(),this.jwtProperties.getPublicKey());
        } catch (Exception e) {
            e.printStackTrace();
            //拦截
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        return chain.filter(exchange);
    }
}
