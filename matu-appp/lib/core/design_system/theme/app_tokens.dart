import 'package:flutter/material.dart';
import 'package:tdesign_flutter/tdesign_flutter.dart';

import 'app_metrics.dart';

/// iOS 语义色令牌。
///
/// 独立于 TDesign 的 `whiteColor1` 等表面色：TDesign 主题预设未覆盖深浅两套
/// 表面色，暗色模式下仍是白底，因此新组件层统一从这里取色。
@immutable
class AppTokens extends ThemeExtension<AppTokens> {
  /// 创建语义色令牌
  const AppTokens({
    required this.brightness,
    required this.accent,
    required this.onAccent,
    required this.accentSoft,
    required this.bgPage,
    required this.bgSurface,
    required this.bgSurfaceSecondary,
    required this.hairline,
    required this.separator,
    required this.labelPrimary,
    required this.labelSecondary,
    required this.labelTertiary,
    required this.labelQuaternary,
    required this.fillPrimary,
    required this.fillSecondary,
    required this.systemRed,
    required this.systemGreen,
    required this.systemOrange,
    required this.systemBlue,
    required this.systemGray,
    required this.cardShadow,
    required this.bgGradientTop,
    required this.bgGradientBottom,
    required this.glassTint,
    required this.glassSheen,
    required this.glassEdge,
    required this.glassHighlight,
    required this.glassRim,
  });

  /// 明暗模式标识
  final Brightness brightness;

  //==================== 强调色 ====================
  /// 主强调色，沿用当前 TDesign 品牌预设（默认蓝）
  final Color accent;

  /// 强调色上的前景色
  final Color onAccent;

  /// 强调色的浅色底，用于选中胶囊、浅色标签
  final Color accentSoft;

  //==================== 表面 ====================
  /// 页面背景
  final Color bgPage;

  /// 卡片、导航栏等一级表面
  final Color bgSurface;

  /// 二级分组表面（列表分组底、填充控件底）
  final Color bgSurfaceSecondary;

  /// 0.5px 发丝描边
  final Color hairline;

  /// 列表分隔线
  final Color separator;

  //==================== 文字三级 ====================
  /// 一级文字：标题、正文
  final Color labelPrimary;

  /// 二级文字：副标题、说明
  final Color labelSecondary;

  /// 三级文字：占位、时间戳
  final Color labelTertiary;

  /// 四级文字：禁用、极弱提示
  final Color labelQuaternary;

  //==================== 填充 ====================
  /// 主要填充：输入框、分段控件底
  final Color fillPrimary;

  /// 次要填充：浅色按钮底
  final Color fillSecondary;

  //==================== 状态色 ====================
  /// 危险 / 删除
  final Color systemRed;

  /// 成功
  final Color systemGreen;

  /// 警告
  final Color systemOrange;

  /// 信息
  final Color systemBlue;

  /// 中性灰
  final Color systemGray;

  //==================== 阴影 ====================
  /// 卡片阴影（elevation 2）
  final List<BoxShadow> cardShadow;

  //==================== 中性背景与液态玻璃 ====================
  /// 页面渐变起点，纯净中性，零彩色。
  final Color bgGradientTop;

  /// 页面渐变终点，亮色略灰、暗色近黑。
  final Color bgGradientBottom;

  /// 玻璃底色，零彩度，仅靠不透明度叠加出层次。
  final Color glassTint;

  /// 流动的镜面高光。
  final Color glassSheen;

  /// 玻璃底部边缘暗描边，制造厚度感。
  final Color glassEdge;

  /// 玻璃顶边高光。
  final Color glassHighlight;

  /// 玻璃四周描边，比高光更暗以形成边缘层次。
  final Color glassRim;

  /// 从 [BuildContext] 获取当前令牌
  static AppTokens of(BuildContext context) =>
      Theme.of(context).extension<AppTokens>() ??
      AppTokens.fromTdTheme(TDThemeData.defaultData().light, Brightness.light);

  /// 依据 [tdTheme] 生成明暗两套令牌
  factory AppTokens.fromTdTheme(TDThemeData tdTheme, Brightness brightness) {
    final accent = tdTheme.brandColor7;
    final onAccent =
        ThemeData.estimateBrightnessForColor(accent) == Brightness.dark
        ? Colors.white
        : Colors.black;
    return brightness == Brightness.dark
        ? AppTokens._dark(accent: accent, onAccent: onAccent)
        : AppTokens._light(accent: accent, onAccent: onAccent);
  }

