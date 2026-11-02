package com.demo.university.dao;

import com.demo.university.common.PageQuery;
import com.demo.university.model.Order;
import com.demo.university.vo.OrderVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 订单表数据访问 */
public class OrderDao extends BaseDao {

    /** 订单视图列：订单 + 买卖双方昵称 + 教材快照 + 当前用户是否已评价 */
    private static final String VO_COLUMNS = "SELECT o.id, o.order_no, o.buyer_id, o.seller_id, o.total_amount, "
            + "o.status, o.trade_place, o.contact, o.remark, o.cancel_reason, "
            + "o.created_at, o.paid_at, o.delivered_at, o.finished_at, "
            + "bu.nickname AS buyer_nickname, se.nickname AS seller_nickname, "
            + "oi.book_id, oi.title_snapshot, oi.cover_snapshot, oi.price_snapshot, oi.quantity, "
            + "(SELECT COUNT(*) FROM reviews r WHERE r.order_id = o.id AND r.reviewer_id = ?) AS reviewed_count "
            + "FROM orders o "
            + "LEFT JOIN users bu ON bu.id = o.buyer_id "
            + "LEFT JOIN users se ON se.id = o.seller_id "
            + "LEFT JOIN order_items oi ON oi.order_id = o.id ";

    private static final RowMapper<OrderVO> VO_MAPPER = rs -> {
        OrderVO vo = new OrderVO();
        vo.id = rs.getLong("id");
        vo.orderNo = rs.getString("order_no");
        vo.buyerId = rs.getLong("buyer_id");
        vo.sellerId = rs.getLong("seller_id");
        vo.buyerNickname = rs.getString("buyer_nickname");
        vo.sellerNickname = rs.getString("seller_nickname");
        vo.totalAmount = rs.getBigDecimal("total_amount");
        vo.status = rs.getInt("status");
        vo.bookId = rs.getLong("book_id");
        vo.bookTitle = rs.getString("title_snapshot");
        vo.bookCover = rs.getString("cover_snapshot");
        vo.price = rs.getBigDecimal("price_snapshot");
        vo.quantity = rs.getInt("quantity");
        vo.tradePlace = rs.getString("trade_place");
        vo.contact = rs.getString("contact");
        vo.remark = rs.getString("remark");
        vo.cancelReason = rs.getString("cancel_reason");
        vo.createdAt = toLocal(rs.getTimestamp("created_at"));
        vo.paidAt = toLocal(rs.getTimestamp("paid_at"));
        vo.deliveredAt = toLocal(rs.getTimestamp("delivered_at"));
        vo.finishedAt = toLocal(rs.getTimestamp("finished_at"));
        vo.reviewed = rs.getLong("reviewed_count") > 0;
        vo.applyLabels();
        return vo;
    };

    private static final RowMapper<Order> MAPPER = rs -> {
        Order order = new Order();
        order.id = rs.getLong("id");
        order.orderNo = rs.getString("order_no");
        order.buyerId = rs.getLong("buyer_id");
        order.sellerId = rs.getLong("seller_id");
        order.totalAmount = rs.getBigDecimal("total_amount");
        order.status = rs.getInt("status");
        order.tradePlace = rs.getString("trade_place");
        order.contact = rs.getString("contact");
        order.remark = rs.getString("remark");
        order.cancelReason = rs.getString("cancel_reason");
        order.createdAt = toLocal(rs.getTimestamp("created_at"));
        order.paidAt = toLocal(rs.getTimestamp("paid_at"));
        order.deliveredAt = toLocal(rs.getTimestamp("delivered_at"));
        order.finishedAt = toLocal(rs.getTimestamp("finished_at"));
        return order;
    };

