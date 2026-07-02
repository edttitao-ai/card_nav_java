-- =========================================
-- card_nav 数据库表结构
-- 创建时间: 2026-07-01
-- 说明: 从 JSON 字段打平为独立 SQL 字段设计
-- =========================================

-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS card_nav
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE card_nav;

-- =========================================
-- 表1: sidebar - 侧边栏导航配置
-- =========================================
DROP TABLE IF EXISTS sidebar;
CREATE TABLE sidebar (
    id VARCHAR(50) PRIMARY KEY COMMENT '侧边栏ID',
    label VARCHAR(100) NOT NULL COMMENT '显示名称',
    icon VARCHAR(50) COMMENT '图标名称',
    sort_order INT DEFAULT 0 COMMENT '排序顺序'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='侧边栏导航配置表';

-- 初始化侧边栏数据
INSERT INTO sidebar (id, label, icon, sort_order) VALUES
('dashboard', '首页', 'dashboard', 1),
('portfolio', '个人 网站', 'portfolio', 2),
('github', 'GitHub 项目', 'github', 3),
('learning', '学习', 'tools', 4),
('ai-chat', 'AI聊天网站', 'dashboard', 5),
('recruitment', '招聘网站', 'tools', 6),
('tools', '工具', 'dashboard', 7),
('video-websites', '视频网站', 'notes', 8);

-- =========================================
-- 表2: category - 分类字典
-- =========================================
DROP TABLE IF EXISTS category;
CREATE TABLE category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    sort_order INT DEFAULT 0 COMMENT '排序顺序'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类字典表';

-- 初始化分类数据
INSERT INTO category (name, sort_order) VALUES
('前端', 1),
('后端', 2),
('AI', 3),
('工具', 4),
('DevOps', 5),
('视频', 6),
('阅读', 7),
('学习', 8),
('工作', 9),
('文档', 10),
('生活', 11);

-- =========================================
-- 表3: cards - 卡片主表
-- =========================================
DROP TABLE IF EXISTS cards;
CREATE TABLE cards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    external_id VARCHAR(100) NOT NULL COMMENT '前端生成的唯一ID',
    title VARCHAR(255) NOT NULL COMMENT '卡片标题',
    url VARCHAR(500) NOT NULL COMMENT '链接地址',
    description VARCHAR(500) COMMENT '卡片描述',
    category_id BIGINT COMMENT '分类ID',
    sidebar_id VARCHAR(50) COMMENT '侧边栏ID',
    favicon VARCHAR(500) COMMENT '网站图标URL',
    pinned TINYINT(1) DEFAULT 0 COMMENT '是否置顶: 0-否, 1-是',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    deleted_at DATETIME COMMENT '软删除时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卡片主表';

-- 添加索引
CREATE INDEX idx_cards_external_id ON cards(external_id);
CREATE INDEX idx_cards_sidebar_id ON cards(sidebar_id);
CREATE INDEX idx_cards_category_id ON cards(category_id);
CREATE INDEX idx_cards_pinned ON cards(pinned);
CREATE INDEX idx_cards_deleted_at ON cards(deleted_at);

-- =========================================
-- 表4: visitors - 访客日志
-- =========================================
DROP TABLE IF EXISTS visitors;
CREATE TABLE visitors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    ip VARCHAR(50) COMMENT '访客IP地址',
    browser VARCHAR(100) COMMENT '浏览器类型',
    device VARCHAR(50) COMMENT '设备类型: PC端/手机端',
    timestamp DATETIME NOT NULL COMMENT '访问时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访客日志表';

-- 添加索引用于分页查询
CREATE INDEX idx_visitors_timestamp ON visitors(timestamp DESC);

-- =========================================
-- 表5: clicks - 卡片点击统计
-- =========================================
DROP TABLE IF EXISTS clicks;
CREATE TABLE clicks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    card_id BIGINT NOT NULL COMMENT '卡片ID',
    count BIGINT DEFAULT 0 COMMENT '点击次数',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卡片点击统计表';

-- 添加索引
CREATE INDEX idx_clicks_card_id ON clicks(card_id);

-- =========================================
-- 表6: card_logs - 卡片操作日志
-- =========================================
DROP TABLE IF EXISTS card_logs;
CREATE TABLE card_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    card_id BIGINT NOT NULL COMMENT '卡片ID',
    action VARCHAR(20) NOT NULL COMMENT '操作类型: INSERT/UPDATE/DELETE',
    operator_ip VARCHAR(50) COMMENT '操作者IP',
    created_at DATETIME NOT NULL COMMENT '操作时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='卡片操作日志表';

-- 添加索引
CREATE INDEX idx_card_logs_card_id ON card_logs(card_id);
CREATE INDEX idx_card_logs_created_at ON card_logs(created_at DESC);
CREATE INDEX idx_card_logs_action ON card_logs(action);

-- =========================================
-- SQL 统计查询示例（可删除）
-- =========================================

-- 查询最近7天每天的访问量
-- SELECT DATE(timestamp) as date, COUNT(*) as count
-- FROM visitors
-- WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 7 DAY)
-- GROUP BY DATE(timestamp)
-- ORDER BY date;

-- 查询各分类的卡片数量
-- SELECT c.name as category, COUNT(cr.id) as card_count
-- FROM category c
-- LEFT JOIN cards cr ON c.id = cr.category_id AND cr.deleted_at IS NULL
-- GROUP BY c.id, c.name;

-- 查询最热门的卡片（点击量最高）
-- SELECT cr.title, cr.url, cl.count
-- FROM cards cr
-- JOIN clicks cl ON cr.id = cl.card_id
-- WHERE cr.deleted_at IS NULL
-- ORDER BY cl.count DESC
-- LIMIT 10;

-- 查询某个卡片的所有操作日志
-- SELECT cl.action, cl.operator_ip, cl.created_at
-- FROM card_logs cl
-- WHERE cl.card_id = ?
-- ORDER BY cl.created_at DESC;

-- =========================================
-- 表7: favorites - 收藏夹
-- =========================================
DROP TABLE IF EXISTS favorites;
CREATE TABLE favorites (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           card_id BIGINT NOT NULL UNIQUE COMMENT '卡片ID(数字)',
                           title VARCHAR(255) COMMENT '卡片标题',
                           url VARCHAR(500) COMMENT '卡片URL',
                           description VARCHAR(500) COMMENT '描述',
                           category VARCHAR(100) COMMENT '分类',
                           favicon VARCHAR(500) COMMENT '网站图标',
                           created_at DATETIME COMMENT '收藏时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏夹表';

SELECT 'card_nav 表结构创建完成!' AS message;
