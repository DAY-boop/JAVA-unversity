package com.demo.university.service;

import com.demo.university.common.ApiException;
import com.demo.university.dao.UserDao;
import com.demo.university.dto.ProfileDTO;
import com.demo.university.model.User;
import com.demo.university.vo.UserVO;

/** 个人资料 */
public class UserService {

    private final UserDao userDao = new UserDao();

    public UserVO getById(long userId) {
        User user = userDao.findById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }
        return UserVO.of(user);
    }

    public UserVO updateProfile(long userId, ProfileDTO dto) {
        if (dto == null) {
            throw ApiException.badRequest("参数不能为空");
        }
        User user = userDao.findById(userId);
        if (user == null) {
            throw ApiException.notFound("用户不存在");
        }
        String nickname = dto.nickname() == null || dto.nickname().isBlank()
                ? user.nickname : dto.nickname().trim();
        if (nickname.length() > 32) {
            throw ApiException.badRequest("昵称不能超过 32 个字符");
        }
        userDao.updateProfile(userId, nickname, dto.phone(), dto.campus(),
                dto.avatar() == null ? user.avatar : dto.avatar());
        return UserVO.of(userDao.findById(userId));
    }
}
