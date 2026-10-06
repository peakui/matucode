import 'package:flutter/material.dart';
import 'package:get/get.dart';
import 'package:share_plus/share_plus.dart';
import '../../../core/base/base_network/base_network_logic.dart';
import '../../../core/data/repository/learning_repository.dart';
import '../../../core/data/repository/question_bookmark_repository.dart';
import '../../../core/model/matu/learning_models.dart';
import '../../../core/model/response/base/base_response.dart';
import '../../../core/service/matu_session_service.dart';
import '../../../routes/learning/learning_navigator.dart';
import '../states/learning_states.dart';
import 'learning_list_logic.dart';

/// 内容详情、交流、答案权限与面试学习操作。
class LearningDetailLogic extends BaseNetworkLogic<LearningContent> {
  LearningDetailLogic(
    this.kind,
    this.id, {
    this.listContext,
    LearningRepository? repository,
    MatuSessionService? session,
  }) : _learningRepository = repository ?? LearningRepository(session: session),
       session = session ?? Get.find<MatuSessionService>();
  final LearningKind kind;
  String id;
  final LearningListLogic? listContext;
  final LearningRepository _learningRepository;
  final MatuSessionService session;
  final _questionBookmarkRepository = QuestionBookmarkRepository();
  final LearningDetailState state = LearningDetailState();
  final commentController = TextEditingController();
  final scrollController = ScrollController();
  int _generation = 0;
  int _discussionPage = 0;
  Worker? _worker;

  /// 分享公开标题；配置真实网站域名后追加对应内容链接。
  Future<void> share(Rect origin) async {
    final item = state.content.value;
    if (item == null) return;
    const website = String.fromEnvironment('WEB_BASE_URL');
    final base = Uri.tryParse(website);
    final path = switch (kind) {
      LearningKind.article => '/article/$id',
      LearningKind.check => '/check-in/$id',
      LearningKind.qa => '/qa/question/$id',
      LearningKind.course => '/detail/course/$id',
      LearningKind.interview => '/detail/interview-question/$id',
    };
    final link =
        base != null &&
            ['http', 'https'].contains(base.scheme) &&
            base.host.isNotEmpty
        ? base.resolve(path).toString()
        : '';
    try {
      await SharePlus.instance.share(
        ShareParams(
          text: [item.title, link].where((e) => e.isNotEmpty).join('\n'),
          sharePositionOrigin: origin,
        ),
      );
    } on Object catch (e) {
      if (!isClosed) state.actionError.value = e.toString();
    }
  }

  @override
  LearningDetailState get networkState => state;
  @override
  Future<BaseResponse<LearningContent>> Function()? get apiRequest =>
      () => _learningRepository.detail(kind, id);
  @override
  void onInit() {
    super.onInit();
    _worker = ever(session.revision, (_) {
      loadData();
    });
  }

  @override
  Future<void> loadData() async {
    final generation = ++_generation;
    state.content.value = null;
    state.expanded.value = false;
    state.error.value = '';
    state.discussions.clear();
    state.discussionDone.value = false;
    state.progress.value = 0;
    state.bookmarked.value = false;
    setStatusLoad();
    try {
      final data = (await apiRequest!()).data!;
      if (generation != _generation || isClosed) return;
      state.content.value = data;
      setStatusSuccess();
      if (kind == LearningKind.interview) {
        if (session.signedIn) {
          final bookmarks = await _questionBookmarkRepository.read(
            session.user.value!.id,
          );
          if (generation != _generation || isClosed) return;
          state.bookmarked.value = bookmarks.any((e) => e.id == id);
          try {
            final progress = await _learningRepository.questionProgress(id);
            if (generation == _generation && !isClosed) {
              state.progress.value = progress;
            }
          } on Object catch (e) {
            if (generation == _generation &&
                !e.toString().contains('学习进度不存在')) {
              state.actionError.value = e.toString();
            }
          }
        }
      } else if (kind != LearningKind.course) {
        await loadDiscussions(reset: true);
      }
    } on Object catch (e) {
      if (generation != _generation || isClosed) return;
      state.error.value = e.toString();
      setStatusError();
    }
  }

