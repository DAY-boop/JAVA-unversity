package com.demo.university.config;

import com.demo.university.dao.HealthDao;
import com.demo.university.service.AdminService;
import com.demo.university.service.AuthService;
import com.demo.university.service.BookService;
import com.demo.university.service.CategoryService;
import com.demo.university.service.FavoriteService;
import com.demo.university.service.OrderService;
import com.demo.university.service.ReviewService;
import com.demo.university.service.UserService;
import com.demo.university.service.WantedBookService;

/**
 * 极简依赖装配点（替代 CDI 注入）。
 * <p>
 * 选择手工装配的原因：项目部署在 Tomcat（非 Jakarta EE 全容器），
 * weld-se 的注入在 WAR 中不稳定；显式装配零魔法、出错位置一目了然。
 * 所有组件都是无状态单例，线程安全。
 */
public final class AppContext {

    private static final AuthService AUTH_SERVICE = new AuthService();
    private static final UserService USER_SERVICE = new UserService();
    private static final CategoryService CATEGORY_SERVICE = new CategoryService();
    private static final BookService BOOK_SERVICE = new BookService();
    private static final OrderService ORDER_SERVICE = new OrderService();
    private static final ReviewService REVIEW_SERVICE = new ReviewService();
    private static final FavoriteService FAVORITE_SERVICE = new FavoriteService();
    private static final AdminService ADMIN_SERVICE = new AdminService();
    private static final WantedBookService WANTED_SERVICE = new WantedBookService();
    private static final HealthDao HEALTH_DAO = new HealthDao();

    private AppContext() {
    }

    public static AuthService auth() {
        return AUTH_SERVICE;
    }

    public static UserService user() {
        return USER_SERVICE;
    }

    public static CategoryService category() {
        return CATEGORY_SERVICE;
    }

    public static BookService book() {
        return BOOK_SERVICE;
    }

    public static OrderService order() {
        return ORDER_SERVICE;
    }

    public static ReviewService review() {
        return REVIEW_SERVICE;
    }

    public static FavoriteService favorite() {
        return FAVORITE_SERVICE;
    }

    public static AdminService admin() {
        return ADMIN_SERVICE;
    }

    public static WantedBookService wanted() {
        return WANTED_SERVICE;
    }

    public static HealthDao healthDao() {
        return HEALTH_DAO;
    }
}
