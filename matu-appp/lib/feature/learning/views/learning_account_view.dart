import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../../../application.dart';
import '../../../core/base/base/base_view.dart';
import '../../../core/design_system/theme/app_metrics.dart';
import '../../../core/design_system/theme/app_tokens.dart';
import '../../../core/design_system/ui/ui.dart';
import '../../../core/service/matu_session_service.dart';
import '../../../core/ui/preview/app_preview_annotations.dart';
import '../../../routes/learning/learning_navigator.dart';
import '../logics/learning_account_logic.dart';
import '../localization/learning_strings.dart';
import '../widgets/learning_content_body.dart';

/// 登录、个人中心、资料与设置页面，操作由账号 Logic 管理。
class LearningAccountView extends BaseView<LearningAccountLogic> {
  /// 创建账号页
  const LearningAccountView({
    super.key,
    super.logic,
    required this.mode,
    this.embedded = false,
  });

  /// 页面模式
  final String mode;

  /// 是否嵌入首页多标签容器
  final bool embedded;

  @override
  bool get isHiddenNav => embedded;
  @override
  bool get useGlassBackground => !embedded;
  @override
  Color? get backgroundColor => embedded ? Colors.transparent : null;

  @override
  String get navTitle => switch (mode) {
    'login' => LearningStrings.login,
    'profile' => LearningStrings.profile,
    'settings' => LearningStrings.settings,
    _ => LearningStrings.tabs[3],
  };

  /// 导航栏统一由 iOS 大标题承担，避免 TDesign 导航栏在暗色下的白底。
  @override
  PreferredSizeWidget? head() => null;

