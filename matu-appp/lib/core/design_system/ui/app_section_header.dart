import 'package:flutter/material.dart';

import '../theme/app_metrics.dart';
import '../theme/app_tokens.dart';

/// 分组标题，可携带尾部操作。
class AppSectionHeader extends StatelessWidget {
  /// 创建分组标题
  const AppSectionHeader(this.title, {super.key, this.action, this.padding});

  /// 标题文案
  final String title;

  /// 尾部操作
  final Widget? action;

  /// 自定义内边距
  final EdgeInsetsGeometry? padding;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding:
          padding ??
          const EdgeInsets.fromLTRB(
            AppSpacing.contentPadding,
            AppSpacing.xl,
            AppSpacing.contentPadding,
            AppSpacing.sm,
          ),
      child: Row(
        children: [
          Expanded(
            child: Text(
              title,
              style: TextStyle(
                fontSize: AppTypography.summary,
                letterSpacing: 0.4,
                fontWeight: FontWeight.w600,
                color: context.tokens.labelSecondary,
              ),
            ),
          ),
          if (action != null) action!,
        ],
      ),
    );
  }
}
