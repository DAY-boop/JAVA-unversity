package com.demo.university.dto;

/** 评价入参 */
public record ReviewCreateDTO(Long orderId, Integer rating, String content, String tags) {
}
