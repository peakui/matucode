import 'package:flutter/material.dart';
import '../theme/app_metrics.dart';

/// 弹层入场：轻微回弹放大 + 淡入，仅在 `initState` 播放一次。
///
/// 与系统弹层的上滑过渡叠加使用，让玻璃面板落定时更有"弹"的手感；
/// 无循环、无计时器，可安全配合 `pumpAndSettle`。
class AppSpringIn extends StatefulWidget {
  /// 创建弹性入场包装
  const AppSpringIn({super.key, required this.child, this.from = 0.92});

  /// 子内容。
  final Widget child;

  /// 起始缩放比例。
  final double from;

  @override
  State<AppSpringIn> createState() => _AppSpringInState();
}

class _AppSpringInState extends State<AppSpringIn>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: AppMotion.slow,
  );
  late final Animation<double> _scale = Tween<double>(
    begin: widget.from,
    end: 1,
  ).animate(CurvedAnimation(parent: _controller, curve: AppMotion.spring));
  late final Animation<double> _fade = CurvedAnimation(
    parent: _controller,
    curve: Curves.easeOut,
  );

  @override
  void initState() {
    super.initState();
    _controller.forward(from: 0);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: _fade,
      child: ScaleTransition(scale: _scale, child: widget.child),
    );
  }
}
