package com.demo.university.model;

import java.time.LocalDateTime;

/**
 * 用户实体（买家/卖家共用）。
 * 说明：实体与 VO 统一使用 public 字段以减少样板代码，Jackson 默认会序列化 public 字段；
 * 实体只在 Service/DAO 内部流转，不会直接返回给前端（对外用 UserVO 脱敏）。
 */
public class User {

    public Long id;
    public String username;
    public String password;
    public String salt;
    public String nickname;
    public String phone;
    public String campus;
    public String avatar;
    public Integer role;
    public Integer creditScore;
    public Integer status;
    public LocalDateTime createdAt;
}
