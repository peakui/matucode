import 'package:flutter/material.dart';
import '../extensions/widget/effect_extension.dart';
import '../theme/app_metrics.dart';
import '../theme/app_tokens.dart';

/// 液态玻璃表面：强模糊 + 饱和度增益 + 镜面高光 + 边缘暗边。
///
/// 树序约定（测试依赖）：第一个带 gradient 的 [DecoratedBox] 必须是玻璃底，
/// 且顶边为 0.5px 发丝；因此所有额外视觉层都排在它之后，
/// 并一律使用 [DecoratedBox] / [Positioned.fill] / [IgnorePointer]，不引入 [Container]。
class AppGlassSurface extends StatelessWidget {
  /// 创建玻璃表面；[radius] 为零时适合铺满导航栏。
  const AppGlassSurface({
    super.key,
    required this.child,
    this.radius = AppRadius.card,
    this.blur = true,
    this.grouped = false,
    this.saturated = false,
  });

  /// 表面内容。
  final Widget child;

  /// 表面圆角。
  final double radius;

  /// 阅读正文可关闭模糊，仅使用半透明表面以降低长页面绘制成本。
  final bool blur;

  /// 加入祖先 `BackdropGroup` 共享背景快照，适合同屏多个固定 chrome。
  final bool grouped;

  /// 叠加饱和度增益，让透出的色彩更鲜艳。
  final bool saturated;

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    final dark = tokens.brightness == Brightness.dark;
    final tint = tokens.glassTint;
    // 带圆角的边框颜色必须统一；顶边高光 / 底边暗边改由独立层叠加，避免非均匀边框限制。
    final content = DecoratedBox(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            tint.withValues(alpha: dark ? 0.82 : 0.78),
            tint.withValues(alpha: dark ? 0.70 : 0.60),
            tint.withValues(alpha: dark ? 0.62 : 0.50),
          ],
        ),
        borderRadius: BorderRadius.circular(radius),
        border: Border.all(
          color: tokens.glassRim,
          width: AppElevation.hairline,
        ),
      ),
      child: child,
    );
    // 镜面高光：上半部渐隐，模拟玻璃对光源的反射。
    final sheen = DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(radius),
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          stops: const [0, 0.55],
          colors: [tokens.glassSheen, tokens.glassSheen.withValues(alpha: 0)],
        ),
      ),
      child: const SizedBox.expand(),
    );
    // 底边暗边：制造玻璃厚度。
    final edge = DecoratedBox(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(radius),
        gradient: LinearGradient(
          begin: Alignment.bottomCenter,
          end: Alignment.topCenter,
          stops: const [0, 0.35],
          colors: [tokens.glassEdge, tokens.glassEdge.withValues(alpha: 0)],
        ),
      ),
      child: const SizedBox.expand(),
    );
    // 顶部单边描边：无圆角，绕开“圆角边框须颜色统一”的限制。
    final rim = DecoratedBox(
      decoration: BoxDecoration(
        border: Border(
          top: BorderSide(
            color: tokens.glassHighlight,
            width: AppElevation.hairline,
          ),
        ),
      ),
      child: const SizedBox.expand(),
    );
    return ClipRRect(
      borderRadius: BorderRadius.circular(radius),
      // passthrough：让玻璃底与内容一起铺满表面。若用默认的 loose，
      // 内容为 Column 等纵向布局时会按最宽子项收缩，玻璃底随之变窄，
      // 卡片右侧只剩阴影底色，出现一块明显色差（短内容卡片尤甚）。
      child: Stack(
        fit: StackFit.passthrough,
        children: [
          blur
              ? content.backgroundBlur(
                  sigmaX: AppGlass.blurStrong,
                  sigmaY: AppGlass.blurStrong,
                  grouped: grouped,
                  saturate: saturated ? AppGlass.saturation : null,
                )
              : content,
          Positioned.fill(child: IgnorePointer(child: sheen)),
          Positioned.fill(child: IgnorePointer(child: edge)),
          Positioned.fill(child: IgnorePointer(child: rim)),
        ],
      ),
    );
  }
}

/// 静态中性背景：亮色近白、暗色近黑，零彩色、零动画。
///
/// 单色微层次让玻璃表面有可透出的明度变化，同时保证任意页面都干净耐看。
class AppGlassBackground extends StatelessWidget {
  /// 创建背景。
  const AppGlassBackground({super.key, required this.child});

  /// 前景内容。
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    // 用 SizedBox.expand 而非 Stack：body 若不撑满（如单行文本）时背景仍满屏。
    return SizedBox.expand(
      child: DecoratedBox(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [tokens.bgGradientTop, tokens.bgGradientBottom],
          ),
        ),
        child: child,
      ),
    );
  }
}