  factory AppTokens._light({required Color accent, required Color onAccent}) {
    return AppTokens(
      brightness: Brightness.light,
      accent: accent,
      onAccent: onAccent,
      accentSoft: accent.withValues(alpha: 0.12),
      bgPage: const Color(0xFFF2F2F7),
      bgSurface: const Color(0xFFFFFFFF),
      bgSurfaceSecondary: const Color(0xFFF2F2F7),
      hairline: const Color(0x1F000000),
      separator: const Color(0x2E3C3C43),
      labelPrimary: const Color(0xFF000000),
      labelSecondary: const Color(0xFF677184),
      labelTertiary: const Color(0xFF7C8799),
      labelQuaternary: const Color(0x2E3C3C43),
      fillPrimary: const Color(0x1F787880),
      fillSecondary: const Color(0x14787880),
      systemRed: const Color(0xFFFF3B30),
      systemGreen: const Color(0xFF34C759),
      systemOrange: const Color(0xFFFF9500),
      systemBlue: const Color(0xFF007AFF),
      systemGray: const Color(0xFF8E8E93),
      cardShadow: const [
        BoxShadow(
          color: Color(0x14000000),
          blurRadius: 10,
          spreadRadius: -2,
          offset: Offset(0, 2),
        ),
      ],
      bgGradientTop: const Color(0xFFFFFFFF),
      bgGradientBottom: const Color(0xFFF2F2F7),
      glassTint: const Color(0xFFFFFFFF),
      glassSheen: const Color(0x80FFFFFF),
      glassEdge: const Color(0x0F000000),
      glassHighlight: const Color(0xE6FFFFFF),
      glassRim: const Color(0x2EFFFFFF),
    );
  }

  factory AppTokens._dark({required Color accent, required Color onAccent}) {
    return AppTokens(
      brightness: Brightness.dark,
      accent: accent,
      onAccent: onAccent,
      accentSoft: accent.withValues(alpha: 0.24),
      bgPage: const Color(0xFF08080B),
      bgSurface: const Color(0xFF1C1C1E),
      bgSurfaceSecondary: const Color(0xFF2C2C2E),
      hairline: const Color(0x26FFFFFF),
      separator: const Color(0x3D545458),
      labelPrimary: const Color(0xFFFFFFFF),
      labelSecondary: const Color(0x99EBEBF5),
      labelTertiary: const Color(0x4DEBEBF5),
      labelQuaternary: const Color(0x2EEBEBF5),
      fillPrimary: const Color(0x2E787880),
      fillSecondary: const Color(0x1F787880),
      systemRed: const Color(0xFFFF453A),
      systemGreen: const Color(0xFF30D158),
      systemOrange: const Color(0xFFFF9F0A),
      systemBlue: const Color(0xFF0A84FF),
      systemGray: const Color(0xFF8E8E93),
      cardShadow: const [
        BoxShadow(
          color: Color(0x66000000),
          blurRadius: 10,
          spreadRadius: -2,
          offset: Offset(0, 2),
        ),
      ],
      bgGradientTop: const Color(0xFF141419),
      bgGradientBottom: const Color(0xFF08080B),
      glassTint: const Color(0xFF2C2C31),
      glassSheen: const Color(0x40FFFFFF),
      glassEdge: const Color(0x66000000),
      glassHighlight: const Color(0x40FFFFFF),
      glassRim: const Color(0x24FFFFFF),
    );
  }

  @override
  AppTokens copyWith({
    Brightness? brightness,
    Color? accent,
    Color? onAccent,
    Color? accentSoft,
    Color? bgPage,
    Color? bgSurface,
    Color? bgSurfaceSecondary,
    Color? hairline,
    Color? separator,
    Color? labelPrimary,
    Color? labelSecondary,
    Color? labelTertiary,
    Color? labelQuaternary,
    Color? fillPrimary,
    Color? fillSecondary,
    Color? systemRed,
    Color? systemGreen,
    Color? systemOrange,
    Color? systemBlue,
    Color? systemGray,
    List<BoxShadow>? cardShadow,
    Color? bgGradientTop,
    Color? bgGradientBottom,
    Color? glassTint,
    Color? glassSheen,
    Color? glassEdge,
    Color? glassHighlight,
    Color? glassRim,
  }) {
    return AppTokens(
      brightness: brightness ?? this.brightness,
      accent: accent ?? this.accent,
      onAccent: onAccent ?? this.onAccent,
      accentSoft: accentSoft ?? this.accentSoft,
      bgPage: bgPage ?? this.bgPage,
      bgSurface: bgSurface ?? this.bgSurface,
      bgSurfaceSecondary: bgSurfaceSecondary ?? this.bgSurfaceSecondary,
      hairline: hairline ?? this.hairline,
      separator: separator ?? this.separator,
      labelPrimary: labelPrimary ?? this.labelPrimary,
      labelSecondary: labelSecondary ?? this.labelSecondary,
      labelTertiary: labelTertiary ?? this.labelTertiary,
      labelQuaternary: labelQuaternary ?? this.labelQuaternary,
      fillPrimary: fillPrimary ?? this.fillPrimary,
      fillSecondary: fillSecondary ?? this.fillSecondary,
      systemRed: systemRed ?? this.systemRed,
      systemGreen: systemGreen ?? this.systemGreen,
      systemOrange: systemOrange ?? this.systemOrange,
      systemBlue: systemBlue ?? this.systemBlue,
      systemGray: systemGray ?? this.systemGray,
      cardShadow: cardShadow ?? this.cardShadow,
      bgGradientTop: bgGradientTop ?? this.bgGradientTop,
      bgGradientBottom: bgGradientBottom ?? this.bgGradientBottom,
      glassTint: glassTint ?? this.glassTint,
      glassSheen: glassSheen ?? this.glassSheen,
      glassEdge: glassEdge ?? this.glassEdge,
      glassHighlight: glassHighlight ?? this.glassHighlight,
      glassRim: glassRim ?? this.glassRim,
    );
  }

