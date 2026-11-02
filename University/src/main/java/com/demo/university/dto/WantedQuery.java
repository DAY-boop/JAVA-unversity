package com.demo.university.dto;

import com.demo.university.common.PageQuery;
import jakarta.ws.rs.QueryParam;

import java.util.List;

/** 求购检索条件 */
public class WantedQuery {

    @QueryParam("q")
    public String q;

    @QueryParam("categoryId")
    public Long categoryId;

    /** 0 求购中 / 1 已买到 / 2 已关闭；不传默认只看求购中 */
    @QueryParam("status")
    public Integer status;

    @QueryParam("page")
    public Integer page = 1;

    @QueryParam("size")
    public Integer size = 12;

    /** 由 Service 填充：选中一级类目时带上子类目 */
    public List<Long> categoryIds;

    public Long userId;

    public boolean includeAllStatus;

    public PageQuery pageQuery() {
        return PageQuery.of(page, size);
    }
}
