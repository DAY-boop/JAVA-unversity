package com.demo.university.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 数据源提供者（HikariCP 单例）。
 * 单例用静态持有而不是 CDI，原因是 ServletContextListener 启动阶段就需要它，
 * 此时容器尚未完成任何注入；静态方式在这里更简单、更可控。
 */
public final class DataSourceProvider {

    private static final Logger LOG = Logger.getLogger(DataSourceProvider.class.getName());
    private static final Properties PROPS = loadProperties();

    private static volatile HikariDataSource dataSource;
    private static volatile boolean initAttempted;
    private static volatile String lastError;

    private DataSourceProvider() {
    }

    public static synchronized void init() {
        if (initAttempted) {
            return;
        }
        initAttempted = true;
        String url = getProperty("db.url", null);
        if (url == null || url.isBlank()) {
            lastError = "db.properties 中缺少 db.url";
            LOG.severe("数据库初始化跳过：" + lastError);
            return;
        }
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(getProperty("db.username", "root"));
            config.setPassword(getProperty("db.password", ""));
            String driver = getProperty("db.driver", null);
            if (driver != null && !driver.isBlank()) {
                config.setDriverClassName(driver);
            }
            config.setMaximumPoolSize(getIntProperty("db.pool.maximumPoolSize", 10));
            config.setMinimumIdle(getIntProperty("db.pool.minimumIdle", 2));
            config.setConnectionTimeout(getIntProperty("db.pool.connectionTimeout", 5000));
            config.setIdleTimeout(getIntProperty("db.pool.idleTimeout", 600000));
            config.setPoolName("UniversityPool");
            // 启动阶段快速失败，避免 Tomcat 启动卡死
            config.setInitializationFailTimeout(-1);

            HikariDataSource created = new HikariDataSource(config);
            try (Connection conn = created.getConnection()) {
                if (!conn.isValid(3)) {
                    throw new IllegalStateException("连接校验失败");
                }
            }
            dataSource = created;
            lastError = null;
            LOG.info("数据库连接池初始化成功：" + maskUrl(url));
        } catch (Exception e) {
            lastError = e.getMessage();
            close();
            LOG.log(Level.SEVERE, "数据库连接池初始化失败，系统将以降级模式运行：" + lastError, e);
        }
    }

    /** 取数据源；未初始化或初始化失败时抛异常，由全局异常处理器转成 500 */
    public static HikariDataSource getDataSource() {
        HikariDataSource ds = dataSource;
        if (ds == null) {
            init();
            ds = dataSource;
        }
        if (ds == null) {
            throw new IllegalStateException("数据库不可用：" + (lastError == null ? "未初始化" : lastError));
        }
        return ds;
    }

    /** 健康检查用：真实探测一次连接，失败不抛异常 */
    public static boolean isAvailable() {
        HikariDataSource ds = dataSource;
        if (ds == null) {
            init();
            ds = dataSource;
        }
        if (ds == null || ds.isClosed()) {
            return false;
        }
        try (Connection conn = ds.getConnection()) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    public static String getLastError() {
        return lastError;
    }

    public static synchronized void close() {
        HikariDataSource ds = dataSource;
        dataSource = null;
        if (ds != null && !ds.isClosed()) {
            ds.close();
        }
    }

    public static String getProperty(String key, String defaultValue) {
        String fromSystem = System.getProperty(key);
        if (fromSystem != null && !fromSystem.isBlank()) {
            return fromSystem;
        }
        String value = PROPS.getProperty(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    private static int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = DataSourceProvider.class.getResourceAsStream("/db.properties")) {
            if (in == null) {
                LOG.warning("未找到 classpath:/db.properties，将使用系统属性或默认值");
                return props;
            }
            props.load(in);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "读取 db.properties 失败", e);
        }
        return props;
    }

    private static String maskUrl(String url) {
        int idx = url.indexOf('?');
        return idx > 0 ? url.substring(0, idx) : url;
    }
}
