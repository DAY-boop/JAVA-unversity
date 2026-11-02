package com.demo.university.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JDBC 模板基类：封装"取连接-绑定参数-执行-释放"的重复代码。
 * 子类只关心 SQL 和 ResultSet 到实体的映射。
 */
public abstract class BaseDao {

    protected <T> List<T> queryList(String sql, RowMapper<T> mapper, Object... params) {
        Connection connection = Db.acquire();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapper.map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询失败: " + sql, e);
        } finally {
            Db.release(connection);
        }
    }

    protected <T> T queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = queryList(sql, mapper, params);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 查询单个数值（COUNT / MAX / 求和等） */
    protected long queryLong(String sql, Object... params) {
        Connection connection = Db.acquire();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new DataAccessException("查询失败: " + sql, e);
        } finally {
            Db.release(connection);
        }
    }

    /** INSERT / UPDATE / DELETE，返回受影响行数 */
    protected int update(String sql, Object... params) {
        Connection connection = Db.acquire();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bindParams(ps, params);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("更新失败: " + sql, e);
        } finally {
            Db.release(connection);
        }
    }

    /** INSERT 并返回自增主键 */
    protected long insert(String sql, Object... params) {
        Connection connection = Db.acquire();
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParams(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
                throw new DataAccessException("插入成功但未取得自增主键: " + sql);
            }
        } catch (SQLException e) {
            throw new DataAccessException("插入失败: " + sql, e);
        } finally {
            Db.release(connection);
        }
    }

    private void bindParams(PreparedStatement ps, Object... params) throws SQLException {
        if (params == null) {
            return;
        }
        for (int i = 0; i < params.length; i++) {
            Object value = params[i];
            int index = i + 1;
            if (value == null) {
                ps.setNull(index, java.sql.Types.NULL);
            } else if (value instanceof LocalDateTime dateTime) {
                ps.setTimestamp(index, Timestamp.valueOf(dateTime));
            } else if (value instanceof LocalDate date) {
                ps.setDate(index, Date.valueOf(date));
            } else if (value instanceof Enum<?> enumValue) {
                ps.setString(index, enumValue.name());
            } else {
                ps.setObject(index, value);
            }
        }
    }
}
