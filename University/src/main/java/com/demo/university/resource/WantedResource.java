package com.demo.university.resource;

import com.demo.university.common.ApiException;
import com.demo.university.common.PageResult;
import com.demo.university.common.Result;
import com.demo.university.config.AppContext;
import com.demo.university.config.AuthUtil;
import com.demo.university.dto.StatusDTO;
import com.demo.university.dto.WantedQuery;
import com.demo.university.dto.WantedSaveDTO;
import com.demo.university.service.WantedBookService;
import com.demo.university.vo.WantedBookVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.Map;

/** 教材求购 */
@Path("/wanted")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WantedResource {

    private final WantedBookService wantedBookService = AppContext.wanted();

    /** 公开浏览：未登录时联系方式置空 */
    @GET
    public Result<PageResult<WantedBookVO>> list(@BeanParam WantedQuery query,
                                                 @Context HttpServletRequest request) {
        return Result.ok(wantedBookService.search(query, AuthUtil.userId(request)));
    }

    @GET
    @Path("/mine")
    public Result<PageResult<WantedBookVO>> mine(@QueryParam("page") Integer page,
                                                 @QueryParam("size") Integer size,
                                                 @Context HttpServletRequest request) {
        long userId = AuthUtil.requireUserId(request);
        return Result.ok(wantedBookService.myList(userId, page, size));
    }

    @GET
    @Path("/{id}")
    public Result<WantedBookVO> detail(@PathParam("id") long id, @Context HttpServletRequest request) {
        return Result.ok(wantedBookService.detail(id, AuthUtil.userId(request)));
    }

    @POST
    public Result<Map<String, Object>> create(WantedSaveDTO dto, @Context HttpServletRequest request) {
        long userId = AuthUtil.requireUserId(request);
        Map<String, Object> data = new HashMap<>();
        data.put("id", wantedBookService.create(userId, dto));
        return Result.ok(data);
    }

    @PUT
    @Path("/{id}")
    public Result<Void> update(@PathParam("id") long id, WantedSaveDTO dto,
                               @Context HttpServletRequest request) {
        wantedBookService.update(AuthUtil.requireUserId(request), id, dto);
        return Result.ok();
    }

    /** body {"status": 0|1|2} */
    @PUT
    @Path("/{id}/status")
    public Result<Void> changeStatus(@PathParam("id") long id, StatusDTO dto,
                                     @Context HttpServletRequest request) {
        if (dto == null || dto.status() == null) {
            throw ApiException.badRequest("状态不能为空");
        }
        wantedBookService.changeStatus(AuthUtil.requireUserId(request), id, dto.status());
        return Result.ok();
    }
}
