package com.demo.university.vo;

import com.demo.university.model.Role;
import com.demo.university.model.User;

import java.time.LocalDateTime;

/** 用户对外视图：不包含 password / salt */
public class UserVO {

    public Long id;
    public String username;
    public String nickname;
    public String phone;
    public String campus;
    public String avatar;
    public Integer role;
    public String roleLabel;
    public Integer creditScore;
    public Integer status;
    public String statusLabel;
    public LocalDateTime createdAt;

    public static UserVO of(User user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.id = user.id;
        vo.username = user.username;
        vo.nickname = user.nickname;
        vo.phone = user.phone;
        vo.campus = user.campus;
        vo.avatar = user.avatar;
        vo.role = user.role;
        vo.roleLabel = Role.labelOf(user.role);
        vo.creditScore = user.creditScore;
        vo.status = user.status;
        vo.statusLabel = user.status != null && user.status == 1 ? "已封禁" : "正常";
        vo.createdAt = user.createdAt;
        return vo;
    }
}
