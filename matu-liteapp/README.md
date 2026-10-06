# 码途 · 微信小程序

Vue 3 + uni-app 的轻量学习客户端，沿用 HBuilderX 工程布局。前端开发位于本目录，没有修改 PC 端、管理端和后端。

## 已实现

| 入口 | 功能 |
| --- | --- |
| 首页 | 文章 / 打卡 / 问答切换，独立分页与筛选、下拉刷新、触底加载、列表位置恢复 |
| 内容详情 | Markdown/HTML 正文、图片预览、评论/回答、点赞、文章收藏、问题关注、微信分享 |
| 教程 | 课程搜索、难度与排序、课程简介、章节目录、鉴权视频播放、15 秒节流进度上报及暂停/离开保存 |
| 面试 | 分类、关键词与难度筛选、题目详情、权限判断、展开答案、上一题/下一题、掌握状态 |
| 我的 | 游客态、个人信息、真实学习统计、我的打卡、我的问答、面试题本机收藏、设置与退出 |
| 登录 | 账号密码、微信能力检查、微信登录、绑定已有账号 / 明确选择创建新账号 |
| 个人资料 | 昵称、签名、头像编辑；不提交后端 DTO 不支持的学校/公司等字段 |

共 11 个页面。原生底部 Tab 顺序为：首页、教程、面试、我的。

当前不包含 PC 的 AI 聊天、OJ、私信、比赛、班级、协同编辑、内容发布后台或支付。

## 运行

1. 在此目录执行 `npm ci` 安装业务依赖。`vue` 是用于 Node 测试的开发依赖，应用编译仍使用 HBuilderX 的 uni-app 编译器。
2. 使用支持 Vue 3 的 HBuilderX 导入 **整个 `matu-liteapp` 目录**，安装/启用 uni-app Vue 3 编译器插件。
3. 编辑 `config/index.js` 的 `API_BASE_URL`。
4. 编辑 `manifest.json` → `mp-weixin.appid`，填写你自己的微信小程序 AppID。不要在客户端填写 AppSecret。
5. HBuilderX 中选择“运行 → 运行到小程序模拟器 → 微信开发者工具”。编译产物目录由运行/发行模式决定。

本次已经生成的微信发行产物位于：

```text
unpackage/dist/build/mp-weixin
```

也可以直接在微信开发者工具中导入上述目录。AppID 为空时编译器生成 `touristappid`，仅供游客模式检查界面，不能替代真实微信登录/真机验收。不要直接把 Vue 源码目录作为原生微信小程序导入。

