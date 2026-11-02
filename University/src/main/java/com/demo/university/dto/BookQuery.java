package com.demo.university.dto;

import com.demo.university.common.PageQuery;
import jakarta.ws.rs.QueryParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * 教材检索条件。用 @BeanParam 自动绑定 query string，避免方法签名上挂十几个 @QueryParam。
 */
public class BookQuery {

    @QueryParam("q")
    public String q;

    @QueryParam("categoryId")
    public Long categoryId;

    @QueryParam("minPrice")
    public BigDecimal minPrice;

    @QueryParam("maxPrice")
    public BigDecimal maxPrice;

    @QueryParam("conditionLevel")
    public Integer conditionLevel;

    /** latest | priceAsc | priceDesc | hot */
    @QueryParam("sort")
    public String sort = "latest";

    @QueryParam("page")
    public Integer page = 1;

    @QueryParam("size")
    public Integer size = 12;

    /** 管理后台用：按状态筛选 */
    @QueryParam("status")
    public Integer status;

    /** 含一级类目下所有子类目，由 Service 填充 */
    public List<Long> categoryIds;

    /** 卖家视角（我发布的） */
    public Long sellerId;

    /** true 时不过滤 status=1（后台/我的发布） */
    public boolean includeAllStatus;

    public PageQuery pageQuery() {
        return PageQuery.of(page, size);
    }
}
