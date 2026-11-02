package com.demo.university.dto;

/** 下单入参：一个订单只买一本教材（校园面交场景，够用且简单） */
public record OrderCreateDTO(Long bookId, Integer quantity, String tradePlace, String contact, String remark) {
}
