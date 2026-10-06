import 'dart:async';
import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:retrofit/retrofit.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:matu_appp/core/data/repository/learning_repository.dart';
import 'package:matu_appp/core/data/repository/matu_session_repository.dart';
import 'package:matu_appp/core/data/repository/question_bookmark_repository.dart';
import 'package:matu_appp/core/model/matu/learning_models.dart';
import 'package:matu_appp/core/model/response/base/base_list_response.dart';
import 'package:matu_appp/core/model/response/base/base_response.dart';
import 'package:matu_appp/core/network/datasource/matu/matu_network_data_source.dart';
import 'package:matu_appp/core/network/matu_response_codec.dart';
import 'package:matu_appp/core/service/matu_session_service.dart';
import 'package:matu_appp/feature/learning/logics/learning_list_logic.dart';

/// 隔离平台插件的会话测试存储。
class MemorySessionRepository extends MatuSessionRepository {
  String? value;
  @override
  Future<String?> read() async => value;
  @override
  Future<void> save(String raw) async {
    value = raw;
  }

  @override
  Future<void> clear() async {
    value = null;
  }
}

/// 可控传输用于模拟过期和迟到响应。
class RecordingSource implements MatuNetworkDataSource {
  @override
  Future<HttpResponse<String>> upload(
    MultipartFile file,
    String bizType,
    String isPublic,
    String? authorization,
  ) => invoke('UPLOAD', authorization);
  final calls = <String>[];
  Future<HttpResponse<String>> Function(String method, String? auth) handler =
      (_, _) async => response(null);
  static HttpResponse<String> response(Object? data, {int code = 0}) =>
      HttpResponse(
        jsonEncode({'code': code, 'data': data, 'message': 'failure'}),
        Response<String>(requestOptions: RequestOptions(), statusCode: 200),
      );
  Future<HttpResponse<String>> invoke(String method, String? auth) {
    calls.add('$method:${auth ?? 'guest'}');
    return handler(method, auth);
  }

  @override
  Future<HttpResponse<String>> read(
    String path,
    Map<String, dynamic> query,
    String? authorization,
  ) => invoke('GET', authorization);
  @override
  Future<HttpResponse<String>> create(
    String path,
    Map<String, dynamic> body,
    String? authorization,
  ) => invoke('POST', authorization);
  @override
  Future<HttpResponse<String>> update(
    String path,
    Map<String, dynamic> body,
    String? authorization,
  ) => invoke('PUT', authorization);
  @override
  Future<HttpResponse<String>> delete(String path, String? authorization) =>
      invoke('DELETE', authorization);
}

/// 分页响应完成时机可控。
class PagingRepository extends LearningRepository {
  PagingRepository(MatuSessionService session)
    : super(dataSource: RecordingSource(), session: session);
  final pending =
      <Completer<BaseResponse<BaseListResponse<LearningContent>>>>[];
  final pages = <int>[];
  @override
  Future<BaseResponse<BaseListResponse<LearningContent>>> list(
    LearningKind kind,
    Map<String, dynamic> query, {
    String? mine,
  }) {
    pages.add(query['pageNum'] as int);
    final completer =
        Completer<BaseResponse<BaseListResponse<LearningContent>>>();
    pending.add(completer);
    return completer.future;
  }
}

