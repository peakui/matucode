import 'package:flutter/material.dart';
import '../theme/app_metrics.dart';
import '../theme/app_tokens.dart';

/// 按下时在表面上扫过一道镜面高光，模拟液态玻璃的形变与流动。
///
/// 只在 [active] 由 false 变 true 时播放一次；抬起时反向收起，均会自然结束，
/// 不使用 `repeat()`，可安全配合 `pumpAndSettle`。
class AppLiquidHighlight extends StatefulWidget {
  /// 创建高光流动包装
  const AppLiquidHighlight({
    super.key,
    required this.child,
    this.active = false,
    this.radius = AppRadius.card,
  });

  /// 被包裹的内容。
  final Widget child;

  /// 是否处于按压态。
  final bool active;

  /// 裁剪圆角。
  final double radius;

  @override
  State<AppLiquidHighlight> createState() => _AppLiquidHighlightState();
}

class _AppLiquidHighlightState extends State<AppLiquidHighlight>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: AppMotion.slow,
  );
  late final CurvedAnimation _sweep = CurvedAnimation(
    parent: _controller,
    curve: AppMotion.emphasized,
  );

  @override
  void didUpdateWidget(covariant AppLiquidHighlight oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.active == oldWidget.active) return;
    if (widget.active) {
      _controller.forward(from: 0);
    } else {
      _controller.reverse();
    }
  }

  @override
  void dispose() {
    _sweep.dispose();
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final sheen = context.tokens.glassSheen;
    return Stack(
      children: [
        widget.child,
        Positioned.fill(
          child: IgnorePointer(
            child: ClipRRect(
              borderRadius: BorderRadius.circular(widget.radius),
              child: AnimatedBuilder(
                animation: _sweep,
                builder: (context, _) {
                  final t = _sweep.value;
                  // 两端不绘制：既省一次 save layer，也避免退化渐变。
                  if (t <= 0 || t >= 1) return const SizedBox.shrink();
                  final center = -0.35 + 1.7 * t;
                  final lower = (center - 0.28).clamp(0.0, 1.0);
                  final mid = center.clamp(lower, 1.0);
                  final upper = (center + 0.28).clamp(mid, 1.0);
                  return DecoratedBox(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topLeft,
                        end: Alignment.bottomRight,
                        stops: [lower, mid, upper],
                        colors: [
                          sheen.withValues(alpha: 0),
                          sheen,
                          sheen.withValues(alpha: 0),
                        ],
                      ),
                    ),
                    child: const SizedBox.expand(),
                  );
                },
              ),
            ),
          ),
        ),
      ],
    );
  }
}
