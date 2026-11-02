package com.demo.university.config;

import com.demo.university.common.ApiException;
import com.demo.university.model.Role;
import com.demo.university.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 登录态工具：用户信息存 HttpSession，Resource 通过它取当前用户。
 * 未登录访问受保护接口时抛 401，由全局异常处理器转 JSON。
 */
public final class AuthUtil {

    private static final String SESSION_USER_ID = "loginUserId";
    private static final String SESSION_USER_ROLE = "loginUserRole";
    private static final String SESSION_USER_NAME = "loginUserNickname";

    private AuthUtil() {
    }

    public static void login(HttpServletRequest request, User user) {
        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_USER_ID, user.id);
        session.setAttribute(SESSION_USER_ROLE, user.role);
        session.setAttribute(SESSION_USER_NAME, user.nickname);
    }

    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public static Long userId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_USER_ID);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public static long requireUserId(HttpServletRequest request) {
        Long userId = userId(request);
        if (userId == null) {
            throw ApiException.unauthorized("请先登录");
        }
        return userId;
    }

    /** 判断是否为管理员（含普通管理员与超级管理员） */
    public static boolean isAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object role = session.getAttribute(SESSION_USER_ROLE);
        return role instanceof Number number && number.intValue() >= Role.ADMIN.code;
    }

    public static void requireAdmin(HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw ApiException.forbidden("需要管理员权限");
        }
    }

    /** 判断是否为超级管理员（原始 admin 账号） */
    public static boolean isSuperAdmin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        Object role = session.getAttribute(SESSION_USER_ROLE);
        return role instanceof Number number && number.intValue() == Role.SUPER_ADMIN.code;
    }

    /** 要求超级管理员权限，否则返回 403 */
    public static void requireSuperAdmin(HttpServletRequest request) {
        if (!isSuperAdmin(request)) {
            throw ApiException.forbidden("仅超级管理员可执行此操作");
        }
    }
}
