# CareCubeAi Backend

睿立方感控AI助手 — 后端服务，基于 [RuoYi v3.9.2](https://gitee.com/y_project/RuoYi-Vue) 定制开发。

## 技术栈

| 组件 | 版本 |
|------|------|
| Spring Boot | 4.0.6 |
| Spring Security | JWT 认证 |
| MyBatis | 动态 SQL |
| Druid | 连接池 + 监控 |
| Redis | 缓存 & Session |
| MySQL | 8.0+ |

## 核心功能

- **AI 聊天助手** — 对接 RAGFlow API，支持多会话管理、SSE 流式对话
- **知识库管理** — 知识库 CRUD、文档上传/解析/检索、搜索测试
- **文件管理** — 类 Windows 资源管理器，无限层级文件夹、上传/移动/重命名/关联知识库
- **系统管理** — 用户、角色、菜单、字典、参数、通知公告
- **监控运维** — 在线用户、操作日志、登录日志、服务监控、缓存监控、定时任务
- **代码生成器** — 一键生成前后端 CRUD 代码

## 快速开始

```bash
# 启动 MySQL 和 Redis
# 创建数据库 ry-vue，导入 sql/ 目录下的 SQL 文件

# 编译
mvn clean install -DskipTests

# 运行
java -jar ruoyi-admin/target/ruoyi-admin.jar

# 后端启动在 http://localhost:8081
# Swagger: http://localhost:8081/swagger-ui.html
# Druid:   http://localhost:8081/druid (ruoyi / 123456)
```

## Docker 部署

```bash
# 在项目根目录执行
docker compose up -d --build
```

## 配置说明

| 文件 | 说明 |
|------|------|
| `application.yml` | 主配置（端口、Redis、JWT 密钥） |
| `application-druid.yml` | 数据源配置（MySQL 连接） |
| `docker/application.yml` | Docker 环境覆盖配置（RAGFlow 地址、API Key） |

## 默认账号

- 登录：`admin` / `admin123`

## License

MIT
