import 'dart:convert';

/// 码途响应解包与长整数精度保护。
abstract final class MatuResponseCodec {
  /// 在 JSON 解码前将长整数转成字符串，覆盖 Web 的数值精度限制。
  static dynamic decode(String raw) {
    final safe = raw.replaceAllMapped(
      RegExp(r'"(?:[^"\\]|\\.)*"|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?'),
      (match) {
        final token = match[0]!;
        return RegExp(r'^-?\d{16,}$').hasMatch(token) ? '"$token"' : token;
      },
    );
    return jsonDecode(safe);
  }

  /// 仅接受后端已定义的成功包，空 401 也保留 HTTP 状态。
  static dynamic unwrap(int status, dynamic raw) {
    dynamic body;
    try {
      body = raw is String ? decode(raw) : raw;
    } on FormatException {
      throw MatuApiException(status, '服务器响应异常');
    }
    if (body is! Map) throw MatuApiException(status, '服务器响应异常');
    final code = int.tryParse('${body['code']}');
    if (status < 200 || status >= 300 || code != 0) {
      throw MatuApiException(
          status == 401 ? 401 : code ?? status, '${body['message'] ?? '请求失败'}');
    }
    return body['data'];
  }
}

/// 保留后端错误码用于鉴权与用户提示。
class MatuApiException implements Exception {
  /// 创建接口异常。
  const MatuApiException(this.code, this.message);

  /// HTTP 或业务错误码。
  final int code;

  /// 可展示信息。
  final String message;
  @override
  String toString() => message;
}
