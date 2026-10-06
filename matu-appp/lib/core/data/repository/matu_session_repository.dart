import '../../datastore/datasource/matu_session_store.dart';

/// 会话安全存储仓库。
class MatuSessionRepository {
  const MatuSessionRepository(
      {MatuSessionStore store = const MatuSessionStore()})
      : _store = store;
  final MatuSessionStore _store;
  Future<String?> read() => _store.read();
  Future<void> save(String value) => _store.write(value);
  Future<void> clear() => _store.clear();
}
