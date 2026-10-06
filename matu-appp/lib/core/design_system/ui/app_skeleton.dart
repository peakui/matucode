import 'package:flutter/material.dart';

import '../theme/app_metrics.dart';
import '../theme/app_tokens.dart';

/// 单个骨架块，以呼吸式明暗循环替代空白闪烁。
class AppSkeleton extends StatefulWidget {
  /// 创建骨架块
  const AppSkeleton({
    super.key,
    this.width,
    this.widthFactor,
    this.height = 14,
    this.radius = 6,
  });

  /// 固定宽度
  final double? width;

  /// 相对父级的宽度比例
  final double? widthFactor;

  /// 高度
  final double height;

  /// 圆角
  final double radius;

  @override
  State<AppSkeleton> createState() => _AppSkeletonState();
}

class _AppSkeletonState extends State<AppSkeleton>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 900),
  )..repeat(reverse: true);

  late final Animation<double> _opacity = Tween<double>(
    begin: 0.35,
    end: 0.85,
  ).animate(CurvedAnimation(parent: _controller, curve: Curves.easeInOut));

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final block = AnimatedBuilder(
      animation: _opacity,
      builder: (context, child) =>
          Opacity(opacity: _opacity.value, child: child),
      child: Container(
        width: widget.width,
        height: widget.height,
        decoration: BoxDecoration(
          color: context.tokens.fillPrimary,
          borderRadius: BorderRadius.circular(widget.radius),
        ),
      ),
    );
    if (widget.widthFactor == null) return block;
    return FractionallySizedBox(
      alignment: Alignment.centerLeft,
      widthFactor: widget.widthFactor,
      child: block,
    );
  }
}

/// 列表骨架，形状对齐卡片列表项。
class AppSkeletonList extends StatelessWidget {
  /// 创建列表骨架
  const AppSkeletonList({super.key, this.count = 4, this.showCover = false});

  /// 占位条目数
  final int count;

  /// 是否展示封面占位
  final bool showCover;

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    return ListView.separated(
      padding: const EdgeInsets.all(AppSpacing.contentPadding),
      physics: const NeverScrollableScrollPhysics(),
      itemCount: count,
      separatorBuilder: (_, _) => const SizedBox(height: AppSpacing.lg),
      itemBuilder: (_, _) => Container(
        clipBehavior: Clip.antiAlias,
        decoration: BoxDecoration(
          color: tokens.bgSurface,
          borderRadius: BorderRadius.circular(AppRadius.card),
          border: Border.all(
            color: tokens.hairline,
            width: AppElevation.hairline,
          ),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (showCover) const AppSkeleton(height: 160, radius: 0),
            Padding(
              padding: const EdgeInsets.all(AppSpacing.lg),
              child: const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  AppSkeleton(widthFactor: 0.35, height: 12),
                  SizedBox(height: AppSpacing.md),
                  AppSkeleton(height: 18),
                  SizedBox(height: AppSpacing.sm),
                  AppSkeleton(widthFactor: 0.7, height: 18),
                  SizedBox(height: AppSpacing.md),
                  AppSkeleton(widthFactor: 0.5, height: 12),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

/// 详情骨架，形状对齐标题 + 元信息 + 正文段落。
class AppSkeletonDetail extends StatelessWidget {
  /// 创建详情骨架
  const AppSkeletonDetail({super.key, this.paragraphs = 3});

  /// 段落数量
  final int paragraphs;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(AppSpacing.contentPadding),
      physics: const NeverScrollableScrollPhysics(),
      children: [
        const AppSkeleton(widthFactor: 0.9, height: 26, radius: 8),
        const SizedBox(height: AppSpacing.md),
        const AppSkeleton(widthFactor: 0.45, height: 13),
        const SizedBox(height: AppSpacing.xl),
        for (var i = 0; i < paragraphs; i++) ...[
          const AppSkeleton(height: 14),
          const SizedBox(height: AppSpacing.sm),
          const AppSkeleton(height: 14),
          const SizedBox(height: AppSpacing.sm),
          AppSkeleton(widthFactor: i.isEven ? 0.8 : 0.6, height: 14),
          const SizedBox(height: AppSpacing.xl),
        ],
      ],
    );
  }
}
