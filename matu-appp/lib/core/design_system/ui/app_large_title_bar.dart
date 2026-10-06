import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'app_glass_surface.dart';

import '../theme/app_metrics.dart';
import '../theme/app_tokens.dart';
import '../theme/app_theme.dart';

/// 大标题导航栏（sliver）。
///
/// 作为 `CustomScrollView` 的首个 sliver：滚动前展示大标题，滚动后收起为
/// 顶部小标题，同时提供返回入口。
class AppLargeTitleBar extends StatelessWidget {
  /// 创建大标题导航栏
  const AppLargeTitleBar({
    super.key,
    required this.title,
    this.actions,
    this.automaticallyImplyLeading = true,
  });

  /// 标题文案
  final String title;

  /// 尾部操作
  final List<Widget>? actions;

  /// 是否自动提供返回按钮
  final bool automaticallyImplyLeading;

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    return CupertinoSliverNavigationBar(
      automaticallyImplyLeading: automaticallyImplyLeading,
      automaticallyImplyTitle: false,
      transitionBetweenRoutes: false,
      middle: Text(
        title,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          fontSize: AppTypography.cardTitle,
          fontWeight: FontWeight.w600,
          color: tokens.labelPrimary,
        ),
      ),
      largeTitle: Text(
        title,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          fontSize: AppTypography.pageTitle,
          fontWeight: FontWeight.w700,
          height: 1.15,
          color: tokens.labelPrimary,
        ),
      ),
      trailing: actions == null
          ? null
          : Row(mainAxisSize: MainAxisSize.min, children: actions!),
      backgroundColor: tokens.bgSurface.withValues(
        alpha: context.isDarkMode
            ? AppGlass.darkOpacity
            : AppGlass.lightOpacity,
      ),
      border: Border(
        bottom: BorderSide(
          color: tokens.hairline,
          width: AppElevation.hairline,
        ),
      ),
    );
  }
}

/// 阅读页紧凑固定导航；完整标题放在正文头部，避免重复和截断。
class AppReadingNavigationBar extends StatelessWidget {
  /// 创建阅读导航，提供 [title] 与可选 [actions]。
  const AppReadingNavigationBar({super.key, required this.title, this.actions});

  /// 简短栏目标题。
  final String title;

  /// 分享等操作。
  final List<Widget>? actions;
  @override
  Widget build(BuildContext context) => SliverAppBar(
    systemOverlayStyle: AppTheme.buildSystemUiOverlayStyle(
      AppTheme.of(context).tdTheme,
      Theme.of(context).brightness,
    ),
    pinned: true,
    title: Text(
      title,
      style: TextStyle(
        fontSize: AppTypography.cardTitle,
        fontWeight: FontWeight.w600,
        color: context.tokens.labelPrimary,
      ),
    ),
    centerTitle: true,
    leading: Navigator.of(context).canPop()
        ? CupertinoNavigationBarBackButton(color: context.tokens.accent)
        : null,
    actions: actions == null
        ? null
        : [...actions!, const SizedBox(width: AppSpacing.sm)],
    backgroundColor: Colors.transparent,
    surfaceTintColor: Colors.transparent,
    elevation: 0,
    scrolledUnderElevation: 0,
    flexibleSpace: const AppGlassSurface(
      radius: 0,
      grouped: true,
      saturated: true,
      child: SizedBox.expand(),
    ),
  );
}

/// 常驻大标题，用于不便使用 sliver 的页面（如嵌套 IndexedStack 的首页）。
class AppLargeTitleHeader extends StatelessWidget {
  /// 创建常驻大标题
  const AppLargeTitleHeader({
    super.key,
    required this.title,
    this.subtitle,
    this.trailing,
    this.padding,
  });

  /// 标题文案
  final String title;

  /// 副标题
  final String? subtitle;

  /// 尾部内容
  final Widget? trailing;

  /// 自定义内边距
  final EdgeInsetsGeometry? padding;

  @override
  Widget build(BuildContext context) {
    final tokens = context.tokens;
    return AppGlassSurface(
      radius: 0,
      grouped: true,
      child: Padding(
        padding:
            padding ??
            const EdgeInsets.fromLTRB(
              AppSpacing.contentPadding,
              AppSpacing.sm,
              AppSpacing.contentPadding,
              AppSpacing.md,
            ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    title,
                    style: TextStyle(
                      fontSize: AppTypography.pageTitle,
                      fontWeight: FontWeight.w700,
                      height: 1.15,
                      color: tokens.labelPrimary,
                    ),
                  ),
                  if (subtitle != null)
                    Padding(
                      padding: const EdgeInsets.only(top: AppSpacing.xs),
                      child: Text(
                        subtitle!,
                        style: TextStyle(
                          fontSize: AppTypography.control,
                          color: tokens.labelSecondary,
                        ),
                      ),
                    ),
                ],
              ),
            ),
            if (trailing != null) trailing!,
          ],
        ),
      ),
    );
  }
}
