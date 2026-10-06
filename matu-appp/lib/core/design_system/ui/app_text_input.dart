import 'package:flutter/cupertino.dart';

import '../theme/app_metrics.dart';
import '../theme/app_tokens.dart';

/// iOS 风格输入框：10px 圆角、填充底、发丝描边、强调色光标。
class AppTextInput extends StatelessWidget {
  /// 创建输入框
  const AppTextInput({
    super.key,
    required this.controller,
    this.hint,
    this.obscureText = false,
    this.maxLines = 1,
    this.onChanged,
    this.keyboardType,
    this.prefix,
    this.focusNode,
  });

  /// 文本控制器
  final TextEditingController controller;

  /// 占位文案
  final String? hint;

  /// 是否密文
  final bool obscureText;

  /// 行数
  final int maxLines;

  /// 内容变化回调
  final ValueChanged<String>? onChanged;

  /// 键盘类型
  final TextInputType? keyboardType;

  /// 前置图标
  final Widget? prefix;

  /// 焦点节点
  final FocusNode? focusNode;

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    return CupertinoTextField(
      controller: controller,
      focusNode: focusNode,
      placeholder: hint,
      obscureText: obscureText,
      maxLines: maxLines,
      onChanged: onChanged,
      keyboardType: keyboardType,
      padding: const EdgeInsets.symmetric(
        horizontal: AppSpacing.lg,
        vertical: 13,
      ),
      cursorColor: tokens.accent,
      style: TextStyle(
        fontSize: AppTypography.body,
        color: tokens.labelPrimary,
      ),
      placeholderStyle: TextStyle(
        fontSize: AppTypography.body,
        color: tokens.labelTertiary,
      ),
      prefix: prefix == null
          ? null
          : Padding(
              padding: const EdgeInsets.only(left: AppSpacing.md),
              child: prefix,
            ),
      decoration: BoxDecoration(
        color: tokens.fillPrimary,
        borderRadius: BorderRadius.circular(AppRadius.input),
        border: Border.all(
          color: tokens.hairline,
          width: AppElevation.hairline,
        ),
      ),
    );
  }
}
