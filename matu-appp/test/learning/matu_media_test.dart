import 'dart:async';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:video_player/video_player.dart';
import 'package:matu_appp/core/data/repository/learning_repository.dart';
import 'package:matu_appp/core/model/matu/learning_models.dart';
import 'package:matu_appp/core/network/media_url_resolver.dart';
import 'package:matu_appp/core/service/matu_session_service.dart';
import 'package:matu_appp/feature/learning/logics/learning_course_logic.dart';
import 'package:matu_appp/feature/learning/localization/learning_strings.dart';
import 'matu_session_paging_test.dart'
    show RecordingSource, MemorySessionRepository;

/// 用受控授权响应测试播放器，不绕过真实页面的授权调用。
class PlaybackRepository extends LearningRepository {
  PlaybackRepository(MatuSessionService session)
    : super(dataSource: RecordingSource(), session: session);
  bool allowed = true;
  int requests = 0;
  @override
  Future<Map<String, dynamic>> play(String id) async {
    requests++;
    return {
      'playable': allowed,
      'videoUrl': 'https://www.example.com/image/video.mp4',
    };
  }
}

/// 可暂停初始化的播放器，覆盖快速切换和失败释放。
class ControlledPlayer extends VideoPlayerController {
  ControlledPlayer(Uri uri, {this.failure, this.pending})
    : super.networkUrl(uri);
  final Exception? failure;
  final Completer<void>? pending;
  bool released = false;
  @override
  Future<void> initialize() async {
    if (pending != null) await pending!.future;
    if (failure != null) throw failure!;
    value = const VideoPlayerValue(
      duration: Duration(seconds: 100),
      size: Size(640, 360),
      isInitialized: true,
    );
  }

  @override
  Future<void> play() async {
    value = value.copyWith(isPlaying: true);
  }

  @override
  Future<void> pause() async {
    value = value.copyWith(isPlaying: false);
  }

  @override
  Future<void> seekTo(Duration position) async {
    value = value.copyWith(position: position);
  }

  @override
  Future<void> dispose() async {
    released = true;
    await super.dispose();
  }
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  test('只迁移已核验的公共媒体旧域名，保留 HTTPS 与原路径', () {
    final result = MediaUrlResolver.resolve(
      'https://www.example.com/image/a.mp4',
      baseUrl: 'http://localhost:8080',
    );
    expect(
      result.toString(),
      'https://your-bucket.oss-cn-hangzhou.aliyuncs.com/image/a.mp4',
    );
    expect(
      MediaUrlResolver.resolve(
        '/image/a.mp4',
        baseUrl: 'https://api.example.test',
      ).toString(),
      'https://api.example.test/image/a.mp4',
    );
    expect(
      MediaUrlResolver.resolve(
        'javascript:alert(1)',
        baseUrl: 'https://api.example.test',
      ),
      null,
    );
  });
  test('签名、第三方、带端口和非公共路径不得被地址迁移改变', () {
    for (final url in [
      'https://www.example.com/image/a.mp4?signature=abc',
      'https://other.example/image/a.mp4',
      'https://www.example.com:8443/image/a.mp4',
      'https://www.example.com/private/a.mp4',
    ]) {
      expect(
        MediaUrlResolver.resolve(
          url,
          baseUrl: 'http://localhost:8080',
        ).toString(),
        url,
      );
    }
  });
  test('授权拒绝时不创建播放器', () async {
    final session = MatuSessionService(repository: MemorySessionRepository());
    final repository = PlaybackRepository(session)..allowed = false;
    var created = 0;
    final logic = LearningCourseLogic(
      '1',
      repository: repository,
      session: session,
      playerFactory: (uri) {
        created++;
        return ControlledPlayer(uri);
      },
    );
    await logic.select(const CourseLesson('1', 'lesson', 100));
    expect(repository.requests, 1);
    expect(created, 0);
    expect(logic.playError.value, LearningStrings.videoUnavailable);
    logic.onClose();
  });
  test('初始化失败释放播放器并展示可读反馈', () async {
    final session = MatuSessionService(repository: MemorySessionRepository());
    final repository = PlaybackRepository(session);
    late ControlledPlayer player;
    final logic = LearningCourseLogic(
      '1',
      repository: repository,
      session: session,
      playerFactory: (uri) => player = ControlledPlayer(
        uri,
        failure: PlatformException(
          code: 'VideoError',
          message: 'SSL certificate mismatch',
        ),
      ),
    );
    await logic.select(const CourseLesson('1', 'lesson', 100));
    expect(player.released, true);
    expect(logic.player, null);
    expect(logic.playError.value, LearningStrings.videoCertificateError);
    expect(logic.playBusy.value, false);
    logic.onClose();
  });
  test('快速切换课时丢弃迟到播放器，仅播放最终选中的课时', () async {
    final session = MatuSessionService(repository: MemorySessionRepository());
    final repository = PlaybackRepository(session);
    final pending = Completer<void>();
    final created = <ControlledPlayer>[];
    final logic = LearningCourseLogic(
      '1',
      repository: repository,
      session: session,
      playerFactory: (uri) {
        final player = ControlledPlayer(
          uri,
          pending: created.isEmpty ? pending : null,
        );
        created.add(player);
        return player;
      },
    );
    final first = logic.select(const CourseLesson('1', 'first', 100));
    await Future<void>.delayed(Duration.zero);
    await logic.select(const CourseLesson('2', 'second', 100));
    pending.complete();
    await first;
    expect(created.first.released, true);
    expect(logic.player, same(created.last));
    expect(logic.player!.value.isPlaying, true);
    expect(logic.selected.value!.id, '2');
    logic.onClose();
  });
}
