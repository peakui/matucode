# matu-appp · 码途 App

基于 FlutterKit，将 `matu-liteapp` 微信小程序的学习功能迁移到 Flutter 原生页面。主导航为 **首页 / 教程 / 面试 / 我的**，已接入真实码途接口。

## 功能

- 文章、打卡、问答：独立筛选与分页、下拉刷新、列表位置保留、富文本、评论/回答、点赞、文章收藏、问题关注。
- 教程：搜索、难度、排序、课程章节、授权视频、恢复进度、15 秒节流保存、暂停/后台保存。
- 面试：分类、关键词、难度、答案权限、上一题/下一题、掌握状态、按账号隔离的本机收藏。
- 我的：游客引导、账号密码登录、真实学习统计、我的打卡/问答、资料与头像、主题、实际安装包版本和退出。
- 原生分享：未设置网站域名时分享标题；设置 `WEB_BASE_URL` 后附带真实网站对应内容链接。

页面与接口说明见 [APP功能与接口](docs/APP功能与接口.md)，实施需求见 [APP开发提示词](APP开发提示词.md)，上游框架文档保存在 [README_FLUTTERKIT.md](README_FLUTTERKIT.md)。

## 开发环境

本次使用 Flutter **3.44.0** / Dart **3.12.0**。当前依赖锁定文件需要这一代 SDK；不要用机器上旧的 Flutter 3.24.5。

工作区独立 SDK 位于 `../.tools/flutter-3.44.0`，来自 Flutter 官方 3.44.0 源码归档及其固定引擎产物。没有替换系统 Flutter。`tool/flutter.ps1` 优先使用该 SDK，并仅在当前进程关闭桌面插件符号链接生成，避免 Windows 开发者模式要求影响移动端工作。

```powershell
# 在 matu-appp 目录执行
.\tool\flutter.ps1 pub get
..\.tools\flutter-3.44.0\bin\dart.bat run build_runner build
.\tool\flutter.ps1 analyze --no-pub
.\tool\flutter.ps1 test --no-pub
.\tool\flutter.ps1 run -d <设备ID> --dart-define=API_BASE_URL=http://<开发机局域网IP>:8080
```

其他电脑安装相容 Flutter 后，可直接使用标准 `flutter` / `dart` 命令。

## 网关配置与安装包

调试环境默认：Android 模拟器使用 `http://10.0.2.2:8080`，桌面/Web 使用 `http://127.0.0.1:8080`。**真机需要通过 `API_BASE_URL` 传入手机可达的开发机地址或测试域名。** PC 的 Vite `/api` 前缀不能直接搬入网关地址。

```powershell
.\tool\flutter.ps1 build apk --debug --no-pub --target-platform android-arm64,android-x64 --dart-define=API_BASE_URL=http://<开发机局域网IP>:8080
# 可选：--dart-define=WEB_BASE_URL=https://<实际码途网站域名>
```

构建产物位于 `build/app/outputs/flutter-apk/app-debug.apk`；交付副本位于 `dist/matu-appp-debug.apk`。当前交付包使用默认模拟器网关，包含 arm64 与 x64，是开发调试包。

`ENV=dev/test/pre/prod` 保留原框架环境标志。非开发环境必须配置网关；Release 必须显式传入真实 HTTPS 地址。Debug Manifest 允许开发 HTTP，Release 未扩大明文访问权限。

## 验证

- 静态检查：`flutter analyze --no-pub` 无问题。
- 全量测试：**339 项通过**，默认跳过 5 项显式开启的真实网关测试。
- 真实网关：文章、打卡、问答、课程、面试 **5 项只读契约联调通过**。
- 页面测试：320×640、640×360、800×1000，覆盖深浅色、四入口、空登录校验、受限答案、课程目录。
- Android：arm64+x64 调试 APK 构建通过；Android 14 x64 模拟器启动并查看真实列表、内容详情、课程空态、游客个人中心。

```powershell
.\tool\flutter.ps1 test --no-pub --dart-define=MATU_LIVE_API=true test/learning/matu_live_api_test.dart
```

详细记录、截图和未验证项见 [验证说明](docs/verification/验证说明.md)。

## 兼容处理与边界

- TDesign 0.2.7 图标类继承了 Flutter 3.44 已标为 final 的 IconData。项目内 `vendor/tdesign_flutter` 保留上游许可证，使用可重现脚本重新生成图标常量；未修改共享 Pub 缓存。见 [兼容说明](vendor/tdesign_flutter/MATU_COMPATIBILITY.md)。
- Windows 的工程在 E 盘、Pub 缓存在 C 盘；已关闭该工程的 Kotlin 增量缓存，解决跨盘相对路径构建失败。
- 真实账号登录后的写入、头像上传、实际视频播放及进度落库仍需账号和课程数据验收；目前只读联调中的课程列表为空。
- 原生微信登录尚未接入；没有复用小程序微信 code 冒充 App 登录。iOS 未在 Xcode 或真机验证。
- 原生应用标识、图标、启动页与版权仍沿用模板。调试包标识为 `com.joker.flutterkit.debug`；正式发行前需实际品牌、包标识及签名。模板 release 仍使用 debug 签名，当前未生成商店发布包。
- 当前工作目录没有 Git 元数据，未创建提交或 PR。
