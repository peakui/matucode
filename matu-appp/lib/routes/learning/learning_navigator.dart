import 'package:get/get.dart';
import '../../core/model/matu/learning_models.dart';
import '../../core/service/matu_session_service.dart';
import '../../feature/learning/logics/learning_list_logic.dart';

/// 学习路由的明确参数。
class LearningDetailParams {
  const LearningDetailParams(this.kind, this.id, this.context);
  final LearningKind kind;
  final String id;
  final LearningListLogic? context;
}

/// 码途页面导航入口。
abstract final class LearningNavigator {
  static Future<void> detail(
    LearningKind kind,
    String id, {
    LearningListLogic? context,
  }) async {
    await Get.toNamed<void>(
      kind == LearningKind.course ? '/learning/course' : '/learning/detail',
      arguments: LearningDetailParams(kind, id, context),
    );
  }

  static Future<void> login() async {
    // GetX 命名路由生成 Route<dynamic>；登录结果以会话服务为准。
    await Get.toNamed<dynamic>('/learning/login');
  }

  static Future<bool> requireLogin() async {
    final session = Get.find<MatuSessionService>();
    if (!session.signedIn) await login();
    return session.signedIn;
  }

  static Future<void> account(String mode) async {
    if (mode == 'profile' && !await requireLogin()) return;
    await Get.toNamed<void>('/learning/$mode');
  }

  static Future<void> mine(String kind) async {
    if (!await requireLogin()) return;
    await Get.toNamed<void>('/learning/mine', arguments: kind);
  }
}
