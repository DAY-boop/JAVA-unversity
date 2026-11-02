package com.demo.university.dao;

import com.demo.university.model.OrderItem;

/** 订单明细表数据访问 */
public class OrderItemDao extends BaseDao {

    private static final RowMapper<OrderItem> MAPPER = rs -> {
        OrderItem item = new OrderItem();
        item.id = rs.getLong("id");
        item.orderId = rs.getLong("order_id");
        item.bookId = rs.getLong("book_id");
        item.titleSnapshot = rs.getString("title_snapshot");
        item.coverSnapshot = rs.getString("cover_snapshot");
        item.priceSnapshot = rs.getBigDecimal("price_snapshot");
        item.quantity = rs.getInt("quantity");
        item.subtotal = rs.getBigDecimal("subtotal");
        return item;
    };

    public long insert(OrderItem item) {
        return insert("INSERT INTO order_items(order_id, book_id, title_snapshot, cover_snapshot, "
                        + "price_snapshot, quantity, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?)",
                item.orderId, item.bookId, item.titleSnapshot, item.coverSnapshot,
                item.priceSnapshot, item.quantity, item.subtotal);
    }

    public OrderItem findByOrderId(long orderId) {
        return queryOne("SELECT * FROM order_items WHERE order_id = ?", MAPPER, orderId);
    }
}
