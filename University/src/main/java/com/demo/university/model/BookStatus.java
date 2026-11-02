package com.demo.university.model;

/** 教材状态 */
public enum BookStatus {

    PENDING(0, "待审核"),
    ON_SALE(1, "在售"),
    OFFLINE(2, "已下架"),
    SOLD_OUT(3, "已售出");

    public final int code;
    public final String label;

    BookStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static String labelOf(Integer code) {
        for (BookStatus status : values()) {
            if (code != null && status.code == code) {
                return status.label;
            }
        }
        return "未知";
    }
}
