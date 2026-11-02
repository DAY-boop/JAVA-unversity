package com.demo.university.dao;

import java.sql.Connection;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 系统自检用 DAO：只承担连接探活与表行数统计。
 */
public class HealthDao extends BaseDao {

    private static final Pattern TABLE_NAME = Pattern.compile("^[a-z_]{1,32}$");
    private static final Set<String> KNOWN_TABLES = Set.of(
            "users", "categories", "books", "orders", "order_items", "reviews", "favorites",
            "messages", "wanted_books");

    /** 真实探活：能拿到可用连接才算数据库正常 */
    public boolean ping() {
        Connection connection = null;
        try {
            connection = Db.acquire();
            return connection.isValid(2);
        } catch (Exception e) {
            return false;
        } finally {
            Db.release(connection);
        }
    }

    /** 统计表行数；表名做白名单校验，防止被拼接到 SQL 里 */
    public long count(String table) {
        if (table == null || !TABLE_NAME.matcher(table).matches() || !KNOWN_TABLES.contains(table)) {
            throw new IllegalArgumentException("非法的表名: " + table);
        }
        try {
            return queryLong("SELECT COUNT(*) FROM " + table);
        } catch (Exception e) {
            return -1L;
        }
    }
}
