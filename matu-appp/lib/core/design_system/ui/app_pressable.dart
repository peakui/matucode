import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/app_metrics.dart';
import 'app_liquid_highlight.dart';

/// 统一按压反馈：按下时降低透明度并轻微回弹缩放。
///
/// 移动端触发一次轻量触感；Web 无该能力，直接跳过。
/// [flow] 为 true 时额外叠加一道镜面高光扫过，用于液态玻璃表面。
class AppPressable extends StatefulWidget {
  /// 创建可按压包装
  const AppPressable({
    super.key,
    required this.child,
    this.onTap,
    this.onLongPress,
    this.pressedOpacity = 0.7,
    this.pressedScale = 0.98,
    this.haptics = true,
    this.behavior = HitTestBehavior.deferToChild,
    this.flow = false,
  });

  /// 被包裹的内容
  final Widget child;

  /// 点击回调，为空时组件不响应手势
  final VoidCallback? onTap;

  /// 长按回调
  final VoidCallback? onLongPress;

  /// 按下时的不透明度
  final double pressedOpacity;

  /// 按下时的缩放比例
  final double pressedScale;

  /// 是否触发触感反馈
  final bool haptics;

  /// 命中测试策略
  final HitTestBehavior behavior;

  /// 是否叠加液态高光扫过
  final bool flow;

  @override
  State<AppPressable> createState() => _AppPressableState();
}

class _AppPressableState extends State<AppPressable> {
  bool _pressed = false;

  bool get _enabled => widget.onTap != null || widget.onLongPress != null;

  void _setPressed(bool value) {
    if (!_enabled || _pressed == value) return;
    setState(() => _pressed = value);
  }

  void _handleTap() {
    if (widget.onTap == null) return;
    if (widget.haptics && !kIsWeb) {
      HapticFeedback.selectionClick();
    }
    widget.onTap!.call();
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      behavior: widget.behavior,
      onTapDown: _enabled ? (_) => _setPressed(true) : null,
      onTapUp: _enabled ? (_) => _setPressed(false) : null,
      onTapCancel: _enabled ? () => _setPressed(false) : null,
      onTap: _enabled ? _handleTap : null,
      onLongPress: widget.onLongPress,
      child: AnimatedOpacity(
        duration: AppMotion.fast,
        curve: Curves.easeOut,
        opacity: _pressed ? widget.pressedOpacity : 1,
        child: AnimatedScale(
          duration: _pressed ? AppMotion.press : AppMotion.release,
          curve: AppMotion.spring,
          scale: _pressed ? widget.pressedScale : 1,
          child: widget.flow
              ? AppLiquidHighlight(active: _pressed, child: widget.child)
              : widget.child,
        ),
      ),
    );
  }
}
