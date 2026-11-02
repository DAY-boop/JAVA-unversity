package com.demo.university.dto;

/** 修改密码入参 */
public record ChangePasswordDTO(String oldPassword, String newPassword) {
}