    private static LocalDateTime toLocal(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    public long insert(Order order) {
        return insert("INSERT INTO orders(order_no, buyer_id, seller_id, total_amount, status, trade_place, "
                        + "contact, remark, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                order.orderNo, order.buyerId, order.sellerId, order.totalAmount, order.status,
                order.tradePlace, order.contact, order.remark, order.createdAt);
    }

    public Order findById(long id) {
        return queryOne("SELECT * FROM orders WHERE id = ?", MAPPER, id);
    }

    public int markPaid(long id) {
        return update("UPDATE orders SET status = 1, paid_at = ? WHERE id = ? AND status = 0",
                LocalDateTime.now(), id);
    }

    public int markDelivered(long id) {
        return update("UPDATE orders SET status = 2, delivered_at = ? WHERE id = ? AND status = 1",
                LocalDateTime.now(), id);
    }

    public int markCompleted(long id) {
        return update("UPDATE orders SET status = 3, finished_at = ? WHERE id = ? AND status = 2",
                LocalDateTime.now(), id);
    }

    public int markCancelled(long id, String reason) {
        return update("UPDATE orders SET status = 4, cancel_reason = ? WHERE id = ? AND status IN (0, 1)",
                reason, id);
    }

    public List<OrderVO> findVOByBuyer(long buyerId, Integer status, PageQuery page, long viewerId) {
        StringBuilder sql = new StringBuilder(VO_COLUMNS).append(" WHERE o.buyer_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(viewerId);
        args.add(buyerId);
        if (status != null) {
            sql.append(" AND o.status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY o.id DESC LIMIT ? OFFSET ?");
        args.add(page.getSize());
        args.add(page.getOffset());
        return queryList(sql.toString(), VO_MAPPER, args.toArray());
    }

    public long countByBuyer(long buyerId, Integer status) {
        if (status == null) {
            return queryLong("SELECT COUNT(*) FROM orders WHERE buyer_id = ?", buyerId);
        }
        return queryLong("SELECT COUNT(*) FROM orders WHERE buyer_id = ? AND status = ?", buyerId, status);
    }

    public List<OrderVO> findVOBySeller(long sellerId, Integer status, PageQuery page, long viewerId) {
        StringBuilder sql = new StringBuilder(VO_COLUMNS).append(" WHERE o.seller_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(viewerId);
        args.add(sellerId);
        if (status != null) {
            sql.append(" AND o.status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY o.id DESC LIMIT ? OFFSET ?");
        args.add(page.getSize());
        args.add(page.getOffset());
        return queryList(sql.toString(), VO_MAPPER, args.toArray());
    }

    public long countBySeller(long sellerId, Integer status) {
        if (status == null) {
            return queryLong("SELECT COUNT(*) FROM orders WHERE seller_id = ?", sellerId);
        }
        return queryLong("SELECT COUNT(*) FROM orders WHERE seller_id = ? AND status = ?", sellerId, status);
    }

    public OrderVO findVOById(long id, long viewerId) {
        return queryOne(VO_COLUMNS + " WHERE o.id = ?", VO_MAPPER, viewerId, id);
    }

    /** 超时未付款订单（定时任务用） */
    public List<Long> findTimeoutIds(int minutes) {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(minutes);
        return queryList("SELECT id FROM orders WHERE status = 0 AND created_at < ?", rs -> rs.getLong(1), deadline);
    }

    public List<OrderVO> searchAll(String keyword, Integer status, PageQuery page, long viewerId) {
        StringBuilder sql = new StringBuilder(VO_COLUMNS).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        args.add(viewerId);
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (o.order_no LIKE ? OR oi.title_snapshot LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (status != null) {
            sql.append(" AND o.status = ?");
            args.add(status);
        }
        sql.append(" ORDER BY o.id DESC LIMIT ? OFFSET ?");
        args.add(page.getSize());
        args.add(page.getOffset());
        return queryList(sql.toString(), VO_MAPPER, args.toArray());
    }

    public long countSearchAll(String keyword, Integer status) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM orders o "
                + "LEFT JOIN order_items oi ON oi.order_id = o.id WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND (o.order_no LIKE ? OR oi.title_snapshot LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if (status != null) {
            sql.append(" AND o.status = ?");
            args.add(status);
        }
        return queryLong(sql.toString(), args.toArray());
    }

    public long countAll() {
        return queryLong("SELECT COUNT(*) FROM orders");
    }

    public long countCompleted() {
        return queryLong("SELECT COUNT(*) FROM orders WHERE status = 3");
    }

    public BigDecimal sumCompletedAmount() {
        return queryOne("SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE status = 3",
                rs -> rs.getBigDecimal(1));
    }
}
