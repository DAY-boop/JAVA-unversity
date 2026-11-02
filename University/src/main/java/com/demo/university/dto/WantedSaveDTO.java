package com.demo.university.dto;

import java.math.BigDecimal;

/** 发布/编辑求购入参 */
public record WantedSaveDTO(String title, String author, String publisher, String courseName,
                            Long categoryId, BigDecimal expectedPrice, Integer conditionLevel,
                            String description, String contact, String campus) {
}
