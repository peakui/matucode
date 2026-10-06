/// iOS 风格几何常量：圆角、间距与层级。
///
/// 数值不依赖 [BuildContext]，可在任何构造期使用；色值请改用 `AppTokens`。
library;

import 'package:flutter/animation.dart';

//==================== 圆角 ====================
/// 圆角规范：卡片 16、按钮 12、输入框 10。
abstract final class AppRadius {
  /// 卡片、面板圆角
  static const double card = 16;

  /// 按钮圆角
  static const double button = 12;

  /// 输入框圆角
  static const double input = 10;

  /// 胶囊圆角，用于标签、分段控件
  static const double pill = 999;

  /// 底部弹层圆角
  static const double sheet = 20;
}

//==================== 间距（8pt 网格） ====================
/// 间距规范：以 8pt 为基准，内容左右内边距 20。
abstract final class AppSpacing {
  /// 基准单位
  static const double unit = 8;

  /// 4
  static const double xs = 4;

  /// 8
  static const double sm = 8;

  /// 12
  static const double md = 12;

  /// 16，模块间距
  static const double lg = 16;

  /// 20，内容左右内边距
  static const double contentPadding = 20;

  /// 24
  static const double xl = 24;

  /// 32
  static const double xxl = 32;

  /// 48
  static const double xxxl = 48;

  /// 列表行最小高度
  static const double listRowMinHeight = 48;

  /// 列表前置图标尺寸
  static const double listRowIconSize = 24;
}

//==================== 层级 ====================
/// 阴影层级，与 0.5px 发丝描边配合使用。
abstract final class AppElevation {
  /// 卡片阴影高度
  static const double card = 2;

  /// 发丝描边宽度
  static const double hairline = 0.5;
}

/// 紧凑阅读排版；使用逻辑像素并保留系统无障碍文字缩放。
abstract final class AppTypography {
  /// 首页标题。
  static const double pageTitle = 24;

  /// 完整文章标题。
  static const double articleTitle = 21;

  /// 正文小标题。
  static const double sectionTitle = 16;

  /// 列表卡片标题。
  static const double cardTitle = 15;

  /// 阅读正文。
  static const double body = 14;

  /// 控件文字。
  static const double control = 13;

  /// 摘要。
  static const double summary = 12.5;

  /// 元信息与代码。
  static const double caption = 11.5;

  /// 舒适阅读行高。
  static const double readingHeight = 1.75;
}

/// 毛玻璃强度与阅读宽度，所有表面共用同一组设计常量。
abstract final class AppGlass {
  /// 局部模糊半径，裁剪在卡片内，避免全屏反复滤镜。
  static const double blur = 12;

  /// 液态玻璃强化模糊半径，用于悬浮 chrome。
  static const double blurStrong = 24;

  /// 页面桌面端阅读宽度。
  static const double readingWidth = 760;

  /// 浅色表面不透明度。
  static const double lightOpacity = 0.72;

  /// 深色表面不透明度。
  static const double darkOpacity = 0.80;

  /// 底部弹层模糊半径，略强于常规表面以突出层次。
  static const double sheetBlur = 18;

  /// 玻璃表面饱和度增益，让透出的色彩更鲜艳。
  static const double saturation = 1.35;

  /// 悬浮胶囊左右留边。
  static const double tabMargin = 12;

  /// 悬浮胶囊内容高度（不含安全区）。
  static const double tabHeight = 60;

  /// 胶囊底部与屏幕的间隙。
  static const double tabBottomGap = 8;
}

/// 动效时长与曲线；所有动效均为一次性，禁止循环，否则会挂死 `pumpAndSettle`。
abstract final class AppMotion {
  /// 按压等即时反馈。
  static const Duration fast = Duration(milliseconds: 160);

  /// 按下回弹（短促，跟手）。
  static const Duration press = Duration(milliseconds: 90);

  /// 抬起回弹（略慢，带过冲）。
  static const Duration release = Duration(milliseconds: 160);

  /// 内容切换、淡入。
  static const Duration medium = Duration(milliseconds: 280);

  /// 高光流动等较长过渡。
  static const Duration slow = Duration(milliseconds: 600);

  /// 列表入场总时长。
  static const Duration entrance = Duration(milliseconds: 420);

  /// 错峰入场每项延迟比例。
  static const double staggerStep = 0.08;

  /// 回弹曲线，用于按压与弹出。
  static const Curve spring = Curves.easeOutBack;

  /// 强调曲线，用于滑动指示条。
  static const Curve emphasized = Curves.easeOutCubic;
}
