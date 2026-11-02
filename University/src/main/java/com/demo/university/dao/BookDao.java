package com.demo.university.dao;

import com.demo.university.dto.BookQuery;
import com.demo.university.model.Book;
import com.demo.university.vo.BookVO;

import java.util.ArrayList;
import java.util.List;

/** 教材表数据访问 */
public class BookDao extends BaseDao {

    /** 列表/详情共用的列：书籍 + 卖家 + 类目 + 评分统计 */
    public static final String VO_COLUMNS = "b.id, b.seller_id, b.category_id, b.title, b.author, b.publisher, "
            + "b.isbn, b.edition, b.course_name, b.condition_level, b.original_price, b.price, b.stock, "
            + "b.cover_url, b.description, b.status, b.view_count, b.created_at, "
            + "u.nickname AS seller_nickname, u.credit_score AS seller_credit, c.name AS category_name, "
            + "(SELECT AVG(r.rating) FROM reviews r WHERE r.book_id = b.id) AS avg_rating, "
            + "(SELECT COUNT(*) FROM reviews r WHERE r.book_id = b.id) AS review_count";

    public static final RowMapper<BookVO> VO_MAPPER = rs -> {
        BookVO vo = new BookVO();
        vo.id = rs.getLong("id");
        vo.sellerId = rs.getLong("seller_id");
        vo.sellerNickname = rs.getString("seller_nickname");
        vo.sellerCredit = rs.getInt("seller_credit");
        vo.categoryId = rs.getLong("category_id");
        vo.categoryName = rs.getString("category_name");
        vo.title = rs.getString("title");
        vo.author = rs.getString("author");
        vo.publisher = rs.getString("publisher");
        vo.isbn = rs.getString("isbn");
        vo.edition = rs.getString("edition");
        vo.courseName = rs.getString("course_name");
        vo.conditionLevel = rs.getInt("condition_level");
        vo.originalPrice = rs.getBigDecimal("original_price");
        vo.price = rs.getBigDecimal("price");
        vo.stock = rs.getInt("stock");
        vo.coverUrl = rs.getString("cover_url");
        vo.description = rs.getString("description");
        vo.status = rs.getInt("status");
        vo.viewCount = rs.getInt("view_count");
        vo.createdAt = rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime();
        vo.avgRating = rs.getBigDecimal("avg_rating") == null ? 0.0 : rs.getBigDecimal("avg_rating").doubleValue();
        vo.reviewCount = rs.getLong("review_count");
        vo.applyLabels();
        return vo;
    };

    private static final RowMapper<Book> MAPPER = rs -> {
        Book book = new Book();
        book.id = rs.getLong("id");
        book.sellerId = rs.getLong("seller_id");
        book.categoryId = rs.getLong("category_id");
        book.title = rs.getString("title");
        book.author = rs.getString("author");
        book.publisher = rs.getString("publisher");
        book.isbn = rs.getString("isbn");
        book.edition = rs.getString("edition");
        book.courseName = rs.getString("course_name");
        book.conditionLevel = rs.getInt("condition_level");
        book.originalPrice = rs.getBigDecimal("original_price");
        book.price = rs.getBigDecimal("price");
        book.stock = rs.getInt("stock");
        book.coverUrl = rs.getString("cover_url");
        book.description = rs.getString("description");
        book.status = rs.getInt("status");
        book.viewCount = rs.getInt("view_count");
        book.createdAt = rs.getTimestamp("created_at") == null ? null : rs.getTimestamp("created_at").toLocalDateTime();
        book.updatedAt = rs.getTimestamp("updated_at") == null ? null : rs.getTimestamp("updated_at").toLocalDateTime();
        return book;
    };

    public long insert(Book book) {
        return insert("INSERT INTO books(seller_id, category_id, title, author, publisher, isbn, edition, "
                        + "course_name, condition_level, original_price, price, stock, cover_url, description, "
                        + "status, view_count, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                book.sellerId, book.categoryId, book.title, book.author, book.publisher, book.isbn,
                book.edition, book.courseName, book.conditionLevel, book.originalPrice, book.price,
                book.stock, book.coverUrl, book.description, book.status, book.viewCount,
                book.createdAt, book.updatedAt);
    }

    /** 编辑：带上 seller_id 条件，防止越权修改他人教材 */
    public int update(Book book) {
        return update("UPDATE books SET category_id = ?, title = ?, author = ?, publisher = ?, isbn = ?, "
                        + "edition = ?, course_name = ?, condition_level = ?, original_price = ?, price = ?, "
                        + "stock = ?, cover_url = ?, description = ?, status = ?, updated_at = ? "
                        + "WHERE id = ? AND seller_id = ?",
                book.categoryId, book.title, book.author, book.publisher, book.isbn, book.edition,
                book.courseName, book.conditionLevel, book.originalPrice, book.price, book.stock,
                book.coverUrl, book.description, book.status, book.updatedAt, book.id, book.sellerId);
    }