  Future<void> loadDiscussions({bool reset = false}) async {
    if (!reset && (state.discussionBusy.value || state.discussionDone.value)) {
      return;
    }
    final generation = _generation;
    final page = reset ? 1 : _discussionPage + 1;
    state.discussionBusy.value = true;
    state.discussionError.value = '';
    try {
      final items = await _learningRepository.discussions(kind, id, page);
      if (generation != _generation || isClosed) return;
      if (reset) state.discussions.clear();
      final known = state.discussions.map((e) => e.id).toSet();
      state.discussions.addAll(items.where((e) => known.add(e.id)));
      _discussionPage = page;
      state.discussionDone.value = kind != LearningKind.qa || items.length < 10;
    } on Object catch (e) {
      if (generation == _generation && !isClosed) {
        state.discussionError.value = e.toString();
      }
    } finally {
      if (generation == _generation && !isClosed) {
        state.discussionBusy.value = false;
      }
    }
  }

  Future<void> act(String action) async {
    if (!await LearningNavigator.requireLogin() ||
        state.acting.value ||
        state.content.value == null) {
      return;
    }
    final content = state.content.value!;
    state.acting.value = true;
    state.actionError.value = '';
    final generation = _generation;
    try {
      final active = action == 'like'
          ? content.liked
          : action == 'collect'
          ? content.collected
          : content.followed;
      await _learningRepository.interact(kind, id, action, active);
      final next = (await _learningRepository.detail(kind, id)).data;
      if (generation == _generation && !isClosed) state.content.value = next;
    } on Object catch (e) {
      if (!isClosed) state.actionError.value = e.toString();
    } finally {
      if (!isClosed) state.acting.value = false;
    }
  }

  Future<void> sendComment() async {
    if (commentController.text.trim().isEmpty ||
        state.acting.value ||
        !await LearningNavigator.requireLogin()) {
      return;
    }
    state.acting.value = true;
    state.actionError.value = '';
    try {
      await _learningRepository.comment(
        kind,
        id,
        commentController.text.trim(),
      );
      if (isClosed) return;
      commentController.clear();
      await loadDiscussions(reset: true);
    } on Object catch (e) {
      if (!isClosed) state.actionError.value = e.toString();
    } finally {
      if (!isClosed) state.acting.value = false;
    }
  }

  Future<void> mark(int status) async {
    if (!await LearningNavigator.requireLogin() || state.acting.value) return;
    final generation = _generation;
    state.acting.value = true;
    state.actionError.value = '';
    try {
      await _learningRepository.markQuestion(id, status);
      if (generation == _generation && !isClosed) state.progress.value = status;
    } on Object catch (e) {
      if (!isClosed) state.actionError.value = e.toString();
    } finally {
      if (!isClosed) state.acting.value = false;
    }
  }

  Future<void> bookmark() async {
    if (!await LearningNavigator.requireLogin() ||
        state.acting.value ||
        state.content.value == null) {
      return;
    }
    final generation = _generation;
    state.acting.value = true;
    try {
      final saved = await _questionBookmarkRepository.toggle(
        session.user.value!.id,
        state.content.value!,
      );
      if (generation == _generation && !isClosed) {
        state.bookmarked.value = saved;
      }
    } on Object catch (e) {
      state.actionError.value = e.toString();
    } finally {
      state.acting.value = false;
    }
  }

  int get position =>
      listContext?.state.dataList.indexWhere((e) => e.id == id) ?? -1;
  bool get hasPrevious => position > 0;
  bool get hasNext =>
      position >= 0 &&
      (position < listContext!.state.dataList.length - 1 ||
          !listContext!.state.noMoreData);
  Future<void> move(int direction) async {
    if (state.acting.value || position < 0) return;
    final next = position + direction;
    state.acting.value = true;
    try {
      if (next >= listContext!.state.dataList.length) {
        await listContext!.loadMore();
      }
      if (next >= 0 && next < listContext!.state.dataList.length) {
        id = listContext!.state.dataList[next].id;
        await loadData();
        if (scrollController.hasClients) scrollController.jumpTo(0);
      } else if (listContext!.state.error.value.isNotEmpty) {
        state.actionError.value = listContext!.state.error.value;
      }
    } finally {
      state.acting.value = false;
    }
  }

  @override
  void onClose() {
    _generation++;
    _worker?.dispose();
    commentController.dispose();
    scrollController.dispose();
    super.onClose();
  }
}
