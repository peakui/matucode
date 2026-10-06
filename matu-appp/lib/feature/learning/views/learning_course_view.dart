import 'package:flutter/cupertino.dart';
import 'package:get/get.dart';
import 'package:video_player/video_player.dart';
import '../../../core/base/base_network/base_network_view.dart';
import '../../../core/design_system/theme/app_metrics.dart';
import '../../../core/design_system/theme/app_tokens.dart';
import '../../../core/design_system/ui/ui.dart';
import '../../../core/model/matu/learning_models.dart';
import '../../../core/service/matu_session_service.dart';
import '../../../core/ui/preview/app_preview_annotations.dart';
import '../../../routes/learning/learning_navigator.dart';
import '../logics/learning_course_logic.dart';
import '../localization/learning_strings.dart';
import '../widgets/learning_content_body.dart';

/// 授权课程播放器、章节列表及进度反馈。
class LearningCourseView extends BaseNetworkView<LearningCourseLogic> {
  /// 创建课程页
  LearningCourseView({super.key, super.logic});

  @override
  String get navTitle => LearningStrings.tabs[1];

  /// 大标题由 sliver 导航栏承担，页面不再叠加 TDesign 导航栏。
  @override
  PreferredSizeWidget? head() => null;

  @override
  Widget loadWidget() => const AppSkeletonDetail();

  @override
  Widget failWidget(LearningCourseLogic logic) => Center(
    child: SingleChildScrollView(
      child: LearningErrorNotice(
        logic.state.error.value,
        retry: logic.loadData,
      ),
    ),
  );

