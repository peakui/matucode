import 'package:flutter/cupertino.dart';
import 'package:get/get.dart';
import '../../../core/base/base_network/base_network_view.dart';
import '../../../core/design_system/theme/app_metrics.dart';
import '../../../core/design_system/theme/app_tokens.dart';
import '../../../core/design_system/ui/ui.dart';
import '../../../core/model/matu/learning_models.dart';
import '../../../core/service/matu_session_service.dart';
import '../../../core/ui/preview/app_preview_annotations.dart';
import '../../../routes/learning/learning_navigator.dart';
import '../logics/learning_detail_logic.dart';
import '../localization/learning_strings.dart';
import '../widgets/learning_content_body.dart';
import '../models/learning_preview_text.dart';

/// 文章、打卡、问答及面试题详情。
class LearningDetailView extends BaseNetworkView<LearningDetailLogic> {
  /// 创建详情页
  LearningDetailView({super.key, super.logic});

  @override
  String get navTitle => LearningStrings.brand;

  /// 大标题由 sliver 导航栏承担，页面不再叠加 TDesign 导航栏。
  @override
  PreferredSizeWidget? head() => null;

  @override
  Widget loadWidget() => const AppSkeletonDetail();

  @override
  Widget failWidget(LearningDetailLogic logic) => Center(
    child: SingleChildScrollView(
      child: LearningErrorNotice(
        logic.state.error.value,
        retry: logic.loadData,
      ),
    ),
  );

