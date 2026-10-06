import 'package:flutter/material.dart';
import '../theme/app_metrics.dart';

import '../theme/app_tokens.dart';

/// 学习足迹统计：数值 + 标签。
class AppStatTile extends StatelessWidget {
  /// 创建统计块
  const AppStatTile({super.key, required this.value, required this.label});

  /// 数值文案
  final String value;

  /// 标签文案
  final String label;

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          value,
          style: TextStyle(
            fontSize: AppTypography.pageTitle,
            fontWeight: FontWeight.w700,
            height: 1.1,
            color: tokens.accent,
          ),
        ),
        const SizedBox(height: 2),
        Text(
          label,
          style: TextStyle(
            fontSize: AppTypography.summary,
            color: tokens.labelSecondary,
          ),
        ),
      ],
    );
  }
}
