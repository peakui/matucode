import 'dart:convert';
import 'package:get/get.dart';
import '../data/repository/matu_session_repository.dart';
import '../model/matu/learning_models.dart';

/// 码途会话生命周期及请求代次隔离。
class MatuSessionService extends GetxService {
  MatuSessionService({
    MatuSessionRepository repository = const MatuSessionRepository(),
  }) : _matuSessionRepository = repository;
  final MatuSessionRepository _matuSessionRepository;
  final user = Rxn<MatuProfile>();
  final revision = 0.obs;
  String authorization = '';
  DateTime? expiresAt;
  Future<void> _persistence = Future<void>.value();
  bool get signedIn => authorization.isNotEmpty && user.value != null;

  /// 恢复未过期会话；无法读取安全存储时以游客启动。
  Future<void> initialize() async {
    try {
      final raw = await _matuSessionRepository.read();
      if (raw == null) return;
      final data = jsonDecode(raw) as Map<String, dynamic>;
      expiresAt = DateTime.tryParse('${data['expiresAt']}');
      if (expiresAt == null || !expiresAt!.isAfter(DateTime.now())) {
        await clear();
        return;
      }
      authorization = stringValue(data['authorization']);
      user.value = MatuProfile.fromJson(
        Map<String, dynamic>.from(data['user'] as Map),
      );
    } on Object {
      authorization = '';
      user.value = null;
    }
  }

  /// 写入登录结果，不携带账号密码。
  Future<void> accept(Map<String, dynamic> data) async {
    final token = stringValue(data['authorization']);
    final profile = MatuProfile.fromJson(
      Map<String, dynamic>.from(data['user'] as Map),
    );
    if (token.isEmpty || profile.id.isEmpty) {
      throw const FormatException('登录响应缺少凭证');
    }
    authorization = token;
    expiresAt = DateTime.now().add(
      Duration(seconds: countValue(data['expiresIn'])),
    );
    user.value = profile;
    revision.value++;
    await _save();
  }

  /// 资料更新不会改变会话代次。
  Future<void> updateProfile(MatuProfile profile) async {
    if (!signedIn) return;
    user.value = MatuProfile(
      id: user.value!.id,
      nickname: profile.nickname,
      avatar: profile.avatar,
      signature: profile.signature,
    );
    await _save();
  }

  Future<void> _save() {
    final snapshot = jsonEncode({
      'authorization': authorization,
      'expiresAt': expiresAt?.toIso8601String(),
      'user': user.value?.toJson(),
    });
    return _enqueue(() => _matuSessionRepository.save(snapshot));
  }

  /// 先清内存再串行清存储，阻止旧请求污染新账号。
  Future<void> clear() {
    authorization = '';
    expiresAt = null;
    user.value = null;
    revision.value++;
    return _enqueue(_matuSessionRepository.clear);
  }

  Future<void> _enqueue(Future<void> Function() action) {
    _persistence = _persistence.catchError((Object _) {}).then((_) => action());
    return _persistence;
  }
}
