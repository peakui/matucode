import 'package:dio/dio.dart';
import 'package:retrofit/retrofit.dart';
part 'matu_network_data_source.g.dart';

/// 码途 REST 传输层，保留原始文本以无损解析长 ID。
@RestApi()
abstract class MatuNetworkDataSource {
  /// 头像 multipart 上传复用与普通请求相同的鉴权与解包链路。
  @POST('/files/upload')
  @MultiPart()
  @DioResponseType(ResponseType.plain)
  @Extra({'matu': true})
  Future<HttpResponse<String>> upload(
    @Part(name: 'file') MultipartFile file,
    @Part(name: 'bizType') String bizType,
    @Part(name: 'isPublic') String isPublic,
    @Header('Authorization') String? authorization,
  );
  factory MatuNetworkDataSource(Dio dio, {String? baseUrl}) =
      _MatuNetworkDataSource;

  @GET('{path}')
  @DioResponseType(ResponseType.plain)
  @Extra({'matu': true})
  Future<HttpResponse<String>> read(
    @Path('path') String path,
    @Queries() Map<String, dynamic> query,
    @Header('Authorization') String? authorization,
  );

  @POST('{path}')
  @DioResponseType(ResponseType.plain)
  @Extra({'matu': true})
  Future<HttpResponse<String>> create(
    @Path('path') String path,
    @Body() Map<String, dynamic> body,
    @Header('Authorization') String? authorization,
  );

  @PUT('{path}')
  @DioResponseType(ResponseType.plain)
  @Extra({'matu': true})
  Future<HttpResponse<String>> update(
    @Path('path') String path,
    @Body() Map<String, dynamic> body,
    @Header('Authorization') String? authorization,
  );

  @DELETE('{path}')
  @DioResponseType(ResponseType.plain)
  @Extra({'matu': true})
  Future<HttpResponse<String>> delete(
    @Path('path') String path,
    @Header('Authorization') String? authorization,
  );
}
