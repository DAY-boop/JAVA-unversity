package com.demo.university.resource;

import com.demo.university.common.ApiException;
import com.demo.university.common.PageResult;
import com.demo.university.common.Result;
import com.demo.university.config.AppContext;
import com.demo.university.config.AuthUtil;
import com.demo.university.dto.StatusDTO;
import com.demo.university.model.Role;
import com.demo.university.service.AdminService;
import com.demo.university.vo.BookVO;
import com.demo.university.vo.OrderVO;
import com.demo.university.vo.StatsVO;
import com.demo.university.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/** 后台管理（AuthFilter 已统一做管理员校验） */
@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminResource {

    private final AdminService adminService = AppContext.admin();

    @GET
    @Path("/stats")
    public Result<StatsVO> stats(@Context HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return Result.ok(adminService.stats());
    }

    @GET
    @Path("/users")
    public Result<PageResult<UserVO>> users(@QueryParam("q") String keyword,
                                            @QueryParam("page") Integer page,
                                            @QueryParam("size") Integer size,
                                            @Context HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return Result.ok(adminService.users(keyword, page, size));
    }

    @PUT
    @Path("/users/{id}/status")
    public Result<Void> setUserStatus(@PathParam("id") long id, StatusDTO dto,
                                      @Context HttpServletRequest request) {
        long operatorId = AuthUtil.requireUserId(request);
        if (dto == null || dto.status() == null) {
            throw ApiException.badRequest("状态不能为空");
        }
        adminService.setUserStatus(operatorId, id, dto.status());
        return Result.ok();
    }

    @GET
    @Path("/books")
    public Result<PageResult<BookVO>> books(@QueryParam("q") String keyword,
                                            @QueryParam("status") Integer status,
                                            @QueryParam("page") Integer page,
                                            @QueryParam("size") Integer size,
                                            @Context HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return Result.ok(adminService.books(keyword, status, page, size));
    }

    @PUT
    @Path("/books/{id}/status")
    public Result<Void> setBookStatus(@PathParam("id") long id, StatusDTO dto,
                                      @Context HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        if (dto == null || dto.status() == null) {
            throw ApiException.badRequest("状态不能为空");
        }
        adminService.setBookStatus(id, dto.status());
        return Result.ok();
    }

    @GET
    @Path("/orders")
    public Result<PageResult<OrderVO>> orders(@QueryParam("q") String keyword,
                                              @QueryParam("status") Integer status,
                                              @QueryParam("page") Integer page,
                                              @QueryParam("size") Integer size,
                                              @Context HttpServletRequest request) {
        AuthUtil.requireAdmin(request);
        return Result.ok(adminService.orders(keyword, status, page, size));
    }

    /** 提升用户为管理员（仅超级管理员可操作） */
    @PUT
    @Path("/users/{id}/promote")
    public Result<UserVO> promoteToAdmin(@PathParam("id") long userId,
                                         @Context HttpServletRequest request) {
        AuthUtil.requireSuperAdmin(request);
        return Result.ok(adminService.promoteToAdmin(userId));
    }

    /** 降级管理员为普通用户（仅超级管理员可操作） */
    @PUT
    @Path("/users/{id}/demote")
    public Result<UserVO> demoteFromAdmin(@PathParam("id") long userId,
                                          @Context HttpServletRequest request) {
        AuthUtil.requireSuperAdmin(request);
        return Result.ok(adminService.demoteFromAdmin(userId));
    }
}
