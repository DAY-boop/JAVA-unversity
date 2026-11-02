package com.demo.university.vo;

import java.util.ArrayList;
import java.util.List;

/** 类目视图：一级类目带 children，前端直接渲染两级下拉 */
public class CategoryVO {

    public Long id;
    public String name;
    public Long parentId;
    public Integer sort;
    public List<CategoryVO> children = new ArrayList<>();
}
