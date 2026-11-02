package com.demo.university.common;

/**
 * 业务错误码。与 HTTP 状态码保持一致，前端只需看 code 即可。
 */
public final class ErrorCode {

    /** 成功 */
    public static final int OK = 200;
    /** 参数校验失败 */
    public static final int PARAM_ERROR = 400;
    /** 未登录 / 登录态失效 */
    public static final int UNAUTHORIZED = 401;
    /** 已登录但无权限 */
    public static final int FORBIDDEN = 403;
    /** 资源不存在 */
    public static final int NOT_FOUND = 404;
    /** 业务冲突：库存不足、重复评价、状态非法等 */
    public static final int CONFLICT = 409;
    /** 服务器内部错误 */
    public static final int INTERNAL_ERROR = 500;

    private ErrorCode() {
    }
}
