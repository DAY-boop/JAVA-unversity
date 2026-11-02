package com.demo.university.service;

import com.demo.university.common.ApiException;
import com.demo.university.common.PasswordUtil;
import com.demo.university.dao.UserDao;
import com.demo.university.dto.ChangePasswordDTO;
import com.demo.university.dto.LoginDTO;
import com.demo.university.dto.RegisterDTO;
import com.demo.university.model.Role;
import com.demo.university.model.User;
import com.demo.university.vo.UserVO;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/** 注册、登录、改密 */
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{4,20}$");

    private final UserDao userDao = new UserDao();

    public UserVO register(RegisterDTO dto) {
        if (dto == null || dto.username() == null || dto.password() == null) {
            throw ApiException.badRequest("用户名和密码不能为空");
        }
        String username = dto.username().trim();
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw ApiException.badRequest("用户名需为 4-20 位字母、数字或下划线");
        }
        if (dto.password().length() < 6) {
            throw ApiException.badRequest("密码至少 6 位");
        }
        String nickname = dto.nickname() == null || dto.nickname().isBlank()
                ? username : dto.nickname().trim();
        if (nickname.length() > 32) {
            throw ApiException.badRequest("昵称不能超过 32 个字符");
        }
        if (userDao.findByUsername(username) != null) {
            throw ApiException.conflict("用户名已被占用");
        }

        String salt = PasswordUtil.randomSalt();
        User user = new User();
        user.username = username;
        user.salt = salt;
        user.password = PasswordUtil.hash(dto.password(), salt);
        user.nickname = nickname;
        user.phone = dto.phone();
        user.campus = dto.campus();
        user.role = Role.STUDENT.code;
        user.creditScore = 100;
        user.status = 0;
        user.createdAt = LocalDateTime.now();

        long id = userDao.insert(user);
        return UserVO.of(userDao.findById(id));
    }

    /** 登录成功返回完整用户（含角色），由 Resource 写入 Session */
    public User login(LoginDTO dto) {
        if (dto == null || dto.username() == null || dto.password() == null) {
            throw ApiException.badRequest("用户名和密码不能为空");
        }
        User user = userDao.findByUsername(dto.username().trim());
        if (user == null || !PasswordUtil.matches(dto.password(), user.salt, user.password)) {
            throw ApiException.unauthorized("用户名或密码错误");
        }
        if (user.status != null && user.status == 1) {
            throw ApiException.forbidden("账号已被封禁，请联系管理员");
        }
        return user;
    }

    public void changePassword(long userId, ChangePasswordDTO dto) {
        if (dto == null || dto.oldPassword() == null || dto.newPassword() == null) {
            throw ApiException.badRequest("原密码和新密码不能为空");
        }
        if (dto.newPassword().length() < 6) {
            throw ApiException.badRequest("新密码至少 6 位");
        }
        User user = userDao.findById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }
        if (!PasswordUtil.matches(dto.oldPassword(), user.salt, user.password)) {
            throw ApiException.badRequest("原密码不正确");
        }
        String salt = PasswordUtil.randomSalt();
        userDao.updatePassword(userId, PasswordUtil.hash(dto.newPassword(), salt), salt);
    }
}
