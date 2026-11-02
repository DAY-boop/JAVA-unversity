package com.demo.university.resource;

import com.demo.university.common.Result;
import com.demo.university.config.AppContext;
import com.demo.university.config.AuthUtil;
import com.demo.university.dto.LoginDTO;
import com.demo.university.dto.RegisterDTO;
import com.demo.university.model.User;
import com.demo.university.service.AuthService;
import com.demo.university.service.UserService;
import com.demo.university.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/** 注册 / 登录 / 退出 / 当前用户 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService authService = AppContext.auth();
    private final UserService userService = AppContext.user();

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Result<UserVO> register(RegisterDTO dto, @Context HttpServletRequest request) {
        UserVO vo = authService.register(dto);
        // 注册即登录，省掉一次手动登录
        User user = authService.login(new LoginDTO(dto.username(), dto.password()));
        AuthUtil.login(request, user);
        return Result.ok(vo);
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    public Result<UserVO> login(LoginDTO dto, @Context HttpServletRequest request) {
        User user = authService.login(dto);
        AuthUtil.login(request, user);
        return Result.ok(UserVO.of(user));
    }

    @POST
    @Path("/logout")
    public Result<Void> logout(@Context HttpServletRequest request) {
        AuthUtil.logout(request);
        return Result.ok();
    }

    /** 未登录时返回 data 为空的成功响应，前端据此渲染未登录状态 */
    @GET
    @Path("/me")
    public Result<UserVO> me(@Context HttpServletRequest request) {
        Long userId = AuthUtil.userId(request);
        if (userId == null) {
            return Result.ok(null);
        }
        return Result.ok(userService.getById(userId));
    }
}
