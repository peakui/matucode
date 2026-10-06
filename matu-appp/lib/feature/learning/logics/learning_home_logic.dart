import '../../../core/base/base/base_logic.dart';
import '../states/learning_states.dart';

/// 四入口与首页内容类型切换。
class LearningHomeLogic extends BaseLogic {
  final LearningHomeState state = LearningHomeState();
  void selectTab(int index) => state.tab.value = index;
  void selectFeed(int index) => state.feed.value = index;
}
