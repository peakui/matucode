import 'package:flutter/material.dart';
import '../theme/app_metrics.dart';

/// 切换 [index] 时让内容淡入并轻微上移，同时保留各子页状态与滚动位置。
///
/// 内部仍是 [IndexedStack]，子页既不重建也不销毁，也不额外引入 [Scrollable]。
/// 过渡为一次性 `forward(from: 0)`，不使用 `repeat()`。
class AnimatedIndexedStack extends StatefulWidget {
  /// 创建带过渡的索引栈
  const AnimatedIndexedStack({
    super.key,
    required this.index,
    required this.children,
    this.duration = AppMotion.medium,
    this.slideOffset = 0.06,
  });

  /// 当前显示的子页索引。
  final int index;

  /// 子页列表。
  final List<Widget> children;

  /// 过渡时长。
  final Duration duration;

  /// 起始纵向位移比例（相对于子页高度）。
  final double slideOffset;

  @override
  State<AnimatedIndexedStack> createState() => _AnimatedIndexedStackState();
}

class _AnimatedIndexedStackState extends State<AnimatedIndexedStack>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: widget.duration,
    value: 1,
  );
  late final Animation<double> _fade = CurvedAnimation(
    parent: _controller,
    curve: Curves.easeOut,
  );
  late final Animation<Offset> _slide = Tween<Offset>(
    begin: Offset(0, widget.slideOffset),
    end: Offset.zero,
  ).animate(CurvedAnimation(parent: _controller, curve: AppMotion.emphasized));

  @override
  void didUpdateWidget(covariant AnimatedIndexedStack oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.duration != oldWidget.duration) {
      _controller.duration = widget.duration;
    }
    if (widget.index != oldWidget.index) {
      _controller.forward(from: 0);
    }
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
      child: SlideTransition(
        position: _slide,
        child: IndexedStack(index: widget.index, children: widget.children),
      ),
    );
  }
}
