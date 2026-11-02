package com.demo.university.config;

import com.demo.university.common.PasswordUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 启动时自动建表 + 灌入种子数据（幂等，重复执行不会产生重复数据）。
 * 生产环境可把 db.autoInit 设为 false，改用 DBA 手工执行 schema.sql。
 */
public final class DatabaseInitializer {

    private static final Logger LOG = Logger.getLogger(DatabaseInitializer.class.getName());

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "123456";

    /** 一级类目 -> 二级类目 */
    private static final Map<String, String[]> CATEGORY_TREE = new LinkedHashMap<>();

    static {
        CATEGORY_TREE.put("公共基础课", new String[]{"高等数学", "大学英语", "大学物理", "计算机基础"});
        CATEGORY_TREE.put("专业必修课", new String[]{"计算机类", "经济管理类", "机械电子类", "文史哲类"});
        CATEGORY_TREE.put("考研考证", new String[]{"考研数学", "考研英语", "四六级", "教师资格"});
        CATEGORY_TREE.put("语言考试", new String[]{"雅思托福", "日语韩语"});
        CATEGORY_TREE.put("其他", new String[]{"其他教材"});
    }

    private DatabaseInitializer() {
    }

    public static void initialize() {
        if (!"true".equalsIgnoreCase(DataSourceProvider.getProperty("db.autoInit", "true"))) {
            LOG.info("db.autoInit=false，跳过自动建表与种子数据");
            return;
        }
        try (Connection conn = DataSourceProvider.getDataSource().getConnection()) {
            int executed = runScript(conn, "/db/schema.sql");
            LOG.info("schema.sql 执行成功，共 " + executed + " 条语句");
            seedCategories(conn);
            seedAdmin(conn);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "数据库初始化失败，请检查 db.properties 与 MySQL 服务状态", e);
        }
    }

    private static int runScript(Connection conn, String resource) throws IOException, SQLException {
        InputStream in = DatabaseInitializer.class.getResourceAsStream(resource);
        if (in == null) {
            LOG.warning("未找到 SQL 脚本: " + resource);
            return 0;
        }
        String content;
        try (in) {
            content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<String> statements = splitStatements(content);
        try (Statement st = conn.createStatement()) {
            for (String sql : statements) {
                st.execute(sql);
            }
        }
        return statements.size();
    }

    /** 去掉 -- 注释后按分号切分；本项目脚本不含存储过程，无需处理 DELIMITER */
    private static List<String> splitStatements(String content) {
        StringBuilder cleaned = new StringBuilder();
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                continue;
            }
            cleaned.append(line).append('\n');
        }
        List<String> statements = new ArrayList<>();
        for (String part : cleaned.toString().split(";")) {
            String sql = part.trim();
            if (!sql.isEmpty()) {
                statements.add(sql);
            }
        }
        return statements;
    }

    private static void seedCategories(Connection conn) throws SQLException {
        int sort = 0;
        for (Map.Entry<String, String[]> entry : CATEGORY_TREE.entrySet()) {
            long parentId = ensureCategory(conn, entry.getKey(), 0L, sort);
            int childSort = 0;
            for (String child : entry.getValue()) {
                ensureCategory(conn, child, parentId, childSort++);
            }
            sort += 10;
        }
    }

    private static long ensureCategory(Connection conn, String name, long parentId, int sort) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM categories WHERE parent_id = ? AND name = ?")) {
            ps.setLong(1, parentId);
            ps.setString(2, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO categories(name, parent_id, sort) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setLong(2, parentId);
            ps.setInt(3, sort);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : 0L;
            }
        }
    }

    private static void seedAdmin(Connection conn) throws SQLException {
        // 先查是否存在；若存在且 role < 2 则自动升级为超级管理员
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, role FROM users WHERE username = ?")) {
            ps.setString(1, ADMIN_USERNAME);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int existingRole = rs.getInt("role");
                    if (existingRole < 2) {
                        try (PreparedStatement up = conn.prepareStatement(
                                "UPDATE users SET role = 2 WHERE id = ?")) {
                            up.setLong(1, rs.getLong("id"));
                            up.executeUpdate();
                        }
                        LOG.info("已将管理员 " + ADMIN_USERNAME + " 升级为超级管理员（role: " + existingRole + " → 2）");
                    }
                    return;
                }
            }
        }
        // 首次创建：直接设为超级管理员（role=2）
        String salt = PasswordUtil.randomSalt();
        String hash = PasswordUtil.hash(ADMIN_PASSWORD, salt);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users(username, password, salt, nickname, role, credit_score, status, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, ADMIN_USERNAME);
            ps.setString(2, hash);
            ps.setString(3, salt);
            ps.setString(4, "系统管理员");
            ps.setInt(5, 2);
            ps.setInt(6, 100);
            ps.setInt(7, 0);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
        LOG.info("已创建超级管理员账号，请妥善保管密码");
    }
}
