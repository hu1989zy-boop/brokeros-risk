import 'package:brokeros_risk_console/core/api/api_contract.dart';
import 'package:brokeros_risk_console/features/riskcase/application/risk_case_notifiers.dart';
import 'package:brokeros_risk_console/features/riskcase/data/risk_case_models.dart';
import 'package:flutter_test/flutter_test.dart';

import 'support/fake_risk_case_repository.dart';

void main() {
  group('RiskCaseListNotifier', () {
    test('exposes success and empty pages', () async {
      final repository = FakeRiskCaseRepository();
      final notifier = RiskCaseListNotifier(repository);

      await notifier.load();
      expect(notifier.state.requireValue.items, hasLength(1));

      repository.listResult = ApiSuccess(sampleListPage(items: []));
      await notifier.load();
      expect(notifier.state.requireValue.items, isEmpty);
    });

    test('exposes a typed error', () async {
      final repository = FakeRiskCaseRepository()
        ..listResult = const ApiError<RiskCaseSummaryPage>(unavailable);
      final notifier = RiskCaseListNotifier(repository);

      await notifier.load();

      expect(notifier.state.error, isNotNull);
      expect(notifier.state.error, same(unavailable));
    });
  });

  group('RiskCaseDetailNotifier', () {
    test('exposes loaded detail and history', () async {
      final notifier = RiskCaseDetailNotifier(
        FakeRiskCaseRepository(),
        sampleSummary().caseNumber,
      );

      await notifier.load();

      expect(notifier.state.requireValue.detail.caseNumber, sampleSummary().caseNumber);
      expect(notifier.state.requireValue.history.entries, hasLength(1));
    });

    test('exposes a typed error', () async {
      final repository = FakeRiskCaseRepository()
        ..detailResult = const ApiError<RiskCaseView>(unavailable);
      final notifier = RiskCaseDetailNotifier(repository, sampleSummary().caseNumber);

      await notifier.load();

      expect(notifier.state.error, same(unavailable));
    });
  });

  group('RiskCaseOperationNotifier', () {
    test('sends expectedVersion and reloads after success', () async {
      final repository = FakeRiskCaseRepository();
      var reloads = 0;
      final notifier = RiskCaseOperationNotifier(
        repository,
        sampleSummary().caseNumber,
        onReload: () async {
          reloads++;
        },
      );

      final success = await notifier.addNote(
        content: 'Objective investigation note',
        expectedVersion: 3,
      );

      expect(success, isTrue);
      expect(repository.lastExpectedVersion, 3);
      expect(repository.lastNoteContent, 'Objective investigation note');
      expect(reloads, 1);
      expect(notifier.state.isError, isFalse);
    });

    test('makes version conflict explicit and reloads latest detail', () async {
      final repository = FakeRiskCaseRepository()
        ..noteResult = const ApiError<RiskCaseNote>(versionConflict);
      var reloads = 0;
      final notifier = RiskCaseOperationNotifier(
        repository,
        sampleSummary().caseNumber,
        onReload: () async {
          reloads++;
        },
      );

      final success = await notifier.addNote(content: 'Stale note', expectedVersion: 2);

      expect(success, isFalse);
      expect(reloads, 1);
      expect(notifier.state.isVersionConflict, isTrue);
      expect(notifier.state.message, contains('latest version was reloaded'));
    });

    test('surfaces non-conflict backend errors without a reload', () async {
      final repository = FakeRiskCaseRepository()
        ..noteResult = const ApiError<RiskCaseNote>(unavailable);
      var reloads = 0;
      final notifier = RiskCaseOperationNotifier(
        repository,
        sampleSummary().caseNumber,
        onReload: () async {
          reloads++;
        },
      );

      await notifier.addNote(content: 'Note', expectedVersion: 3);

      expect(reloads, 0);
      expect(notifier.state.message, 'API unavailable');
    });
  });
}
