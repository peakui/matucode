import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:matu_appp/core/data/repository/learning_repository.dart';
import 'package:matu_appp/core/model/matu/learning_models.dart';
import 'package:matu_appp/core/network/datasource/matu/matu_network_data_source.dart';
import 'package:matu_appp/core/network/interceptor/interceptor.dart';
import 'package:matu_appp/core/service/matu_session_service.dart';

/// 显式开启的本地只读联调，默认测试不依赖网络。
void main() {
  const enabled = bool.fromEnvironment('MATU_LIVE_API');
  for (final kind in LearningKind.values) {
    test('真实网关 ${kind.name} 列表与详情契约', () async {
      final dio = Dio(
        BaseOptions(
          baseUrl: const String.fromEnvironment(
            'API_BASE_URL',
            defaultValue: 'http://127.0.0.1:8080',
          ),
          connectTimeout: const Duration(seconds: 8),
          receiveTimeout: const Duration(seconds: 8),
        ),
      )..interceptors.add(HttpInterceptor());
      final repository = LearningRepository(
        dataSource: MatuNetworkDataSource(dio),
        session: MatuSessionService(),
      );
      final result = await repository.list(kind, {
        'pageNum': 1,
        'pageSize': 10,
      });
      expect(result.isSucceeded, true);
      if (result.data!.list!.isNotEmpty) {
        final id = result.data!.list!.first.id;
        expect(id, matches(RegExp(r'^\d+$')));
        final detail = await repository.detail(kind, id);
        expect(detail.data!.id, id);
        if (kind == LearningKind.article ||
            kind == LearningKind.check ||
            kind == LearningKind.qa) {
          await repository.discussions(kind, id, 1);
        }
      }
      await repository.categories(kind);
      dio.close();
    }, skip: !enabled);
  }
}
