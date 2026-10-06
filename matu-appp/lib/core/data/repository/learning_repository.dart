import 'package:dio/dio.dart';
import 'package:get/get.dart' hide FormData, MultipartFile;
import 'package:retrofit/retrofit.dart';
import '../../model/matu/learning_models.dart';
import '../../model/response/base/base_list_response.dart';
import '../../model/response/base/base_response.dart';
import '../../network/datasource/matu/matu_network_data_source.dart';
import '../../network/matu_response_codec.dart';
import '../../network/provider/dio_provider.dart';
import '../../service/matu_session_service.dart';

/// 码途学习数据仓库，统一鉴权、业务解包与页面模型。
class LearningRepository {
  LearningRepository({
    MatuNetworkDataSource? dataSource,
    MatuSessionService? session,
  }) : _dataSource = dataSource ?? MatuNetworkDataSource(DioProvider().dio),
       _session = session ?? Get.find<MatuSessionService>();
  final MatuNetworkDataSource _dataSource;
  final MatuSessionService _session;

  /// 公开读取最多游客重试一次，私有读取和写入不重放。
  Future<dynamic> _request(
    String path, {
    String method = 'GET',
    Map<String, dynamic> data = const {},
    bool guest = false,
    bool publicAuth = false,
    bool retried = false,
    MultipartFile? upload,
  }) async {
    if (_session.expiresAt != null &&
        !_session.expiresAt!.isAfter(DateTime.now())) {
      await _session.clear();
    }
    final revision = _session.revision.value;
    final auth = publicAuth || _session.authorization.isEmpty
        ? null
        : _session.authorization;
    final clean = Map<String, dynamic>.from(data)
      ..removeWhere((key, value) => value == null || value == '');
    try {
      final HttpResponse<String> response = switch (method) {
        'UPLOAD' => await _dataSource.upload(upload!, 'avatar', '1', auth),
        'POST' => await _dataSource.create(path, clean, auth),
        'PUT' => await _dataSource.update(path, clean, auth),
        'DELETE' => await _dataSource.delete(path, auth),
        _ => await _dataSource.read(path, clean, auth),
      };
      final result = MatuResponseCodec.unwrap(
        response.response.statusCode ?? 0,
        response.data,
      );
      if (revision != _session.revision.value) {
        throw const MatuApiException(409, '账号状态已变化，请重试');
      }
      return result;
    } on Object catch (error) {
      MatuApiException failure;
      if (error is DioException) {
        try {
          MatuResponseCodec.unwrap(
            error.response?.statusCode ?? 0,
            error.response?.data,
          );
          failure = const MatuApiException(0, '网络连接失败，请重试');
        } on MatuApiException catch (e) {
          failure = e;
        }
      } else if (error is MatuApiException) {
        failure = error;
      } else {
        rethrow;
      }
      final expired =
          failure.code == 401 ||
          (failure.code == 400 &&
              RegExp('未登录|登录已失效|请重新登录|登录凭证无效').hasMatch(failure.message));
      if (expired && auth != null && revision == _session.revision.value) {
        await _session.clear();
        if (guest && method == 'GET' && !retried) {
          return _request(
            path,
            data: data,
            guest: true,
            publicAuth: true,
            retried: true,
          );
        }
      }
      throw failure;
    }
  }

  Future<BaseResponse<BaseListResponse<LearningContent>>> list(
    LearningKind kind,
    Map<String, dynamic> query, {
    String? mine,
  }) async {
    final path = mine == 'questions' ? '/qa/questions/mine' : kind.path;
    final result = await _request(path, data: query, guest: mine == null);
    final page = Map<String, dynamic>.from(result as Map);
    final rows = page['records'] as List;
    return BaseResponse(
      data: BaseListResponse(
        list: rows
            .map(
              (e) => LearningContent.fromJson(
                Map<String, dynamic>.from(e as Map),
                kind,
              ),
            )
            .toList(),
        pagination: PageMeta(
          total: countValue(page['total']),
          size: countValue(query['pageSize']),
          page: countValue(query['pageNum']),
        ),
      ),
    );
  }

