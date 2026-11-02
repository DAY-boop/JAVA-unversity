package com.demo.university.model;

/** 教材类目实体，parentId = 0 表示一级类目 */
public class Category {

    public Long id;
    public String name;
    public Long parentId;
    public Integer sort;
}
