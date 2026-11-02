package com.demo.university.config;

import com.demo.university.common.ErrorCode;
import com.demo.university.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

/**
 * 登录拦截：白名单之外的接口必须登录，admin/ 开头还必须具备管理员角色。
 * 采用「路径白名单」而不是注解，规则集中在这一个方法里，一眼看全。
 */
@Provider
@PreMatching
public class AuthFilter implements ContainerRequestFilter {

    @Context
    private HttpServletRequest servletRequest;

    @Override
    public void filter(ContainerRequestContext context) {
        String method = context.getMethod();
        // 预检请求交给 CorsFilter 处理
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return;
        }
        String path = normalize(context.getUriInfo().getPath());
        if (isPublic(method, path)) {
            return;
        }
        Long userId = AuthUtil.userId(servletRequest);
        if (userId == null) {
            context.abortWith(reject(ErrorCode.UNAUTHORIZED, "请先登录"));
            return;
        }
        if (path.startsWith("admin/") && !AuthUtil.isAdmin(servletRequest)) {
            context.abortWith(reject(ErrorCode.FORBIDDEN, "需要管理员权限"));
        }
    }

    /** pre-matching 阶段拿到的路径可能带 api/ 前缀，统一成 books/1 这种形式 */
    private String normalize(String path) {
        String value = path == null ? "" : path;
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.startsWith("api/")) {
            value = value.substring(4);
        }
        return value;
    }

    private boolean isPublic(String method, String path) {
        if (path.equals("ping") || path.startsWith("auth/") || path.startsWith("files/")) {
            return true;
        }
        if (!"GET".equalsIgnoreCase(method)) {
            return false;
        }
        if (path.equals("categories")) {
            return true;
        }
        if (path.equals("books")) {
            return true;
        }
        if (path.startsWith("books/") && !path.startsWith("books/mine")) {
            return true;
        }
        if (path.equals("wanted")) {
            return true;
        }
        if (path.startsWith("wanted/") && !path.startsWith("wanted/mine")) {
            return true;
        }
        return path.equals("reviews");
    }

    private Response reject(int code, String message) {
        return Response.status(code)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(Result.fail(code, message))
                .build();
    }
}
