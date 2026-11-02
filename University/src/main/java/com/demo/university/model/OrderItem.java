package com.demo.university.model;

import java.math.BigDecimal;

/** 订单明细：保存下单瞬间的书名/价格/封面快照 */
public class OrderItem {

    public Long id;
    public Long orderId;
    public Long bookId;
    public String titleSnapshot;
    public String coverSnapshot;
    public BigDecimal priceSnapshot;
    public Integer quantity;
    public BigDecimal subtotal;
}
