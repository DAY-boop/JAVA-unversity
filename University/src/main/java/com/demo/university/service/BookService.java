package com.demo.university.service;

import com.demo.university.common.ApiException;
import com.demo.university.common.PageQuery;
import com.demo.university.common.PageResult;
import com.demo.university.dao.BookDao;
import com.demo.university.dao.FavoriteDao;
import com.demo.university.dto.BookQuery;
import com.demo.university.dto.BookSaveDTO;
import com.demo.university.model.Book;
import com.demo.university.model.BookStatus;
import com.demo.university.vo.BookVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 教材发布、检索、上下架 */
public class BookService {

    private final BookDao bookDao = new BookDao();
    private final FavoriteDao favoriteDao = new FavoriteDao();
    private final CategoryService categoryService = new CategoryService();

    public PageResult<BookVO> search(BookQuery query) {
        if (query.categoryId != null) {
            query.categoryIds = categoryService.descendantIds(query.categoryId);
        }
        List<BookVO> list = bookDao.search(query);
        long total = bookDao.count(query);
        return new PageResult<>(list, total, query.pageQuery());
    }

    public BookVO detail(long bookId, Long viewerId) {
        BookVO vo = bookDao.findVOById(bookId);
        if (vo == null) {
            throw ApiException.notFound("教材不存在或已被删除");
        }
        bookDao.incrementView(bookId);
        vo.viewCount = (vo.viewCount == null ? 0 : vo.viewCount) + 1;
        if (viewerId != null) {
            vo.favorite = favoriteDao.exists(viewerId, bookId);
        }
        return vo;
    }

    public long publish(long sellerId, BookSaveDTO dto) {
        validate(dto);
        Book book = toEntity(dto);
        book.sellerId = sellerId;
        book.status = BookStatus.ON_SALE.code;
        book.viewCount = 0;
        book.createdAt = LocalDateTime.now();
        book.updatedAt = book.createdAt;
        return bookDao.insert(book);
    }

    public void update(long sellerId, long bookId, BookSaveDTO dto) {
        validate(dto);
        Book existing = requireOwnedBook(sellerId, bookId);
        Book book = toEntity(dto);
        book.id = bookId;
        book.sellerId = sellerId;
        book.updatedAt = LocalDateTime.now();
        // 已售完的教材补货后自动回到在售
        book.status = existing.status;
        if (existing.status != null && existing.status == BookStatus.SOLD_OUT.code && book.stock > 0) {
            book.status = BookStatus.ON_SALE.code;
        }
        bookDao.update(book);
    }

    /** status: 1 重新上架，2 下架 */
    public void changeStatus(long sellerId, long bookId, int status) {
        if (status != BookStatus.ON_SALE.code && status != BookStatus.OFFLINE.code) {
            throw ApiException.badRequest("不支持的状态值");
        }
        Book existing = requireOwnedBook(sellerId, bookId);
        if (status == BookStatus.ON_SALE.code && (existing.stock == null || existing.stock <= 0)) {
            throw ApiException.conflict("库存为 0，无法重新上架，请先编辑库存");
        }
        bookDao.updateStatus(bookId, sellerId, status);
    }

    public PageResult<BookVO> myBooks(long sellerId, Integer page, Integer size) {
        BookQuery query = new BookQuery();
        query.sellerId = sellerId;
        query.includeAllStatus = true;
        query.page = page;
        query.size = size;
        List<BookVO> list = bookDao.search(query);
        long total = bookDao.count(query);
        return new PageResult<>(list, total, query.pageQuery());
    }

    public PageResult<BookVO> adminList(String keyword, Integer status, Integer page, Integer size) {
        BookQuery query = new BookQuery();
        query.q = keyword;
        query.includeAllStatus = true;
        query.status = status;
        query.page = page;
        query.size = size;
        List<BookVO> list = bookDao.search(query);
        long total = bookDao.count(query);
        return new PageResult<>(list, total, query.pageQuery());
    }

    private Book requireOwnedBook(long sellerId, long bookId) {
        Book book = bookDao.findEntityById(bookId);
        if (book == null) {
            throw ApiException.notFound("教材不存在");
        }
        if (book.sellerId == null || book.sellerId != sellerId) {
            throw ApiException.forbidden("只能操作自己发布的教材");
        }
        return book;
    }

    private void validate(BookSaveDTO dto) {
        if (dto == null) {
            throw ApiException.badRequest("参数不能为空");
        }
        if (dto.title() == null || dto.title().isBlank()) {
            throw ApiException.badRequest("书名不能为空");
        }
        if (dto.title().length() > 120) {
            throw ApiException.badRequest("书名不能超过 120 个字符");
        }
        if (dto.categoryId() == null) {
            throw ApiException.badRequest("请选择教材类目");
        }
        if (dto.price() == null || dto.price().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("售价必须大于 0");
        }
        if (dto.price().compareTo(new BigDecimal("100000")) > 0) {
            throw ApiException.badRequest("售价格式不正确");
        }
        if (dto.conditionLevel() != null && (dto.conditionLevel() < 1 || dto.conditionLevel() > 5)) {
            throw ApiException.badRequest("成色取值应为 1-5");
        }
        if (dto.stock() != null && (dto.stock() < 1 || dto.stock() > 99)) {
            throw ApiException.badRequest("库存应为 1-99");
        }
        if (dto.description() != null && dto.description().length() > 2000) {
            throw ApiException.badRequest("描述不能超过 2000 个字符");
        }
    }

    private Book toEntity(BookSaveDTO dto) {
        Book book = new Book();
        book.categoryId = dto.categoryId();
        book.title = dto.title() == null ? null : dto.title().trim();
        book.author = dto.author();
        book.publisher = dto.publisher();
        book.isbn = dto.isbn();
        book.edition = dto.edition();
        book.courseName = dto.courseName();
        book.conditionLevel = dto.conditionLevel() == null ? 3 : dto.conditionLevel();
        book.originalPrice = dto.originalPrice();
        book.price = dto.price();
        book.stock = dto.stock() == null ? 1 : dto.stock();
        book.coverUrl = dto.coverUrl();
        book.description = dto.description();
        return book;
    }
}
