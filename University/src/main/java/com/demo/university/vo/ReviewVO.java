package com.demo.university.vo;

import java.time.LocalDateTime;

/** 评价视图 */
public class ReviewVO {

    public Long id;
    public Long orderId;
    public Long bookId;
    public String bookTitle;
    public Long reviewerId;
    public String reviewerNickname;
    public Long revieweeId;
    public String revieweeNickname;
    public Integer rating;
    public String content;
    public String tags;
    public LocalDateTime createdAt;
}
