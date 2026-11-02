package com.demo.university.model;

import java.time.LocalDateTime;

/** 评价实体（买卖双方互评） */
public class Review {

    public Long id;
    public Long orderId;
    public Long bookId;
    public Long reviewerId;
    public Long revieweeId;
    public Integer rating;
    public String content;
    public String tags;
    public LocalDateTime createdAt;
}
