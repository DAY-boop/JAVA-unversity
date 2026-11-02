package com.demo.university.service;

import com.demo.university.common.ApiException;
import com.demo.university.common.PageQuery;
import com.demo.university.common.PageResult;
import com.demo.university.dao.BookDao;
import com.demo.university.dao.Db;
import com.demo.university.dao.OrderDao;
import com.demo.university.dao.OrderItemDao;
import com.demo.university.dto.OrderCreateDTO;
import com.demo.university.model.Book;
import com.demo.university.model.BookStatus;
import com.demo.university.model.Order;
import com.demo.university.model.OrderItem;
import com.demo.university.model.OrderStatus;
import com.demo.university.util.OrderNoGenerator;
import com.demo.university.vo.OrderVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易核心：下单（扣库存 + 建单 + 明细，同一事务）、状态流转、超时关单。
 */
public class OrderService {

    private static final int MAX_QUANTITY = 5;

    private final OrderDao orderDao = new OrderDao();
    private final OrderItemDao orderItemDao = new OrderItemDao();
    private final BookDao bookDao = new BookDao();

    public long create(long buyerId, OrderCreateDTO dto) {
        if (dto == null || dto.bookId() == null) {
            throw ApiException.badRequest("请选择要购买的教材");
        }
        int quantity = dto.quantity() == null ? 1 : dto.quantity();
        if (quantity < 1 || quantity > MAX_QUANTITY) {
            throw ApiException.badRequest("购买数量应为 1-" + MAX_QUANTITY);
        }
        long bookId = dto.bookId();

        return Db.inTransaction(() -> {
            Book book = bookDao.findEntityById(bookId);
            if (book == null) {
                throw ApiException.notFound("教材不存在");
            }
            if (book.status == null || book.status != BookStatus.ON_SALE.code) {
                throw ApiException.conflict("该教材已下架或已售出");
            }
            if (book.sellerId != null && book.sellerId == buyerId) {
                throw ApiException.conflict("不能购买自己发布的教材");
            }
            // 条件更新防超卖：库存不足时影响行数为 0
            if (bookDao.deductStock(bookId, quantity) == 0) {
                throw ApiException.conflict("手慢了，该教材已被他人下单");
            }
            bookDao.markSoldOutIfEmpty(bookId);

            BigDecimal unitPrice = book.price;
            BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));

            Order order = new Order();
            order.orderNo = OrderNoGenerator.next();
            order.buyerId = buyerId;
            order.sellerId = book.sellerId;
            order.totalAmount = total;
            order.status = OrderStatus.CREATED.code;
            order.tradePlace = dto.tradePlace();
            order.contact = dto.contact();
            order.remark = dto.remark();
            order.createdAt = LocalDateTime.now();
            long orderId = orderDao.insert(order);

            OrderItem item = new OrderItem();
            item.orderId = orderId;
            item.bookId = bookId;
            item.titleSnapshot = book.title;
            item.coverSnapshot = book.coverUrl;
            item.priceSnapshot = unitPrice;
            item.quantity = quantity;
            item.subtotal = total;
            orderItemDao.insert(item);

            return orderId;
        });
    }

    public void pay(long userId, long orderId) {
        Order order = requireOrder(orderId);
        if (order.buyerId == null || order.buyerId != userId) {
            throw ApiException.forbidden("只有买家可以付款");
        }
        if (orderDao.markPaid(orderId) == 0) {
            throw ApiException.conflict("订单当前状态无法付款，请刷新后重试");
        }
    }

    public void deliver(long userId, long orderId) {
        Order order = requireOrder(orderId);
        if (order.sellerId == null || order.sellerId != userId) {
            throw ApiException.forbidden("只有卖家可以确认交付");
        }
        if (orderDao.markDelivered(orderId) == 0) {
            throw ApiException.conflict("订单当前状态无法交付，请刷新后重试");
        }
    }

    public void confirm(long userId, long orderId) {
        Order order = requireOrder(orderId);
        if (order.buyerId == null || order.buyerId != userId) {
            throw ApiException.forbidden("只有买家可以确认收货");
        }
        if (orderDao.markCompleted(orderId) == 0) {
            throw ApiException.conflict("订单当前状态无法确认收货，请刷新后重试");
        }
    }

    public void cancel(long userId, long orderId, String reason) {
        Order order = requireOrder(orderId);
        boolean participant = (order.buyerId != null && order.buyerId == userId)
                || (order.sellerId != null && order.sellerId == userId);
        if (!participant) {
            throw ApiException.forbidden("无权操作该订单");
        }
        Db.inTransaction(() -> {
            if (orderDao.markCancelled(orderId, reason == null || reason.isBlank() ? "用户取消" : reason) == 0) {
                throw ApiException.conflict("订单当前状态无法取消（可能已完成或已取消）");
            }
            OrderItem item = orderItemDao.findByOrderId(orderId);
            if (item != null) {
                bookDao.restoreStock(item.bookId, item.quantity);
                bookDao.updateStatus(item.bookId, null, BookStatus.ON_SALE.code);
            }
            return null;
        });
    }

    public PageResult<OrderVO> list(long userId, String role, Integer status, Integer page, Integer size) {
        PageQuery pageQuery = PageQuery.of(page, size);
        boolean asSeller = "seller".equalsIgnoreCase(role);
        List<OrderVO> list = asSeller
                ? orderDao.findVOBySeller(userId, status, pageQuery, userId)
                : orderDao.findVOByBuyer(userId, status, pageQuery, userId);
        long total = asSeller
                ? orderDao.countBySeller(userId, status)
                : orderDao.countByBuyer(userId, status);
        return new PageResult<>(list, total, pageQuery);
    }

    public OrderVO detail(long userId, long orderId, boolean admin) {
        OrderVO vo = orderDao.findVOById(orderId, userId);
        if (vo == null) {
            throw ApiException.notFound("订单不存在");
        }
        boolean participant = (vo.buyerId != null && vo.buyerId == userId)
                || (vo.sellerId != null && vo.sellerId == userId);
        if (!participant && !admin) {
            throw ApiException.forbidden("无权查看该订单");
        }
        return vo;
    }

    /** 定时任务：取消超时未付款订单并回滚库存 */
    public int cancelTimeoutOrders(int minutes) {
        List<Long> ids = orderDao.findTimeoutIds(minutes);
        int cancelled = 0;
        for (Long orderId : ids) {
            try {
                Db.inTransaction(() -> {
                    if (orderDao.markCancelled(orderId, "超时未付款，系统自动取消") == 0) {
                        return null;
                    }
                    OrderItem item = orderItemDao.findByOrderId(orderId);
                    if (item != null) {
                        bookDao.restoreStock(item.bookId, item.quantity);
                        bookDao.updateStatus(item.bookId, null, BookStatus.ON_SALE.code);
                    }
                    return null;
                });
                cancelled++;
            } catch (RuntimeException e) {
                // 单笔失败不影响其它订单
            }
        }
        return cancelled;
    }

    public PageResult<OrderVO> adminList(String keyword, Integer status, Integer page, Integer size) {
        PageQuery pageQuery = PageQuery.of(page, size);
        List<OrderVO> list = orderDao.searchAll(keyword, status, pageQuery, 0L);
        return new PageResult<>(list, orderDao.countSearchAll(keyword, status), pageQuery);
    }

    private Order requireOrder(long orderId) {
        Order order = orderDao.findById(orderId);
        if (order == null) {
            throw ApiException.notFound("订单不存在");
        }
        return order;
    }
}
