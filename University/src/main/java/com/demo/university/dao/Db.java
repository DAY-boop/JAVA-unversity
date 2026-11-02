package com.demo.university.dao;

import com.demo.university.config.DataSourceProvider;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 连接与事务的统一入口。
 * <p>
 * 事务实现说明：Tomcat 下 CDI 的 @Transactional 不可用（需要 JTA 容器），
 * 因此这里用 ThreadLocal 绑定同一个 Connection，实现"模板式"事务：
 * 在 inTransaction 里调用的所有 DAO 方法自动加入同一事务，无需层层传参。
 * <p>
 * 用法：<pre>
 * Long orderId = Db.inTransaction(() -> {
 *     int rows = bookDao.deductStock(bookId, 1);
 *     if (rows == 0) throw ApiException.conflict("库存不足");
 *     return orderDao.insert(order);
 * });
 * </pre>
 */
public final class Db {

    private static final ThreadLocal<Connection> CURRENT_TX = new ThreadLocal<>();

    private Db() {
    }

    /** 取连接：事务内复用同一个连接，事务外新建（调用方负责 release） */
    public static Connection acquire() {
        Connection tx = CURRENT_TX.get();
        if (tx != null) {
            return tx;
        }
        try {
            return DataSourceProvider.getDataSource().getConnection();
        } catch (SQLException e) {
            throw new DataAccessException("获取数据库连接失败", e);
        }
    }

    /** 释放连接：事务连接由 inTransaction 统一关闭，这里直接忽略 */
    public static void release(Connection connection) {
        if (connection == null || connection == CURRENT_TX.get()) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
            // 关闭失败无需影响主流程
        }
    }

    /**
     * 在事务中执行；若已处于事务中则直接加入（支持嵌套调用）。
     */
    public static <T> T inTransaction(TxWork<T> work) {
        if (CURRENT_TX.get() != null) {
            return work.execute();
        }
        Connection connection = null;
        try {
            connection = DataSourceProvider.getDataSource().getConnection();
            connection.setAutoCommit(false);
            CURRENT_TX.set(connection);
            T result = work.execute();
            connection.commit();
            return result;
        } catch (RuntimeException e) {
            rollback(connection);
            throw e;
        } catch (SQLException e) {
            rollback(connection);
            throw new DataAccessException("事务执行失败", e);
        } finally {
            CURRENT_TX.remove();
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                    // ignore
                }
                try {
                    connection.close();
                } catch (SQLException ignored) {
                    // ignore
                }
            }
        }
    }

    /** 当前线程是否处于事务中（单元测试与断言用） */
    public static boolean inTransaction() {
        return CURRENT_TX.get() != null;
    }

    private static void rollback(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // ignore
        }
    }

    @FunctionalInterface
    public interface TxWork<T> {
        T execute();
    }
}
