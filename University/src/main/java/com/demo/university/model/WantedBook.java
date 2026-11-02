package com.demo.university.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 求购信息实体：买家发布"我想买这本书"，卖家看到后可主动联系 */
public class WantedBook {

    public Long id;
    public Long userId;
    public Long categoryId;
    public String title;
    public String author;
    public String publisher;
    public String courseName;
    public BigDecimal expectedPrice;
    public Integer conditionLevel;
    public String description;
    public String contact;
    public String campus;
    public Integer status;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
