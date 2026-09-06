-- 邻车（shared-transportation）MariaDB 初始化脚本
-- 用法（需要 root 权限，root 默认走 unix_socket 认证）：
--   sudo mariadb < schema.sql
--
-- 表结构由 Spring Data JPA 的 ddl-auto=update 自动创建，这里只建库和授权。

-- 1. 建库
CREATE DATABASE IF NOT EXISTS shared_transportation
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- 2. 应用专用用户（避免应用直接用 root，root 走 unix_socket 无法通过 TCP 连接）
CREATE USER IF NOT EXISTS 'linche'@'localhost' IDENTIFIED BY 'linche123';
CREATE USER IF NOT EXISTS 'linche'@'%' IDENTIFIED BY 'linche123';

-- 3. 授权
GRANT ALL PRIVILEGES ON shared_transportation.* TO 'linche'@'localhost';
GRANT ALL PRIVILEGES ON shared_transportation.* TO 'linche'@'%';
FLUSH PRIVILEGES;
