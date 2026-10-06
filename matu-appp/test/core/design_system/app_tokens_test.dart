import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tdesign_flutter/tdesign_flutter.dart';

import 'package:matu_appp/core/design_system/theme/app_metrics.dart';
import 'package:matu_appp/core/design_system/theme/app_tokens.dart';

/// AppTokens 明暗两套语义色、复制与插值行为测试。
void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  final TDThemeData source = TDThemeData.defaultData().light;
  final AppTokens light = AppTokens.fromTdTheme(source, Brightness.light);
  final AppTokens dark = AppTokens.fromTdTheme(source, Brightness.dark);

  test('明暗两套令牌在表面与文字色上取值不同', () {
    expect(light.brightness, Brightness.light);
    expect(dark.brightness, Brightness.dark);
    expect(light.bgPage, isNot(dark.bgPage));
    expect(light.bgSurface, isNot(dark.bgSurface));
    expect(light.labelPrimary, isNot(dark.labelPrimary));
    expect(light.hairline, isNot(dark.hairline));
    expect(light.cardShadow.first.color, isNot(dark.cardShadow.first.color));
  });

  test('强调色沿用 TDesign 品牌色，前景色随其亮度取反', () {
    expect(light.accent, source.brandColor7);
    expect(dark.accent, source.brandColor7);
    expect(light.onAccent, isNot(light.accent));
    expect(dark.onAccent, isNot(dark.accent));
  });

  test('copyWith 覆盖指定字段并保留其余字段', () {
    final AppTokens copied = light.copyWith(
      bgSurface: Colors.purple,
      labelPrimary: Colors.green,
    );

    expect(copied.bgSurface, Colors.purple);
    expect(copied.labelPrimary, Colors.green);
    expect(copied.bgPage, light.bgPage);
    expect(copied.accent, light.accent);
    expect(copied.brightness, light.brightness);
  });

  test('lerp 按比例插值颜色，并在中点前后切换亮度标识', () {
    final AppTokens early = light.lerp(dark, 0.25);
    final AppTokens late = light.lerp(dark, 0.75);

    expect(early.bgSurface, Color.lerp(light.bgSurface, dark.bgSurface, 0.25));
    expect(late.bgSurface, Color.lerp(light.bgSurface, dark.bgSurface, 0.75));
    expect(early.brightness, Brightness.light);
    expect(late.brightness, Brightness.dark);
  });

  test('lerp 传入非 AppTokens 时返回自身', () {
    expect(light.lerp(null, 0.5), same(light));
  });

  test('卡片装饰使用统一圆角与发丝描边', () {
    final BoxDecoration decoration = appCardDecoration(light);

    expect(decoration.color, light.bgSurface);
    expect(
      decoration.borderRadius,
      BorderRadius.circular(AppRadius.card),
    );
    expect(decoration.border!.top.width, AppElevation.hairline);
    expect(decoration.border!.top.color, light.hairline);
    expect(decoration.boxShadow, light.cardShadow);
  });
}
