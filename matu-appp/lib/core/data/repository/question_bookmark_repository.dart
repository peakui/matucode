import 'dart:convert';
import 'package:shared_preferences/shared_preferences.dart';
import '../../model/matu/learning_models.dart';

/// 按账号隔离的本机面试收藏，不使用全站收藏接口。
class QuestionBookmarkRepository {
  Future<List<LearningContent>> read(String userId) async {
    if (userId.isEmpty) return [];
    final store = await SharedPreferences.getInstance();
    final raw = store.getString('matu-app-bookmarks-$userId');
    if (raw == null) return [];
    return (jsonDecode(raw) as List)
        .map((e) => LearningContent.fromJson(
            Map<String, dynamic>.from(e as Map), LearningKind.interview))
        .toList();
  }

  Future<bool> toggle(String userId, LearningContent question) async {
    if (userId.isEmpty) throw StateError('请先登录');
    final list = await read(userId);
    final exists = list.any((e) => e.id == question.id);
    list.removeWhere((e) => e.id == question.id);
    if (!exists) list.insert(0, question);
    final store = await SharedPreferences.getInstance();
    await store.setString('matu-app-bookmarks-$userId',
        jsonEncode(list.map((e) => e.toBookmark()).toList()));
    return !exists;
  }
}
