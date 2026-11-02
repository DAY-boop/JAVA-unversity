package com.demo.university.common;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * 处理 JAX-RS 自带异常（路径不存在 404、方法不允许 405、请求体格式错误 400 等），
 * 保证任何错误都返回 { code, message } 而不是 Tomcat 的 HTML 错误页。
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException ex) {
        int status = ex.getResponse() != null ? ex.getResponse().getStatus() : ErrorCode.INTERNAL_ERROR;
        String message;
        if (status == 404) {
            message = "接口或资源不存在";
        } else if (status == 405) {
            message = "请求方法不支持";
        } else if (status == 415) {
            message = "请求内容类型不支持";
        } else if (status >= 500) {
            message = "服务器内部错误";
        } else {
            message = "请求失败";
        }
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(Result.fail(status, message))
                .build();
    }
}
