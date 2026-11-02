package com.demo.university.vo;

import com.demo.university.model.BookStatus;
import com.demo.university.model.ConditionLevel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 教材视图：书籍字段 + 卖家信息 + 类目名 + 评分统计 + 是否已收藏 */
public class BookVO {

    public Long id;
    public Long sellerId;
    public String sellerNickname;
    public Integer sellerCredit;
    public Long categoryId;
    public String categoryName;
    public String title;
    public String author;
    public String publisher;
    public String isbn;
    public String edition;
    public String courseName;
    public Integer conditionLevel;
    public String conditionLabel;
    public BigDecimal originalPrice;
    public BigDecimal price;
    public Integer stock;
    public String coverUrl;
    public String description;
    public Integer status;
    public String statusLabel;
    public Integer viewCount;
    public LocalDateTime createdAt;
    public Double avgRating;
    public Long reviewCount;
    public Boolean favorite;

    public void applyLabels() {
        this.conditionLabel = ConditionLevel.labelOf(conditionLevel);
        this.statusLabel = BookStatus.labelOf(status);
        if (avgRating == null) {
            avgRating = 0.0;
        }
        if (reviewCount == null) {
            reviewCount = 0L;
        }
    }
}
