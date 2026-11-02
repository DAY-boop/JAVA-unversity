package com.demo.university.model;

/**
 * 订单状态。只允许以下迁移，Service 层按白名单校验：
 * CREATED -> PAID -> DELIVERED -> COMPLETED
 * CREATED/PAID -> CANCELLED（取消时回滚库存）
 */
public enum OrderStatus {

    CREATED(0, "待付款"),
    PAID(1, "待交付"),
    DELIVERED(2, "待确认"),
    COMPLETED(3, "已完成"),
    CANCELLED(4, "已取消");

    public final int code;
    public final String label;

    OrderStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static String labelOf(Integer code) {
        for (OrderStatus status : values()) {
            if (code != null && status.code == code) {
                return status.label;
            }
        }
        return "未知";
    }

    public static OrderStatus of(Integer code) {
        for (OrderStatus status : values()) {
            if (code != null && status.code == code) {
                return status;
            }
        }
        return null;
    }
}
