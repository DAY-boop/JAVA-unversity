package com.demo.university.common;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 兜底异常处理：任何未预期异常都记录堆栈并返回 500，绝不把堆栈暴露给前端。
 */
@Provider
public class DefaultExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(DefaultExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        if (ex instanceof ApiException apiEx) {
            return new ApiExceptionMapper().toResponse(apiEx);
        }
        if (ex instanceof WebApplicationException webEx) {
            return new WebApplicationExceptionMapper().toResponse(webEx);
        }
        LOG.log(Level.SEVERE, "未处理异常: " + ex.getClass().getName(), ex);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(Result.fail(ErrorCode.INTERNAL_ERROR, "服务器内部错误"))
                .build();
    }
}
