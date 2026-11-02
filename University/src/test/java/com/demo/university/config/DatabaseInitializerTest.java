package com.demo.university.config;

import com.demo.university.dao.HealthDao;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证：建表脚本可在 H2(MySQL 模式) 上完整执行，种子数据幂等。
 */
class DatabaseInitializerTest {

    private static final HealthDao HEALTH_DAO = new HealthDao();

    @BeforeAll
    static void initDatabase() {
        DataSourceProvider.init();
        DatabaseInitializer.initialize();
    }

    @Test
    void databaseShouldBeReachable() {
        assertTrue(DataSourceProvider.isAvailable(), "H2 内存库应可用");
        assertTrue(HEALTH_DAO.ping());
    }

    @Test
    void schemaCreatesAllTables() {
        for (String table : List.of("users", "categories", "books", "orders",
                "order_items", "reviews", "favorites", "messages", "wanted_books")) {
            assertTrue(HEALTH_DAO.count(table) >= 0, "表 " + table + " 应存在（-1 表示查询失败）");
        }
    }

    @Test
    void seedDataIsIdempotent() {
        long usersAfterFirst = HEALTH_DAO.count("users");
        long categoriesAfterFirst = HEALTH_DAO.count("categories");
        assertTrue(usersAfterFirst >= 1, "应存在默认管理员");
        assertEquals(20, categoriesAfterFirst, "应为 5 个一级 + 15 个二级类目");

        DatabaseInitializer.initialize();

        assertEquals(usersAfterFirst, HEALTH_DAO.count("users"), "重复初始化不应重复插入用户");
        assertEquals(categoriesAfterFirst, HEALTH_DAO.count("categories"), "重复初始化不应重复插入类目");
    }
}
