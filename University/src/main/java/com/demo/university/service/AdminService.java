package com.demo.university.service;

import com.demo.university.common.ApiException;
import com.demo.university.common.PageQuery;
import com.demo.university.common.PageResult;
import com.demo.university.dao.BookDao;
import com.demo.university.dao.OrderDao;
import com.demo.university.dao.ReviewDao;
import com.demo.university.dao.UserDao;
import com.demo.university.dto.BookQuery;
import com.demo.university.model.BookStatus;
import com.demo.university.model.Role;
import com.demo.university.model.User;
import com.demo.university.vo.BookVO;
import com.demo.university.vo.OrderVO;
import com.demo.university.vo.StatsVO;
import com.demo.university.vo.UserVO;

import java.util.List;

/** 后台管理 */
public class AdminService {

    private final UserDao userDao = new UserDao();
    private final BookDao bookDao = new BookDao();
    private final OrderDao orderDao = new OrderDao();
    private final ReviewDao reviewDao = new ReviewDao();
    private final OrderService orderService = new OrderService();

    public StatsVO stats() {
        StatsVO vo = new StatsVO();
        vo.userCount = userDao.countAll();
        vo.bookCount = bookDao.countAll();
        vo.onSaleBookCount = bookDao.countOnSale();
        vo.orderCount = orderDao.countAll();
        vo.completedOrderCount = orderDao.countCompleted();
        vo.reviewCount = reviewDao.countAll();
        vo.completedAmount = orderDao.sumCompletedAmount();
        return vo;
    }

    public PageResult<UserVO> users(String keyword, Integer page, Integer size) {
        PageQuery pageQuery = PageQuery.of(page, size);
        List<UserVO> list = userDao.search(keyword, pageQuery).stream().map(UserVO::of).toList();
        return new PageResult<>(list, userDao.countSearch(keyword), pageQuery);
    }

    public void setUserStatus(long operatorId, long userId, int status) {
        if (status != 0 && status != 1) {
            throw ApiException.badRequest("状态取值应为 0 或 1");
        }
        if (operatorId == userId) {
            throw ApiException.conflict("不能封禁自己");
        }
        User user = userDao.findById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }
        userDao.updateStatus(userId, status);
    }

    /** 提升用户为管理员（仅超级管理员可调用） */
    public UserVO promoteToAdmin(long userId) {
        User user = userDao.findById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }
        if (user.role != null && user.role >= Role.ADMIN.code) {
            throw ApiException.conflict("该用户已是管理员，无需重复操作");
        }
        userDao.updateRole(userId, Role.ADMIN.code);
        return UserVO.of(userDao.findById(userId));
    }

    /** 降级管理员为普通用户（仅超级管理员可调用） */
    public UserVO demoteFromAdmin(long userId) {
        User user = userDao.findById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }
        if (user.role == null || user.role < Role.ADMIN.code) {
            throw ApiException.conflict("该用户并非管理员，无需降级");
        }
        if (user.role == Role.SUPER_ADMIN.code) {
            throw ApiException.forbidden("不能降级超级管理员");
        }
        userDao.updateRole(userId, Role.STUDENT.code);
        return UserVO.of(userDao.findById(userId));
    }

    public PageResult<BookVO> books(String keyword, Integer status, Integer page, Integer size) {
        BookQuery query = new BookQuery();
        query.q = keyword;
        query.includeAllStatus = true;
        query.status = status;
        query.page = page;
        query.size = size;
        List<BookVO> list = bookDao.search(query);
        return new PageResult<>(list, bookDao.count(query), query.pageQuery());
    }

    /** 管理员下架/恢复教材，不受 seller_id 限制 */
    public void setBookStatus(long bookId, int status) {
        if (status != BookStatus.ON_SALE.code && status != BookStatus.OFFLINE.code) {
            throw ApiException.badRequest("只支持上架(1)或下架(2)");
        }
        if (bookDao.findEntityById(bookId) == null) {
            throw ApiException.notFound("教材不存在");
        }
        bookDao.updateStatus(bookId, null, status);
    }

    public PageResult<OrderVO> orders(String keyword, Integer status, Integer page, Integer size) {
        return orderService.adminList(keyword, status, page, size);
    }
}
