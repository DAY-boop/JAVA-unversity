package com.demo.university.model;

/** 用户角色 */
public enum Role {

    STUDENT(0, "学生"),
    ADMIN(1, "管理员"),
    SUPER_ADMIN(2, "超级管理员");

    public final int code;
    public final String label;

    Role(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static String labelOf(Integer code) {
        for (Role role : values()) {
            if (code != null && role.code == code) {
                return role.label;
            }
        }
        return "学生";
    }
}
