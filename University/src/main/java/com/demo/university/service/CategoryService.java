package com.demo.university.service;

import com.demo.university.dao.CategoryDao;
import com.demo.university.model.Category;
import com.demo.university.vo.CategoryVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 类目树 */
public class CategoryService {

    private final CategoryDao categoryDao = new CategoryDao();

    /** 返回两级类目树 */
    public List<CategoryVO> tree() {
        List<Category> all = categoryDao.findAll();
        Map<Long, CategoryVO> roots = new LinkedHashMap<>();
        for (Category category : all) {
            if (category.parentId == 0) {
                roots.put(category.id, toVO(category));
            }
        }
        for (Category category : all) {
            if (category.parentId != 0) {
                CategoryVO parent = roots.get(category.parentId);
                if (parent != null) {
                    parent.children.add(toVO(category));
                }
            }
        }
        return new ArrayList<>(roots.values());
    }

    /** 级联筛选：选中一级类目时把其子类目一起带上 */
    public List<Long> descendantIds(Long categoryId) {
        if (categoryId == null) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        ids.add(categoryId);
        for (Category child : categoryDao.findByParent(categoryId)) {
            ids.add(child.id);
        }
        return ids;
    }

    public String nameOf(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = categoryDao.findById(categoryId);
        return category == null ? null : category.name;
    }

    private CategoryVO toVO(Category category) {
        CategoryVO vo = new CategoryVO();
        vo.id = category.id;
        vo.name = category.name;
        vo.parentId = category.parentId;
        vo.sort = category.sort;
        return vo;
    }
}
