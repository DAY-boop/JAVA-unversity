package com.demo.university.config;

import com.demo.university.scheduler.OrderTimeoutJob;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import java.util.logging.Logger;

/**
 * 应用生命周期入口：启动时初始化连接池并建表，关闭时释放资源。
 * 在 web.xml 中显式注册；即使数据库不可用也不让应用启动失败，
 * 接口层会返回明确错误，便于前端先行开发。
 */
public class AppListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppListener.class.getName());

    private OrderTimeoutJob orderTimeoutJob;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.info("=== 校园二手教材交易系统 启动 ===");
        DataSourceProvider.init();
        if (DataSourceProvider.isAvailable()) {
            DatabaseInitializer.initialize();
            orderTimeoutJob = new OrderTimeoutJob();
            orderTimeoutJob.start();
            LOG.info("数据库就绪，健康检查地址: " + sce.getServletContext().getContextPath() + "/api/ping");
        } else {
            LOG.warning("数据库不可用（" + DataSourceProvider.getLastError()
                    + "），系统以降级模式启动，请检查 src/main/resources/db.properties");
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (orderTimeoutJob != null) {
            orderTimeoutJob.stop();
        }
        DataSourceProvider.close();
        LOG.info("=== 校园二手教材交易系统 已停止，连接池已关闭 ===");
    }
}