  @override
  AppTokens lerp(covariant ThemeExtension<AppTokens>? other, double t) {
    if (other is! AppTokens) return this;
    return AppTokens(
      brightness: t < 0.5 ? brightness : other.brightness,
      accent: Color.lerp(accent, other.accent, t)!,
      onAccent: Color.lerp(onAccent, other.onAccent, t)!,
      accentSoft: Color.lerp(accentSoft, other.accentSoft, t)!,
      bgPage: Color.lerp(bgPage, other.bgPage, t)!,
      bgSurface: Color.lerp(bgSurface, other.bgSurface, t)!,
      bgSurfaceSecondary: Color.lerp(
        bgSurfaceSecondary,
        other.bgSurfaceSecondary,
        t,
      )!,
      hairline: Color.lerp(hairline, other.hairline, t)!,
      separator: Color.lerp(separator, other.separator, t)!,
      labelPrimary: Color.lerp(labelPrimary, other.labelPrimary, t)!,
      labelSecondary: Color.lerp(labelSecondary, other.labelSecondary, t)!,
      labelTertiary: Color.lerp(labelTertiary, other.labelTertiary, t)!,
      labelQuaternary: Color.lerp(labelQuaternary, other.labelQuaternary, t)!,
      fillPrimary: Color.lerp(fillPrimary, other.fillPrimary, t)!,
      fillSecondary: Color.lerp(fillSecondary, other.fillSecondary, t)!,
      systemRed: Color.lerp(systemRed, other.systemRed, t)!,
      systemGreen: Color.lerp(systemGreen, other.systemGreen, t)!,
      systemOrange: Color.lerp(systemOrange, other.systemOrange, t)!,
      systemBlue: Color.lerp(systemBlue, other.systemBlue, t)!,
      systemGray: Color.lerp(systemGray, other.systemGray, t)!,
      cardShadow: BoxShadow.lerpList(cardShadow, other.cardShadow, t)!,
      bgGradientTop: Color.lerp(bgGradientTop, other.bgGradientTop, t)!,
      bgGradientBottom: Color.lerp(
        bgGradientBottom,
        other.bgGradientBottom,
        t,
      )!,
      glassTint: Color.lerp(glassTint, other.glassTint, t)!,
      glassSheen: Color.lerp(glassSheen, other.glassSheen, t)!,
      glassEdge: Color.lerp(glassEdge, other.glassEdge, t)!,
      glassHighlight: Color.lerp(glassHighlight, other.glassHighlight, t)!,
      glassRim: Color.lerp(glassRim, other.glassRim, t)!,
    );
  }
}

/// 便捷读取令牌的 [BuildContext] 扩展
extension AppTokensContext on BuildContext {
  /// 当前主题的语义色令牌
  AppTokens get tokens => AppTokens.of(this);

  /// 是否为暗色模式
  bool get isDarkMode => Theme.of(this).brightness == Brightness.dark;
}

/// 卡片统一的圆角 + 发丝描边 + 阴影装饰
BoxDecoration appCardDecoration(AppTokens tokens) => BoxDecoration(
  color: tokens.bgSurface,
  borderRadius: BorderRadius.circular(AppRadius.card),
  border: Border.all(color: tokens.hairline, width: AppElevation.hairline),
  boxShadow: tokens.cardShadow,
);
