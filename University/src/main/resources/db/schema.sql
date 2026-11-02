-- ============================================================
-- 校园二手教材交易系统 建表脚本
-- 兼容 MySQL 8 与 H2(MODE=MySQL)，所有语句幂等
-- 约定：不使用物理外键，关联关系由应用层保证（便于分库/软删除）
-- ============================================================

-- 用户表：买家与卖家共用一张表，role 区分身份
CREATE TABLE IF NOT EXISTS users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(32)  NOT NULL,
    password     VARCHAR(64)  NOT NULL,
    salt         VARCHAR(32)  NOT NULL,
    nickname     VARCHAR(32)  NOT NULL,
    phone        VARCHAR(20),
    campus       VARCHAR(64),
    avatar       VARCHAR(255),
    role         TINYINT      NOT NULL DEFAULT 0,
    credit_score INT          NOT NULL DEFAULT 100,
    status       TINYINT      NOT NULL DEFAULT 0,
    created_at   DATETIME     NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username)
);

-- 类目表：parent_id = 0 表示一级类目
CREATE TABLE IF NOT EXISTS categories (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(32) NOT NULL,
    parent_id BIGINT      NOT NULL DEFAULT 0,
    sort      INT         NOT NULL DEFAULT 0,
    CONSTRAINT uk_categories_parent_name UNIQUE (parent_id, name)
);

-- 教材表：status 0待审 1在售 2已下架 3已售完
CREATE TABLE IF NOT EXISTS books (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    seller_id       BIGINT        NOT NULL,
    category_id     BIGINT        NOT NULL,
    title           VARCHAR(120)  NOT NULL,
    author          VARCHAR(64),
    publisher       VARCHAR(64),
    isbn            VARCHAR(20),
    edition         VARCHAR(32),
    course_name     VARCHAR(64),
    condition_level TINYINT       NOT NULL DEFAULT 3,
    original_price  DECIMAL(10,2),
    price           DECIMAL(10,2) NOT NULL,
    stock           INT           NOT NULL DEFAULT 1,
    cover_url       VARCHAR(255),
    description     VARCHAR(2000),
    status          TINYINT       NOT NULL DEFAULT 1,
    view_count      INT           NOT NULL DEFAULT 0,
    created_at      DATETIME      NOT NULL,
    updated_at      DATETIME      NOT NULL
);

-- 订单表：status 0待付款 1待交付 2待确认 3已完成 4已取消
CREATE TABLE IF NOT EXISTS orders (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no      VARCHAR(32)   NOT NULL,
    buyer_id      BIGINT        NOT NULL,
    seller_id     BIGINT        NOT NULL,
    total_amount  DECIMAL(10,2) NOT NULL,
    status        TINYINT       NOT NULL DEFAULT 0,
    trade_place   VARCHAR(128),
    contact       VARCHAR(64),
    remark        VARCHAR(255),
    cancel_reason VARCHAR(255),
    created_at    DATETIME      NOT NULL,
    paid_at       DATETIME,
    delivered_at  DATETIME,
    finished_at   DATETIME,
    CONSTRAINT uk_orders_order_no UNIQUE (order_no)
);

-- 订单明细表：保存下单时的书名/价格快照，卖家改价不影响历史订单
CREATE TABLE IF NOT EXISTS order_items (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id       BIGINT        NOT NULL,
    book_id        BIGINT        NOT NULL,
    title_snapshot VARCHAR(120)  NOT NULL,
    cover_snapshot VARCHAR(255),
    price_snapshot DECIMAL(10,2) NOT NULL,
    quantity       INT           NOT NULL DEFAULT 1,
    subtotal       DECIMAL(10,2) NOT NULL
);

-- 评价表：同一订单每人只能评一次
CREATE TABLE IF NOT EXISTS reviews (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT       NOT NULL,
    book_id     BIGINT       NOT NULL,
    reviewer_id BIGINT       NOT NULL,
    reviewee_id BIGINT       NOT NULL,
    rating      TINYINT      NOT NULL,
    content     VARCHAR(1000),
    tags        VARCHAR(128),
    created_at  DATETIME     NOT NULL,
    CONSTRAINT uk_reviews_order_reviewer UNIQUE (order_id, reviewer_id)
);

-- 收藏表
CREATE TABLE IF NOT EXISTS favorites (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT   NOT NULL,
    book_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_favorites_user_book UNIQUE (user_id, book_id)
);

-- 教材求购表：买家挂需求，卖家联系成交；status 0求购中 1已买到 2已关闭
CREATE TABLE IF NOT EXISTS wanted_books (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT        NOT NULL,
    category_id     BIGINT,
    title           VARCHAR(120)  NOT NULL,
    author          VARCHAR(64),
    publisher       VARCHAR(64),
    course_name     VARCHAR(64),
    expected_price  DECIMAL(10,2),
    condition_level TINYINT,
    description     VARCHAR(1000),
    contact         VARCHAR(64),
    campus          VARCHAR(64),
    status          TINYINT       NOT NULL DEFAULT 0,
    created_at      DATETIME      NOT NULL,
    updated_at      DATETIME      NOT NULL
);

-- 站内消息表（P2，可后置实现）
CREATE TABLE IF NOT EXISTS messages (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_user_id BIGINT        NOT NULL,
    to_user_id   BIGINT        NOT NULL,
    book_id      BIGINT,
    content      VARCHAR(1000) NOT NULL,
    is_read      TINYINT       NOT NULL DEFAULT 0,
    created_at   DATETIME      NOT NULL
);
