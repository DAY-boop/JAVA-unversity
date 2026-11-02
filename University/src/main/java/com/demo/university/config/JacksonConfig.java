package com.demo.university.config;

import com.demo.university.common.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * 让 JAX-RS 使用 JsonUtil 的 ObjectMapper，
 * 解决 LocalDateTime 被序列化成 [2026,10,22,10,15,30] 数组的经典问题。
 */
@Provider
public class JacksonConfig implements ContextResolver<ObjectMapper> {

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return JsonUtil.mapper();
    }
}
