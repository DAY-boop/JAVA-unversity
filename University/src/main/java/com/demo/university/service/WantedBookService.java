package com.demo.university.service;

import com.demo.university.common.ApiException;
import com.demo.university.common.PageResult;
import com.demo.university.dao.WantedBookDao;
import com.demo.university.dto.WantedQuery;
import com.demo.university.dto.WantedSaveDTO;
import com.demo.university.model.WantedBook;
import com.demo.university.model.WantedStatus;
import com.demo.university.vo.WantedBookVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 教材求购：买家挂需求，卖家看到后线下联系成交。
 * 联系方式只对登录用户返回，未登录只能看到"登录后可见"。
 */
public class WantedBookService {

    private final WantedBookDao wantedBookDao = new WantedBookDao();
    private final CategoryService categoryService = new CategoryService();

    public PageResult<WantedBookVO> search(WantedQuery query, Long viewerId) {
        if (query.categoryId != null) {
            query.categoryIds = categoryService.descendantIds(query.categoryId);
        }
        List<WantedBookVO> list = wantedBookDao.search(query);
        list.forEach(vo -> maskContact(vo, viewerId));
        return new PageResult<>(list, wantedBookDao.count(query), query.pageQuery());
    }

    public WantedBookVO detail(long id, Long viewerId) {
        WantedBookVO vo = wantedBookDao.findVOById(id);
        if (vo == null) {
            throw ApiException.notFound("求购信息不存在或已删除");
        }
        maskContact(vo, viewerId);
        return vo;
    }

    public PageResult<WantedBookVO> myList(long userId, Integer page, Integer size) {
        WantedQuery query = new WantedQuery();
        query.userId = userId;
        query.includeAllStatus = true;
        query.page = page;
        query.size = size;
        List<WantedBookVO> list = wantedBookDao.search(query);
        return new PageResult<>(list, wantedBookDao.count(query), query.pageQuery());
    }

    public long create(long userId, WantedSaveDTO dto) {
        validate(dto);
        WantedBook book = toEntity(dto);
        book.userId = userId;
        book.status = WantedStatus.OPEN.code;
        book.createdAt = LocalDateTime.now();
        book.updatedAt = book.createdAt;
        return wantedBookDao.insert(book);
    }

    public void update(long userId, long id, WantedSaveDTO dto) {
        validate(dto);
        WantedBook existing = requireOwned(userId, id);
        WantedBook book = toEntity(dto);
        book.id = id;
        book.userId = userId;
        book.updatedAt = LocalDateTime.now();
        if (wantedBookDao.update(book) == 0) {
            throw ApiException.forbidden("只能修改自己发布的求购");
        }
        // 已买到/已关闭的求购重新编辑后自动回到求购中
        if (existing.status != null && existing.status != WantedStatus.OPEN.code) {
            wantedBookDao.updateStatus(id, userId, WantedStatus.OPEN.code);
        }
    }

    /** status: 0 求购中 / 1 已买到 / 2 已关闭 */
    public void changeStatus(long userId, long id, int status) {
        if (WantedStatus.OPEN.code != status && WantedStatus.DONE.code != status
                && WantedStatus.CLOSED.code != status) {
            throw ApiException.badRequest("不支持的状态值");
        }
        requireOwned(userId, id);
        if (wantedBookDao.updateStatus(id, userId, status) == 0) {
            throw ApiException.forbidden("只能操作自己发布的求购");
        }
    }

    public long countAll() {
        return wantedBookDao.countAll();
    }

    private void maskContact(WantedBookVO vo, Long viewerId) {
        boolean visible = viewerId != null;
        vo.contactVisible = visible;
        if (!visible) {
            vo.contact = null;
        }
    }

    private WantedBook requireOwned(long userId, long id) {
        WantedBook book = wantedBookDao.findEntityById(id);
        if (book == null) {
            throw ApiException.notFound("求购信息不存在");
        }
        if (book.userId == null || book.userId != userId) {
            throw ApiException.forbidden("只能操作自己发布的求购");
        }
        return book;
    }

    private void validate(WantedSaveDTO dto) {
        if (dto == null || dto.title() == null || dto.title().isBlank()) {
            throw ApiException.badRequest("请填写想要求购的书名");
        }
        if (dto.title().length() > 120) {
            throw ApiException.badRequest("书名不能超过 120 个字符");
        }
        if (dto.expectedPrice() != null
                && (dto.expectedPrice().compareTo(BigDecimal.ZERO) < 0
                || dto.expectedPrice().compareTo(new BigDecimal("100000")) > 0)) {
            throw ApiException.badRequest("期望价格式不正确");
        }
        if (dto.conditionLevel() != null && (dto.conditionLevel() < 1 || dto.conditionLevel() > 5)) {
            throw ApiException.badRequest("成色要求取值应为 1-5");
        }
        if (dto.description() != null && dto.description().length() > 1000) {
            throw ApiException.badRequest("补充说明不能超过 1000 个字符");
        }
        if (dto.contact() != null && dto.contact().length() > 64) {
            throw ApiException.badRequest("联系方式过长");
        }
    }

    private WantedBook toEntity(WantedSaveDTO dto) {
        WantedBook book = new WantedBook();
        book.categoryId = dto.categoryId();
        book.title = dto.title() == null ? null : dto.title().trim();
        book.author = dto.author();
        book.publisher = dto.publisher();
        book.courseName = dto.courseName();
        book.expectedPrice = dto.expectedPrice();
        book.conditionLevel = dto.conditionLevel();
        book.description = dto.description();
        book.contact = dto.contact();
        book.campus = dto.campus();
        return book;
    }
}
