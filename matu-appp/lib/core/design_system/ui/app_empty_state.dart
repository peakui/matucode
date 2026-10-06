import 'package:flutter/cupertino.dart';

import '../theme/app_metrics.dart';
import '../theme/app_tokens.dart';
import 'app_button.dart';

/// 空数据占位：图标 + 文案 + 可选操作。
class AppEmptyState extends StatelessWidget {
  /// 创建空态
  const AppEmptyState({
    super.key,
    required this.message,
    this.icon = CupertinoIcons.tray,
    this.actionLabel,
    this.onAction,
  });

  /// 提示文案
  final String message;

  /// 图标
  final IconData icon;

  /// 操作文案
  final String? actionLabel;

  /// 操作回调
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(AppSpacing.xl),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: AppSpacing.xxxl, color: tokens.labelQuaternary),
            const SizedBox(height: AppSpacing.md),
            Text(
              message,
              textAlign: TextAlign.center,
              style: TextStyle(
                fontSize: AppTypography.body,
                color: tokens.labelSecondary,
              ),
            ),
            if (actionLabel != null && onAction != null) ...[
              const SizedBox(height: AppSpacing.lg),
              AppButton(
                label: actionLabel!,
                variant: AppButtonVariant.secondary,
                onPressed: onAction,
              ),
            ],
          ],
        ),
      ),
    );
  }
}
