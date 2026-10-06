import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';
import 'package:matu_appp/core/design_system/ui/ui.dart';
import 'package:matu_appp/core/model/matu/learning_models.dart';
import 'package:matu_appp/core/service/matu_session_service.dart';
import 'package:matu_appp/feature/learning/localization/learning_strings.dart';
import 'package:matu_appp/feature/learning/logics/learning_account_logic.dart';
import 'package:matu_appp/feature/learning/logics/learning_detail_logic.dart';
import 'package:matu_appp/feature/learning/views/learning_account_view.dart';
import 'package:matu_appp/feature/learning/views/learning_course_view.dart';
import 'package:matu_appp/feature/learning/views/learning_detail_view.dart';
import 'package:matu_appp/feature/learning/views/learning_home_view.dart';
import '../support/test_environment.dart';
import 'package:matu_appp/routes/learning/learning_pages.dart';
import 'package:matu_appp/routes/learning/learning_navigator.dart';

/// 窄屏、横屏、深浅色和关键交互验证。
void main() {
  setUp(initializeCoreTestEnvironment);
  tearDown(resetCoreTestEnvironment);
  testWidgets('长文章标题完整换行且窄屏阅读没有横向溢出', (tester) async {
    tester.view.physicalSize = const Size(320, 640);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    final logic = LearningDetailLogic(
      LearningKind.article,
      '1',
      session: MatuSessionService(),
    );
    const title = 'React 19 新特性梳理：Actions 与 use() 到底解决了什么问题，以及在项目中的实践方式';
    logic.state.content.value = const LearningContent(
      id: '1',
      title: title,
      content:
          '## 为什么需要 Actions\n\n让每一次学习都有迹可循。\n\n```dart\nfinal value = await repository.load();\n```',
    );
    logic.state.discussionDone.value = true;
    logic.setStatusSuccess();
    await tester.pumpWidget(
      buildCoreTestApp(home: LearningDetailView(logic: logic)),
    );
    await tester.pumpAndSettle();
    final heading = tester.widget<Text>(
      find.byKey(const ValueKey('article-full-title')),
    );
    expect(heading.data, title);
    expect(heading.maxLines, isNull);
    expect(heading.overflow, isNot(TextOverflow.ellipsis));
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
    logic.onClose();
  });
  testWidgets('真实命名路由打开登录并取消返回，不发生泛型路由转换异常', (tester) async {
    Get.put(MatuSessionService());
    await tester.pumpWidget(
      buildCoreTestApp(
        routes: LearningPages.routes,
        home: Scaffold(
          body: TextButton(
            onPressed: LearningNavigator.login,
            child: const Text('打开登录'),
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();
    await tester.tap(find.text('打开登录'));
    await tester.pumpAndSettle();
    expect(find.byType(LearningAccountView), findsOneWidget);
    expect(tester.takeException(), isNull);
    Get.back<dynamic>();
    await tester.pumpAndSettle();
    expect(find.text('打开登录'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
  for (final size in [
    const Size(320, 640),
    const Size(640, 360),
    const Size(800, 1000),
  ]) {
    for (final brightness in Brightness.values) {
      testWidgets('四入口在 $size / $brightness 无布局溢出', (tester) async {
        tester.view.physicalSize = size;
        tester.view.devicePixelRatio = 1;
        addTearDown(tester.view.resetPhysicalSize);
        addTearDown(tester.view.resetDevicePixelRatio);
        await tester.pumpWidget(
          buildCoreTestApp(home: previewLearningHome(), brightness: brightness),
        );
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull);
        for (final tab in ['教程', '面试', '我的', '首页']) {
          await tester.tap(find.text(tab).last);
          await tester.pumpAndSettle();
          expect(tester.takeException(), isNull);
        }
      });
    }
  }
  testWidgets('空登录提交展示校验错误，不发送请求', (tester) async {
    final logic = LearningAccountLogic(
      mode: 'login',
      session: MatuSessionService(),
    );
    await tester.pumpWidget(
      buildCoreTestApp(
        home: LearningAccountView(logic: logic, mode: 'login'),
      ),
    );
    await tester.pumpAndSettle();
    final button = find.byWidgetPredicate(
      (widget) => widget is AppButton && widget.label == LearningStrings.login,
    );
    await tester.ensureVisible(button);
    await tester.tap(button);
    await tester.pumpAndSettle();
    expect(logic.state.error.value, LearningStrings.loginValidation);
    await tester.scrollUntilVisible(
      find.text(LearningStrings.loginValidation),
      100,
      scrollable: find.byType(Scrollable).first,
    );
    expect(find.text(LearningStrings.loginValidation), findsOneWidget);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
    logic.onClose();
  });
  testWidgets('受限面试题不渲染答案或展开按钮', (tester) async {
    final logic = LearningDetailLogic(
      LearningKind.interview,
      '1',
      session: MatuSessionService(),
    );
    logic.state.content.value = const LearningContent(
      id: '1',
      title: '题目',
      answer: 'secret',
      answerVisible: false,
    );
    logic.setStatusSuccess();
    await tester.pumpWidget(
      buildCoreTestApp(home: LearningDetailView(logic: logic)),
    );
    await tester.pumpAndSettle();
    expect(find.text('secret'), findsNothing);
    expect(find.text(LearningStrings.showAnswer), findsNothing);
    expect(find.text(LearningStrings.answerLocked), findsOneWidget);
    expect(tester.takeException(), isNull);
    await tester.pumpWidget(const SizedBox());
    logic.onClose();
  });
  testWidgets('课程预览渲染目录，不创建真实播放器', (tester) async {
    await tester.pumpWidget(buildCoreTestApp(home: previewLearningCourse()));
    await tester.pumpAndSettle();
    expect(find.text(LearningStrings.selectLesson), findsOneWidget);
    await tester.scrollUntilVisible(
      find.text('认识 Dart'),
      180,
      scrollable: find.byType(Scrollable).first,
    );
    expect(find.text('认识 Dart'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
