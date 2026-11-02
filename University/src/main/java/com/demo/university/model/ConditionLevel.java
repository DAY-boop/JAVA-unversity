package com.demo.university.model;

/** 成色等级：数字越大书况越差 */
public enum ConditionLevel {

    L1(1, "九五新"),
    L2(2, "九成新"),
    L3(3, "八成新"),
    L4(4, "七成新"),
    L5(5, "六成新及以下");

    public final int code;
    public final String label;

    ConditionLevel(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static String labelOf(Integer code) {
        for (ConditionLevel level : values()) {
            if (code != null && level.code == code) {
                return level.label;
            }
        }
        return "未知";
    }
}
