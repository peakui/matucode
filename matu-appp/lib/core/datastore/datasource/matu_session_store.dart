import 'package:flutter_secure_storage/flutter_secure_storage.dart';

/// 账号授权信息使用平台安全存储，不保存密码。
class MatuSessionStore {
  const MatuSessionStore();
  static const _storage = FlutterSecureStorage();
  Future<String?> read() => _storage.read(key: 'matu.app.session');
  Future<void> write(String value) =>
      _storage.write(key: 'matu.app.session', value: value);
  Future<void> clear() => _storage.delete(key: 'matu.app.session');
}
