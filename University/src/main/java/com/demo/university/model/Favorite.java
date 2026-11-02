package com.demo.university.model;

import java.time.LocalDateTime;

/** 收藏实体 */
public class Favorite {

    public Long id;
    public Long userId;
    public Long bookId;
    public LocalDateTime createdAt;
}
