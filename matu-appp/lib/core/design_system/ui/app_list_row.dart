import 'package:flutter/cupertino.dart';

import '../theme/app_metrics.dart';
import '../theme/app_tokens.dart';
import 'app_pressable.dart';

/// iOS 风格列表行：行高 48–56、24px 前置图标、尾部 chevron 或状态。
class AppListRow extends StatelessWidget {
  /// 创建列表行
  const AppListRow({
    super.key,
    this.leading,
    this.title,
    this.subtitle,
    this.trailing,
    this.onTap,
    this.showChevron = false,
    this.minHeight = AppSpacing.listRowMinHeight,
    this.padding = const EdgeInsets.symmetric(
      horizontal: AppSpacing.lg,
      vertical: AppSpacing.md,
    ),
  });

  /// 前置图标
  final Widget? leading;

  /// 主标题
  final String? title;

  /// 副标题
  final String? subtitle;

  /// 尾部自定义内容
  final Widget? trailing;

  /// 点击回调
  final VoidCallback? onTap;

  /// 是否展示尾部 chevron
  final bool showChevron;

  /// 最小行高
  final double minHeight;

  /// 内容内边距
  final EdgeInsetsGeometry padding;

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    final texts = Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        if (title != null)
          Text(
            title!,
            style: TextStyle(
              fontSize: AppTypography.body,
              height: 1.3,
              color: tokens.labelPrimary,
            ),
          ),
        if (subtitle != null)
          Padding(
            padding: const EdgeInsets.only(top: 2),
            child: Text(
              subtitle!,
              style: TextStyle(
                fontSize: AppTypography.summary,
                height: 1.3,
                color: tokens.labelSecondary,
              ),
            ),
          ),
      ],
    );

    final content = ConstrainedBox(
      constraints: BoxConstraints(minHeight: minHeight),
      child: Padding(
        padding: padding,
        child: Row(
          children: [
            if (leading != null) ...[
              SizedBox(
                width: AppSpacing.listRowIconSize,
                height: AppSpacing.listRowIconSize,
                child: Center(child: leading),
              ),
              const SizedBox(width: AppSpacing.md),
            ],
            Expanded(child: texts),
            if (trailing != null) ...[
              const SizedBox(width: AppSpacing.sm),
              trailing!,
            ],
            if (showChevron) ...[
              const SizedBox(width: AppSpacing.xs),
              Icon(
                CupertinoIcons.chevron_forward,
                size: 14,
                color: tokens.labelTertiary,
              ),
            ],
          ],
        ),
      ),
    );

    if (onTap == null) return content;
    return AppPressable(
      onTap: onTap,
      behavior: HitTestBehavior.opaque,
      child: content,
    );
  }
}