    public int updateStatus(long bookId, Long sellerId, int status) {
        if (sellerId == null) {
            return update("UPDATE books SET status = ?, updated_at = ? WHERE id = ?",
                    status, java.time.LocalDateTime.now(), bookId);
        }
        return update("UPDATE books SET status = ?, updated_at = ? WHERE id = ? AND seller_id = ?",
                status, java.time.LocalDateTime.now(), bookId, sellerId);
    }

    /** 扣库存：条件更新，影响行数为 0 说明库存不足（防超卖的关键） */
    public int deductStock(long bookId, int quantity) {
        return update("UPDATE books SET stock = stock - ?, updated_at = ? WHERE id = ? AND stock >= ?",
                quantity, java.time.LocalDateTime.now(), bookId, quantity);
    }

    public int restoreStock(long bookId, int quantity) {
        return update("UPDATE books SET stock = stock + ?, updated_at = ? WHERE id = ?",
                quantity, java.time.LocalDateTime.now(), bookId);
    }

    /** 库存清零时把状态置为已售出 */
    public int markSoldOutIfEmpty(long bookId) {
        return update("UPDATE books SET status = 3, updated_at = ? WHERE id = ? AND stock <= 0",
                java.time.LocalDateTime.now(), bookId);
    }

    public int incrementView(long bookId) {
        return update("UPDATE books SET view_count = view_count + 1 WHERE id = ?", bookId);
    }

    public Book findEntityById(long id) {
        return queryOne("SELECT * FROM books WHERE id = ?", MAPPER, id);
    }

    public BookVO findVOById(long id) {
        return queryOne("SELECT " + VO_COLUMNS + " FROM books b "
                + "LEFT JOIN users u ON u.id = b.seller_id "
                + "LEFT JOIN categories c ON c.id = b.category_id WHERE b.id = ?", VO_MAPPER, id);
    }

    public List<BookVO> search(BookQuery query) {
        List<Object> args = new ArrayList<>();
        String where = buildWhere(query, args);
        String sql = "SELECT " + VO_COLUMNS + " FROM books b "
                + "LEFT JOIN users u ON u.id = b.seller_id "
                + "LEFT JOIN categories c ON c.id = b.category_id "
                + where + orderBy(query.sort) + " LIMIT ? OFFSET ?";
        args.add(query.pageQuery().getSize());
        args.add(query.pageQuery().getOffset());
        return queryList(sql, VO_MAPPER, args.toArray());
    }

    public long count(BookQuery query) {
        List<Object> args = new ArrayList<>();
        String where = buildWhere(query, args);
        return queryLong("SELECT COUNT(*) FROM books b " + where, args.toArray());
    }

    public long countAll() {
        return queryLong("SELECT COUNT(*) FROM books");
    }

    public long countOnSale() {
        return queryLong("SELECT COUNT(*) FROM books WHERE status = 1");
    }

    private String buildWhere(BookQuery query, List<Object> args) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        if (!query.includeAllStatus) {
            where.append(" AND b.status = 1");
        } else if (query.status != null) {
            where.append(" AND b.status = ?");
            args.add(query.status);
        }
        if (query.sellerId != null) {
            where.append(" AND b.seller_id = ?");
            args.add(query.sellerId);
        }
        if (query.q != null && !query.q.isBlank()) {
            where.append(" AND (b.title LIKE ? OR b.author LIKE ? OR b.course_name LIKE ? OR b.isbn LIKE ?)");
            String like = "%" + query.q.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (query.categoryIds != null && !query.categoryIds.isEmpty()) {
            where.append(" AND b.category_id IN (");
            where.append(String.join(",", query.categoryIds.stream().map(id -> "?").toList()));
            where.append(")");
            args.addAll(query.categoryIds);
        }
        if (query.minPrice != null) {
            where.append(" AND b.price >= ?");
            args.add(query.minPrice);
        }
        if (query.maxPrice != null) {
            where.append(" AND b.price <= ?");
            args.add(query.maxPrice);
        }
        if (query.conditionLevel != null) {
            where.append(" AND b.condition_level = ?");
            args.add(query.conditionLevel);
        }
        return where.toString();
    }

    private String orderBy(String sort) {
        if (sort == null) {
            return " ORDER BY b.created_at DESC";
        }
        return switch (sort) {
            case "priceAsc" -> " ORDER BY b.price ASC";
            case "priceDesc" -> " ORDER BY b.price DESC";
            case "hot" -> " ORDER BY b.view_count DESC";
            default -> " ORDER BY b.created_at DESC";
        };
    }
}
