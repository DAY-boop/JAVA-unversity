package com.demo.university.dao;

import com.demo.university.common.PageQuery;
import com.demo.university.model.User;

import java.util.ArrayList;
import java.util.List;

/** 用户表数据访问 */
public class UserDao extends BaseDao {

    private static final RowMapper<User> MAPPER = rs -> {
        User user = new User();
        user.id = rs.getLong("id");
        user.username = rs.getString("username");
        user.password = rs.getString("password");
        user.salt = rs.getString("salt");
        user.nickname = rs.getString("nickname");
        user.phone = rs.getString("phone");
        user.campus = rs.getString("campus");
        user.avatar = rs.getString("avatar");
        user.role = rs.getInt("role");
        user.creditScore = rs.getInt("credit_score");
        user.status = rs.getInt("status");
        user.createdAt = rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime();
        return user;
    };

    public User findByUsername(String username) {
        return queryOne("SELECT * FROM users WHERE username = ?", MAPPER, username);
    }

    public User findById(long id) {
        return queryOne("SELECT * FROM users WHERE id = ?", MAPPER, id);
    }

    public long insert(User user) {
        return insert("INSERT INTO users(username, password, salt, nickname, phone, campus, avatar, role, "
                        + "credit_score, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                user.username, user.password, user.salt, user.nickname, user.phone, user.campus,
                user.avatar, user.role, user.creditScore, user.status, user.createdAt);
    }

    public int updateProfile(long id, String nickname, String phone, String campus, String avatar) {
        return update("UPDATE users SET nickname = ?, phone = ?, campus = ?, avatar = ? WHERE id = ?",
                nickname, phone, campus, avatar, id);
    }

    public int updatePassword(long id, String password, String salt) {
        return update("UPDATE users SET password = ?, salt = ? WHERE id = ?", password, salt, id);
    }

    /** 信用分增减，限制在 0~150 */
    public int addCredit(long id, int delta) {
        return update("UPDATE users SET credit_score = LEAST(150, GREATEST(0, credit_score + ?)) WHERE id = ?",
                delta, id);
    }

    public int updateStatus(long id, int status) {
        return update("UPDATE users SET status = ? WHERE id = ?", status, id);
    }

    public int updateRole(long id, int role) {
        return update("UPDATE users SET role = ? WHERE id = ?", role, id);
    }

    public List<User> search(String keyword, PageQuery page) {
        StringBuilder sql = new StringBuilder("SELECT * FROM users");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" WHERE username LIKE ? OR nickname LIKE ?");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY id DESC LIMIT ? OFFSET ?");
        args.add(page.getSize());
        args.add(page.getOffset());
        return queryList(sql.toString(), MAPPER, args.toArray());
    }

    public long countSearch(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return queryLong("SELECT COUNT(*) FROM users");
        }
        String like = "%" + keyword.trim() + "%";
        return queryLong("SELECT COUNT(*) FROM users WHERE username LIKE ? OR nickname LIKE ?", like, like);
    }

    public long countAll() {
        return queryLong("SELECT COUNT(*) FROM users");
    }
}
