package com.demo.university.dao;

import com.demo.university.model.Review;
import com.demo.university.vo.ReviewVO;

import java.util.List;

/** 评价表数据访问 */
public class ReviewDao extends BaseDao {

    private static final String VO_COLUMNS = "SELECT r.id, r.order_id, r.book_id, r.reviewer_id, r.reviewee_id, "
            + "r.rating, r.content, r.tags, r.created_at, b.title AS book_title, "
            + "rv.nickname AS reviewer_nickname, re.nickname AS reviewee_nickname "
            + "FROM reviews r "
            + "LEFT JOIN books b ON b.id = r.book_id "
            + "LEFT JOIN users rv ON rv.id = r.reviewer_id "
            + "LEFT JOIN users re ON re.id = r.reviewee_id ";

    private static final RowMapper<ReviewVO> VO_MAPPER = rs -> {
        ReviewVO vo = new ReviewVO();
        vo.id = rs.getLong("id");
        vo.orderId = rs.getLong("order_id");
        vo.bookId = rs.getLong("book_id");
        vo.bookTitle = rs.getString("book_title");
        vo.reviewerId = rs.getLong("reviewer_id");
        vo.reviewerNickname = rs.getString("reviewer_nickname");
        vo.revieweeId = rs.getLong("reviewee_id");
        vo.revieweeNickname = rs.getString("reviewee_nickname");
        vo.rating = rs.getInt("rating");
        vo.content = rs.getString("content");
        vo.tags = rs.getString("tags");
        vo.createdAt = rs.getTimestamp("created_at") == null
                ? null : rs.getTimestamp("created_at").toLocalDateTime();
        return vo;
    };

    public long insert(Review review) {
        return insert("INSERT INTO reviews(order_id, book_id, reviewer_id, reviewee_id, rating, content, tags, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                review.orderId, review.bookId, review.reviewerId, review.revieweeId,
                review.rating, review.content, review.tags, review.createdAt);
    }

    public boolean exists(long orderId, long reviewerId) {
        return queryLong("SELECT COUNT(*) FROM reviews WHERE order_id = ? AND reviewer_id = ?",
                orderId, reviewerId) > 0;
    }

    public List<ReviewVO> findByBook(long bookId) {
        return queryList(VO_COLUMNS + "WHERE r.book_id = ? ORDER BY r.id DESC", VO_MAPPER, bookId);
    }

    /** 我收到的评价 */
    public List<ReviewVO> findByReviewee(long userId) {
        return queryList(VO_COLUMNS + "WHERE r.reviewee_id = ? ORDER BY r.id DESC", VO_MAPPER, userId);
    }

    public long countAll() {
        return queryLong("SELECT COUNT(*) FROM reviews");
    }
}
