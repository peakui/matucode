import 'package:get/get.dart';
import '../../feature/learning/bindings/learning_binding.dart';
import '../../feature/learning/logics/learning_list_logic.dart';
import '../../feature/learning/logics/learning_account_logic.dart';
import '../../feature/learning/views/learning_home_view.dart';
import '../../feature/learning/views/learning_list_view.dart';
import '../../feature/learning/views/learning_detail_view.dart';
import '../../feature/learning/views/learning_course_view.dart';
import '../../feature/learning/views/learning_account_view.dart';
import '../../feature/learning/localization/learning_strings.dart';

/// 正式应用路由，示例路由不进入主导航。
abstract final class LearningPages {
  static final routes = <GetPage<dynamic>>[
    GetPage(
      name: '/main',
      page: () => const LearningHomeView(),
      binding: LearningBinding('home'),
    ),
    GetPage(
      name: '/learning/detail',
      page: () => LearningDetailView(),
      binding: LearningBinding('detail'),
    ),
    GetPage(
      name: '/learning/course',
      page: () => LearningCourseView(),
      binding: LearningBinding('course'),
    ),
    for (final mode in ['login', 'profile', 'settings'])
      GetPage(
        name: '/learning/$mode',
        page: () => LearningAccountView(
          logic: Get.find<LearningAccountLogic>(tag: mode),
          mode: mode,
        ),
        binding: LearningBinding(mode),
      ),
    GetPage(
      name: '/learning/mine',
      page: () => LearningListView(
        logic: Get.find<LearningListLogic>(tag: 'mine'),
        title: switch (Get.arguments as String) {
          'checks' => LearningStrings.myChecks,
          'questions' => LearningStrings.myQuestions,
          _ => LearningStrings.bookmarks,
        },
      ),
      binding: LearningBinding('mine'),
    ),
  ];
}
