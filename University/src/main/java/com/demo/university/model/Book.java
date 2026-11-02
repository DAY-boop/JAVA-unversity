package com.demo.university.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 教材实体 */
public class Book {

    public Long id;
    public Long sellerId;
    public Long categoryId;
    public String title;
    public String author;
    public String publisher;
    public String isbn;
    public String edition;
    public String courseName;
    public Integer conditionLevel;
    public BigDecimal originalPrice;
    public BigDecimal price;
    public Integer stock;
    public String coverUrl;
    public String description;
    public Integer status;
    public Integer viewCount;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
