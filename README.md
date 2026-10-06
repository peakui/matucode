# 码途 matu

**计算机学习与求职成长一体化平台**

从刷题学习，到求职上岸

在线判题 · 面试题库 · 班级作业 · 问答社区 · 教程课程 · 打卡签到 · AI 助手

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](#开源许可)
[![Backend](https://img.shields.io/badge/backend-Java%2017%20%C2%B7%20Spring%20Cloud-6db33f.svg)](#技术栈)
[![Web](https://img.shields.io/badge/web-React%2019%20%C2%B7%20Vue%203-61dafb.svg)](#技术栈)
[![Judge](https://img.shields.io/badge/judge-Docker%20Sandbox-2496ed.svg)](#核心技术实现)

## 简介

码途（matu）是一个面向计算机专业学生与开发者的学习社区平台。它把「学、练、问、评」串成一条完整的链路：用教程与课程讲知识，用在线判题即时反馈，用面试题库备岗，用班级作业与排名支撑教学，用问答社区沉淀经验，再用 AI 助手降低学习与创作的启动成本。

项目采用**四端协同 + 微服务后端**的结构：Web 用户端供学习者使用，管理后台供运营维护内容，微信小程序与移动 App 覆盖移动场景；后端按业务域拆分为 14 个微服务，统一经网关鉴权，配套消息队列、缓存、搜索引擎与容器化判题沙箱。

## 功能特性

| 模块 | 说明 |
| --- | --- |
| 在线判题 OJ | 多语言代码编辑器（Monaco），提交判题、样例对比、提交记录与题解，Docker 沙箱隔离执行 |
| 面试题库 | 按分类与难度整理的面试题，支持 VIP 解锁、收藏与学习进度记录 |
| 班级与作业 | 班级创建与成员审核、班级题目池、作业布置与截止时间，提交后自动统计排名 |
| 竞赛 | 按 ICPC 罚时赛制生成竞赛排行榜，输出逐题网格榜 |
| 问答社区 | 提问、回答、采纳与评论，配合搜索与标签定位问题 |
| 教程与课程 | 教程文档与视频课程，章节学习与学习进度追踪 |
| 文章与创作 | Markdown 编辑器、封面与分类标签、一键发布到首页信息流，支持多人协同编辑 |
| 打卡签到 | 每日打卡、连续天数与最长连击统计、成就体系与补签 |
| 消息与私信 | 站内通知、点赞评论提醒与一对一私信（WebSocket 实时推送） |
| AI 助手 | 基于工具调用（MCP）的智能问答、文章摘要与内容生成，流式输出 |
| 会员与支付 | 会员权益、支付宝扫码/网页支付与订单管理 |
| 统一搜索 | Elasticsearch 全站检索文章、课程、面试题与题目 |
| 文件服务 | 对象存储分片上传、断点续传与 MD5 秒传 |
| 用户与权限 | 注册登录、RBAC 角色、企业/学校/身份认证 |

## 技术栈

| 层次 | 技术选型 |
| --- | --- |
| 用户端 Web（matu-user） | React 19 · TypeScript · Ant Design 6 · Redux Toolkit · React Router 7 · Vite · Monaco Editor |
| 管理后台（matu_admin） | Vue 3 · Element Plus · Pinia · Vue Router · Vite · Sass |
| 微信小程序（matu-liteapp） | uni-app |
| 移动 App（matu-appp） | Flutter · GetX |
| 后端（matu-backend） | Java 17 · Spring Boot 3 · Spring Cloud（Nacos / Gateway / OpenFeign）· MyBatis-Plus · Flyway |
| 数据与中间件 | MySQL 8（12 个业务库）· Redis · RabbitMQ · Elasticsearch 8 · PostgreSQL + pgvector（AI 向量检索） |
| 判题沙箱（matu-code-sandbox） | Docker · openjdk / python / gcc 镜像，容器级资源隔离 |

## 系统架构

```
┌────────────────────────── 客户端 ──────────────────────────┐
│   用户端 Web        管理后台        微信小程序        移动 App   │
└───────────────────────────┬───────────────────────────────┘
                            │  HTTP · WebSocket · SSE
                    ┌───────▼────────┐
                    │    Gateway     │  Sa-Token 鉴权 · HMAC 身份签名 · 限流
                    └───────┬────────┘
                            │  OpenFeign（内部令牌）
    ┌────────┬────────┬─────┴─────┬────────┬────────┬────────┐
  auth      post      oj      message   search     pay      ai  ...
    └────────┴────────┴───────────┴────────┴────────┴────────┘
                            │
      MySQL · Redis · RabbitMQ · Elasticsearch · 对象存储 · Docker 判题沙箱
```

## 项目结构

```
matu/
├── matu-user/            用户端 Web（React + TypeScript + Ant Design）
├── matu_admin/           管理后台（Vue 3 + Element Plus）
├── matu-liteapp/         微信小程序（uni-app）
├── matu-appp/            移动 App（Flutter + GetX）
├── matu-backend/         后端微服务（Spring Cloud）
│   ├── gateway/          统一网关：鉴权、身份签名、限流
│   ├── common/           公共模块：通用返回、安全、内部契约
│   ├── service/          14 个业务微服务
│   │   ├── service-auth/       认证、用户、角色与会员
│   │   ├── service-post/       文章、动态、点赞与评论
│   │   ├── service-oj/         题库、提交与判题调度
│   │   ├── service-check/      打卡、统计与成就
│   │   ├── service-message/    私信、通知与协同编辑
│   │   ├── service-search/     Elasticsearch 统一搜索
│   │   ├── service-pay/        支付宝支付与订单
│   │   ├── service-ai/         AI Agent、RAG 与异步任务
│   │   ├── service-mcp/        MCP 工具服务
│   │   └── ...                 课程、面试、问答、文件、信息
│   └── database/         数据库初始化脚本（bootstrap / seeds）
├── matu-code-sandbox/    代码判题沙箱（Docker）
└── nacos-configs/        Nacos 服务配置模板
```

## 亮点技术

从 AI Agent 到支付交易、统一搜索、消息流转，挑十几个最能体现工程深度的子系统展开讲讲。

### AI Agent · 工具调用与 MCP

**自研 OpenAI 兼容客户端 · 多轮 Function Calling · MCP 工具协议 · 流式输出**

- **不绑定厂商**：自研 `OpenAiCompatibleClient`（WebClient + SSE 流式），Embedding 兼容 OpenAI 与 DashScope 两套响应格式，换模型只改配置。
- **多轮工具循环**：`AiAgentService` 解析 `tool_calls` → 执行工具 → 回填结果 → 递归，由 `ai.agent.max-tool-calls`（默认 4）封顶，避免无限调用。
- **工具即服务**：工具后端是独立的 service-mcp，JSON-RPC 2.0 分发（`McpDispatcher`）；内置课程检索、OJ 题目与提交查询、联网搜索、网页抓取（含 SSRF 防护）。
- **上下文与检索**：Redis List `ai:memory:{userId}:{conversationId}` 存近期对话（约 20 条 / 30 天），MySQL 落库持久化；pgvector `ai_knowledge` 做向量检索（1024 维，topK 5 / 阈值 0.7），检索异常静默降级。
- **安全治理**：`PromptGuard` 提示注入过滤、`AiRateLimiter` 限流（20 次/分、并发 2），重任务走 MQ 异步并配死信队列。

**Function Calling** · **MCP / JSON-RPC** · **SSE 流式** · **pgvector RAG** · **限流与降级**

### 点赞系统 · 高并发一致性

**Redis 原子脚本 · 三级缓存 · 版本号 CAS · Outbox + 对账**

- **写路径全原子**：单条 Lua 脚本在 Redis 内一次性完成「幂等判定 + 状态翻转 + 版本号 +1 + 写入待发队列」，天然免疫并发重复点赞。
- **三级读取**：Caffeine 本地缓存 → Redis（权威数据源）→ MySQL 兜底；本地缓存带随机抖动过期防雪崩，DB 兜底用信号量限 8 并发保护数据库。
- **版本号 CAS**：每次变更携带 `version`，落后版本返回 `STALE`，保证「新操作绝不被失败的旧操作回滚」。
- **可靠投递**：Redis Hash 当 outbox，定时任务补偿发 MQ，成功才清理；另有一份每日 2 点对账任务扫描 Redis 与 MySQL 差异并自动修复。
- **热 key 探测**：用 HeavyKeeper（重击者算法）统计访问频次，超过阈值才升级到本地缓存；所有键用 hash tag `post:like:{postId}:*` 保证同槽位、适配 Redis Cluster。

**Redis Lua** · **Caffeine L1** · **版本号 CAS** · **Outbox** · **对账补偿** · **热 key 探测**

### Docker 判题 OJ · 异步判题与沙箱

**RabbitMQ 异步流水线 · 容器级隔离 · 资源限额 · 死信兜底**

- **异步解耦**：提交先落库为 `WAITING`，事务提交后（`afterCommit`）才投递 `oj.judge.queue`；消费者手动 ack、prefetch=1，配合 Redis 锁 `oj:judge:lock:{id}` 防重复消费。
- **失败兜底**：应用内重试 3 次，耗尽后 `basicNack(requeue=false)` 经死信交换机 `oj.judge.dlx` 落入 `oj.judge.dlq`，不阻塞主队列。
- **容器隔离**：独立沙箱服务（docker-java）常驻容器 + exec 执行；关闭网络、只读根文件系统、tmpfs `/tmp` 限额 64M、CPU 1 核、内存上限且交换分区为 0。
- **多语言与限额**：`openjdk:8-alpine` / `python:3.9-alpine` / `gcc:12.2.0`；编译限 15s，运行默认 1000ms，退出码 137 判定为内存超限。
- **状态机**：8 态枚举（WAITING / AC / WA / RE / TLE / MLE / CE / SYSTEM_ERROR），逐样例比对，全部通过才判 AC。

**RabbitMQ** · **死信队列** · **Docker 沙箱** · **cgroups 限额** · **Redis 幂等锁**

### 协同编辑 · 自研 OT 实时同步

**Operational Transform · WebSocket 房间广播 · LMAX Disruptor**

- **自研 OT（非 CRDT）**：把编辑抽象为 `insert` / `delete` 的 TextOperation，由变换器做冲突合并（含重叠删除），服务端保存操作日志而非整篇覆盖。
- **定序与幂等**：提交以客户端 `baseRevision` 为基准，与之后已提交的操作逐一 transform 后再应用，全局 revision + 1；`(clientId, requestId)` 去重，重发请求直接返回原结果。
- **实时通道**：与私信共用 WebSocket 端点，握手统一走 Sa-Token 鉴权；房间内广播加入/离开/光标/正在输入，并把操作实时下发到各端。
- **前端协同**：编辑器本地 diff 出操作增量后经 WebSocket 上行，在线光标与选区随操作一同同步，实现「同文档多人同时编辑」。
- **削峰异步**：操作提交流水线接入 LMAX Disruptor 环形队列，把高频编辑从请求线程卸下来，避免阻塞。

**Operational Transform** · **WebSocket** · **Sa-Token 鉴权** · **LMAX Disruptor** · **光标共享**

### 支付宝支付 · 订单与回调一致性

**官方 SDK · RSA2 验签 · 回调幂等 · 权益发放**

- **官方 SDK 接入**：独立服务 service-pay（独立库），用 alipay-sdk-java 的 `DefaultAlipayClient`，支持扫码（Precreate）与网页（PagePay）两种下单方式。
- **回调闭环**：支付宝异步通知 `POST /pay/alipay/notify` → 全量验签并校验 `app_id` 与金额一致 → 更新订单与流水 → Feign 调用 service-auth 发放会员权益。
- **双重幂等**：通知日志按 `notify_id` 唯一去重；权益发放用 `activation_key = "pay:" + 订单号` 唯一键 + insertIgnore，重复通知不会重复发权益。
- **并发串行化**：订单与支付流水更新走 `SELECT ... FOR UPDATE` 行锁，避免同一订单被并发改单。
- **状态机**：`OrderStatus`（待支付 / 已支付 / 已关闭 / 已退款 / 部分退款）流转，下单与拉起支付时惰性判断 30 分钟支付截止时间并关闭超时单。

**支付宝 SDK** · **RSA2 验签** · **回调幂等** · **行锁** · **Feign 发权益**

### Elasticsearch 统一搜索 · 一栈搜全站

**官方 Java API Client · 多字段加权 · 别名原子切换**

- **独立搜索服务**：service-search 用官方 elasticsearch-java 客户端，统一检索文章 / 课程 / 面试题 / OJ 题目四类内容，type 区分、`type=all` 时聚合返回。
- **字段加权检索**：multiMatch 对标题、摘要、标签、分类做加权匹配（如标题权重 4、摘要权重 2），配合类型与状态过滤、深分页上限保护。
- **零停机重建**：同步时写入带时间戳的临时索引，bulk 导入完成后原子切换别名，失败即删除新索引——查询全程不中断。
- **数据来源安全**：各业务服务通过 `/internal/search-documents` 内部端点提供文档（游标分页推进），带共享令牌校验；网关对内部端点直接返回 404，杜绝公网直连。
- **触发方式**：定时自动重建（默认每 30 分钟）或管理端一键触发，重建期间旧索引照常服务。

**Elasticsearch 8** · **multiMatch 加权** · **别名原子切换** · **内部令牌**

### 分布式消息流转 · 事件驱动

**4 组交换机 · 手动 ack · 死信兜底 · Outbox 补偿**

- **统一拓扑**：4 组直连交换机各配死信队列，覆盖点赞、AI 文章摘要、AI 异步任务与 OJ 判题（如判题链路 `oj.judge.exchange → oj.judge.queue`，失败进 `oj.judge.dlq`）。
- **可靠消费**：手动 ack、prefetch 限流、应用内重试 3 次，耗尽后 `basicNack(requeue=false)` 落入死信队列，不再反复入队阻塞主链路。
- **事务后投递**：判题消息在事务 `afterCommit` 回调里才发送，从根上避免「消息已发出但事务回滚」的脏消息。
- **Outbox 兜底**：数据库本地消息表 + 定时补投（AI 摘要），点赞侧用 Redis Hash 当 outbox；均等发布确认成功后才清理记录。
- **跨服务事件**：文章发布事件由内容服务投递到 AI 队列、由 AI 服务消费生成摘要，再经 HTTP 回写——典型的服务解耦与异步化。

**RabbitMQ** · **手动 ack** · **死信队列** · **Outbox** · **发布确认**

### 网关零信任鉴权 · 身份签名与限流

**Sa-Token · HMAC 身份签名 · 内部端点隔离**

- **集中式鉴权**：网关用 Sa-Token 统一拦截，登录、注册、支付回调等白名单之外一律校验 token，业务服务不再各自重复鉴权。
- **身份签名防伪造**：网关校验 token 后签发用户 ID / 角色 / 时间戳，并用 HmacSHA256 签名；下游验签 + 30 秒时间戳偏移校验 + 常量时间比较，同时清除伪造的转发头，构成零信任边界。
- **内部端点隔离**：内部同步接口在网关侧强制登录或直接返回 404；服务间调用另带共享令牌（支付、搜索、AI 各有独立令牌）。
- **网关限流与名单**：敏感接口用 Redis Lua 固定窗口限流，另有 IP 黑白名单过滤器。
- **会话与密码**：同端登录可顶下线；密码用 BCrypt（强度 12）存储，历史 SHA-256 账号登录成功后透明升级。

**Sa-Token** · **HmacSHA256** · **内部令牌** · **Lua 限流** · **BCrypt**

### 文件服务 · 分片上传与秒传

**对象存储分片 · 断点续传 · MD5 去重**

- **分片上传**：走对象存储的「初始化 → 上传分片 → 完成」三段式，用分片记录表保存已传进度，支持大文件断点续传。
- **秒传**：以文件 MD5 + 大小做去重，命中已存在文件直接返回句柄，跳过整个上传过程。
- **僵尸清理**：定时任务定期中止长期未完成的分片上传，回收存储空间与未完成配额。
- **上传安全**：服务端按白名单判定内容类型，防止把 HTML 伪装成图片造成存储型 XSS；私有文件下载走签名 URL。

**OSS 分片上传** · **断点续传** · **MD5 秒传** · **Content-Type 白名单**

### 竞赛排名 · ICPC 罚时赛制

**真实赛制算法 · 网格榜输出**

- **赛制规则**：按「解题数 > 罚时 > 最后 AC 时间」排序；罚时 = AC 用时 + 错误提交数 × 20 分钟，基准时间取竞赛开始或最早提交，贴近真实 ICPC 规则。
- **网格榜**：返回逐题状态的网格数据（每题是否通过、该题尝试次数），前端可直接渲染成竞赛榜单。
- **范围约束**：只统计竞赛时间窗内、竞赛题目池内的提交，避免场外提交刷榜。

**ICPC 罚时** · **排行榜** · **竞赛时间窗**

### 打卡统计 · 连续天数与成就

**增量统计 · 连击计算 · 成就体系 · 补签**

- **连续天数**：扫描日历计算当前连续与历史最长连续打卡天数，跨月也能正确衔接。
- **增量统计**：按月的统计表增量刷新，避免每次心跳都全量重算，排行榜与个人页读取更快。
- **成就体系**：按累计打卡天数、发布内容数、最长连击等多维度判定成就并实时刷新。
- **补签**：支持补签（限一年内），补签后自动重建当月的统计与连续天数。

**连续天数** · **增量统计表** · **成就判定** · **补签**

### 面试接口防护 · 多级限流与动态熔断

**网关 + 服务双配额 · 阶梯封禁 · Sentinel 热更新**

- **第一层 · 网关限流**：Redis Lua 固定窗口限流，拦住粗粒度的高频流量。
- **第二层 · 服务内双配额**：Lua 脚本同时维护短期与 24 小时两级配额，超限后阶梯封禁（多次触发则封禁时长从分钟级升到小时级）。
- **第三层 · 动态熔断**：Sentinel 的 QPS 流控与响应时间降级规则由 Nacos 下发并热更新；配置非法时回退到上一次生效的规则，避免改错配置反而打穿保护。
- **效果**：爬取、刷接口等异常流量在三层拦截下逐级衰减，正常用户几乎无感。

**Lua 固定窗口** · **双配额** · **阶梯封禁** · **Sentinel + Nacos**

### 其它工程细节

**贯穿各模块的通用设计取舍**

- **服务治理**：Nacos 注册与配置中心 + Gateway 统一入口，服务间用 OpenFeign 内网调用并带内部密钥头，超时可控。
- **数据访问**：MyBatis-Plus + 游标分页 + 批量操作；数据库变更用 Flyway 版本化迁移，改表前做列存在性守卫保证幂等可重复执行。
- **权限模型**：RBAC 角色（普通用户 / 管理员 / 讲师 / 会员），网关与服务内双重校验，权限绝不信任前端。
- **实时能力分层**：AI 问答走 SSE 流式，协同与私信走 WebSocket，判题、摘要、点赞走 MQ——按场景选型而非一栈到底。
- **安全细节**：网页抓取工具做 SSRF 防护（拦截环回、内网与云元数据地址），AI 侧有提示注入词表过滤，文件上传有内容类型白名单。
- **可观测与容错**：关键链路带日志埋点；缓存、检索、下游服务异常均有降级路径，单点故障不拖垮主流程。

## 快速开始

### 环境要求

- **JDK 17**、Maven 3.9+
- **Node.js 18+**（Web 端与小程序）
- **Flutter 3.x**（移动 App）
- **MySQL 8**、**Redis**、**RabbitMQ**、**Elasticsearch 8**、**Nacos 2.x**
- **Docker**（判题沙箱）
- 可选：PostgreSQL + pgvector（AI 向量检索）、对象存储（文件服务）、支付宝账号（支付）、大模型 API（AI）

### 1. 初始化数据库

项目按服务拆分为 12 个 MySQL 业务库。为每个库创建空库（字符集 `utf8mb4`）后导入对应脚本：

```sql
CREATE DATABASE matu_info CHARACTER SET utf8mb4;
USE matu_info;
SOURCE matu-backend/database/bootstrap/matu_info.sql;
```

对 `matu-backend/database/bootstrap/` 下的 12 个文件重复上述步骤，再在 `matu_auth` 库执行 `seeds/matu_auth.sql` 初始化角色。脚本不含任何用户、订单与业务数据，也不含默认账号密码。

### 2. 配置本地变量与 Nacos

- 复制根目录的 `.env.example` 为 `.env`，填入你自己的数据库连接等信息；
- 将 `nacos-configs/` 下的配置导入 Nacos（分组 `nacos`），并填入你自己的 Redis、RabbitMQ、Elasticsearch、对象存储与模型服务凭据。

> **切勿把真实密钥提交到公开仓库**。仓库内的配置均为模板，请通过环境变量或本地未跟踪文件注入。

### 3. 启动后端

```bash
cd matu-backend
mvn -s settings.xml -pl service/service-auth -am -DskipTests package
# 按需启动其余服务与网关；首次启动由 Flyway 自动执行版本迁移建表
```

### 4. 启动判题沙箱

```bash
cd matu-code-sandbox
mvn -DskipTests package
# 构建镜像并启动服务（默认端口 8091）
# 需通过环境变量 SANDBOX_AUTH_SECRET 注入调用方共享密钥
```

### 5. 启动前端

```bash
# 用户端 Web —— http://localhost:5173
cd matu-user && npm install && npm run dev

# 管理后台 —— http://localhost:5174
cd matu_admin && npm install && npm run dev

# 微信小程序：构建后用微信开发者工具导入产物目录
cd matu-liteapp && npm install && npm run build:mp-weixin

# 移动 App
cd matu-appp && flutter run
```

### 6. 健康检查

```bash
python matu-backend/scripts/smoke_public.py --base-url http://127.0.0.1:8080
```

## 开源许可

本项目采用 [Apache License 2.0](LICENSE) 开源协议。你可以自由使用、修改与分发本项目，包括用于商业用途；使用时请保留原始版权与许可声明。协议同时包含明确的专利授权条款。

移动端 App 基于 [FlutterKit](https://gitee.com/Joker-x-dev/FlutterKit) 模板开发，其上游代码以 MIT 协议授权，署名声明见 `matu-appp/LICENSE-FlutterKit`。

## 贡献

欢迎提交 Issue 反馈问题，或通过 Pull Request 提交改进。提交前请确保：

1. 不包含任何真实密钥、账号、订单或个人数据；
2. 变更与现有代码风格保持一致；
3. 相关模块能够正常构建。

## 致谢

本项目的完成离不开开源社区。谨向所有被使用的开源项目及其贡献者致以诚挚谢意。
