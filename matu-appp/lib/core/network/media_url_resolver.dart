/// 码途公共媒体地址解析，不修改鉴权参数或绕过课程授权接口。
abstract final class MediaUrlResolver {
  /// 已核验与旧公共媒体域名使用同一 Bucket 的 HTTPS 源站。
  static const String publicOrigin = String.fromEnvironment(
    'MEDIA_PUBLIC_ORIGIN',
    defaultValue: 'https://your-bucket.oss-cn-hangzhou.aliyuncs.com',
  );

  /// 解析 [value]，仅迁移旧域名的无签名公共资源。
  ///
  /// 旧域名证书与域名不匹配；源站使用有效 HTTPS，绝不降低为 HTTP。
  /// 带查询参数的签名地址、第三方域名和非公共路径均保持原样。
  static Uri? resolve(String value, {required String baseUrl}) {
    final relative = Uri.tryParse(value.trim());
    final base = Uri.tryParse(baseUrl);
    if (value.trim().isEmpty || relative == null || base == null) return null;
    final uri = base.resolveUri(relative);
    if (!['http', 'https'].contains(uri.scheme) ||
        uri.host.isEmpty ||
        uri.userInfo.isNotEmpty) {
      return null;
    }
    if (uri.host == 'www.example.com' &&
        uri.path.startsWith('/image/') &&
        !uri.hasQuery &&
        !uri.hasFragment &&
        !uri.hasPort) {
      final origin = Uri.tryParse(publicOrigin);
      if (origin != null &&
          origin.scheme == 'https' &&
          origin.host.isNotEmpty &&
          origin.userInfo.isEmpty &&
          !origin.hasQuery) {
        return uri.replace(
          scheme: 'https',
          host: origin.host,
          port: origin.hasPort ? origin.port : null,
        );
      }
    }
    return uri;
  }
}
