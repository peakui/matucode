import 'package:flutter/cupertino.dart';

import '../theme/app_metrics.dart';
import '../theme/app_tokens.dart';
import 'app_button.dart';

/// 错误提示：文案 + 可选重试操作。空文案时完全不占位。
class AppErrorState extends StatelessWidget {
  /// 创建错误态
  const AppErrorState({
    super.key,
    required this.message,
    this.retryLabel,
    this.onRetry,
  });

  /// 错误文案
  final String message;

  /// 重试按钮文案
  final String? retryLabel;

  /// 重试回调
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    if (message.isEmpty) return const SizedBox.shrink();
    final tokens = context.tokens;
    return Padding(
      padding: const EdgeInsets.all(AppSpacing.lg),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(
            CupertinoIcons.exclamationmark_circle,
            size: 28,
            color: tokens.systemOrange,
          ),
          const SizedBox(height: AppSpacing.sm),
          Text(
            message,
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: AppTypography.control,
              color: tokens.labelSecondary,
            ),
          ),
          if (retryLabel != null && onRetry != null) ...[
            const SizedBox(height: AppSpacing.md),
            AppButton(
              label: retryLabel!,
              variant: AppButtonVariant.secondary,
              onPressed: onRetry,
            ),
          ],
        ],
      ),
    );
  }
}
