package com.demo.university.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单实体 */
public class Order {

    public Long id;
    public String orderNo;
    public Long buyerId;
    public Long sellerId;
    public BigDecimal totalAmount;
    public Integer status;
    public String tradePlace;
    public String contact;
    public String remark;
    public String cancelReason;
    public LocalDateTime createdAt;
    public LocalDateTime paidAt;
    public LocalDateTime deliveredAt;
    public LocalDateTime finishedAt;
}