  /// 栏目导航与完整标题分离，正文按阅读宽度呈现。
  @override
  Widget bodyContent(LearningDetailLogic logic) => Obx(() {
    final item = logic.state.content.value;
    if (item == null) return const SizedBox.shrink();
    final tokens = Get.context!.tokens;
    return CustomScrollView(
      controller: logic.scrollController,
      slivers: [
        AppReadingNavigationBar(
          title: switch (logic.kind) {
            LearningKind.article => LearningStrings.feeds[0],
            LearningKind.check => LearningStrings.feeds[1],
            LearningKind.qa => LearningStrings.feeds[2],
            _ => LearningStrings.tabs[2],
          },
          actions: [
            Builder(
              builder: (context) => AppIconButton(
                icon: CupertinoIcons.share,
                onPressed: () {
                  final box = context.findRenderObject()! as RenderBox;
                  logic.share(box.localToGlobal(Offset.zero) & box.size);
                },
              ),
            ),
          ],
        ),
        SliverPadding(
          padding: const EdgeInsets.all(AppSpacing.contentPadding),
          sliver: SliverList.list(
            children: [
              _readingWidth(
                AppCard(
                  blur: false,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Wrap(
                        spacing: AppSpacing.sm,
                        runSpacing: AppSpacing.sm,
                        crossAxisAlignment: WrapCrossAlignment.center,
                        children: [
                          if (item.category.isNotEmpty)
                            Text(
                              item.category,
                              style: TextStyle(
                                fontSize: AppTypography.caption,
                                fontWeight: FontWeight.w600,
                                color: tokens.accent,
                              ),
                            ),
                          Text(
                            '${(item.content.length / 400).ceil().clamp(1, 999)} ${LearningStrings.readingMinute}',
                            style: TextStyle(
                              fontSize: AppTypography.caption,
                              color: tokens.labelSecondary,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: AppSpacing.md),
                      Text(
                        item.title,
                        key: const ValueKey('article-full-title'),
                        style: TextStyle(
                          fontSize: AppTypography.articleTitle,
                          fontWeight: FontWeight.w700,
                          height: 1.4,
                          color: tokens.labelPrimary,
                        ),
                      ),
                      const SizedBox(height: AppSpacing.lg),
                      Wrap(
                        spacing: AppSpacing.md,
                        runSpacing: AppSpacing.xs,
                        children: [
                          if (item.author.isNotEmpty)
                            Text(
                              item.author,
                              style: TextStyle(
                                fontSize: AppTypography.caption,
                                fontWeight: FontWeight.w500,
                                color: tokens.labelSecondary,
                              ),
                            ),
                          if (item.date.isNotEmpty)
                            Text(
                              item.date.split('T').first,
                              style: TextStyle(
                                fontSize: AppTypography.caption,
                                color: tokens.labelTertiary,
                              ),
                            ),
                        ],
                      ),
                      if (LearningPreviewText.clean(
                        item.summary,
                      ).isNotEmpty) ...[
                        const SizedBox(height: AppSpacing.md),
                        Text(
                          LearningPreviewText.clean(item.summary),
                          style: TextStyle(
                            fontSize: AppTypography.summary,
                            height: 1.65,
                            color: tokens.labelSecondary,
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.lg),
              _readingWidth(
                AppGlassSurface(
                  blur: false,
                  child: Padding(
                    padding: const EdgeInsets.all(AppSpacing.contentPadding),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        LearningContentBody(item.content),
                        for (final url in item.images)
                          Padding(
                            padding: const EdgeInsets.only(top: AppSpacing.md),
                            child: GestureDetector(
                              onTap: () =>
                                  LearningContentBody.previewImage(url),
                              child: ClipRRect(
                                borderRadius: BorderRadius.circular(
                                  AppRadius.input,
                                ),
                                child: Image.network(
                                  LearningContentBody.assetUrl(url),
                                  frameBuilder: _fadeInFrame,
                                  errorBuilder: (_, _, _) =>
                                      const Icon(CupertinoIcons.photo),
                                ),
                              ),
                            ),
                          ),
                      ],
                    ),
                  ),
                ),
              ),
              const SizedBox(height: AppSpacing.lg),
              _readingWidth(
                AppCard(
                  blur: false,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      if (logic.kind == LearningKind.interview)
                        ..._interview(item)
                      else
                        ..._content(item),
                      LearningErrorNotice(logic.state.actionError.value),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  });

  /// 大屏限制阅读行长，手机宽度保持自适应。
  Widget _readingWidth(Widget child) => Center(
    child: ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: AppGlass.readingWidth),
      child: SizedBox(width: double.infinity, child: child),
    ),
  );

  List<Widget> _interview(LearningContent item) => [
    const AppHairlineDivider(),
    const SizedBox(height: AppSpacing.lg),
    _sectionTitle(LearningStrings.answer),
    const SizedBox(height: AppSpacing.md),
    if (!item.answerVisible) ...[
      Padding(
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.lg),
        child: Text(
          LearningStrings.answerLocked,
          style: TextStyle(
            fontSize: AppTypography.control,
            color: Get.context!.tokens.labelSecondary,
          ),
        ),
      ),
      if (!logic.session.signedIn)
        AppButton(
          label: LearningStrings.login,
          onPressed: LearningNavigator.login,
        ),
    ] else ...[
      AppButton(
        label: logic.state.expanded.value
            ? LearningStrings.hideAnswer
            : LearningStrings.showAnswer,
        variant: AppButtonVariant.secondary,
        onPressed: () => logic.state.expanded.toggle(),
      ),
      if (logic.state.expanded.value) LearningContentBody(item.answer),
    ],
    Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.lg),
      child: Wrap(
        spacing: AppSpacing.sm,
        runSpacing: AppSpacing.sm,
        children: [
          for (var i = 1; i <= 3; i++)
            AppChip(
              label: LearningStrings.statuses[i],
              selected: logic.state.progress.value == i,
              onTap: logic.state.acting.value ? null : () => logic.mark(i),
            ),
        ],
      ),
    ),
    AppButton(
      label: logic.state.bookmarked.value
          ? LearningStrings.bookmarked
          : LearningStrings.bookmark,
      variant: logic.state.bookmarked.value
          ? AppButtonVariant.secondary
          : AppButtonVariant.primary,
      enabled: !logic.state.acting.value,
      onPressed: logic.bookmark,
    ),
    Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.lg),
      child: Row(
        children: [
          Expanded(
            child: AppButton(
              label: LearningStrings.previous,
              variant: AppButtonVariant.secondary,
              enabled: logic.hasPrevious && !logic.state.acting.value,
              onPressed: () => logic.move(-1),
            ),
          ),
          const SizedBox(width: AppSpacing.md),
          Expanded(
            child: AppButton(
              label: LearningStrings.next,
              variant: AppButtonVariant.secondary,
              enabled: logic.hasNext && !logic.state.acting.value,
              onPressed: () => logic.move(1),
            ),
          ),
        ],
      ),
    ),
  ];

  List<Widget> _content(LearningContent item) => [
    Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.lg),
      child: Wrap(
        spacing: AppSpacing.sm,
        runSpacing: AppSpacing.sm,
        children: [
          if (logic.kind != LearningKind.qa)
            AppChip(
              label:
                  '${item.liked ? LearningStrings.liked : LearningStrings.like} ${item.likes}',
              selected: item.liked,
              icon: item.liked
                  ? CupertinoIcons.hand_thumbsup_fill
                  : CupertinoIcons.hand_thumbsup,
              onTap: logic.state.acting.value ? null : () => logic.act('like'),
            ),
          if (logic.kind == LearningKind.article)
            AppChip(
              label: item.collected
                  ? LearningStrings.collected
                  : LearningStrings.collect,
              selected: item.collected,
              icon: item.collected
                  ? CupertinoIcons.bookmark_fill
                  : CupertinoIcons.bookmark,
              onTap: logic.state.acting.value
                  ? null
                  : () => logic.act('collect'),
            ),
          if (logic.kind == LearningKind.qa)
            AppChip(
              label: item.followed
                  ? LearningStrings.followed
                  : LearningStrings.follow,
              selected: item.followed,
              icon: item.followed
                  ? CupertinoIcons.checkmark_alt_circle_fill
                  : CupertinoIcons.add_circled,
              onTap: logic.state.acting.value
                  ? null
                  : () => logic.act('follow'),
            ),
        ],
      ),
    ),
    const AppHairlineDivider(),
    const SizedBox(height: AppSpacing.lg),
    _sectionTitle(LearningStrings.discussions),
    for (final comment in logic.state.discussions)
      Padding(
        padding: const EdgeInsets.only(top: AppSpacing.lg),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              comment.author,
              style: TextStyle(
                fontSize: AppTypography.control,
                fontWeight: FontWeight.w600,
                color: Get.context!.tokens.accent,
              ),
            ),
            const SizedBox(height: AppSpacing.xs),
            LearningContentBody(comment.content),
            const SizedBox(height: AppSpacing.md),
            const AppHairlineDivider(),
          ],
        ),
      ),
    LearningErrorNotice(
      logic.state.discussionError.value,
      retry: () =>
          logic.loadDiscussions(reset: logic.state.discussions.isEmpty),
    ),
    if (!logic.state.discussionDone.value)
      AppButton(
        label: logic.state.discussionBusy.value
            ? LearningStrings.working
            : LearningStrings.more,
        variant: AppButtonVariant.secondary,
        enabled: !logic.state.discussionBusy.value,
        onPressed: logic.loadDiscussions,
      ),
    if (logic.kind != LearningKind.qa) ...[
      Padding(
        padding: const EdgeInsets.only(top: AppSpacing.lg),
        child: AppTextInput(
          controller: logic.commentController,
          hint: LearningStrings.comment,
          maxLines: 3,
        ),
      ),
      const SizedBox(height: AppSpacing.md),
      AppButton(
        label: LearningStrings.send,
        enabled: !logic.state.acting.value,
        onPressed: logic.sendComment,
      ),
    ],
  ];

  Widget _sectionTitle(String text) => Text(
    text,
    style: TextStyle(
      fontSize: AppTypography.sectionTitle,
      fontWeight: FontWeight.w600,
      color: Get.context!.tokens.labelPrimary,
    ),
  );

  /// 网络图解码完成前保持透明，避免图片"跳"进正文。
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
Widget previewLearningDetail() {
  final logic = LearningDetailLogic(
    LearningKind.article,
    'preview',
    session: MatuSessionService(),
  );
  logic.state.content.value = const LearningContent(
    id: 'preview',
    title: '记录每一次成长',
    content: '## 今日所学\n把新知识放进自己的知识体系。',
  );
  logic.state.discussionDone.value = true;
  logic.setStatusSuccess();
  return LearningDetailView(logic: logic);
}
