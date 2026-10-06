import 'dart:async';
import 'package:get/get.dart';
import 'package:video_player/video_player.dart';
import '../../../core/data/repository/learning_repository.dart';
import '../../../core/model/matu/learning_models.dart';
import '../../../core/env/env.dart';
import '../../../core/network/media_url_resolver.dart';
import '../../../core/service/matu_session_service.dart';
import '../localization/learning_strings.dart';
import 'learning_detail_logic.dart';

/// 课程授权播放和节流进度，播放器随页面生命周期释放。
class LearningCourseLogic extends LearningDetailLogic {
  LearningCourseLogic(
    String id, {
    LearningRepository? repository,
    MatuSessionService? session,
    VideoPlayerController Function(Uri)? playerFactory,
  }) : _courseRepository = repository ?? LearningRepository(session: session),
       _playerFactory = playerFactory ?? VideoPlayerController.networkUrl,
       super(LearningKind.course, id, repository: repository, session: session);
  final LearningRepository _courseRepository;

  /// 播放器构造入口，测试可注入可控播放器验证生命周期。
  final VideoPlayerController Function(Uri) _playerFactory;
  final playerRevision = 0.obs;
  final playBusy = false.obs;
  final playError = ''.obs;
  final progressError = ''.obs;
  final selected = Rxn<CourseLesson>();
  VideoPlayerController? player;
  Timer? _timer;
  int _selection = 0;
  final Map<String, int> _watched = {};
  Future<void> _saveQueue = Future<void>.value();
  int _playSession = -1;
  bool _completionSaved = false;

  @override
  Future<void> loadData() async {
    _selection++;
    _timer?.cancel();
    final old = player;
    player = null;
    playerRevision.value++;
    await old?.dispose();
    selected.value = null;
    _watched.clear();
    await super.loadData();
    if (!session.signedIn || isClosed) return;
    final revision = session.revision.value;
    try {
      final progress = await _courseRepository.progress(id);
      if (revision == session.revision.value && !isClosed) {
        _watched.addAll(progress);
      }
    } on Object {
      progressError.value = LearningStrings.progressFailed;
    }
  }

  Future<void> select(CourseLesson lesson) async {
    await saveProgress();
    final selection = ++_selection;
    _completionSaved = false;
    _timer?.cancel();
    final old = player;
    player = null;
    playerRevision.value++;
    await old?.dispose();
    selected.value = lesson;
    playBusy.value = true;
    playError.value = '';
    final revision = session.revision.value;
    VideoPlayerController? candidate;
    try {
      final info = await _courseRepository.play(lesson.id);
      if (selection != _selection ||
          revision != session.revision.value ||
          isClosed) {
        return;
      }
      if (info['playable'] != true || stringValue(info['videoUrl']).isEmpty) {
        playError.value = stringValue(info['message']).isEmpty
            ? LearningStrings.videoUnavailable
            : stringValue(info['message']);
        return;
      }
      final uri = MediaUrlResolver.resolve(
        stringValue(info['videoUrl']),
        baseUrl: Env.baseUrl,
      );
      if (uri == null) {
        throw const FormatException(LearningStrings.videoAddressError);
      }
      final controller = _playerFactory(uri);
      candidate = controller;
      await controller.initialize();
      if (selection != _selection ||
          revision != session.revision.value ||
          isClosed) {
        return;
      }
      player = controller;
      _playSession = revision;
      final offset = LessonProgress.resume(
        _watched[lesson.id] ?? 0,
        controller.value.duration.inSeconds,
      );
      await controller.seekTo(Duration(seconds: offset));
      controller.addListener(_onPlayerChanged);
      playerRevision.value++;
      await controller.play();
      _timer = Timer.periodic(const Duration(seconds: 15), (_) {
        saveProgress();
      });
    } on Object catch (e) {
      if (selection == _selection && !isClosed) {
        playError.value = playbackError(e);
        if (identical(player, candidate)) {
          player = null;
          playerRevision.value++;
        }
      }
    } finally {
      if (candidate != null && !identical(player, candidate)) {
        candidate.removeListener(_onPlayerChanged);
        await candidate.dispose();
      }
      if (selection == _selection && !isClosed) playBusy.value = false;
    }
  }

  void _onPlayerChanged() {
    if (player == null || isClosed) return;
    if (player!.value.hasError) {
      playError.value = playbackError(player!.value.errorDescription ?? '');
      _timer?.cancel();
    }
    if (player!.value.isCompleted && !_completionSaved) {
      _completionSaved = true;
      _timer?.cancel();
      saveProgress();
    }
  }

  /// 将播放器平台异常转为用户可处理的反馈，不暴露底层堆栈与签名地址。
  static String playbackError(Object error) {
    final text = error.toString().toLowerCase();
    if (text.contains('certificate') ||
        text.contains('ssl') ||
        text.contains('handshake')) {
      return LearningStrings.videoCertificateError;
    }
    if (text.contains('403') || text.contains('401')) {
      return LearningStrings.videoPermissionError;
    }
    if (text.contains('404') || error is FormatException) {
      return LearningStrings.videoAddressError;
    }
    if (text.contains('decoder') ||
        text.contains('unsupported') ||
        text.contains('format')) {
      return LearningStrings.videoFormatError;
    }
    return LearningStrings.videoNetworkError;
  }

  Future<void> togglePlayback() async {
    final controller = player;
    if (controller == null) return;
    if (controller.value.isPlaying) {
      await controller.pause();
      await saveProgress();
    } else {
      await controller.play();
    }
    playerRevision.value++;
  }

  Future<void> saveProgress() {
    final controller = player;
    if (controller == null ||
        !session.signedIn ||
        selected.value == null ||
        _playSession != session.revision.value) {
      return Future<void>.value();
    }
    final data = LessonProgress.snapshot(
      selected.value!.id,
      controller.value.position,
      controller.value.duration,
      completed: controller.value.isCompleted,
    );
    if (data == null) return Future<void>.value();
    final revision = _playSession;
    final courseId = id;
    _saveQueue = _saveQueue.catchError((Object _) {}).then((_) async {
      if (revision != session.revision.value) return;
      try {
        await _courseRepository.saveProgress(courseId, data);
        if (!isClosed) progressError.value = '';
      } on Object {
        if (!isClosed) progressError.value = LearningStrings.progressFailed;
      }
    });
    return _saveQueue;
  }

  void pause() {
    player?.pause();
    saveProgress();
  }

  @override
  void onInactive() => pause();
  @override
  void onPaused() => pause();
  @override
  void onHidden() => pause();
  @override
  void onClose() {
    saveProgress();
    _selection++;
    _timer?.cancel();
    final controller = player;
    player = null;
    controller?.removeListener(_onPlayerChanged);
    controller?.dispose();
    super.onClose();
  }
}
