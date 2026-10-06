import 'package:flutter/material.dart';

import '../theme/app_metrics.dart';
import '../theme/app_tokens.dart';

/// 0.5px 发丝分隔线，用于列表分组与卡片描边之间的区隔。
class AppHairlineDivider extends StatelessWidget {
  /// 创建发丝分隔线
  const AppHairlineDivider({super.key, this.indent = 0, this.endIndent = 0});

  /// 左侧缩进
  final double indent;

  /// 右侧缩进
  final double endIndent;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: AppElevation.hairline,
      margin: EdgeInsetsDirectional.only(start: indent, end: endIndent),
      color: context.tokens.separator,
    );
  }
}
