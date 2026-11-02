package com.demo.university.resource;

import com.demo.university.common.ApiException;
import com.demo.university.common.PageResult;
import com.demo.university.common.Result;
import com.demo.university.config.AppContext;
import com.demo.university.config.AuthUtil;
import com.demo.university.dto.BookQuery;
import com.demo.university.dto.BookSaveDTO;
import com.demo.university.dto.StatusDTO;
import com.demo.university.model.BookStatus;
import com.demo.university.service.BookService;
import com.demo.university.vo.BookVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
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

/** 教材发布与检索 */
@Path("/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BookResource {

    private final BookService bookService = AppContext.book();

    /** 公开检索：关键词 / 类目 / 价格区间 / 成色 / 排序 / 分页 */
    @GET
    public Result<PageResult<BookVO>> list(@BeanParam BookQuery query) {
        return Result.ok(bookService.search(query));
    }

    @GET
    @Path("/mine")
    public Result<PageResult<BookVO>> mine(@QueryParam("page") Integer page,
                                           @QueryParam("size") Integer size,
                                           @Context HttpServletRequest request) {
        long userId = AuthUtil.requireUserId(request);
        return Result.ok(bookService.myBooks(userId, page, size));
    }

    @GET
    @Path("/{id}")
    public Result<BookVO> detail(@PathParam("id") long id, @Context HttpServletRequest request) {
        return Result.ok(bookService.detail(id, AuthUtil.userId(request)));
    }

    @POST
    public Result<Map<String, Object>> create(BookSaveDTO dto, @Context HttpServletRequest request) {
        long userId = AuthUtil.requireUserId(request);
        Map<String, Object> data = new HashMap<>();
        data.put("id", bookService.publish(userId, dto));
        return Result.ok(data);
    }

    @PUT
    @Path("/{id}")
    public Result<Void> update(@PathParam("id") long id, BookSaveDTO dto,
                               @Context HttpServletRequest request) {
        bookService.update(AuthUtil.requireUserId(request), id, dto);
        return Result.ok();
    }

    /** 卖家上下架：body {"status": 1|2} */
    @PUT
    @Path("/{id}/status")
    public Result<Void> changeStatus(@PathParam("id") long id, StatusDTO dto,
                                     @Context HttpServletRequest request) {
        if (dto == null || dto.status() == null) {
            throw ApiException.badRequest("状态不能为空");
        }
        bookService.changeStatus(AuthUtil.requireUserId(request), id, dto.status());
        return Result.ok();
    }

    /** 下架（软删除），历史订单不受影响 */
    @DELETE
    @Path("/{id}")
    public Result<Void> offline(@PathParam("id") long id, @Context HttpServletRequest request) {
        bookService.changeStatus(AuthUtil.requireUserId(request), id, BookStatus.OFFLINE.code);
        return Result.ok();
    }
}
