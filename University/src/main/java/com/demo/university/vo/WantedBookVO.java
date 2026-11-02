package com.demo.university.vo;

import com.demo.university.model.ConditionLevel;
import com.demo.university.model.WantedStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 求购视图：联系方式只对登录用户可见 */
public class WantedBookVO {

    public Long id;
    public Long userId;
    public String userNickname;
    public Long categoryId;
    public String categoryName;
    public String title;
    public String author;
    public String publisher;
    public String courseName;
    public BigDecimal expectedPrice;
    public Integer conditionLevel;
    public String conditionLabel;
    public String description;
    public String contact;
    public Boolean contactVisible;
    public String campus;
    public Integer status;
    public String statusLabel;
    public LocalDateTime createdAt;

    public void applyLabels() {
        this.statusLabel = WantedStatus.labelOf(status);
        this.conditionLabel = conditionLevel == null ? null : ConditionLevel.labelOf(conditionLevel);
        if (contactVisible == null) {
            contactVisible = contact != null;
        }
    }
}
