import 'package:flutter/foundation.dart';

/// 开发环境配置
abstract final class EnvDev {
  /// 禁止实例化开发环境配置类
  EnvDev._();

  /// 环境标识
  static const String flavor = 'dev';

  /// 基础接口域名
  static String get baseUrl =>
      defaultTargetPlatform == TargetPlatform.android && !kIsWeb
      ? 'http://10.0.2.2:8080'
      : 'http://127.0.0.1:8080';
}
