import 'dart:async';
import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../../../core/base/base_list/base_list_logic.dart';
import '../../../core/model/matu/learning_models.dart';
import '../../../core/model/response/base/base_list_response.dart';
import '../../../core/model/response/base/base_response.dart';
import '../../../core/data/repository/learning_repository.dart';
import '../../../core/data/repository/question_bookmark_repository.dart';
import '../../../core/service/matu_session_service.dart';
import '../../../routes/learning/learning_navigator.dart';
import '../states/learning_states.dart';

/// 独立保留各类列表、筛选和分页，过期响应不能覆盖新筛选。
class LearningListLogic extends BaseListLogic<LearningContent> {
  LearningListLogic(
    this.kind, {
    this.mine,
    LearningRepository? repository,
    MatuSessionService? session,
  }) : _learningRepository = repository ?? LearningRepository(session: session),
       session = session ?? Get.find<MatuSessionService>();
  final LearningKind kind;
  final String? mine;
  final LearningRepository _learningRepository;
  final MatuSessionService session;
  final LearningListState state = LearningListState();
  final searchController = TextEditingController();
  final scrollController = ScrollController();
  int _generation = 0;
  Timer? _debounce;
  Worker? _sessionWorker;
  final _questionBookmarkRepository = QuestionBookmarkRepository();
  @override
  LearningListState get listState => state;
  Map<String, dynamic> get filters => {
    'keyword': state.keyword.value,
    'categoryId': state.category.value,
    if (kind == LearningKind.course)
      'level': state.level.value == 0 ? null : state.level.value,
    if (kind == LearningKind.interview)
      'difficulty': state.level.value == 0 ? null : state.level.value,
    if (kind == LearningKind.course) 'sortBy': state.sort.value,
    if (state.month.value != null) ...{
      'year': state.month.value!.year,
      'month': state.month.value!.month,
    },
    if (mine == 'checks') 'userId': session.user.value?.id,
  };
  @override
  Future<BaseResponse<BaseListResponse<LearningContent>>> Function()?
  get apiRequest =>
      () => _learningRepository.list(kind, {
        ...filters,
        'pageNum': state.currentPage,
        'pageSize': state.pageSize,
      }, mine: mine);
  @override
  void onInit() {
    super.onInit();
    _sessionWorker = ever(session.revision, (_) {
      refresh();
    });
  }

  @override
  void initData() {
    loadData();
    loadCategories();
  }

  Future<void> loadCategories() async {
    try {
      state.categories.assignAll(await _learningRepository.categories(kind));
      state.categoryError.value = '';
    } on Object catch (e) {
      state.categoryError.value = e.toString();
    }
  }

  @override
  Future<void> loadData() async {
    if (state.fetching.value || isClosed) return;
    final generation = _generation;
    final revision = session.revision.value;
    state.fetching.value = true;
    state.error.value = '';
    try {
      if (mine != null && !session.signedIn) {
        state.dataList.clear();
        setStatusEmpty();
        return;
      }
      final BaseListResponse<LearningContent> data;
      if (mine == 'bookmarks') {
        final records = await _questionBookmarkRepository.read(
          session.user.value!.id,
        );
        data = BaseListResponse(
          list: records,
          pagination: PageMeta(
            total: records.length,
            size: records.length,
            page: 1,
          ),
        );
      } else {
        data = (await apiRequest!()).data!;
      }
      if (generation != _generation ||
          revision != session.revision.value ||
          isClosed) {
        return;
      }
      if (state.currentPage == 1) state.dataList.clear();
      requestOk(data);
      if (state.dataList.isNotEmpty) setStatusSuccess();
    } on Object catch (e) {
      if (generation != _generation || isClosed) return;
      state.error.value = e.toString();
      onRequestError(e.toString(), e);
    } finally {
      if (generation == _generation && !isClosed) state.fetching.value = false;
    }
  }

  @override
  Future<void> refresh() async {
    _generation++;
    state.fetching.value = false;
    state.dataList.clear();
    setStatusLoad();
    await super.refresh();
  }

  @override
  Future<void> loadMore() async {
    if (state.fetching.value || mine == 'bookmarks') return;
    await super.loadMore();
  }

  void search(String value) {
    state.keyword.value = value;
    _debounce?.cancel();
    _debounce = Timer(const Duration(milliseconds: 350), refresh);
  }

  void filter({
    String? category,
    int? level,
    String? sort,
    DateTime? month,
    bool clearMonth = false,
  }) {
    _debounce?.cancel();
    if (category != null) state.category.value = category;
    if (level != null) state.level.value = level;
    if (sort != null) state.sort.value = sort;
    if (month != null || clearMonth) state.month.value = month;
    refresh();
  }

  Future<void> open(LearningContent item) async {
    await LearningNavigator.detail(kind, item.id, context: this);
    if (mine == 'bookmarks') await refresh();
  }

  @override
  void onClose() {
    _generation++;
    _debounce?.cancel();
    _sessionWorker?.dispose();
    searchController.dispose();
    scrollController.dispose();
    state.easyRefreshController.dispose();
    super.onClose();
  }
}
