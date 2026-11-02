package com.demo.university.dao;

import com.demo.university.common.PageQuery;
import com.demo.university.vo.BookVO;

import java.time.LocalDateTime;
import java.util.List;

/** 收藏表数据访问 */
public class FavoriteDao extends BaseDao {

    public long insert(long userId, long bookId) {
        return insert("INSERT INTO favorites(user_id, book_id, created_at) VALUES (?, ?, ?)",
                userId, bookId, LocalDateTime.now());
    }

    public int delete(long userId, long bookId) {
        return update("DELETE FROM favorites WHERE user_id = ? AND book_id = ?", userId, bookId);
    }

    public boolean exists(long userId, long bookId) {
        return queryLong("SELECT COUNT(*) FROM favorites WHERE user_id = ? AND book_id = ?",
                userId, bookId) > 0;
    }

    public List<BookVO> findByUser(long userId, PageQuery page) {
        return queryList("SELECT " + BookDao.VO_COLUMNS + " FROM favorites f "
                        + "JOIN books b ON b.id = f.book_id "
                        + "LEFT JOIN users u ON u.id = b.seller_id "
                        + "LEFT JOIN categories c ON c.id = b.category_id "
                        + "WHERE f.user_id = ? ORDER BY f.id DESC LIMIT ? OFFSET ?",
                BookDao.VO_MAPPER, userId, page.getSize(), page.getOffset());
    }

    public long countByUser(long userId) {
        return queryLong("SELECT COUNT(*) FROM favorites WHERE user_id = ?", userId);
    }
}
