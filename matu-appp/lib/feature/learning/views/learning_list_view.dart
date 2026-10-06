import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../../../core/base/base_list/base_list_view.dart';
import '../../../core/design_system/theme/app_metrics.dart';
import '../../../core/design_system/theme/app_tokens.dart';
import '../../../core/design_system/ui/ui.dart';
import '../../../core/model/matu/learning_models.dart';
import '../../../core/ui/preview/app_preview_annotations.dart';
import '../../../core/service/matu_session_service.dart';
import '../logics/learning_list_logic.dart';
import '../localization/learning_strings.dart';
import '../widgets/learning_content_body.dart';
import '../models/learning_preview_text.dart';

/// 内容列表，复用网络状态与 EasyRefresh 基础能力。
class LearningListView
    extends BaseListView<LearningListLogic, LearningContent> {
  /// 创建内容列表
  LearningListView({
    super.key,
    required LearningListLogic logic,
    this.embedded = false,
    this.title,
  }) : super(logic: logic);

  /// 是否嵌入首页多标签容器
  final bool embedded;

  /// 页面标题
  final String? title;

  @override
  bool get isHiddenNav => embedded;
  @override
  bool get useGlassBackground => !embedded;
  @override
  bool get extendBodyBehindAppBar => !embedded;
  @override
  Color? get backgroundColor => embedded ? Colors.transparent : null;

  @override
  String? get navTitle => title;

  /// 独立的列表页使用紧凑 iOS 导航栏，避免 TDesign 导航栏在暗色下的白底。
  @override
  PreferredSizeWidget? head() {
    if (embedded) return null;
    final tokens = Get.context!.tokens;
    return CupertinoNavigationBar(
      middle: Text(
        title ?? '',
        style: TextStyle(
          fontSize: AppTypography.cardTitle,
          fontWeight: FontWeight.w600,
          color: tokens.labelPrimary,
        ),
      ),
      backgroundColor: tokens.bgSurface.withValues(
        alpha: tokens.brightness == Brightness.dark
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

  @override
  Widget body() => Column(
    children: [
      // 内容穿透玻璃导航栏，须自行让出导航栏 + 状态栏高度。
      if (!embedded)
        SizedBox(height: 44 + MediaQuery.paddingOf(Get.context!).top),
      if (logic.mine == null) _filters(),
      if (logic.mine == 'bookmarks')
        Padding(
          padding: const EdgeInsets.all(AppSpacing.contentPadding),
          child: Text(
            LearningStrings.bookmarkHint,
            style: TextStyle(
              fontSize: AppTypography.control,
              color: Get.context!.tokens.labelSecondary,
            ),
          ),
        ),
      Expanded(child: super.body()),
    ],
  );

  Widget _filters() => Column(
    children: [
      if (logic.kind != LearningKind.check)
        Padding(
          padding: const EdgeInsets.symmetric(
            horizontal: AppSpacing.contentPadding,
            vertical: AppSpacing.sm,
          ),
          child: AppTextInput(
            controller: logic.searchController,
            hint: LearningStrings.search,
            onChanged: (value) => logic.search(value),
          ),
        ),
      if (logic.kind == LearningKind.check)
        Obx(
          () => Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.contentPadding,
              vertical: AppSpacing.sm,
            ),
            child: Row(
              children: [
                AppChip(
                  label: logic.state.month.value == null
                      ? LearningStrings.chooseMonth
                      : '${logic.state.month.value!.year}-${logic.state.month.value!.month}',
                  icon: CupertinoIcons.calendar,
                  selected: logic.state.month.value != null,
                  onTap: () async {
                    final date = await showDatePicker(
                      context: Get.context!,
                      initialDate: logic.state.month.value ?? DateTime.now(),
                      firstDate: DateTime(2000),
                      lastDate: DateTime.now(),
                    );
                    if (date != null) logic.filter(month: date);
                  },
                ),
                if (logic.state.month.value != null) ...[
                  const SizedBox(width: AppSpacing.sm),
                  AppChip(
                    label: LearningStrings.clearFilter,
                    onTap: () => logic.filter(clearMonth: true),
                  ),
                ],
              ],
            ),
          ),
        ),
      Obx(
        () => logic.state.categories.isEmpty
            ? const SizedBox.shrink()
            : SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                padding: const EdgeInsets.symmetric(
                  horizontal: AppSpacing.contentPadding,
                  vertical: AppSpacing.xs,
                ),
                child: Row(
                  children: [
                    AppChip(
                      label: LearningStrings.all,
                      selected: logic.state.category.value.isEmpty,
                      onTap: () => logic.filter(category: ''),
                    ),
                    for (final c in logic.state.categories)
                      Padding(
                        padding: const EdgeInsets.only(left: AppSpacing.sm),
                        child: AppChip(
                          label: c.name,
                          selected: logic.state.category.value == c.id,
                          onTap: () => logic.filter(category: c.id),
                        ),
                      ),
                  ],
                ),
              ),
      ),
      Obx(
        () => LearningErrorNotice(
          logic.state.categoryError.value,
          retry: logic.loadCategories,
        ),
      ),
      if ([LearningKind.course, LearningKind.interview].contains(logic.kind))
        Obx(
          () => Padding(
            padding: const EdgeInsets.symmetric(
              horizontal: AppSpacing.contentPadding,
              vertical: AppSpacing.xs,
            ),
            child: Wrap(
              spacing: AppSpacing.sm,
              runSpacing: AppSpacing.sm,
              children: [
                AppMenuButton<int>(
                  value: logic.state.level.value,
                  options: [
                    for (var i = 0; i < 4; i++)
                      AppMenuOption(value: i, label: LearningStrings.levels[i]),
                  ],
                  onChanged: (i) => logic.filter(level: i),
                ),
                if (logic.kind == LearningKind.course)
                  AppMenuButton<String>(
                    value: logic.state.sort.value,
                    options: [
                      for (var i = 0; i < 3; i++)
                        AppMenuOption(
                          value: ['latest', 'popular', 'rating'][i],
                          label: LearningStrings.sorts[i],
                        ),
                    ],
                    onChanged: (s) => logic.filter(sort: s),
                  ),
              ],
            ),
          ),
        ),
    ],
  );

  @override
  Widget loadWidget() =>
      AppSkeletonList(showCover: logic.kind == LearningKind.course);

  @override
  Widget failWidget(LearningListLogic logic) => Center(
    child: SingleChildScrollView(
      child: LearningErrorNotice(logic.state.error.value, retry: logic.refresh),
    ),
  );

  @override
  Widget emptyWidget() => AppEmptyState(
    message: LearningStrings.empty,
    icon: CupertinoIcons.book,
    actionLabel: LearningStrings.retry,
    onAction: logic.refresh,
  );

  @override
  Widget builderItemWidget() => Obx(
    () => ListView.separated(
      key: PageStorageKey('learning-${logic.kind.name}-${logic.mine}'),
      controller: logic.scrollController,
      // 首页内嵌列表需为悬浮胶囊让出高度，避免最后一张卡片被遮住。
      padding: EdgeInsets.fromLTRB(
        AppSpacing.contentPadding,
        AppSpacing.contentPadding,
        AppSpacing.contentPadding,
        AppSpacing.contentPadding +
            (embedded ? AppGlass.tabHeight + AppGlass.tabBottomGap : 0),
      ),
      itemCount: logic.state.dataList.length + 1,
      separatorBuilder: (_, _) => const SizedBox(height: AppSpacing.xl),
      itemBuilder: (_, i) => i == logic.state.dataList.length
          ? LearningErrorNotice(logic.state.error.value, retry: logic.loadMore)
          : StaggeredEntrance(
              index: i,
              child: itemWidget(logic.state.dataList[i], i),
            ),
    ),
  );

  @override
  Widget itemWidget(LearningContent item, int index) {
    final tokens = Get.context!.tokens;
    final summary = LearningPreviewText.clean(item.summary);
    return AppCard(
      padding: EdgeInsets.zero,
      blur: false,
      onTap: () => logic.open(item),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (logic.kind == LearningKind.course)
            AspectRatio(
              aspectRatio: 16 / 9,
              child: item.cover.isEmpty
                  ? ColoredBox(
                      color: tokens.accentSoft,
                      child: Icon(
                        CupertinoIcons.play_circle,
                        size: AppSpacing.xxxl,
                        color: tokens.accent,
                      ),
                    )
                  : Image.network(
                      LearningContentBody.assetUrl(item.cover),
                      fit: BoxFit.cover,
                      frameBuilder: _fadeInFrame,
                      errorBuilder: (_, _, _) =>
                          const Icon(CupertinoIcons.play_circle),
                    ),
            ),
          Padding(
            padding: const EdgeInsets.all(AppSpacing.lg),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                if (item.category.isNotEmpty) ...[
                  Text(
                    item.category,
                    style: TextStyle(
                      fontSize: AppTypography.summary,
                      fontWeight: FontWeight.w600,
                      color: tokens.accent,
                    ),
                  ),
                  const SizedBox(height: AppSpacing.xs),
                ],
                Text(
                  item.title,
                  maxLines: 3,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    fontSize: AppTypography.cardTitle,
                    fontWeight: FontWeight.w600,
                    height: 1.35,
                    color: tokens.labelPrimary,
                  ),
                ),
                if (summary.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: AppSpacing.md),
                    child: Text(
                      summary,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: AppTypography.control,
                        height: 1.6,
                        color: tokens.labelSecondary,
                      ),
                    ),
                  ),
                Padding(
                  padding: const EdgeInsets.only(top: AppSpacing.lg),
                  child: Wrap(
                    spacing: AppSpacing.md,
                    children: [
                      if (item.author.isNotEmpty) _meta(item.author, tokens),
                      if (item.difficulty > 0 && item.difficulty < 4)
                        _meta(LearningStrings.levels[item.difficulty], tokens),
                      if (item.date.isNotEmpty)
                        _meta(item.date.split('T').first, tokens),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _meta(String text, AppTokens tokens) => Text(
    text,
    style: TextStyle(
      fontSize: AppTypography.caption,
      color: tokens.labelTertiary,
    ),
  );

  /// 网络图解码完成前保持透明，避免图片"跳"进布局。
  Widget _fadeInFrame(
    BuildContext context,
    Widget child,
    int? frame,
    bool wasSynchronouslyLoaded,
  ) {
    if (wasSynchronouslyLoaded) return child;
    return AnimatedOpacity(
      opacity: frame == null ? 0 : 1,
      duration: AppMotion.medium,
      curve: Curves.easeOut,
      child: child,
    );
  }
}

@ResponsivePreview()
Widget previewLearningList() {
  final logic = LearningListLogic(
    LearningKind.article,
    session: MatuSessionService(),
  );
  logic.state.dataList.add(
    const LearningContent(
      id: 'preview',
      title: '把知识串成体系',
      summary: '从一个小问题出发，建立清晰的学习路径。',
      category: '学习方法',
    ),
  );
  logic.setStatusSuccess();
  return LearningListView(logic: logic, title: LearningStrings.brand);
}
