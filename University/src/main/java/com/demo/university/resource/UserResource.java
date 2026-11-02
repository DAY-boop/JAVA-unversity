package com.demo.university.resource;

import com.demo.university.common.Result;
import com.demo.university.config.AppContext;
import com.demo.university.config.AuthUtil;
import com.demo.university.dto.ChangePasswordDTO;
import com.demo.university.dto.ProfileDTO;
import com.demo.university.service.AuthService;
import com.demo.university.service.UserService;
import com.demo.university.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/** 个人资料 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    private final UserService userService = AppContext.user();
    private final AuthService authService = AppContext.auth();

    @GET
    @Path("/{id}")
    public Result<UserVO> detail(@PathParam("id") long id) {
        return Result.ok(userService.getById(id));
    }

    @PUT
    @Path("/me")
    public Result<UserVO> updateProfile(ProfileDTO dto, @Context HttpServletRequest request) {
        long userId = AuthUtil.requireUserId(request);
        return Result.ok(userService.updateProfile(userId, dto));
    }

    @PUT
    @Path("/me/password")
    public Result<Void> changePassword(ChangePasswordDTO dto, @Context HttpServletRequest request) {
        long userId = AuthUtil.requireUserId(request);
        authService.changePassword(userId, dto);
        return Result.ok();
    }
}
