enum ResultCode {
  success('SUCCESS'),
  validationError('VALIDATION_ERROR'),
  malformedRequest('MALFORMED_REQUEST'),
  authenticationRequired('AUTHENTICATION_REQUIRED'),
  authenticationInvalid('AUTHENTICATION_INVALID'),
  actorAccessDenied('ACTOR_ACCESS_DENIED'),
  authorizationDenied('AUTHORIZATION_DENIED'),
  riskCaseNotFound('RISK_CASE_NOT_FOUND'),
  riskCaseVersionConflict('RISK_CASE_VERSION_CONFLICT'),
  riskCaseInvalidTransition('RISK_CASE_INVALID_TRANSITION'),
  riskCaseInvariantViolation('RISK_CASE_INVARIANT_VIOLATION'),
  internalError('INTERNAL_ERROR'),
  unknown('UNKNOWN');

  const ResultCode(this.wireValue);

  final String wireValue;

  static ResultCode fromWire(String value) => values.firstWhere(
        (candidate) => candidate.wireValue == value,
        orElse: () => ResultCode.unknown,
      );
}

class ApiEnvelope<T> {
  const ApiEnvelope({
    required this.code,
    required this.message,
    required this.data,
    required this.timestamp,
  });

  factory ApiEnvelope.fromJson(
    Map<String, dynamic> json,
    T? Function(Object? json) decodeData,
  ) {
    return ApiEnvelope<T>(
      code: ResultCode.fromWire(json['code'] as String),
      message: json['message'] as String,
      data: decodeData(json['data']),
      timestamp: DateTime.parse(json['timestamp'] as String).toUtc(),
    );
  }

  final ResultCode code;
  final String message;
  final T? data;
  final DateTime timestamp;
}

class ApiFailure implements Exception {
  const ApiFailure({
    required this.code,
    required this.message,
    required this.httpStatus,
  });

  final ResultCode code;
  final String message;
  final int? httpStatus;

  bool get isVersionConflict => code == ResultCode.riskCaseVersionConflict;

  @override
  String toString() => message;
}

sealed class ApiResult<T> {
  const ApiResult();
}

final class ApiSuccess<T> extends ApiResult<T> {
  const ApiSuccess(this.value);

  final T value;
}

final class ApiError<T> extends ApiResult<T> {
  const ApiError(this.failure);

  final ApiFailure failure;
}