  @override
  Widget bodyContent(LearningCourseLogic logic) => Obx(() {
    logic.playerRevision.value;
    final item = logic.state.content.value!;
    final player = logic.player;
    final tokens = Get.context!.tokens;
    return CustomScrollView(
      slivers: [
        AppReadingNavigationBar(
          title: LearningStrings.tabs[1],
          actions: [
            Builder(
              builder: (context) => AppIconButton(
                icon: CupertinoIcons.share,
                onPressed: () {
                  logic.pause();
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
              AppCard(
                blur: false,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.title,
                      style: TextStyle(
                        fontSize: AppTypography.articleTitle,
                        height: 1.4,
                        fontWeight: FontWeight.w700,
                        color: tokens.labelPrimary,
                      ),
                    ),
                    if (item.author.isNotEmpty) ...[
                      const SizedBox(height: AppSpacing.sm),
                      Text(
                        item.author,
                        style: TextStyle(
                          fontSize: AppTypography.caption,
                          color: tokens.labelSecondary,
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(height: AppSpacing.lg),
              if (player != null) ...[
                ClipRRect(
                  borderRadius: BorderRadius.circular(AppRadius.card),
                  child: AspectRatio(
                    aspectRatio: player.value.aspectRatio,
                    child: VideoPlayer(player),
                  ),
                ),
                VideoProgressIndicator(
                  player,
                  allowScrubbing: true,
                  colors: VideoProgressColors(
                    playedColor: tokens.accent,
                    bufferedColor: tokens.accentSoft,
                    backgroundColor: tokens.fillPrimary,
                  ),
                  padding: const EdgeInsets.symmetric(vertical: AppSpacing.md),
                ),
                ValueListenableBuilder<VideoPlayerValue>(
                  valueListenable: player,
                  builder: (context, value, _) => Row(
                    children: [
                      Expanded(
                        child: AppButton(
                          label: value.isPlaying
                              ? LearningStrings.pause
                              : LearningStrings.play,
                          icon: value.isPlaying
                              ? CupertinoIcons.pause_fill
                              : CupertinoIcons.play_fill,
                          onPressed: logic.togglePlayback,
                        ),
                      ),
                      const SizedBox(width: AppSpacing.md),
                      Text(
                        '${_time(value.position)} / ${_time(value.duration)}',
                        key: const ValueKey('video-playback-position'),
                        style: TextStyle(
                          fontSize: AppTypography.caption,
                          color: tokens.labelSecondary,
                        ),
                      ),
                    ],
                  ),
                ),
              ] else
                ClipRRect(
                  borderRadius: BorderRadius.circular(AppRadius.card),
                  child: AspectRatio(
                    aspectRatio: 16 / 9,
                    child: ColoredBox(
                      color: tokens.fillPrimary,
                      child: Center(
                        child: Text(
                          logic.playBusy.value
                              ? LearningStrings.working
                              : logic.selected.value == null
                              ? LearningStrings.selectLesson
                              : LearningStrings.videoUnavailable,
                          style: TextStyle(
                            fontSize: AppTypography.control,
                            color: tokens.labelSecondary,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              LearningErrorNotice(
                logic.playError.value,
                retry: logic.selected.value == null
                    ? null
                    : () => logic.select(logic.selected.value!),
              ),
              LearningErrorNotice(
                logic.progressError.value,
                retry: logic.saveProgress,
              ),
              if (!logic.session.signedIn)
                Padding(
                  padding: const EdgeInsets.only(top: AppSpacing.sm),
                  child: AppButton(
                    label: LearningStrings.login,
                    variant: AppButtonVariant.secondary,
                    onPressed: () async {
                      logic.pause();
                      await LearningNavigator.login();
                    },
                  ),
                ),
              const SizedBox(height: AppSpacing.md),
              if (logic.selected.value != null)
                Text(
                  '${LearningStrings.nowPlaying} · ${logic.selected.value!.title}',
                  style: TextStyle(
                    fontSize: AppTypography.caption,
                    color: tokens.labelSecondary,
                  ),
                ),
              LearningErrorNotice(logic.state.actionError.value),
              const SizedBox(height: AppSpacing.lg),
              const AppHairlineDivider(),
              const SizedBox(height: AppSpacing.lg),
              _sectionTitle(LearningStrings.catalog, tokens),
              if (item.chapters.isEmpty)
                Padding(
                  padding: const EdgeInsets.symmetric(vertical: AppSpacing.lg),
                  child: Text(
                    LearningStrings.empty,
                    style: TextStyle(
                      fontSize: AppTypography.control,
                      color: tokens.labelSecondary,
                    ),
                  ),
                ),
              for (final chapter in item.chapters) ...[
                Padding(
                  padding: const EdgeInsets.only(
                    top: AppSpacing.xxl,
                    bottom: AppSpacing.md,
                  ),
                  child: Text(
                    chapter.title,
                    style: TextStyle(
                      fontSize: AppTypography.body,
                      fontWeight: FontWeight.w600,
                      color: tokens.labelPrimary,
                    ),
                  ),
                ),
                for (final video in chapter.videos)
                  AppListRow(
                    leading: Icon(
                      logic.selected.value?.id == video.id
                          ? CupertinoIcons.play_circle_fill
                          : CupertinoIcons.play_circle,
                      color: tokens.accent,
                    ),
                    title: video.title,
                    subtitle: '${(video.duration / 60).ceil()} min',
                    showChevron: true,
                    onTap: () => logic.select(video),
                  ),
              ],
              const SizedBox(height: AppSpacing.lg),
              const AppHairlineDivider(),
              const SizedBox(height: AppSpacing.lg),
              _sectionTitle(LearningStrings.introduction, tokens),
              const SizedBox(height: AppSpacing.sm),
              LearningContentBody(item.content),
            ],
          ),
        ),
      ],
    );
  });

  /// 时间显示与视频解码器的实际播放位置同步。
  String _time(Duration duration) =>
      '${duration.inMinutes.toString().padLeft(2, '0')}:${(duration.inSeconds % 60).toString().padLeft(2, '0')}';

  Widget _sectionTitle(String text, AppTokens tokens) => Text(
    text,
    style: TextStyle(
      fontSize: AppTypography.sectionTitle,
      fontWeight: FontWeight.w600,
      color: tokens.labelPrimary,
    ),
  );
}

/// 课程页面预览只展示静态目录，不初始化播放器。
@ResponsivePreview()
Widget previewLearningCourse() {
  final logic = LearningCourseLogic('preview', session: MatuSessionService());
  logic.state.content.value = const LearningContent(
    id: 'preview',
    title: '系统学习 Dart',
    content: '从语言基础到实际应用。',
    chapters: [
      CourseChapter('第一章 · 起步', [CourseLesson('1', '认识 Dart', 180)]),
    ],
  );
  logic.setStatusSuccess();
  return LearningCourseView(logic: logic);
}
