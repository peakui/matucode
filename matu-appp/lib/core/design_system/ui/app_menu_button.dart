import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';

import '../theme/app_metrics.dart';
import '../theme/app_tokens.dart';
import 'app_glass_surface.dart';
import 'app_list_row.dart';
import 'app_pressable.dart';
import 'app_spring_in.dart';

/// 下拉菜单选项。
class AppMenuOption<T> {
  /// 创建选项
  const AppMenuOption({required this.value, required this.label});

  /// 选项值
  final T value;

  /// 选项文案
  final String label;
}

/// iOS 风格下拉入口：当前值 + chevron，点击从底部弹出选项。
class AppMenuButton<T> extends StatelessWidget {
  /// 创建下拉入口
  const AppMenuButton({
    super.key,
    required this.options,
    required this.onChanged,
    this.value,
    this.placeholder,
  });

  /// 选项列表
  final List<AppMenuOption<T>> options;

  /// 选中变化回调
  final ValueChanged<T> onChanged;

  /// 当前值
  final T? value;

  /// 无选中值时的占位
  final String? placeholder;

  String get _label {
    for (final option in options) {
      if (option.value == value) return option.label;
    }
    return placeholder ?? (options.isEmpty ? '' : options.first.label);
  }

  Future<void> _open(BuildContext context) async {
    final tokens = context.tokens;
    final selected = await showModalBottomSheet<T>(
      context: context,
      backgroundColor: Colors.transparent,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(
          top: Radius.circular(AppRadius.sheet),
        ),
      ),
      builder: (sheetContext) => AppSpringIn(
        child: AppGlassSurface(
          radius: AppRadius.sheet,
          grouped: true,
          saturated: true,
          child: SafeArea(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const SizedBox(height: AppSpacing.sm),
                for (final option in options)
                  AppListRow(
                    title: option.label,
                    onTap: () => Navigator.of(sheetContext).pop(option.value),
                    trailing: option.value == value
                        ? Icon(
                            CupertinoIcons.checkmark,
                            size: 18,
                            color: tokens.accent,
                          )
                        : null,
                  ),
                const SizedBox(height: AppSpacing.sm),
              ],
            ),
          ),
        ),
      ),
    );
    if (selected != null) onChanged(selected);
  }

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    return AppPressable(
      onTap: () => _open(context),
      pressedScale: 0.97,
      child: Container(
        height: 36,
        padding: const EdgeInsets.symmetric(horizontal: AppSpacing.md),
        decoration: BoxDecoration(
          color: tokens.fillPrimary,
          borderRadius: BorderRadius.circular(AppRadius.pill),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              _label,
              style: TextStyle(
                fontSize: AppTypography.control,
                color: tokens.labelPrimary,
              ),
            ),
            const SizedBox(width: AppSpacing.xs),
            Icon(
              CupertinoIcons.chevron_down,
              size: 14,
              color: tokens.labelTertiary,
            ),
          ],
        ),
      ),
    );
  }
}
