package com.demo.university.dto;

import java.math.BigDecimal;

/** 发布/编辑教材入参 */
public record BookSaveDTO(String title, String author, String publisher, String isbn, String edition,
                          String courseName, Long categoryId, Integer conditionLevel,
                          BigDecimal originalPrice, BigDecimal price, Integer stock,
                          String coverUrl, String description) {
}