Future<void> signIn(MatuSessionService session, String id) => session.accept({
  'authorization': 'Bearer $id',
  'expiresIn': 3600,
  'user': {'userId': id, 'nickname': id},
});
BaseResponse<BaseListResponse<LearningContent>> page(
  String id, {
  int total = 30,
}) => BaseResponse(
  data: BaseListResponse(
    list: [LearningContent(id: id, title: id)],
    pagination: PageMeta(total: total, size: 10),
  ),
);

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  late MatuSessionService session;
  late RecordingSource source;
  late LearningRepository repository;
  setUp(() {
    session = MatuSessionService(repository: MemorySessionRepository());
    source = RecordingSource();
    repository = LearningRepository(dataSource: source, session: session);
    SharedPreferences.setMockInitialValues({});
  });
  test('公开 GET 过期只降级重试一次，Authorization 原样传递', () async {
    await signIn(session, 'a');
    source.handler = (_, auth) async => auth != null
        ? RecordingSource.response(null, code: 401)
        : RecordingSource.response({'id': '1', 'title': 'guest'});
    expect(
      (await repository.detail(LearningKind.article, '1')).data!.title,
      'guest',
    );
    expect(source.calls, ['GET:Bearer a', 'GET:guest']);
    expect(session.signedIn, false);
  });
  test('私有 GET 与写操作过期不重放', () async {
    source.handler = (_, _) async => RecordingSource.response(null, code: 401);
    await signIn(session, 'a');
    await expectLater(repository.profile(), throwsA(isA<MatuApiException>()));
    expect(source.calls.length, 1);
    await signIn(session, 'a');
    await expectLater(
      repository.comment(LearningKind.article, '1', 'hello'),
      throwsA(isA<MatuApiException>()),
    );
    expect(source.calls.length, 2);
    expect(source.calls.last, 'POST:Bearer a');
  });
  test('头像上传过期清理会话，不调用资料更新', () async {
    await signIn(session, 'a');
    source.handler = (_, _) async => RecordingSource.response(null, code: 401);
    await expectLater(
      repository.uploadAvatar([1, 2, 3], 'avatar.jpg'),
      throwsA(isA<MatuApiException>()),
    );
    expect(source.calls, ['UPLOAD:Bearer a']);
    expect(session.signedIn, false);
  });
  test('上传完成前切换账号，不把旧头像写入新账号', () async {
    await signIn(session, 'a');
    final response = Completer<HttpResponse<String>>();
    source.handler = (_, _) => response.future;
    final upload = repository.uploadAvatar([1, 2, 3], 'avatar.jpg');
    await signIn(session, 'b');
    response.complete(
      RecordingSource.response({'fileUrl': 'https://example.test/avatar.jpg'}),
    );
    await expectLater(upload, throwsA(isA<MatuApiException>()));
    expect(source.calls, ['UPLOAD:Bearer a']);
    expect(session.user.value!.id, 'b');
  });
  test('旧账号的迟到 401 不清理新账号', () async {
    final response = Completer<HttpResponse<String>>();
    source.handler = (_, _) => response.future;
    await signIn(session, 'a');
    final request = repository.profile();
    await signIn(session, 'b');
    response.complete(RecordingSource.response(null, code: 401));
    await expectLater(request, throwsA(isA<MatuApiException>()));
    expect(session.user.value!.id, 'b');
    expect(session.authorization, 'Bearer b');
  });
  test('旧账号成功响应不进入新账号状态', () async {
    final response = Completer<HttpResponse<String>>();
    source.handler = (_, _) => response.future;
    await signIn(session, 'a');
    final request = repository.profile();
    await signIn(session, 'b');
    response.complete(RecordingSource.response({'id': 'a'}));
    await expectLater(
      request,
      throwsA(isA<MatuApiException>().having((e) => e.code, 'code', 409)),
    );
  });
  test('本机收藏按用户隔离且不会保存答案', () async {
    final bookmarks = QuestionBookmarkRepository();
    await bookmarks.toggle(
      'a',
      const LearningContent(
        id: '1',
        title: 'question',
        answer: 'secret',
        answerVisible: true,
      ),
    );
    expect(await bookmarks.read('b'), isEmpty);
    expect((await bookmarks.read('a')).single.answer, '');
    expect(
      await bookmarks.toggle(
        'a',
        const LearningContent(id: '1', title: 'question'),
      ),
      false,
    );
    expect(await bookmarks.read('a'), isEmpty);
  });
  test('新筛选完成后旧请求不能覆盖列表', () async {
    final paging = PagingRepository(session);
    final logic = LearningListLogic(
      LearningKind.article,
      repository: paging,
      session: session,
    );
    final old = logic.refresh();
    final current = logic.refresh();
    paging.pending[1].complete(page('new'));
    await current;
    paging.pending[0].complete(page('old'));
    await old;
    expect(logic.state.dataList.single.id, 'new');
    logic.onClose();
  });
  test('并发触底只请求一页，失败重试同一页', () async {
    final paging = PagingRepository(session);
    final logic = LearningListLogic(
      LearningKind.article,
      repository: paging,
      session: session,
    );
    final first = logic.refresh();
    paging.pending[0].complete(page('1'));
    await first;
    final second = logic.loadMore();
    await logic.loadMore();
    expect(paging.pages, [1, 2]);
    paging.pending[1].completeError(const MatuApiException(500, 'offline'));
    await second;
    final retry = logic.loadMore();
    expect(paging.pages, [1, 2, 2]);
    paging.pending[2].complete(page('2'));
    await retry;
    expect(logic.state.dataList.map((e) => e.id), ['1', '2']);
    logic.onClose();
  });
}
