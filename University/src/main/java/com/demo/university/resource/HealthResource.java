package com.demo.university.resource;

import com.demo.university.common.Result;
import com.demo.university.config.AppContext;
import com.demo.university.config.DataSourceProvider;
import com.demo.university.dao.HealthDao;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 系统自检接口：GET /api/ping
 * 用于验证「WAR 部署成功 + 数据库连通 + 建表完成」三件事。
 */
@Path("/ping")
public class HealthResource {

    private final HealthDao healthDao = AppContext.healthDao();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Result<Map<String, Object>> ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", "校园二手教材交易系统");
        data.put("status", "running");
        data.put("time", LocalDateTime.now());
        data.put("db", databaseInfo());
        return Result.ok(data);
    }

    private Map<String, Object> databaseInfo() {
        Map<String, Object> db = new LinkedHashMap<>();
        boolean connected = healthDao.ping();
        db.put("connected", connected);
        if (connected) {
            db.put("users", healthDao.count("users"));
            db.put("categories", healthDao.count("categories"));
            db.put("books", healthDao.count("books"));
            db.put("orders", healthDao.count("orders"));
        } else {
            db.put("error", DataSourceProvider.getLastError());
        }
        return db;
    }
}
