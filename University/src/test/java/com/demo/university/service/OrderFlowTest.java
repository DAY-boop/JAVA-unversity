package com.demo.university.service;

import com.demo.university.common.ApiException;
import com.demo.university.config.DataSourceProvider;
import com.demo.university.config.DatabaseInitializer;
import com.demo.university.dao.BookDao;
import com.demo.university.dao.UserDao;
import com.demo.university.dto.OrderCreateDTO;
import com.demo.university.dto.ReviewCreateDTO;
import com.demo.university.model.Book;
import com.demo.university.model.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 交易主流程集成测试（H2）：下单扣库存、防超卖、状态机、评价资格。
 */
class OrderFlowTest {

    private static final UserDao USER_DAO = new UserDao();
    private static final BookDao BOOK_DAO = new BookDao();
    private static final OrderService ORDER_SERVICE = new OrderService();
    private static final ReviewService REVIEW_SERVICE = new ReviewService();

    private static long sellerId;
    private static long buyerId;

    @BeforeAll
    static void setUp() {
        DataSourceProvider.init();
        DatabaseInitializer.initialize();
        sellerId = createUser("flow_seller");
        buyerId = createUser("flow_buyer");
    }

    @Test
    void fullTradeFlow() {
        long bookId = publishBook("高等数学（上）", "25.00", 1);

        long orderId = ORDER_SERVICE.create(buyerId, new OrderCreateDTO(bookId, 1, "图书馆一楼", "13800000000", ""));
        assertTrue(orderId > 0);

        Book afterOrder = BOOK_DAO.findEntityById(bookId);
        assertEquals(0, afterOrder.stock, "下单后库存应被扣减");
        assertEquals(3, afterOrder.status, "库存为 0 应自动标记为已售出");

        // 已售出的书不能再次下单
        assertThrows(ApiException.class, () ->
                ORDER_SERVICE.create(buyerId, new OrderCreateDTO(bookId, 1, "", "", "")));

        // 非买家不能付款
        assertThrows(ApiException.class, () -> ORDER_SERVICE.pay(sellerId, orderId));

        // 未完成订单不能评价
        assertThrows(ApiException.class, () ->
                REVIEW_SERVICE.create(buyerId, new ReviewCreateDTO(orderId, 5, "好评", "")));

        ORDER_SERVICE.pay(buyerId, orderId);
        // 卖家不动手，买家不能直接确认收货
        assertThrows(ApiException.class, () -> ORDER_SERVICE.confirm(buyerId, orderId));
        ORDER_SERVICE.deliver(sellerId, orderId);
        ORDER_SERVICE.confirm(buyerId, orderId);

        REVIEW_SERVICE.create(buyerId, new ReviewCreateDTO(orderId, 5, "书况很好", "书况很好"));
        // 同一订单重复评价被拒绝
        assertThrows(ApiException.class, () ->
                REVIEW_SERVICE.create(buyerId, new ReviewCreateDTO(orderId, 5, "再评一次", "")));

        assertNotNull(BOOK_DAO.findVOById(bookId));
        assertEquals(102, USER_DAO.findById(sellerId).creditScore, "收到 5 星好评信用分 +2");
        assertEquals(1, REVIEW_SERVICE.findByBook(bookId).size());
    }

    @Test
    void cancelRestoresStock() {
        long bookId = publishBook("线性代数", "18.00", 2);
        long orderId = ORDER_SERVICE.create(buyerId, new OrderCreateDTO(bookId, 1, "教学楼", "", ""));
        assertEquals(1, BOOK_DAO.findEntityById(bookId).stock);

        ORDER_SERVICE.cancel(buyerId, orderId, "临时不要了");
        assertEquals(2, BOOK_DAO.findEntityById(bookId).stock, "取消订单应回滚库存");
        assertEquals(1, BOOK_DAO.findEntityById(bookId).status, "库存恢复后应重新上架");

        // 已取消订单不能再付款
        assertThrows(ApiException.class, () -> ORDER_SERVICE.pay(buyerId, orderId));
    }

    @Test
    void cannotBuyOwnBook() {
        long bookId = publishBook("大学物理", "20.00", 1);
        assertThrows(ApiException.class, () ->
                ORDER_SERVICE.create(sellerId, new OrderCreateDTO(bookId, 1, "", "", "")));
        assertEquals(1, BOOK_DAO.findEntityById(bookId).stock, "自己下单不应扣库存");
    }

    @Test
    void quantityOverStockIsRejected() {
        long bookId = publishBook("离散数学", "15.00", 1);
        assertThrows(ApiException.class, () ->
                ORDER_SERVICE.create(buyerId, new OrderCreateDTO(bookId, 3, "", "", "")));
        assertEquals(1, BOOK_DAO.findEntityById(bookId).stock, "失败下单不应改变库存");
    }

    private static long createUser(String username) {
        User user = new User();
        user.username = username;
        user.password = "hash";
        user.salt = "salt";
        user.nickname = username;
        user.role = 0;
        user.creditScore = 100;
        user.status = 0;
        user.createdAt = LocalDateTime.now();
        return USER_DAO.insert(user);
    }

    private static long publishBook(String title, String price, int stock) {
        Book book = new Book();
        book.sellerId = sellerId;
        book.categoryId = 1L;
        book.title = title;
        book.conditionLevel = 3;
        book.price = new BigDecimal(price);
        book.stock = stock;
        book.status = 1;
        book.viewCount = 0;
        book.createdAt = LocalDateTime.now();
        book.updatedAt = book.createdAt;
        return BOOK_DAO.insert(book);
    }
}
