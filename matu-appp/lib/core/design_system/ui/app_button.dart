import 'package:flutter/material.dart';

import '../theme/app_metrics.dart';
import '../theme/app_tokens.dart';
import 'app_pressable.dart';

/// 按钮视觉变体
enum AppButtonVariant {
  /// 强调色实心
  primary,

  /// 浅色填充
  secondary,

  /// 纯文字
  plain,

  /// 危险操作
  destructive,
}

/// iOS 风格按钮：12px 圆角、48 高、内置按压态。
class AppButton extends StatelessWidget {
  /// 创建按钮
  const AppButton({
    super.key,
    required this.label,
    this.onPressed,
    this.variant = AppButtonVariant.primary,
    this.icon,
    this.expand = false,
    this.busy = false,
    this.enabled = true,
  });

  /// 按钮文案
  final String label;

  /// 点击回调
  final VoidCallback? onPressed;

  /// 视觉变体
  final AppButtonVariant variant;

  /// 前置图标
  final IconData? icon;

  /// 是否占满可用宽度
  final bool expand;

  /// 是否展示进行中状态
  final bool busy;

  /// 是否可用
  final bool enabled;

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    final active = enabled && !busy && onPressed != null;
    final (background, foreground) = switch (variant) {
      AppButtonVariant.primary => (tokens.accent, tokens.onAccent),
      AppButtonVariant.secondary => (tokens.fillPrimary, tokens.accent),
      AppButtonVariant.plain => (Colors.transparent, tokens.accent),
      AppButtonVariant.destructive => (tokens.systemRed, Colors.white),
    };

    final content = Row(
      mainAxisSize: expand ? MainAxisSize.max : MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        if (busy)
          SizedBox(
            width: 18,
            height: 18,
            child: CircularProgressIndicator(strokeWidth: 2, color: foreground),
          )
        else if (icon != null)
          Icon(icon, size: 18, color: foreground),
        if (busy || icon != null) const SizedBox(width: AppSpacing.sm),
        Text(
          label,
          style: TextStyle(
            fontSize: AppTypography.body,
            fontWeight: FontWeight.w600,
            color: foreground,
          ),
        ),
      ],
    );

    final body = Container(
      height: 48,
      padding: const EdgeInsets.symmetric(horizontal: AppSpacing.lg),
      alignment: Alignment.center,
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(AppRadius.button),
      ),
      child: content,
    );

    if (!active) {
      return Opacity(opacity: 0.45, child: IgnorePointer(child: body));
    }
    return AppPressable(
      onTap: onPressed,
      behavior: HitTestBehavior.opaque,
      child: body,
    );
  }
}

/// 圆形图标按钮，用于导航栏与紧凑操作位。
class AppIconButton extends StatelessWidget {
  /// 创建图标按钮
  const AppIconButton({
    super.key,
    required this.icon,
    this.onPressed,
    this.color,
    this.size = 20,
    this.diameter = 36,
  });

  /// 图标
  final IconData icon;

  /// 点击回调
  final VoidCallback? onPressed;

  /// 图标与背景色的覆盖值
  final Color? color;

  /// 图标尺寸
  final double size;

  /// 直径
  final double diameter;

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    final tint = color ?? tokens.accent;
    return AppPressable(
      onTap: onPressed,
      child: Container(
        width: diameter,
        height: diameter,
        decoration: BoxDecoration(
          color: tokens.fillPrimary,
          shape: BoxShape.circle,
        ),
        child: Icon(icon, size: size, color: tint),
      ),
    );
  }
}
