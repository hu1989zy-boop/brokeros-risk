import 'package:dio/dio.dart';

import 'api_contract.dart';

abstract interface class AuthTokenDelegate {
  String? get accessToken;

  Future<String?> refresh();
}

class ApiClient {
  ApiClient({
    required String baseUrl,
    required AuthTokenDelegate auth,
    Dio? dio,
  }) : _dio = dio ?? Dio(BaseOptions(baseUrl: baseUrl)) {
    _dio.interceptors.add(_BearerInterceptor(_dio, auth));
  }

  final Dio _dio;

  Future<ApiResult<T>> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    required T Function(Object? json) decode,
  }) async {
    return _request(
      () => _dio.get<Object?>(path, queryParameters: queryParameters),
      decode,
    );
  }

  Future<ApiResult<T>> post<T>(
    String path, {
    Object? body,
    required T Function(Object? json) decode,
  }) async {
    return _request(() => _dio.post<Object?>(path, data: body), decode);
  }

  Future<ApiResult<T>> _request<T>(
    Future<Response<Object?>> Function() send,
    T Function(Object? json) decode,
  ) async {
    try {
      final response = await send();
      final body = _asMap(response.data);
      final envelope = ApiEnvelope<T>.fromJson(body, (json) {
        if (json == null) {
          return null;
        }
        return decode(json);
      });
      if (envelope.code == ResultCode.success && envelope.data != null) {
        return ApiSuccess<T>(envelope.data as T);
      }
      return ApiError<T>(ApiFailure(
        code: envelope.code,
        message: envelope.message,
        httpStatus: response.statusCode,
      ));
    } on DioException catch (error) {
      final response = error.response;
      final data = response?.data;
      if (data is Map) {
        final envelope = ApiEnvelope<Object?>.fromJson(
          data.cast<String, dynamic>(),
          (json) => json,
        );
        return ApiError<T>(ApiFailure(
          code: envelope.code,
          message: envelope.message,
          httpStatus: response?.statusCode,
        ));
      }
      return ApiError<T>(ApiFailure(
        code: response?.statusCode == 403
            ? ResultCode.authorizationDenied
            : ResultCode.unknown,
        message: response?.statusCode == 403
            ? 'You are not authorized to perform this operation.'
            : 'The BrokerOS Risk API is unavailable.',
        httpStatus: response?.statusCode,
      ));
    } on FormatException catch (error) {
      return ApiError<T>(ApiFailure(
        code: ResultCode.unknown,
        message: 'The API returned an invalid response: ${error.message}',
        httpStatus: null,
      ));
    }
  }

  Map<String, dynamic> _asMap(Object? value) {
    if (value is! Map) {
      throw const FormatException('expected an object envelope');
    }
    return value.cast<String, dynamic>();
  }
}

class _BearerInterceptor extends Interceptor {
  _BearerInterceptor(this._dio, this._auth);

  final Dio _dio;
  final AuthTokenDelegate _auth;

  @override
  void onRequest(RequestOptions options, RequestInterceptorHandler handler) {
    final token = _auth.accessToken;
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException error,
    ErrorInterceptorHandler handler,
  ) async {
    final options = error.requestOptions;
    if (error.response?.statusCode == 401 && options.extra['q016Retried'] != true) {
      final refreshed = await _auth.refresh();
      if (refreshed != null) {
        options.extra['q016Retried'] = true;
        options.headers['Authorization'] = 'Bearer $refreshed';
        try {
          handler.resolve(await _dio.fetch<Object?>(options));
          return;
        } on DioException {
          // The original typed API failure is returned below without token data.
        }
      }
    }
    handler.next(error);
  }
}
