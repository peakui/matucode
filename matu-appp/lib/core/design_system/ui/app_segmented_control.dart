import 'package:flutter/cupertino.dart';

import '../theme/app_metrics.dart';
import '../theme/app_tokens.dart';

/// iOS 追踪式分段控件。
class AppSegmentedControl<T extends Object> extends StatelessWidget {
  /// 创建分段控件
  const AppSegmentedControl({
    super.key,
    required this.values,
    required this.labels,
    required this.selected,
    required this.onChanged,
  }) : assert(values.length == labels.length, 'values 与 labels 长度必须一致');

  /// 各分段取值
  final List<T> values;

  /// 各分段文案
  final List<String> labels;

  /// 当前选中值
  final T selected;

  /// 选中变化回调
  final ValueChanged<T> onChanged;

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    return CupertinoSlidingSegmentedControl<T>(
      groupValue: selected,
      onValueChanged: (value) {
        if (value != null) onChanged(value);
      },
      backgroundColor: tokens.fillPrimary,
      thumbColor: tokens.bgSurface,
      children: {
        for (var i = 0; i < values.length; i++)
          values[i]: Padding(
            padding: const EdgeInsets.symmetric(vertical: AppSpacing.sm),
            child: Text(
              labels[i],
              style: TextStyle(
                fontSize: AppTypography.control,
                fontWeight: FontWeight.w500,
                color: values[i] == selected
                    ? tokens.labelPrimary
                    : tokens.labelSecondary,
              ),
            ),
          ),
      },
    );
  }
}
