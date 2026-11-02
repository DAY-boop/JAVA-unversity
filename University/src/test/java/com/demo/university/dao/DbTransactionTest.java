package com.demo.university.dao;

import com.demo.university.config.DataSourceProvider;
import com.demo.university.config.DatabaseInitializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证 ThreadLocal 事务：提交生效、异常回滚、嵌套调用加入同一事务。
 * 这套机制是 Step 4「下单扣库存」不超卖的基础。
 */
class DbTransactionTest {

    private static final TestDao DAO = new TestDao();
    private static long adminId;

    @BeforeAll
    static void setUp() {
        DataSourceProvider.init();
        DatabaseInitializer.initialize();
        adminId = DAO.adminId();
    }

    @Test
    void commitPersistsChange() {
        Db.inTransaction(() -> {
            DAO.rename(adminId, "已提交昵称");
            return null;
        });
        assertEquals("已提交昵称", DAO.nickname(adminId));
    }

    @Test
    void exceptionRollsBack() {
        String before = DAO.nickname(adminId);
        assertThrows(IllegalStateException.class, () -> Db.inTransaction(() -> {
            DAO.rename(adminId, "不应保留的昵称");
            throw new IllegalStateException("模拟业务失败");
        }));
        assertEquals(before, DAO.nickname(adminId), "抛出异常后必须回滚");
    }

    @Test
    void nestedCallJoinsSameTransaction() {
        String before = DAO.nickname(adminId);
        assertThrows(IllegalStateException.class, () -> Db.inTransaction(() -> {
            DAO.rename(adminId, "外层修改");
            // 内层再开事务：应直接加入外层，外层回滚后内层也失效
            Db.inTransaction(() -> {
                DAO.rename(adminId, "内层修改");
                return null;
            });
            throw new IllegalStateException("外层失败");
        }));
        assertEquals(before, DAO.nickname(adminId), "嵌套事务不应独立提交");
    }

    static class TestDao extends BaseDao {

        void rename(long userId, String nickname) {
            update("UPDATE users SET nickname = ? WHERE id = ?", nickname, userId);
        }

        String nickname(long userId) {
            return queryOne("SELECT nickname FROM users WHERE id = ?", rs -> rs.getString(1), userId);
        }

        long adminId() {
            return queryLong("SELECT id FROM users WHERE username = ?", DatabaseInitializer.ADMIN_USERNAME);
        }
    }
}
