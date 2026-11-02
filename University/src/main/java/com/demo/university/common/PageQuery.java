package com.demo.university.common;

/**
 * 分页参数归一化：防止前端传 page=0、size=99999 造成全表扫描。
 */
public class PageQuery {

    private static final int MAX_SIZE = 50;
    private static final int DEFAULT_SIZE = 10;

    private int page = 1;
    private int size = DEFAULT_SIZE;

    public PageQuery() {
    }

    public PageQuery(Integer page, Integer size) {
        this.page = (page == null || page < 1) ? 1 : page;
        this.size = (size == null || size < 1) ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
    }

    public static PageQuery of(Integer page, Integer size) {
        return new PageQuery(page, size);
    }

    /** 供 SQL LIMIT ?, ? 使用 */
    public int getOffset() {
        return (page - 1) * size;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}
