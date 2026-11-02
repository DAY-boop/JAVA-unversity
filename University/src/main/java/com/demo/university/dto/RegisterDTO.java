package com.demo.university.dto;

/** 注册入参 */
public record RegisterDTO(String username, String password, String nickname, String phone, String campus) {
}
