package com.demo.university.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * ResultSet 单行映射器：每个 DAO 提供自己的映射，避免反射映射的黑盒行为。
 */
@FunctionalInterface
public interface RowMapper<T> {

    T map(ResultSet rs) throws SQLException;
}