  @override
  Widget body() => Obx(() {
    final children = switch (mode) {
      'login' => _login(),
      'settings' => _settings(),
      'profile' => _profile(),
      _ => _me(),
    };
    return RefreshIndicator(
      onRefresh: logic.load,
      child: CustomScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        slivers: [
          if (embedded)
            SliverToBoxAdapter(child: AppLargeTitleHeader(title: navTitle))
          else
            AppLargeTitleBar(title: navTitle),
          SliverPadding(
            padding: EdgeInsets.fromLTRB(
              AppSpacing.contentPadding,
              AppSpacing.sm,
              AppSpacing.contentPadding,
              // 嵌入首页时为悬浮胶囊留出高度，避免末行被遮住。
              embedded
                  ? AppSpacing.xxxl + AppGlass.tabHeight + AppGlass.tabBottomGap
                  : AppSpacing.xxxl,
            ),
            sliver: SliverList.list(
              children: [
                ...children,
                LearningErrorNotice(
                  logic.state.error.value,
                  retry: mode == 'login' ? null : logic.load,
                ),
                if (mode != 'login' && logic.state.message.value.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: AppSpacing.md),
                    child: Text(
                      logic.state.message.value,
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: AppTypography.control,
                        color: Get.context!.tokens.systemGreen,
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  });

  List<Widget> _login() => [
    Padding(
      padding: const EdgeInsets.symmetric(vertical: AppSpacing.xl),
      child: Icon(
        CupertinoIcons.person_crop_circle,
        size: AppSpacing.xxl * 2,
        color: Get.context!.tokens.accent,
      ),
    ),
    Text(
      LearningStrings.loginHint,
      textAlign: TextAlign.center,
      style: TextStyle(
        fontSize: AppTypography.control,
        color: Get.context!.tokens.labelSecondary,
      ),
    ),
    const SizedBox(height: AppSpacing.xl),
    AppTextInput(
      controller: logic.accountController,
      hint: LearningStrings.account,
    ),
    const SizedBox(height: AppSpacing.md),
    AppTextInput(
      controller: logic.passwordController,
      hint: LearningStrings.password,
      obscureText: true,
    ),
    const SizedBox(height: AppSpacing.xl),
    AppButton(
      label: logic.state.busy.value
          ? LearningStrings.working
          : LearningStrings.login,
      busy: logic.state.busy.value,
      expand: true,
      onPressed: logic.login,
    ),
    const SizedBox(height: AppSpacing.md),
    AppButton(
      label: LearningStrings.browse,
      variant: AppButtonVariant.plain,
      onPressed: () => Get.back<void>(),
    ),
  ];

  List<Widget> _me() {
    final tokens = Get.context!.tokens;
    final user = logic.session.user.value;
    return [
      Padding(
        padding: const EdgeInsets.symmetric(vertical: AppSpacing.md),
        child: Icon(
          CupertinoIcons.person_crop_circle,
          size: AppSpacing.xxxl,
          color: tokens.accent,
        ),
      ),
      Text(
        user?.nickname ?? LearningStrings.guest,
        textAlign: TextAlign.center,
        style: TextStyle(
          fontSize: AppTypography.articleTitle,
          fontWeight: FontWeight.w700,
          color: tokens.labelPrimary,
        ),
      ),
      if (user == null) ...[
        const SizedBox(height: AppSpacing.sm),
        Text(
          LearningStrings.guestHint,
          textAlign: TextAlign.center,
          style: TextStyle(
            fontSize: AppTypography.control,
            color: tokens.labelSecondary,
          ),
        ),
        const SizedBox(height: AppSpacing.lg),
        AppButton(
          label: LearningStrings.login,
          expand: true,
          onPressed: LearningNavigator.login,
        ),
      ] else ...[
        const SizedBox(height: AppSpacing.md),
        AppButton(
          label: LearningStrings.profile,
          variant: AppButtonVariant.secondary,
          onPressed: () => LearningNavigator.account('profile'),
        ),
      ],
      const AppSectionHeader(LearningStrings.statistics),
      Wrap(
        spacing: AppSpacing.xl,
        runSpacing: AppSpacing.lg,
        children: [
          _stat('totalDays', LearningStrings.totalDays),
          _stat('continuousDays', LearningStrings.continuousDays),
          _stat('totalLearnHours', LearningStrings.learnHours),
        ],
      ),
      const SizedBox(height: AppSpacing.md),
      AppCard(
        padding: EdgeInsets.zero,
        blur: false,
        child: Column(
          children: [
            AppListRow(
              leading: Icon(CupertinoIcons.bookmark, color: tokens.accent),
              title: LearningStrings.bookmarks,
              showChevron: true,
              onTap: () => LearningNavigator.mine('bookmarks'),
            ),
            const AppHairlineDivider(indent: AppSpacing.listRowMinHeight),
            AppListRow(
              leading: Icon(
                CupertinoIcons.checkmark_alt_circle,
                color: tokens.accent,
              ),
              title: LearningStrings.myChecks,
              showChevron: true,
              onTap: () => LearningNavigator.mine('checks'),
            ),
            const AppHairlineDivider(indent: AppSpacing.listRowMinHeight),
            AppListRow(
              leading: Icon(
                CupertinoIcons.question_circle,
                color: tokens.accent,
              ),
              title: LearningStrings.myQuestions,
              showChevron: true,
              onTap: () => LearningNavigator.mine('questions'),
            ),
            const AppHairlineDivider(indent: AppSpacing.listRowMinHeight),
            AppListRow(
              leading: Icon(CupertinoIcons.settings, color: tokens.accent),
              title: LearningStrings.settings,
              showChevron: true,
              onTap: () => LearningNavigator.account('settings'),
            ),
          ],
        ),
      ),
      if (user != null) ...[
        const SizedBox(height: AppSpacing.xl),
        AppButton(
          label: LearningStrings.logout,
          variant: AppButtonVariant.destructive,
          expand: true,
          enabled: !logic.state.busy.value,
          onPressed: logic.logout,
        ),
      ],
    ];
  }

  Widget _stat(String field, String label) =>
      AppStatTile(value: '${logic.state.stats[field] ?? '—'}', label: label);

  List<Widget> _profile() {
    if (!logic.session.signedIn) {
      return [
        AppButton(
          label: LearningStrings.login,
          onPressed: LearningNavigator.login,
        ),
      ];
    }
    return [
      if (logic.state.profile.value?.avatar.isNotEmpty == true)
        Center(
          child: ClipOval(
            child: Image.network(
              LearningContentBody.assetUrl(logic.state.profile.value!.avatar),
              width: AppSpacing.xxl * 2,
              height: AppSpacing.xxl * 2,
              fit: BoxFit.cover,
              frameBuilder: _fadeInFrame,
              errorBuilder: (_, _, _) =>
                  const Icon(CupertinoIcons.person_crop_circle),
            ),
          ),
        ),
      const SizedBox(height: AppSpacing.md),
      AppButton(
        label: LearningStrings.avatar,
        variant: AppButtonVariant.secondary,
        enabled: !logic.state.busy.value,
        onPressed: logic.avatar,
      ),
      const SizedBox(height: AppSpacing.xl),
      AppTextInput(
        controller: logic.nicknameController,
        hint: LearningStrings.nickname,
      ),
      const SizedBox(height: AppSpacing.md),
      AppTextInput(
        controller: logic.signatureController,
        hint: LearningStrings.signature,
        maxLines: 3,
      ),
      const SizedBox(height: AppSpacing.xl),
      AppButton(
        label: logic.state.busy.value
            ? LearningStrings.working
            : LearningStrings.save,
        busy: logic.state.busy.value,
        expand: true,
        onPressed: logic.save,
      ),
    ];
  }

  /// 网络图解码完成前保持透明，避免头像"跳"进布局。
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

  List<Widget> _settings() {
    final tokens = Get.context!.tokens;
    return [
      Text(
        'matu-appp · ${logic.state.version.value}',
        style: TextStyle(
          fontSize: AppTypography.control,
          color: tokens.labelSecondary,
        ),
      ),
      const SizedBox(height: AppSpacing.lg),
      AppCard(
        padding: EdgeInsets.zero,
        blur: false,
        child: AppListRow(
          leading: Icon(CupertinoIcons.moon, color: tokens.accent),
          title: LearningStrings.darkMode,
          trailing: CupertinoSwitch(
            value: Application.themeMode.value == ThemeMode.dark,
            onChanged: (value) => Application.updateThemeMode(
              value ? ThemeMode.dark : ThemeMode.light,
            ),
          ),
          onTap: () => Application.updateThemeMode(
            Application.themeMode.value == ThemeMode.dark
                ? ThemeMode.light
                : ThemeMode.dark,
          ),
        ),
      ),
      const SizedBox(height: AppSpacing.lg),
      Text(
        LearningStrings.bookmarkHint,
        style: TextStyle(
          fontSize: AppTypography.summary,
          color: tokens.labelTertiary,
        ),
      ),
      if (logic.session.signedIn) ...[
        const SizedBox(height: AppSpacing.xl),
        AppButton(
          label: LearningStrings.logout,
          variant: AppButtonVariant.destructive,
          expand: true,
          enabled: !logic.state.busy.value,
          onPressed: logic.logout,
        ),
      ],
    ];
  }
}

@ResponsivePreview()
Widget previewLearningLogin() => LearningAccountView(
  logic: LearningAccountLogic(mode: 'login', session: MatuSessionService()),
  mode: 'login',
);
