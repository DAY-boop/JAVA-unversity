package com.demo.university.vo;

import com.demo.university.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 订单视图：订单 + 教材快照 + 买卖双方昵称 + 是否已评价 */
public class OrderVO {

    public Long id;
    public String orderNo;
    public Long buyerId;
    public String buyerNickname;
    public Long sellerId;
    public String sellerNickname;
    public BigDecimal totalAmount;
    public Integer status;
    public String statusLabel;
    public Long bookId;
    public String bookTitle;
    public String bookCover;
    public BigDecimal price;
    public Integer quantity;
    public String tradePlace;
    public String contact;
    public String remark;
    public String cancelReason;
    public LocalDateTime createdAt;
    public LocalDateTime paidAt;
    public LocalDateTime deliveredAt;
    public LocalDateTime finishedAt;
    public Boolean reviewed;
    public Boolean canReview;

    public void applyLabels() {
        this.statusLabel = OrderStatus.labelOf(status);
        if (reviewed == null) {
            reviewed = false;
        }
        this.canReview = status != null && status == OrderStatus.COMPLETED.code && !reviewed;
    }
}
