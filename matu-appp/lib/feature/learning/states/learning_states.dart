import 'package:get/get.dart';
import '../../../core/base/base_list/base_list_state.dart';
import '../../../core/base/base_network/base_network_state.dart';
import '../../../core/model/matu/learning_models.dart';

/// 学习列表筛选与分页展示状态。
class LearningListState extends BaseListState<LearningContent> {
  @override
  int get pageSize => 10;
  @override
  bool get requestErrorToast => false;
  final categories = <LearningCategory>[].obs;
  final category = ''.obs;
  final keyword = ''.obs;
  final level = 0.obs;
  final sort = 'latest'.obs;
  final month = Rxn<DateTime>();
  final error = ''.obs;
  final categoryError = ''.obs;
  final fetching = false.obs;
}

/// 详情页的业务展示状态。
class LearningDetailState extends BaseNetworkState {
  @override
  bool get requestErrorToast => false;
  final content = Rxn<LearningContent>();
  final error = ''.obs;
  final actionError = ''.obs;
  final acting = false.obs;
  final expanded = false.obs;
  final bookmarked = false.obs;
  final progress = 0.obs;
  final discussions = <LearningDiscussion>[].obs;
  final discussionBusy = false.obs;
  final discussionError = ''.obs;
  final discussionDone = false.obs;
}

/// 账号表单及个人中心状态。
class LearningAccountState {
  final version = ''.obs;
  final busy = false.obs;
  final error = ''.obs;
  final message = ''.obs;
  final profile = Rxn<MatuProfile>();
  final stats = <String, dynamic>{}.obs;
}

/// 首页导航选择。
class LearningHomeState {
  final tab = 0.obs;
  final feed = 0.obs;
}
