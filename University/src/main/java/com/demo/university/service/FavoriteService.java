package com.demo.university.service;

import com.demo.university.common.ApiException;
import com.demo.university.common.PageQuery;
import com.demo.university.common.PageResult;
import com.demo.university.dao.BookDao;
import com.demo.university.dao.FavoriteDao;
import com.demo.university.vo.BookVO;

import java.util.List;

/** 收藏 */
public class FavoriteService {

    private final FavoriteDao favoriteDao = new FavoriteDao();
    private final BookDao bookDao = new BookDao();

    public void add(long userId, long bookId) {
        if (bookDao.findEntityById(bookId) == null) {
            throw ApiException.notFound("教材不存在");
        }
        if (favoriteDao.exists(userId, bookId)) {
            return;
        }
        favoriteDao.insert(userId, bookId);
    }

    public void remove(long userId, long bookId) {
        favoriteDao.delete(userId, bookId);
    }

    public PageResult<BookVO> list(long userId, Integer page, Integer size) {
        PageQuery pageQuery = PageQuery.of(page, size);
        List<BookVO> list = favoriteDao.findByUser(userId, pageQuery);
        list.forEach(vo -> vo.favorite = true);
        long total = favoriteDao.countByUser(userId);
        return new PageResult<>(list, total, pageQuery);
    }
}
