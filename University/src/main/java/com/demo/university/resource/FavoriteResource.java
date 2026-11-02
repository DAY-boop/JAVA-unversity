package com.demo.university.resource;

import com.demo.university.common.PageResult;
import com.demo.university.common.Result;
import com.demo.university.config.AppContext;
import com.demo.university.config.AuthUtil;
import com.demo.university.service.FavoriteService;
import com.demo.university.vo.BookVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

/** 收藏 */
@Path("/favorites")
@Produces(MediaType.APPLICATION_JSON)
public class FavoriteResource {

    private final FavoriteService favoriteService = AppContext.favorite();

    @GET
    public Result<PageResult<BookVO>> list(@QueryParam("page") Integer page,
                                           @QueryParam("size") Integer size,
                                           @Context HttpServletRequest request) {
        long userId = AuthUtil.requireUserId(request);
        return Result.ok(favoriteService.list(userId, page, size));
    }

    @POST
    @Path("/{bookId}")
    public Result<Void> add(@PathParam("bookId") long bookId, @Context HttpServletRequest request) {
        favoriteService.add(AuthUtil.requireUserId(request), bookId);
        return Result.ok();
    }

    @DELETE
    @Path("/{bookId}")
    public Result<Void> remove(@PathParam("bookId") long bookId, @Context HttpServletRequest request) {
        favoriteService.remove(AuthUtil.requireUserId(request), bookId);
        return Result.ok();
    }
}
