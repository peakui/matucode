import 'package:flutter/material.dart';

import '../theme/app_metrics.dart';
import '../theme/app_tokens.dart';
import 'app_pressable.dart';
import 'app_glass_surface.dart';

/// iOS 风格卡片：16px 圆角、elevation 2 阴影、0.5px 发丝描边。
class AppCard extends StatelessWidget {
  /// 创建卡片
  const AppCard({
    super.key,
    required this.child,
    this.padding = const EdgeInsets.all(AppSpacing.lg),
    this.margin,
    this.onTap,
    this.blur = true,
    this.clipBehavior = Clip.antiAlias,
  });

  /// 卡片内容
  final Widget child;

  /// 内容内边距
  final EdgeInsetsGeometry padding;

  /// 卡片外边距
  final EdgeInsetsGeometry? margin;

  /// 点击回调，非空时启用按压反馈
  final VoidCallback? onTap;

  /// 是否对卡片背面应用模糊；滚动列表内应关闭以降低绘制成本。
  final bool blur;

  /// 裁剪方式
  final Clip clipBehavior;

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    final decorated = Container(
      margin: margin,
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(AppRadius.card),
        boxShadow: tokens.cardShadow,
      ),
      child: AppGlassSurface(
        blur: blur,
        child: onTap == null
            ? Padding(padding: padding, child: child)
            : AppPressable(
                onTap: onTap,
                pressedScale: 0.99,
                flow: true,
                child: Padding(padding: padding, child: child),
              ),
      ),
    );
    return decorated;
  }
}
