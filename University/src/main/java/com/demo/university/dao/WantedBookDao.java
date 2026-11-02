package com.demo.university.dao;

import com.demo.university.dto.WantedQuery;
import com.demo.university.model.WantedBook;
import com.demo.university.vo.WantedBookVO;

import java.util.ArrayList;
import java.util.List;

/** 求购表数据访问 */
public class WantedBookDao extends BaseDao {

    private static final String VO_COLUMNS = "SELECT w.id, w.user_id, w.category_id, w.title, w.author, "
            + "w.publisher, w.course_name, w.expected_price, w.condition_level, w.description, "
            + "w.contact, w.campus, w.status, w.created_at, "
            + "u.nickname AS user_nickname, c.name AS category_name "
            + "FROM wanted_books w "
            + "LEFT JOIN users u ON u.id = w.user_id "
            + "LEFT JOIN categories c ON c.id = w.category_id ";

    private static final RowMapper<WantedBookVO> VO_MAPPER = rs -> {
        WantedBookVO vo = new WantedBookVO();
        vo.id = rs.getLong("id");
        vo.userId = rs.getLong("user_id");
        vo.userNickname = rs.getString("user_nickname");
        vo.categoryId = rs.getLong("category_id");
        vo.categoryName = rs.getString("category_name");
        vo.title = rs.getString("title");
        vo.author = rs.getString("author");
        vo.publisher = rs.getString("publisher");
        vo.courseName = rs.getString("course_name");
        vo.expectedPrice = rs.getBigDecimal("expected_price");
        vo.conditionLevel = rs.getInt("condition_level");
        vo.description = rs.getString("description");
        vo.contact = rs.getString("contact");
        vo.campus = rs.getString("campus");
        vo.status = rs.getInt("status");
        vo.createdAt = rs.getTimestamp("created_at") == null
                ? null : rs.getTimestamp("created_at").toLocalDateTime();
        vo.applyLabels();
        return vo;
    };

    private static final RowMapper<WantedBook> MAPPER = rs -> {
        WantedBook book = new WantedBook();
        book.id = rs.getLong("id");
        book.userId = rs.getLong("user_id");
        book.categoryId = rs.getLong("category_id");
        book.title = rs.getString("title");
        book.author = rs.getString("author");
        book.publisher = rs.getString("publisher");
        book.courseName = rs.getString("course_name");
        book.expectedPrice = rs.getBigDecimal("expected_price");
        book.conditionLevel = rs.getInt("condition_level");
        book.description = rs.getString("description");
        book.contact = rs.getString("contact");
        book.campus = rs.getString("campus");
        book.status = rs.getInt("status");
        book.createdAt = rs.getTimestamp("created_at") == null
                ? null : rs.getTimestamp("created_at").toLocalDateTime();
        book.updatedAt = rs.getTimestamp("updated_at") == null
                ? null : rs.getTimestamp("updated_at").toLocalDateTime();
        return book;
    };

    public long insert(WantedBook book) {
        return insert("INSERT INTO wanted_books(user_id, category_id, title, author, publisher, course_name, "
                        + "expected_price, condition_level, description, contact, campus, status, "
                        + "created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                book.userId, book.categoryId, book.title, book.author, book.publisher, book.courseName,
                book.expectedPrice, book.conditionLevel, book.description, book.contact, book.campus,
                book.status, book.createdAt, book.updatedAt);
    }

    /** 编辑：带 user_id 条件，防止越权修改他人求购 */
    public int update(WantedBook book) {
        return update("UPDATE wanted_books SET category_id = ?, title = ?, author = ?, publisher = ?, "
                        + "course_name = ?, expected_price = ?, condition_level = ?, description = ?, "
                        + "contact = ?, campus = ?, updated_at = ? WHERE id = ? AND user_id = ?",
                book.categoryId, book.title, book.author, book.publisher, book.courseName,
                book.expectedPrice, book.conditionLevel, book.description, book.contact, book.campus,
                book.updatedAt, book.id, book.userId);
    }

    public int updateStatus(long id, long userId, int status) {
        return update("UPDATE wanted_books SET status = ?, updated_at = ? WHERE id = ? AND user_id = ?",
                status, java.time.LocalDateTime.now(), id, userId);
    }

    public WantedBook findEntityById(long id) {
        return queryOne("SELECT * FROM wanted_books WHERE id = ?", MAPPER, id);
    }

    public WantedBookVO findVOById(long id) {
        return queryOne(VO_COLUMNS + "WHERE w.id = ?", VO_MAPPER, id);
    }

    public List<WantedBookVO> search(WantedQuery query) {
        List<Object> args = new ArrayList<>();
        String where = buildWhere(query, args);
        String sql = VO_COLUMNS + where + " ORDER BY w.id DESC LIMIT ? OFFSET ?";
        args.add(query.pageQuery().getSize());
        args.add(query.pageQuery().getOffset());
        return queryList(sql, VO_MAPPER, args.toArray());
    }

    public long count(WantedQuery query) {
        List<Object> args = new ArrayList<>();
        String where = buildWhere(query, args);
        return queryLong("SELECT COUNT(*) FROM wanted_books w " + where, args.toArray());
    }

    public long countAll() {
        return queryLong("SELECT COUNT(*) FROM wanted_books");
    }

    private String buildWhere(WantedQuery query, List<Object> args) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (query.userId != null) {
            where.append(" AND w.user_id = ?");
            args.add(query.userId);
        }
        if (!query.includeAllStatus) {
            where.append(" AND w.status = ?");
            args.add(query.status == null ? 0 : query.status);
        } else if (query.status != null) {
            where.append(" AND w.status = ?");
            args.add(query.status);
        }
        if (query.q != null && !query.q.isBlank()) {
            where.append(" AND (w.title LIKE ? OR w.author LIKE ? OR w.course_name LIKE ?)");
            String like = "%" + query.q.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (query.categoryIds != null && !query.categoryIds.isEmpty()) {
            where.append(" AND w.category_id IN (");
            where.append(String.join(",", query.categoryIds.stream().map(id -> "?").toList()));
            where.append(")");
            args.addAll(query.categoryIds);
        }
        return where.toString();
    }
}
