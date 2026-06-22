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
- **智能分析服务** — 面向患者结构化资料输入，调用 RAGFlow Agent Completions SSE API，支持流式辅助分析、结构化结果、引用和后续追问
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

## 智能分析服务

智能分析服务统一放在 `mag` 命名空间下，前端只提交业务侧 `agentCode` 和 CareCubeAi `sessionId`，后端负责读取真实 RAGFlow Agent 配置、维护 RAGFlow `session_id` 映射，并使用当前登录用户生成 `user_id`。

主要接口：

| 方法 | 地址 | 说明 |
|------|------|------|
| GET | `/mag/agent/list` | 智能分析服务列表 |
| GET | `/mag/agent/{agentCode}/schema` | 动态输入表单 Schema |
| POST | `/mag/agent/{agentCode}/chat/stream` | SSE 流式辅助分析和追问 |
| GET | `/mag/agent/session/list` | 当前用户会话列表 |
| GET | `/mag/agent/session/{sessionId}` | 会话详情和历史消息 |
| DELETE | `/mag/agent/session/{sessionId}` | 删除会话 |

SSE 事件已转换为业务事件，不向前端透传 RAGFlow 原始事件：

| 事件 | 说明 |
|------|------|
| `message` | 流式文本片段 |
| `message_end` | 消息结束，包含引用和附件 |
| `status` | 当前执行状态 |
| `structured_result` | 结构化辅助分析结果 |
| `done` | 本次流式响应完成 |
| `error` | 可展示的错误信息 |

初始化 SQL：

```bash
mysql -u root -p ry-vue < sql/mag_agent_service.sql
```

生产环境请在数据库中维护 `agentCode`、真实 `agent_id`、`release`、输入字段映射等配置。不要在 Java 源码中写入 RAGFlow 地址、API Key 或 Agent ID。

## Docker 部署

```bash
# 首次部署时复制示例配置并填写真实值
cp deploy/.env.example deploy/.env

# 在 deploy 目录执行
cd deploy
docker compose up -d --build
```

## 配置说明

| 文件 | 说明 |
|------|------|
| `application.yml` | 主配置（端口、Redis、JWT 密钥） |
| `application-druid.yml` | 数据源配置（MySQL 连接） |
| `deploy/application.yml` | Docker 环境覆盖配置 |
| `deploy/application-druid.yml` | Docker 数据源配置 |
| `deploy/.env.example` | Docker 部署环境变量示例，不包含真实密钥 |

关键环境变量：

| 变量 | 说明 |
|------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 |
| `MYSQL_APP_USER` | 业务库账号 |
| `MYSQL_APP_PASSWORD` | 业务库密码 |
| `TOKEN_SECRET` | JWT 签名密钥 |
| `DRUID_STAT_PASSWORD` | Druid 监控密码 |
| `RAGFLOW_URL` | RAGFlow 服务地址 |
| `RAGFLOW_API_KEY` | RAGFlow API Key |

安全要求：

- 不提交真实 `.env` 文件。
- 不在普通操作日志中记录完整患者资料、完整 AI 回答、API Key 和内部服务地址。
- 医疗输出统一使用“辅助分析”“风险提示”等表述，不作为最终诊断。

## 默认账号

- 登录：`admin` / `admin123`

## License

MIT
