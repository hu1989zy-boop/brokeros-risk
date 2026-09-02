import 'package:brokeros_risk_console/core/api/api_contract.dart';
import 'package:brokeros_risk_console/features/riskcase/data/risk_case_models.dart';
import 'package:brokeros_risk_console/features/riskcase/data/risk_case_repository.dart';

class FakeRiskCaseRepository implements RiskCaseRepository {
  ApiResult<RiskCaseSummaryPage> listResult = ApiSuccess(sampleListPage());
  ApiResult<RiskCaseView> detailResult = ApiSuccess(sampleView());
  ApiResult<RiskCaseNote> noteResult = ApiSuccess(sampleNote());
  int? lastExpectedVersion;
  String? lastNoteContent;

  @override
  Future<ApiResult<RiskCaseSummaryPage>> listCases(
    RiskCaseListQuery query, {
    required int page,
    required int size,
  }) async =>
      listResult;

  @override
  Future<ApiResult<RiskCaseView>> getCase(String caseNumber) async => detailResult;

  @override
  Future<ApiResult<RiskCaseNote>> addNote({
    required String caseNumber,
    required String content,
    required int expectedVersion,
  }) async {
    lastNoteContent = content;
    lastExpectedVersion = expectedVersion;
    return noteResult;
  }
}

final sampleInstant = DateTime.parse('2026-09-02T00:00:00Z');

RiskCaseSummary sampleSummary() => RiskCaseSummary(
      caseNumber: 'RC-16000000-0000-4000-8000-000000000001',
      subjectRef: 'ta-26000000-0000-4000-8000-000000000001',
      status: 'OPEN',
      priority: 'HIGH',
      assigneeRef: '16000000-0000-4000-8000-000000000001',
      createdAt: sampleInstant,
      updatedAt: sampleInstant,
      version: 3,
    );

RiskCaseSummaryPage sampleListPage({List<RiskCaseSummary>? items}) => RiskCaseSummaryPage(
      items: items ?? [sampleSummary()],
      page: 0,
      size: 20,
      hasNext: false,
    );

RiskCaseDetail sampleDetail() => RiskCaseDetail(
      caseNumber: sampleSummary().caseNumber,
      subjectType: 'TRADING_ACCOUNT',
      subjectRef: sampleSummary().subjectRef,
      intakeSource: 'MANUAL',
      intakeSummary: 'Review unusual trading activity.',
      status: 'OPEN',
      priority: 'HIGH',
      assigneeRef: sampleSummary().assigneeRef,
      assignedByRef: sampleSummary().assigneeRef,
      assignedAt: sampleInstant,
      currentDecisionRef: 'dec-36000000-0000-4000-8000-000000000001',
      currentCycleNo: 1,
      createdByRef: sampleSummary().assigneeRef!,
      createdAt: sampleInstant,
      updatedByRef: sampleSummary().assigneeRef!,
      updatedAt: sampleInstant,
      version: 3,
    );

RiskCaseHistoryPage sampleHistory() => RiskCaseHistoryPage(
      entries: [
        RiskCaseHistoryEntry(
          version: 3,
          eventType: 'DECISION_ASSOCIATED',
          affectedRef: 'dec-36000000-0000-4000-8000-000000000001',
          actorRef: sampleSummary().assigneeRef!,
          occurredAt: sampleInstant,
        ),
      ],
    );

RiskCaseView sampleView() => RiskCaseView(
      detail: sampleDetail(),
      history: sampleHistory(),
    );

RiskCaseNote sampleNote() => RiskCaseNote(
      noteRef: 'note-46000000-0000-4000-8000-000000000001',
      version: 4,
      createdByRef: sampleSummary().assigneeRef!,
      createdAt: sampleInstant,
    );

const unavailable = ApiFailure(
  code: ResultCode.internalError,
  message: 'API unavailable',
  httpStatus: 500,
);

const versionConflict = ApiFailure(
  code: ResultCode.riskCaseVersionConflict,
  message: 'Risk case version conflicts with current state',
  httpStatus: 409,
);
