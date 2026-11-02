package com.demo.university.common;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 把 ApiException 转成统一 JSON 响应，HTTP 状态与业务 code 一致。
 */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    private static final Logger LOG = Logger.getLogger(ApiExceptionMapper.class.getName());

    @Override
    public Response toResponse(ApiException ex) {
        int status = (ex.getCode() >= 400 && ex.getCode() < 600) ? ex.getCode() : ErrorCode.INTERNAL_ERROR;
        if (status >= 500) {
            LOG.log(Level.SEVERE, "业务异常: " + ex.getMessage(), ex);
        }
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(Result.fail(ex.getCode(), ex.getMessage()))
                .build();
    }
}
