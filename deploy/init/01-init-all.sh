#!/bin/bash
# MySQL Docker 初始化脚本 — 按顺序导入所有 SQL，并创建业务用户

MYSQL="mysql -uroot -p${MYSQL_ROOT_PASSWORD}"

# 确保数据库使用 utf8mb4
$MYSQL <<SQL
ALTER DATABASE \`ry-vue\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'carecubeai'@'%' IDENTIFIED BY 'carecubeai123';
GRANT ALL PRIVILEGES ON \`ry-vue\`.* TO 'carecubeai'@'%';
FLUSH PRIVILEGES;
SQL

echo ">>> 初始化基础表结构..."
$MYSQL --default-character-set=utf8mb4 ry-vue < /sql/ry_20260417.sql

echo ">>> 初始化 Quartz 定时任务表..."
$MYSQL --default-character-set=utf8mb4 ry-vue < /sql/quartz.sql

echo ">>> 初始化知识库 & AI助手菜单..."
$MYSQL --default-character-set=utf8mb4 ry-vue < /sql/ragflow_knowledge_base_menu.sql

echo ">>> 数据库初始化完成"
