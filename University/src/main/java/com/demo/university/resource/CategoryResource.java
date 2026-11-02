package com.demo.university.resource;

import com.demo.university.common.Result;
import com.demo.university.config.AppContext;
import com.demo.university.vo.CategoryVO;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

/** 类目树 */
@Path("/categories")
@Produces(MediaType.APPLICATION_JSON)
public class CategoryResource {

    @GET
    public Result<List<CategoryVO>> tree() {
        return Result.ok(AppContext.category().tree());
    }
}
