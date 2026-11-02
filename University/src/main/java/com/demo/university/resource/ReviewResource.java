package com.demo.university.resource;

import com.demo.university.common.ApiException;
import com.demo.university.common.Result;
import com.demo.university.config.AppContext;
import com.demo.university.config.AuthUtil;
import com.demo.university.dto.ReviewCreateDTO;
import com.demo.university.service.ReviewService;
import com.demo.university.vo.ReviewVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 评价 */
@Path("/reviews")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ReviewResource {

    private final ReviewService reviewService = AppContext.review();

    @POST
    public Result<Map<String, Object>> create(ReviewCreateDTO dto, @Context HttpServletRequest request) {
        long userId = AuthUtil.requireUserId(request);
        Map<String, Object> data = new HashMap<>();
        data.put("id", reviewService.create(userId, dto));
        return Result.ok(data);
    }

    /** 教材详情页的评价列表，公开访问 */
    @GET
    public Result<List<ReviewVO>> byBook(@QueryParam("bookId") Long bookId) {
        if (bookId == null) {
            throw ApiException.badRequest("缺少 bookId");
        }
        return Result.ok(reviewService.findByBook(bookId));
    }

    /** 我收到的评价 */
    @GET
    @Path("/mine")
    public Result<List<ReviewVO>> mine(@Context HttpServletRequest request) {
        long userId = AuthUtil.requireUserId(request);
        return Result.ok(reviewService.received(userId));
    }
}
