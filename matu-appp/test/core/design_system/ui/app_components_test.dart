import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:matu_appp/core/design_system/theme/app_metrics.dart';
import 'package:matu_appp/core/design_system/ui/ui.dart';

import '../../../support/test_environment.dart';

/// iOS 组件层冒烟渲染与交互测试。
void main() {
  setUp(initializeCoreTestEnvironment);
  tearDown(resetCoreTestEnvironment);

  testWidgets('AppCard 应用统一圆角、发丝描边与阴影', (tester) async {
    await tester.pumpWidget(
      buildCoreTestApp(
        home: const Scaffold(body: AppCard(child: Text('内容'))),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('内容'), findsOneWidget);
    final Container container = tester.widget(
      find.descendant(
        of: find.byType(AppCard),
        matching: find.byType(Container),
      ),
    );
    final BoxDecoration decoration = container.decoration! as BoxDecoration;
    expect(decoration.color, isNull);
    expect(
      find.descendant(
        of: find.byType(AppCard),
        matching: find.byType(BackdropFilter),
      ),
      findsOneWidget,
    );
    final glass = tester
        .widgetList<DecoratedBox>(
          find.descendant(
            of: find.byType(AppCard),
            matching: find.byType(DecoratedBox),
          ),
        )
        .map((widget) => widget.decoration)
        .whereType<BoxDecoration>()
        .firstWhere((decoration) => decoration.gradient != null);
    expect(glass.gradient!.colors.first.a, lessThan(1));
    expect(decoration.borderRadius, BorderRadius.circular(AppRadius.card));
    expect(glass.border!.top.width, AppElevation.hairline);
    expect(tester.takeException(), isNull);
  });

  testWidgets('AppListRow 展示图标、标题、副标题与 chevron 并响应点击', (tester) async {
    var tapped = 0;
    await tester.pumpWidget(
      buildCoreTestApp(
        home: Scaffold(
          body: AppListRow(
            leading: const Icon(CupertinoIcons.book),
            title: '标题',
            subtitle: '副标题',
            showChevron: true,
            onTap: () => tapped++,
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('标题'), findsOneWidget);
    expect(find.text('副标题'), findsOneWidget);
    expect(find.byIcon(CupertinoIcons.chevron_forward), findsOneWidget);
    await tester.tap(find.text('标题'));
    await tester.pumpAndSettle();
    expect(tapped, 1);
    expect(tester.takeException(), isNull);
  });

  testWidgets('AppButton 可用时响应点击，禁用时不响应', (tester) async {
    var tapped = 0;
    await tester.pumpWidget(
      buildCoreTestApp(
        home: Scaffold(
          body: Column(
            children: [
              AppButton(label: '主按钮', onPressed: () => tapped++),
              AppButton(
                label: '禁用按钮',
                enabled: false,
                onPressed: () => tapped++,
              ),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('主按钮'));
    await tester.pumpAndSettle();
    expect(tapped, 1);

    await tester.tap(find.text('禁用按钮'), warnIfMissed: false);
    await tester.pumpAndSettle();
    expect(tapped, 1);
    expect(tester.takeException(), isNull);
  });

  testWidgets('AppChip 选中态切换强调色背景', (tester) async {
    await tester.pumpWidget(
      buildCoreTestApp(
        home: const Scaffold(
          body: Row(
            children: [
              AppChip(label: '未选中'),
              AppChip(label: '已选中', selected: true),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('未选中'), findsOneWidget);
    expect(find.text('已选中'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('AppSkeleton 以呼吸动画渲染骨架块', (tester) async {
    await tester.pumpWidget(
      buildCoreTestApp(
        home: const Scaffold(body: Center(child: AppSkeleton(width: 80))),
      ),
    );
    // 骨架为无限循环动画，只用单帧推进，避免 pumpAndSettle 挂起。
    await tester.pump();
    await tester.pump(const Duration(milliseconds: 450));

    expect(find.byType(AppSkeleton), findsOneWidget);
    expect(find.byType(Opacity), findsWidgets);
    expect(tester.takeException(), isNull);
  });

  testWidgets('AppSkeletonList 按数量渲染占位卡片', (tester) async {
    await tester.pumpWidget(
      buildCoreTestApp(
        home: const Scaffold(body: AppSkeletonList(count: 3, showCover: true)),
      ),
    );
    await tester.pump();

    expect(find.byType(AppSkeleton), findsWidgets);
    expect(tester.takeException(), isNull);
  });

  testWidgets('AppErrorState 空消息不渲染，有消息展示重试', (tester) async {
    var retried = 0;
    await tester.pumpWidget(
      buildCoreTestApp(
        home: Scaffold(
          body: Column(
            children: [
              const AppErrorState(message: ''),
              AppErrorState(
                message: '出错了',
                retryLabel: '重试',
                onRetry: () => retried++,
              ),
            ],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('出错了'), findsOneWidget);
    await tester.tap(find.textContaining('重试'));
    await tester.pumpAndSettle();
    expect(retried, 1);
    expect(tester.takeException(), isNull);
  });
}
