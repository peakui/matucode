import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:get/get.dart';
import '../../../core/base/base/base_view.dart';
import '../../../core/design_system/theme/app_metrics.dart';
import '../../../core/design_system/theme/app_theme.dart';
import '../../../core/design_system/ui/ui.dart';
import '../../../core/model/matu/learning_models.dart';
import '../../../core/service/matu_session_service.dart';
import '../../../core/ui/preview/app_preview_annotations.dart';
import '../logics/learning_account_logic.dart';
import '../logics/learning_home_logic.dart';
import '../logics/learning_list_logic.dart';
import '../localization/learning_strings.dart';
import 'learning_account_view.dart';
import 'learning_list_view.dart';

/// 码途四入口首页，IndexedStack 保留列表与滚动位置。
class LearningHomeView extends BaseView<LearningHomeLogic> {
  /// 创建首页
  const LearningHomeView({
    super.key,
    super.logic,
    this.previewLists,
    this.previewAccount,
  });

  /// 预览态列表逻辑
  final Map<String, LearningListLogic>? previewLists;

  /// 预览态账号逻辑
  final LearningAccountLogic? previewAccount;

  static const List<IconData> _tabIcons = [
    CupertinoIcons.house,
    CupertinoIcons.play_circle,
    CupertinoIcons.chat_bubble,
    CupertinoIcons.person,
  ];
  static const List<IconData> _tabSelectedIcons = [
    CupertinoIcons.house_fill,
    CupertinoIcons.play_circle_fill,
    CupertinoIcons.chat_bubble_fill,
    CupertinoIcons.person_fill,
  ];

  /// 首页没有 AppBar，直接为路由提供状态栏和系统导航栏样式。
  @override
  Widget build(BuildContext context) => AnnotatedRegion<SystemUiOverlayStyle>(
    value: AppTheme.buildSystemUiOverlayStyle(
      AppTheme.of(context).tdTheme,
      Theme.of(context).brightness,
    ),
    child: super.build(context),
  );

  LearningListLogic _list(String tag) =>
      previewLists?[tag] ?? Get.find<LearningListLogic>(tag: tag);

  @override
  bool get isHiddenNav => true;

  /// 底部标签栏与页面表面保持一致，暗色下不再出现白色条带。
  @override
  Color get bottomBackgroundColor => Colors.transparent;

  @override
  bool get extendBody => true;

  @override
  Widget body() => SafeArea(
    child: Obx(
      () => AnimatedIndexedStack(
        index: logic.state.tab.value,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const AppLargeTitleHeader(
                title: LearningStrings.brand,
                subtitle: LearningStrings.headline,
              ),
              Padding(
                padding: const EdgeInsets.fromLTRB(
                  AppSpacing.contentPadding,
                  AppSpacing.xs,
                  AppSpacing.contentPadding,
                  AppSpacing.lg,
                ),
                child: Obx(
                  () => AppSegmentedControl<int>(
                    values: const [0, 1, 2],
                    labels: LearningStrings.feeds,
                    selected: logic.state.feed.value,
                    onChanged: logic.selectFeed,
                  ),
                ),
              ),
              Expanded(
                child: AnimatedIndexedStack(
                  index: logic.state.feed.value,
                  children: [
                    for (final tag in ['article', 'check', 'qa'])
                      LearningListView(logic: _list(tag), embedded: true),
                  ],
                ),
              ),
            ],
          ),
          Column(
            children: [
              AppLargeTitleHeader(
                title: LearningStrings.tabs[1],
                subtitle: LearningStrings.courseHeadline,
              ),
              Expanded(
                child: LearningListView(logic: _list('course'), embedded: true),
              ),
            ],
          ),
          Column(
            children: [
              AppLargeTitleHeader(
                title: LearningStrings.tabs[2],
                subtitle: LearningStrings.interviewHeadline,
              ),
              Expanded(
                child: LearningListView(
                  logic: _list('interview'),
                  embedded: true,
                ),
              ),
            ],
          ),
          LearningAccountView(
            logic: previewAccount ?? Get.find<LearningAccountLogic>(tag: 'me'),
            mode: 'me',
            embedded: true,
          ),
        ],
      ),
    ),
  );

  @override
  Widget bottom() => Obx(
    () => AppBottomTabBar(
      currentIndex: logic.state.tab.value,
      onTap: logic.selectTab,
      items: [
        for (var i = 0; i < 4; i++)
          AppTabItem(
            label: LearningStrings.tabs[i],
            icon: _tabIcons[i],
            selectedIcon: _tabSelectedIcons[i],
          ),
      ],
    ),
  );
}

/// 四入口的离线预览，构造注入不触发控制器生命周期。
@ResponsivePreview()
Widget previewLearningHome() {
  final session = MatuSessionService();
  final lists = <String, LearningListLogic>{};
  for (final kind in LearningKind.values) {
    final list = LearningListLogic(kind, session: session);
    list.setStatusEmpty();
    lists[kind.name] = list;
  }
  lists['article']!.state.dataList.add(
    const LearningContent(
      id: 'preview',
      title: '从理解一个问题，到掌握一类知识',
      summary: '学习笔记，让每一次进步都有迹可循。',
      category: '学习方法',
    ),
  );
  lists['article']!.setStatusSuccess();
  return LearningHomeView(
    logic: LearningHomeLogic(),
    previewLists: lists,
    previewAccount: LearningAccountLogic(session: session),
  );
}
