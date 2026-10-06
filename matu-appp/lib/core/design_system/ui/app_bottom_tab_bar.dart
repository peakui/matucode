import 'package:flutter/material.dart';

import '../theme/app_metrics.dart';
import '../theme/app_tokens.dart';
import 'app_pressable.dart';
import 'app_glass_surface.dart';

/// 底部标签项配置。
class AppTabItem {
  /// 创建标签项
  const AppTabItem({
    required this.label,
    required this.icon,
    required this.selectedIcon,
  });

  /// 文案
  final String label;

  /// 未选中图标
  final IconData icon;

  /// 选中图标
  final IconData selectedIcon;
}

/// iOS 风格悬浮胶囊标签栏：左右留边、大圆角、浮于内容之上，选中项带滑动玻璃指示条。
class AppBottomTabBar extends StatelessWidget {
  /// 创建底部标签栏
  const AppBottomTabBar({
    super.key,
    required this.currentIndex,
    required this.items,
    required this.onTap,
    this.height = AppGlass.tabHeight,
  });

  /// 当前索引
  final int currentIndex;

  /// 标签项
  final List<AppTabItem> items;

  /// 点击回调
  final ValueChanged<int> onTap;

  /// 内容区高度（不含安全区）
  final double height;

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    return Padding(
      padding: const EdgeInsets.fromLTRB(
        AppGlass.tabMargin,
        0,
        AppGlass.tabMargin,
        AppGlass.tabBottomGap,
      ),
      child: SafeArea(
        top: false,
        child: AppGlassSurface(
          radius: AppRadius.pill,
          grouped: true,
          saturated: true,
          child: SizedBox(
            height: height,
            child: Stack(
              children: [
                Positioned.fill(
                  child: IgnorePointer(
                    child: AnimatedAlign(
                      alignment: Alignment(_indicatorAlignment, 0),
                      duration: AppMotion.medium,
                      curve: AppMotion.emphasized,
                      child: FractionallySizedBox(
                        widthFactor: 1 / items.length,
                        child: Padding(
                          padding: const EdgeInsets.symmetric(
                            horizontal: AppSpacing.md,
                            vertical: AppSpacing.sm,
                          ),
                          child: DecoratedBox(
                            decoration: BoxDecoration(
                              color: tokens.accentSoft,
                              borderRadius: BorderRadius.circular(
                                AppRadius.pill,
                              ),
                            ),
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
                Row(
                  children: [
                    for (var i = 0; i < items.length; i++)
                      Expanded(child: _item(context, i)),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// 指示条对齐取值：首项 -1、末项 1，均分到 [-1, 1]。
  double get _indicatorAlignment {
    if (items.length < 2) return 0;
    final t = currentIndex.clamp(0, items.length - 1) / (items.length - 1);
    return -1 + 2 * t;
  }

  Widget _item(BuildContext context, int index) {
    final tokens = context.tokens;
    final item = items[index];
    final selected = index == currentIndex;
    final color = selected ? tokens.accent : tokens.labelTertiary;
    return AppPressable(
      onTap: () => onTap(index),
      behavior: HitTestBehavior.opaque,
      child: Column(
        mainAxisSize: MainAxisSize.min,
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          TweenAnimationBuilder<double>(
            key: ValueKey<bool>(selected),
            tween: Tween<double>(begin: 0.85, end: 1),
            duration: const Duration(milliseconds: 320),
            curve: AppMotion.spring,
            builder: (context, scale, child) =>
                Transform.scale(scale: scale, child: child),
            child: Icon(
              selected ? item.selectedIcon : item.icon,
              size: 26,
              color: color,
            ),
          ),
          const SizedBox(height: 3),
          Text(
            item.label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              fontSize: 10,
              fontWeight: selected ? FontWeight.w600 : FontWeight.w400,
              color: color,
            ),
          ),
        ],
      ),
    );
  }
}
