import '../../../core/api/api_client.dart';
import '../../../core/api/api_contract.dart';
import 'risk_case_models.dart';

abstract interface class RiskCaseRepository {
  Future<ApiResult<RiskCaseSummaryPage>> listCases(
    RiskCaseListQuery query, {
    required int page,
    required int size,
  });

  Future<ApiResult<RiskCaseView>> getCase(String caseNumber);

  Future<ApiResult<RiskCaseNote>> addNote({
    required String caseNumber,
    required String content,
    required int expectedVersion,
  });
}

class DioRiskCaseRepository implements RiskCaseRepository {
  const DioRiskCaseRepository(this._apiClient);

  final ApiClient _apiClient;

  @override
  Future<ApiResult<RiskCaseSummaryPage>> listCases(
    RiskCaseListQuery query, {
    required int page,
    required int size,
  }) {
    return _apiClient.get<RiskCaseSummaryPage>(
      '/api/risk-cases',
      queryParameters: query.toQueryParameters(page: page, size: size),
      decode: (json) => RiskCaseSummaryPage.fromJson(_map(json)),
    );
  }

  @override
  Future<ApiResult<RiskCaseView>> getCase(String caseNumber) async {
    final detailResult = await _apiClient.get<RiskCaseDetail>(
      '/api/risk-cases/$caseNumber',
      decode: (json) => RiskCaseDetail.fromJson(_map(json)),
    );
    if (detailResult case ApiError<RiskCaseDetail>(:final failure)) {
      return ApiError<RiskCaseView>(failure);
    }
    final detail = (detailResult as ApiSuccess<RiskCaseDetail>).value;

    final historyResult = await _apiClient.get<RiskCaseHistoryPage>(
      '/api/risk-cases/$caseNumber/history',
      queryParameters: const {'limit': 100},
      decode: (json) => RiskCaseHistoryPage.fromJson(_map(json)),
    );
    if (historyResult case ApiError<RiskCaseHistoryPage>(:final failure)) {
      return ApiError<RiskCaseView>(failure);
    }
    final history = (historyResult as ApiSuccess<RiskCaseHistoryPage>).value;
    return ApiSuccess<RiskCaseView>(RiskCaseView(
      detail: detail,
      history: history,
    ));
  }

  @override
  Future<ApiResult<RiskCaseNote>> addNote({
    required String caseNumber,
    required String content,
    required int expectedVersion,
  }) {
    return _apiClient.post<RiskCaseNote>(
      '/api/risk-cases/$caseNumber/notes',
      body: <String, Object>{
        'content': content,
        'expectedVersion': expectedVersion,
      },
      decode: (json) => RiskCaseNote.fromJson(_map(json)),
    );
  }

  Map<String, dynamic> _map(Object? value) {
    if (value is! Map) {
      throw const FormatException('expected JSON object');
    }
    return value.cast<String, dynamic>();
  }
}
