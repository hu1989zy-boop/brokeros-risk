import 'package:brokeros_risk_console/core/api/api_contract.dart';
import 'package:brokeros_risk_console/features/riskcase/data/risk_case_models.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('parses the documented risk case list envelope', () {
    final envelope = ApiEnvelope<RiskCaseSummaryPage>.fromJson(
      {
        'code': 'SUCCESS',
        'message': 'Success',
        'data': {
          'items': [
            {
              'caseNumber': 'RC-16000000-0000-4000-8000-000000000001',
              'subjectRef': 'ta-26000000-0000-4000-8000-000000000001',
              'status': 'OPEN',
              'priority': 'HIGH',
              'assigneeRef': null,
              'createdAt': '2026-09-02T00:00:00Z',
              'updatedAt': '2026-09-02T00:01:00Z',
              'version': 1,
            },
          ],
          'page': 0,
          'size': 20,
          'hasNext': false,
        },
        'timestamp': '2026-09-02T00:02:00Z',
      },
      (json) => RiskCaseSummaryPage.fromJson((json! as Map).cast<String, dynamic>()),
    );

    expect(envelope.code, ResultCode.success);
    expect(envelope.data!.items.single.status, 'OPEN');
    expect(envelope.data!.items.single.updatedAt.isUtc, isTrue);
  });

  test('parses detail, history, and note response DTOs', () {
    final detail = RiskCaseDetail.fromJson({
      'caseNumber': 'RC-16000000-0000-4000-8000-000000000001',
      'subjectType': 'TRADING_ACCOUNT',
      'subjectRef': 'ta-26000000-0000-4000-8000-000000000001',
      'intakeSource': 'MANUAL',
      'intakeSummary': 'Review activity',
      'status': 'OPEN',
      'priority': 'NORMAL',
      'assigneeRef': null,
      'assignedByRef': null,
      'assignedAt': null,
      'currentDecisionRef': null,
      'currentCycleNo': 1,
      'createdByRef': '16000000-0000-4000-8000-000000000001',
      'createdAt': '2026-09-02T00:00:00Z',
      'updatedByRef': '16000000-0000-4000-8000-000000000001',
      'updatedAt': '2026-09-02T00:00:00Z',
      'version': 1,
    });
    final history = RiskCaseHistoryPage.fromJson({
      'entries': [
        {
          'version': 1,
          'eventType': 'CREATE',
          'affectedRef': null,
          'actorRef': '16000000-0000-4000-8000-000000000001',
          'occurredAt': '2026-09-02T00:00:00Z',
        },
      ],
      'nextCursor': null,
    });
    final note = RiskCaseNote.fromJson({
      'noteRef': 'note-46000000-0000-4000-8000-000000000001',
      'supersedesNoteRef': null,
      'version': 2,
      'createdByRef': '16000000-0000-4000-8000-000000000001',
      'createdAt': '2026-09-02T00:01:00Z',
    });

    expect(detail.currentCycleNo, 1);
    expect(history.entries.single.eventType, 'CREATE');
    expect(note.version, 2);
  });

  test('maps unknown ResultCode without accepting it as success', () {
    expect(ResultCode.fromWire('FUTURE_CODE'), ResultCode.unknown);
  });
}
