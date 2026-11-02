package com.demo.university;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.HashMap;
import java.util.Map;

/**
 * JAX-RS 应用入口，所有接口前缀 /api。
 * 显式声明扫描包，避免 Jersey 扫描整个 classpath（启动更慢且容易误注册）。
 */
@ApplicationPath("/api")
public class HelloApplication extends Application {

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("jersey.config.server.provider.packages",
                "com.demo.university.resource,com.demo.university.config,com.demo.university.common");
        return properties;
    }
}
