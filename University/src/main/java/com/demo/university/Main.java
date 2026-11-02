package com.demo.university;

import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 嵌入式 Tomcat 启动入口，支持 IDE 一键运行（右键 Run as Java Application）。
 * <p>
 * 启动后访问 http://localhost:8080/api/ping 进行健康检查。
 * 默认使用 H2 内存数据库，无需安装 MySQL。
 */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    private static final int PORT = 8080;
    private static final String WEBAPP_DIR = "src/main/webapp";
    private static final String CLASSES_DIR = "target/classes";
    private static final String DATA_DIR = "./data";

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        int port = parsePort(args);

        // 确保数据目录存在
        Path dataPath = Paths.get(DATA_DIR);
        if (Files.notExists(dataPath)) {
            Files.createDirectories(dataPath);
        }

        // 默认使用 H2 内存数据库，方便本地开发
        if (System.getProperty("db.url") == null) {
            System.setProperty("db.url",
                    "jdbc:h2:mem:university;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
            System.setProperty("db.driver", "org.h2.Driver");
            System.setProperty("db.username", "sa");
            System.setProperty("db.password", "");
            System.setProperty("db.autoInit", "true");
        }

        LOG.info("数据库配置：");
        LOG.info("  db.url     = " + System.getProperty("db.url"));
        LOG.info("  db.driver  = " + System.getProperty("db.driver"));
        LOG.info("  db.autoInit = " + System.getProperty("db.autoInit"));

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(Paths.get("target/tomcat").toAbsolutePath().toString());
        tomcat.setPort(port);
        tomcat.getConnector();

        File webappDir = new File(WEBAPP_DIR);
        if (!webappDir.exists()) {
            throw new IllegalStateException("未找到 webapp 目录：" + webappDir.getAbsolutePath()
                    + "，请从项目根目录启动或在 IDE 中设置 Working directory 为项目根目录");
        }

        StandardContext context = (StandardContext) tomcat.addWebapp("", webappDir.getAbsolutePath());
        File classesDir = new File(CLASSES_DIR);
        if (classesDir.exists()) {
            WebResourceRoot resources = new StandardRoot(context);
            resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                    classesDir.getAbsolutePath(), "/"));
            context.setResources(resources);
        }

        tomcat.start();

        LOG.info("============================================");
        LOG.info("  校园二手教材交易系统 启动成功！");
        LOG.info("  地址:    http://localhost:" + port);
        LOG.info("  健康检查: http://localhost:" + port + "/api/ping");
        LOG.info("  超级管理员账号已就绪（仅此账号可添加管理员）");
        LOG.info("  按 Ctrl+C 停止服务");
        LOG.info("============================================");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                tomcat.stop();
                tomcat.destroy();
                LOG.info("服务已安全停止");
            } catch (Exception e) {
                LOG.log(Level.WARNING, "停止 Tomcat 时出错", e);
            }
        }));

        tomcat.getServer().await();
    }

    private static int parsePort(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("--port".equals(args[i]) && i + 1 < args.length) {
                try {
                    return Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                    LOG.warning("无效端口号：" + args[i + 1] + "，使用默认端口 " + PORT);
                }
            }
        }
        return PORT;
    }
}