  Future<BaseResponse<LearningContent>> detail(
    LearningKind kind,
    String id,
  ) async => BaseResponse(
    data: LearningContent.fromJson(
      Map<String, dynamic>.from(
        await _request('${kind.path}/${Uri.encodeComponent(id)}', guest: true)
            as Map,
      ),
      kind,
    ),
  );
  Future<List<LearningCategory>> categories(LearningKind kind) async {
    if (kind == LearningKind.check || kind == LearningKind.course) return [];
    final path = switch (kind) {
      LearningKind.qa => '/qa/categories',
      LearningKind.interview => '/interview/categories',
      _ => '/posts/categories',
    };
    return (await _request(path, guest: true) as List)
        .map(
          (e) => LearningCategory.fromJson(Map<String, dynamic>.from(e as Map)),
        )
        .toList();
  }

  Future<List<LearningDiscussion>> discussions(
    LearningKind kind,
    String id,
    int page,
  ) async {
    final data = await _request(
      '${kind.path}/${Uri.encodeComponent(id)}/${kind == LearningKind.qa ? 'answers' : 'comments'}',
      data: {'pageNum': page, 'pageSize': 10},
      guest: true,
    );
    final rows = data is List ? data : (data as Map)['records'] as List;
    return rows
        .map(
          (e) =>
              LearningDiscussion.fromJson(Map<String, dynamic>.from(e as Map)),
        )
        .toList();
  }

  Future<void> interact(
    LearningKind kind,
    String id,
    String action,
    bool active,
  ) async {
    if (action == 'follow') {
      await _request(
        '${kind.path}/$id/${active ? 'unfollow' : 'follow'}',
        method: 'POST',
      );
    } else {
      await _request(
        '${kind.path}/$id/$action',
        method: active ? 'DELETE' : 'POST',
      );
    }
  }

  Future<void> comment(LearningKind kind, String id, String content) async =>
      _request(
        '${kind.path}/$id/comments',
        method: 'POST',
        data: {'content': content},
      );
  Future<Map<String, dynamic>> login(String account, String password) async =>
      Map<String, dynamic>.from(
        await _request(
              '/auth/mini/login',
              method: 'POST',
              publicAuth: true,
              data: {'account': account, 'password': password},
            )
            as Map,
      );
  Future<void> logout() async => _request('/auth/mini/logout', method: 'POST');
  Future<MatuProfile> profile() async => MatuProfile.fromJson(
    Map<String, dynamic>.from(await _request('/auth/me') as Map),
  );
  Future<MatuProfile> saveProfile(String nickname, String signature) async =>
      MatuProfile.fromJson(
        Map<String, dynamic>.from(
          await _request(
                '/auth/me',
                method: 'PUT',
                data: {'nickname': nickname, 'signature': signature},
              )
              as Map,
        ),
      );
  Future<Map<String, dynamic>> statistics(String userId) async =>
      Map<String, dynamic>.from(
        await _request('/checks/statistics', data: {'userId': userId}) as Map,
      );
  Future<int> questionProgress(String id) async => countValue(
    (await _request('/interview/questions/$id/progress') as Map?)?['status'],
  );
  Future<void> markQuestion(String id, int status) async => _request(
    '/interview/questions/$id/progress',
    method: 'PUT',
    data: {'status': status},
  );
  Future<Map<String, dynamic>> play(String id) async =>
      Map<String, dynamic>.from(
        await _request('/courses/videos/$id/play', guest: true) as Map,
      );
  Future<Map<String, int>> progress(String courseId) async {
    final rows = await _request('/courses/$courseId/progress/mine') as List;
    return {
      for (final row in rows.cast<Map<String, dynamic>>())
        stringValue(row['videoId']): countValue(row['watchedDuration']),
    };
  }

  Future<void> saveProgress(String courseId, Map<String, dynamic> data) async =>
      _request('/courses/$courseId/progress', method: 'POST', data: data);

  /// 上传与资料更新间保持同一账号，上传使用共享 Dio。
  Future<MatuProfile> uploadAvatar(List<int> bytes, String filename) async {
    final revision = _session.revision.value;
    if (!_session.signedIn) throw const MatuApiException(401, '请先登录');
    final file =
        await _request(
              '/files/upload',
              method: 'UPLOAD',
              upload: MultipartFile.fromBytes(bytes, filename: filename),
            )
            as Map;
    if (revision != _session.revision.value) {
      throw const MatuApiException(409, '账号状态已变化');
    }
    final url = stringValue(file['fileUrl']);
    if (url.isEmpty) throw const MatuApiException(0, '上传响应缺少图片地址');
    return MatuProfile.fromJson(
      Map<String, dynamic>.from(
        await _request(
              '/auth/me/avatar',
              method: 'PUT',
              data: {'avatarUrl': url},
            )
            as Map,
      ),
    );
  }
}
