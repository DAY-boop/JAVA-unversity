package com.demo.university.resource;

import com.demo.university.common.PageResult;
import com.demo.university.common.Result;
import com.demo.university.config.AppContext;
import com.demo.university.config.AuthUtil;
import com.demo.university.dto.CancelDTO;
import com.demo.university.dto.OrderCreateDTO;
import com.demo.university.service.OrderService;
import com.demo.university.vo.OrderVO;
import jakarta.servlet.http.HttpServletRequest;
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

/** 下单与订单流转 */
@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    private final OrderService orderService = AppContext.order();

    @POST
    public Result<Map<String, Object>> create(OrderCreateDTO dto, @Context HttpServletRequest request) {
        long userId = AuthUtil.requireUserId(request);
        Map<String, Object> data = new HashMap<>();
        data.put("id", orderService.create(userId, dto));
        return Result.ok(data);
    }

    /** role=buyer（我买到的，默认）/ role=seller（我卖出的） */
    @GET
    public Result<PageResult<OrderVO>> list(@QueryParam("role") String role,
                                            @QueryParam("status") Integer status,
                                            @QueryParam("page") Integer page,
                                            @QueryParam("size") Integer size,
                                            @Context HttpServletRequest request) {
        long userId = AuthUtil.requireUserId(request);
        return Result.ok(orderService.list(userId, role, status, page, size));
    }

    @GET
    @Path("/{id}")
    public Result<OrderVO> detail(@PathParam("id") long id, @Context HttpServletRequest request) {
        long userId = AuthUtil.requireUserId(request);
        return Result.ok(orderService.detail(userId, id, AuthUtil.isAdmin(request)));
    }

    @PUT
    @Path("/{id}/pay")
    public Result<Void> pay(@PathParam("id") long id, @Context HttpServletRequest request) {
        orderService.pay(AuthUtil.requireUserId(request), id);
        return Result.ok();
    }

    @PUT
    @Path("/{id}/deliver")
    public Result<Void> deliver(@PathParam("id") long id, @Context HttpServletRequest request) {
        orderService.deliver(AuthUtil.requireUserId(request), id);
        return Result.ok();
    }

    @PUT
    @Path("/{id}/confirm")
    public Result<Void> confirm(@PathParam("id") long id, @Context HttpServletRequest request) {
        orderService.confirm(AuthUtil.requireUserId(request), id);
        return Result.ok();
    }

    @PUT
    @Path("/{id}/cancel")
    public Result<Void> cancel(@PathParam("id") long id, CancelDTO dto,
                               @Context HttpServletRequest request) {
        orderService.cancel(AuthUtil.requireUserId(request), id, dto == null ? null : dto.reason());
        return Result.ok();
    }
}
