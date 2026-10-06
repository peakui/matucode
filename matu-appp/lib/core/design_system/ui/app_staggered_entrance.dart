import 'package:flutter/material.dart';
import '../theme/app_metrics.dart';

/// 首屏列表项错峰入场：淡入并轻微上移，仅前 [maxStagger] 项参与。
///
/// 动画在 `initState` 内一次性启动，无计时器、不循环；[Element] 身份保证
/// 滚动或重建不会重复播放。
class StaggeredEntrance extends StatefulWidget {
  /// 创建错峰入场包装
  const StaggeredEntrance({
    super.key,
    required this.child,
    this.index = 0,
    this.maxStagger = 6,
  });

  /// 子项内容。
  final Widget child;

  /// 在列表中的序号，决定错峰延迟。
  final int index;

  /// 参与入场的最大序号，超出的直接显示。
  final int maxStagger;

  @override
  State<StaggeredEntrance> createState() => _StaggeredEntranceState();
}

class _StaggeredEntranceState extends State<StaggeredEntrance>
    with SingleTickerProviderStateMixin {
  AnimationController? _controller;
  CurvedAnimation? _curve;
  late final Animation<Offset> _slide;

  @override
  void initState() {
    super.initState();
    if (widget.index >= widget.maxStagger) {
      _slide = const AlwaysStoppedAnimation<Offset>(Offset.zero);
      return;
    }
    final controller = AnimationController(
      vsync: this,
      duration: AppMotion.entrance,
    );
    final start = (widget.index * AppMotion.staggerStep).clamp(0.0, 0.4);
    final curve = CurvedAnimation(
      parent: controller,
      curve: Interval(
        start,
        (start + 0.6).clamp(0.0, 1.0),
        curve: Curves.easeOutCubic,
      ),
    );
    _controller = controller;
    _curve = curve;
    _slide = Tween<Offset>(
      begin: const Offset(0, 0.08),
      end: Offset.zero,
    ).animate(curve);
    controller.forward(from: 0);
  }

  @override
  void dispose() {
    _curve?.dispose();
    _controller?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final curve = _curve;
    if (curve == null) return widget.child;
    return FadeTransition(
      opacity: curve,
      child: SlideTransition(position: _slide, child: widget.child),
    );
  }
}
