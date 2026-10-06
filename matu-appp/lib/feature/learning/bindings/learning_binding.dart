import 'package:get/get.dart';
import '../../../core/model/matu/learning_models.dart';
import '../../../routes/learning/learning_navigator.dart';
import '../logics/learning_home_logic.dart';
import '../logics/learning_list_logic.dart';
import '../logics/learning_detail_logic.dart';
import '../logics/learning_course_logic.dart';
import '../logics/learning_account_logic.dart';

/// 按页面生命周期注册码途业务依赖。
class LearningBinding extends Bindings {
  LearningBinding(this.page);
  final String page;
  @override
  void dependencies() {
    switch (page) {
      case 'home':
        Get.lazyPut(LearningHomeLogic.new);
        for (final kind in LearningKind.values) {
          Get.lazyPut(() => LearningListLogic(kind), tag: kind.name);
        }
        Get.lazyPut(() => LearningAccountLogic(), tag: 'me');
      case 'detail':
        final params = Get.arguments as LearningDetailParams;
        Get.lazyPut(
          () => LearningDetailLogic(
            params.kind,
            params.id,
            listContext: params.context,
          ),
        );
      case 'course':
        final params = Get.arguments as LearningDetailParams;
        Get.lazyPut(() => LearningCourseLogic(params.id));
      case 'mine':
        final mine = Get.arguments as String;
        Get.lazyPut(
          () => LearningListLogic(
            mine == 'checks'
                ? LearningKind.check
                : mine == 'questions'
                ? LearningKind.qa
                : LearningKind.interview,
            mine: mine,
          ),
          tag: 'mine',
        );
      default:
        Get.lazyPut(() => LearningAccountLogic(mode: page), tag: page);
    }
  }
}
