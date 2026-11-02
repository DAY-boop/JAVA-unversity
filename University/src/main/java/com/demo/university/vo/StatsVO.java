package com.demo.university.vo;

import java.math.BigDecimal;

/** 后台统计卡片数据 */
public class StatsVO {

    public Long userCount;
    public Long bookCount;
    public Long onSaleBookCount;
    public Long orderCount;
    public Long completedOrderCount;
    public Long reviewCount;
    public BigDecimal completedAmount;
}
