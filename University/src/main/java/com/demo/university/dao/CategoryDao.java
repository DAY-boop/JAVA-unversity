package com.demo.university.dao;

import com.demo.university.model.Category;

import java.util.List;

/** 类目表数据访问 */
public class CategoryDao extends BaseDao {

    private static final RowMapper<Category> MAPPER = rs -> {
        Category category = new Category();
        category.id = rs.getLong("id");
        category.name = rs.getString("name");
        category.parentId = rs.getLong("parent_id");
        category.sort = rs.getInt("sort");
        return category;
    };

    public List<Category> findAll() {
        return queryList("SELECT * FROM categories ORDER BY parent_id, sort, id", MAPPER);
    }

    public Category findById(long id) {
        return queryOne("SELECT * FROM categories WHERE id = ?", MAPPER, id);
    }

    public List<Category> findByParent(long parentId) {
        return queryList("SELECT * FROM categories WHERE parent_id = ? ORDER BY sort, id", MAPPER, parentId);
    }
}
