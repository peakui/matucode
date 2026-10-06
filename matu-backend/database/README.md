# 数据库初始化与校验

当前项目按服务拆分 12 个 MySQL 业务库。`bootstrap/` 保存 2026-10-05 从当前开发环境读取的 **95 张表结构**，不含用户、密码、订单、消息、支付日志、Flyway 历史或其他业务数据。AI 的 PostgreSQL 向量库继续使用 service-ai 自身配置和迁移，不包含在这些 MySQL 文件中。

## 新环境

1. 准备 MySQL 8，按 `bootstrap/` 文件名创建对应数据库，字符集使用 utf8mb4。
2. 将每个 SQL 文件导入同名的**空库**。文件不包含 DROP TABLE，也不会覆盖现有表；不要拿初始化脚本升级已有环境。
3. 在新的 matu_auth 库执行 `seeds/matu_auth.sql`，初始化 USER、ADMIN、TEACHER、VIP 角色。不会创建管理员账号或默认密码。认证服务的讲师角色 ID 当前约定为 3。
4. 配置工作区 `.env` 中的 MYSQL_HOST、MYSQL_PORT、MYSQL_USERNAME、MYSQL_PASSWORD，导入 `nacos-configs/` 中服务配置。
5. 启动服务，让 Flyway 创建自己的迁移历史并执行版本迁移。已有 Nacos 配置使用 baseline-on-migrate=true、baseline-version=0。不要手工改写 Flyway 校验和。

SQL 用法示例（在 mysql 客户端内执行，路径以实际仓库为准）：

```sql
CREATE DATABASE matu_info CHARACTER SET utf8mb4;
USE matu_info;
SOURCE matu-backend/database/bootstrap/matu_info.sql;
```

本次新建 `matu_info.feedback_history`：保留处理前后状态、处理人、优先级、指派人、回复及时间，并建立 `(feedback_id, created_at, id)` 索引。对应迁移为 service-info 的 V2。当前开发数据库已经执行该幂等建表语句；服务重启时 Flyway 仍会正常登记迁移。旧反馈不会伪造历史记录，从新版本上线后的首次修改开始记录。

## 校验与维护

业务联调脚本 `run_closure_integration.py` 需先在后端根目录成功运行 `mvn test`，它从 Surefire 报告读取依赖路径，以 Java 17 执行真实 MyBatis/Spring 事务测试。仅对随机临时库写入课程、进度、笔记、评价、证书和反馈数据，并注入历史写入失败验证回滚；完成后清理临时库。数据库账号还需具备临时库上的 TRIGGER 权限。

```powershell
python matu-backend/scripts/database_check.py --verify
python matu-backend/scripts/smoke_public.py --base-url http://127.0.0.1:8080
python matu-backend/scripts/run_closure_integration.py --java "<JAVA17_HOME>/bin/java.exe"
```

`database_check.py` 需要 PyMySQL，读取环境变量或工作区 `.env`，不会输出密码。`--verify` 会创建随机命名的 `matu_verify_*` 临时数据库，依次执行初始化、角色种子、迁移并检查 83 个实体的持久化字段；结束后只删除此次创建的临时库。数据库账号需有 CREATE/DROP DATABASE 权限。此检查验证 SQL 可执行性及字段覆盖，不代替 Flyway 校验和校验或业务验收。

`--export` 从当前配置指向的业务库重新导出表结构，按外键依赖排序，去除 AUTO_INCREMENT 当前计数和 Flyway 历史。只在需要更新初始化快照时运行，日常检查使用 `--verify`。

已有环境按新增 Flyway 迁移升级。历史迁移 V1/V2/V3 没有被重写。新环境仍需配置 Redis、Nacos，以及对应功能依赖的对象存储、RabbitMQ、Elasticsearch、代码沙箱、邮件、支付和 AI 服务；空库初始化本身不会产生课程或其他业务内容。
