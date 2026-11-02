package com.demo.university.config;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.regex.Pattern;

/**
 * 跨域支持：只放行本机来源（localhost / 127.0.0.1 任意端口），
 * 方便前端单独用 Live Server 调试；同源部署时该过滤器不产生任何影响。
 */
@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Pattern LOCAL_ORIGIN =
            Pattern.compile("^https?://(localhost|127\\.0\\.0\\.1)(:\\d+)?$", Pattern.CASE_INSENSITIVE);

    @Override
    public void filter(ContainerRequestContext request) {
        // 预检请求直接放行，不进入业务接口
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) && request.getHeaderString("Origin") != null) {
            request.abortWith(Response.ok().build());
        }
    }

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        String origin = request.getHeaderString("Origin");
        if (origin == null || !LOCAL_ORIGIN.matcher(origin).matches()) {
            return;
        }
        response.getHeaders().add("Access-Control-Allow-Origin", origin);
        response.getHeaders().add("Access-Control-Allow-Credentials", "true");
        response.getHeaders().add("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        response.getHeaders().add("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With");
        response.getHeaders().add("Access-Control-Max-Age", "3600");
        response.getHeaders().add("Vary", "Origin");
    }
}
