package com.demo.university.service;

import com.demo.university.common.ApiException;
import com.demo.university.dao.OrderDao;
import com.demo.university.dao.OrderItemDao;
import com.demo.university.dao.ReviewDao;
import com.demo.university.dao.UserDao;
import com.demo.university.dto.ReviewCreateDTO;
import com.demo.university.model.Order;
import com.demo.university.model.OrderItem;
import com.demo.university.model.OrderStatus;
import com.demo.university.model.Review;
import com.demo.university.vo.ReviewVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价：只有已完成的订单能评价，且每个订单每人只能评一次（数据库唯一索引兜底）。
 * 评完自动调整被评价人的信用分。
 */
public class ReviewService {

    private final ReviewDao reviewDao = new ReviewDao();
    private final OrderDao orderDao = new OrderDao();
    private final OrderItemDao orderItemDao = new OrderItemDao();
    private final UserDao userDao = new UserDao();

    public long create(long reviewerId, ReviewCreateDTO dto) {
        if (dto == null || dto.orderId() == null) {
            throw ApiException.badRequest("请选择要评价的订单");
        }
        if (dto.rating() == null || dto.rating() < 1 || dto.rating() > 5) {
            throw ApiException.badRequest("评分应为 1-5 星");
        }
        if (dto.content() != null && dto.content().length() > 1000) {
            throw ApiException.badRequest("评价内容不能超过 1000 个字符");
        }
        Order order = orderDao.findById(dto.orderId());
        if (order == null) {
            throw ApiException.notFound("订单不存在");
        }
        boolean isBuyer = order.buyerId != null && order.buyerId == reviewerId;
        boolean isSeller = order.sellerId != null && order.sellerId == reviewerId;
        if (!isBuyer && !isSeller) {
            throw ApiException.forbidden("无权评价该订单");
        }
        if (order.status == null || order.status != OrderStatus.COMPLETED.code) {
            throw ApiException.conflict("订单完成后才能评价");
        }
        if (reviewDao.exists(order.id, reviewerId)) {
            throw ApiException.conflict("你已评价过该订单");
        }
        OrderItem item = orderItemDao.findByOrderId(order.id);
        if (item == null) {
            throw ApiException.notFound("订单明细缺失，无法评价");
        }

        Review review = new Review();
        review.orderId = order.id;
        review.bookId = item.bookId;
        review.reviewerId = reviewerId;
        review.revieweeId = isBuyer ? order.sellerId : order.buyerId;
        review.rating = dto.rating();
        review.content = dto.content();
        review.tags = dto.tags();
        review.createdAt = LocalDateTime.now();
        long id = reviewDao.insert(review);

        int delta = dto.rating() >= 4 ? 2 : (dto.rating() == 3 ? 0 : -2);
        if (delta != 0 && review.revieweeId != null) {
            userDao.addCredit(review.revieweeId, delta);
        }
        return id;
    }

    public List<ReviewVO> findByBook(long bookId) {
        return reviewDao.findByBook(bookId);
    }

    public List<ReviewVO> received(long userId) {
        return reviewDao.findByReviewee(userId);
    }
}
