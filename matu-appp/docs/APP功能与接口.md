# 码途 App 功能与接口

业务来源：同级 `matu-liteapp` 的 11 个页面及 `matu-backend` 实际 Controller/VO。App 复用 FlutterKit 的 GetX、TDesign、主题、BaseView/BaseNetworkView/BaseListView、Dio、Retrofit 和启动流程；业务集中在 `lib/feature/learning/`，共享数据位于 Core，路由位于 `lib/routes/learning/`。

| 页面 | Flutter 入口 | 接口/行为 |
| --- | --- | --- |
| 首页 | `/main` | 文章 `/posts`、打卡 `/checks`、问答 `/qa/questions`；独立筛选、分页、刷新与滚动位置 |
| 教程 | `/main` 教程 Tab | `/courses`，关键词、难度、排序 |
| 面试 | `/main` 面试 Tab | `/interview/questions`、`/interview/categories` |
| 我的 | `/main` 我的 Tab | `/auth/me`、`/checks/statistics?userId=...`；游客引导、真实统计 |
| 内容详情 | `/learning/detail` | posts/checks/qa 对应详情、评论/回答；点赞、收藏文章、关注问题；文章/打卡评论 |
| 面试详情 | `/learning/detail` | 题目详情、`GET/PUT /interview/questions/{id}/progress`、权限控制、上一题/下一题、本机收藏 |
| 课程详情 | `/learning/course` | `/courses/{id}`、`/courses/videos/{id}/play`、`/courses/{id}/progress/mine`、`POST /courses/{id}/progress` |
| 登录 | `/learning/login` | `POST /auth/mini/login`，已核对该账号密码入口没有小程序 code 依赖 |
| 我的列表 | `/learning/mine` | 当前账号打卡、`/qa/questions/mine`、按账号保存的面试收藏 |
| 资料 | `/learning/profile` | `GET/PUT /auth/me`、`POST /files/upload`、`PUT /auth/me/avatar` |
| 设置与关于 | `/learning/settings` | 本机主题设置、安装包版本、`POST /auth/mini/logout` |

## 数据与认证

- 后端成功码实际为 0；MatuResponseCodec 先无损解析长整数，再解包。Repository 将结果转为脚手架 BaseResponse/分页模型，不改变示例接口的成功码协议。
- 授权头使用 Session.authorization 原值。密码不保存，用户会话使用 flutter_secure_storage；本机收藏只保存题目元信息，不能包含答案。
- 401 清理当前会话；公开 GET 最多以游客重试一次；私有 GET、评论、资料更新、上传等写入不自动重放。
- 请求代次防止旧账号与旧筛选响应覆盖新状态。头像上传与资料更新共用同一鉴权、异常和代次检查链路。
- 正式网络客户端不接入 Alice/请求正文调试日志，启动时也不初始化其通知能力。

## 原生行为

- 视频仅使用授权 `/play` 返回的 URL；15 秒节流保存，在暂停、切后台和页面释放时保存。真正结束才报 100%，按 watchedDuration 恢复；课程为空不伪造播放器内容。
- 相册用于头像，iOS 已声明用途说明。Markdown/HTML 不执行脚本；图片可预览，外链由用户主动复制。
- 分享调用系统分享面板。未配置 `WEB_BASE_URL` 时只分享标题；配置实际网站域名后按现有 PC 路由分享文章、打卡、问答、课程、面试题链接，不猜测线上域名。
- 小程序微信 code 不能直接用于原生 App。当前仅接账号密码，未接入原生微信开放平台 SDK、支付、推送或商店发布。

## 已知边界

- 面试收藏后端仍缺按用户关系查询的正确契约，因此保留“本机收藏”，不宣称云同步。
- iOS 保留兼容工程与平台声明，但 Windows 环境不能替代 Xcode 构建、签名和 iOS 真机验证。
- 原生应用标识、图标、启动图和版权仍为模板内容；调试包标识是 `com.joker.flutterkit.debug`。正式发行需配置用户实际标识、品牌和签名。
- 模板 release 签名仍是 debug 配置；当前交付的是开发调试包，未生成商店发行包。
