package com.demo.university.model;

/** 求购状态 */
public enum WantedStatus {

    OPEN(0, "求购中"),
    DONE(1, "已买到"),
    CLOSED(2, "已关闭");

    public final int code;
    public final String label;

    WantedStatus(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static String labelOf(Integer code) {
        for (WantedStatus status : values()) {
            if (code != null && status.code == code) {
                return status.label;
            }
        }
        return "未知";
    }
}