参考：[uni-app 的 HBuilderX 开发方式](https://uniapp.dcloud.net.cn/quickstart-hx)。

### 配置网关

默认网关为 `http://127.0.0.1:8080`，用于当前电脑的开发环境。

- 真机需要能访问的开发机局域网地址或测试 HTTPS 域名；手机上的 `127.0.0.1` 不是你的电脑。
- PC 中 `/api` 是 Vite 代理前缀，会在转发时移除。小程序直接请求网关，不依赖 PC 的 Vite 代理。若部署网关确实带前缀，把它配置进 `API_BASE_URL`。
- 源码默认启用微信域名校验。使用 HTTP 局域网联调时，可在微信开发者工具的**本地开发配置**中临时关闭域名校验；上线必须配置正确的 HTTPS request/uploadFile/downloadFile 等合法域名，并恢复校验。
- 视频及图片域名也必须能被小程序访问，视频地址由播放信息接口返回。

### 可选命令行构建

提供的构建脚本复用本机 HBuilderX 编译器，没有把项目迁移为 CLI 模板。

```powershell
$env:HBUILDERX_HOME = 'D:/HBuilderX3.99'
npm run build:mp-weixin
npm run build:h5
npm test
```

`HBUILDERX_HOME` 可省略：脚本会尝试本机的几个已发现安装位置。其他电脑请明确设置自己的安装目录。没有编译器时脚本会报错，不会伪造编译成功。

## 登录与用户数据

- 先读取 `/auth/mini/capabilities`；仅微信端且 `wechatLogin=true` 时展示微信快捷登录。
- 账号密码登录使用 `/auth/mini/login`。
- 微信登录使用临时 code 调用 `/auth/mini/wechat/login`；出现 `needsBinding` 时，用户选择绑定已有账号或创建新账号，再调用 `/auth/mini/wechat/complete`。
- 使用 Session 原样返回的 `authorization`，避免重复 Bearer。密码和微信 ticket 不持久化。
- 使用 `/auth/mini/me` 查询小程序用户，使用 `/auth/me` 读取/编辑资料；二者模型不同。
- token 失效清理会话；仅公开 GET 可作为游客重试一次，写操作不自动重放。
- 上传头像使用 `/files/upload`，随后调用 `/auth/me/avatar`。
- 本次本地后端的能力接口返回 `wechatLogin=false`，因此实际微信登录尚未验证。需配置真实 AppID 以及服务端微信能力后联调。

## 面试题收藏的已知后端问题

当前后端 `InterviewServiceImpl.listCollectedQuestions` 按 `collectCount > 0` 返回全站题目，而不是当前用户的收藏；收藏/取消收藏也只修改题目计数。

为避免误展示其他用户的数据，本次**不调用这组接口**。小程序明确标注“本机收藏”，使用 `matu-question-bookmarks-{userId}` 按账号保存题号、标题、分类及难度，不保存答案。不与 PC 同步；清理设备缓存可能丢失。

题目内容、答案权限和掌握状态仍使用真实后端。后端补齐用户收藏关系后，可替换 `utils/collections.js`；不能只将当前全站收藏接口直接接到“我的收藏”。

## 接口对应

| 模块 | 接口 |
| --- | --- |
| 文章 | `GET /posts`、`GET /posts/categories`、`GET /posts/{id}`；对应 like/collect/comments |
| 打卡 | `GET /checks`、`GET /checks/{id}`；对应 like/comments |
| 打卡筛选 | 仅使用接口支持的 year/month；没有伪造关键词筛选 |
| 问答 | `/qa/questions`、`/qa/categories`、题目详情/answers、follow/unfollow |
| 课程 | `/courses`、`/courses/{id}`、`/courses/videos/{id}/play` |
| 课程进度 | `/courses/{id}/progress/mine`、`POST /courses/{id}/progress` |
| 面试 | `/interview/categories`、`/interview/questions`、题目详情和 progress |
| 我的打卡 | `/checks?userId={当前登录用户}` |
| 我的问答 | `/qa/questions/mine` |
| 统计 | `/checks/statistics?userId={当前登录用户}` |

面试进度状态按后端定义：0 未做、1 已做、2 掌握、3 需复习。答案以 `answerVisible` 为最终判断依据，并兼容锁定标记；不会生成或伪造答案。

视频播放只采用 `/play` 授权响应，禁止回退到课程详情中的直链。恢复进度使用后端 `watchedDuration`，其当前实现保存最远观看位置；接近结束的未完成视频不会提前上报 100%。本期没有假购买入口。

## 工程结构

```text
api/            请求、上传及业务接口
components/     列表卡片、状态视图、头像、富文本正文
config/         网关与超时配置
pages/          四个 Tab 及详情、登录、个人资料等页面
static/         本地 Tab 图标与默认头像
styles/         统一设计样式
utils/          会话、分页、精度安全 JSON、收藏、视频进度
scripts/        HBuilderX 编译器调用脚本
tests/          Node 核心行为测试
```

网络层设置 `dataType: 'text'` 后再解析，避免长整型 ID 被原生 JSON 预先解析丢失精度。参考 [uni.request 官方文档](https://uniapp.dcloud.io/api/request/request)。

正文使用 `markdown-it` 与小程序兼容的 [mp-html](https://jin-yufeng.github.io/mp-html/)，Markdown 禁用内嵌 HTML；已有 HTML 交给组件解析。外链只能由用户点击后选择复制，不在页面中执行脚本或任意跳转。

## 验证记录（2026-10-04）

- 微信小程序生产构建通过（本机 HBuilderX Vue 3 编译器 4.75）。
- H5 生产构建通过。
- 11 个页面的 JS/JSON/WXML/WXSS 产物及 8 个 Tab 图标均存在。
- 15 项核心测试通过：长整数、业务/HTTP 错误、并发分页、分页失败重试、授权传递、过期重试边界、账号收藏隔离、有效期、旧账号迟到响应、空 401、视频进度与恢复位置、头像上传隔离及登录返回目标。
- H5 手机宽度下接入真实本地后端，验证首页文章/打卡/问答、文章详情富文本、面试题列表/详情、关键词搜索、末题禁用、登录表单校验、取消登录返回原详情、返回列表保留筛选、游客登录引导及个人中心；检查 320px 窄屏个人中心无横向溢出。
- 本地课程列表为空：已确认真实空态；视频实播、真实用户进度保存尚未实测。
- 没有使用真实账号密码测试登录、评论、资料上传等写入；没有进行微信开发者工具运行或真机验收，也没有上传/发布至微信。
- 编译器出现已有的 Browserslist 数据过旧提示，不影响本次构建。

预览图片保存在工作区 `docs/matu-liteapp-preview/`。H5 预览仅用于界面检查，不替代微信端验收。
