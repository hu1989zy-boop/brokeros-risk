import 'package:brokeros_risk_console/features/riskcase/data/risk_case_models.dart';
import 'package:brokeros_risk_console/features/riskcase/presentation/add_note_dialog.dart';
import 'package:brokeros_risk_console/features/riskcase/presentation/risk_case_detail_page.dart';
import 'package:brokeros_risk_console/features/riskcase/presentation/risk_case_list_page.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';

import 'support/fake_risk_case_repository.dart';

void main() {
  Widget host(Widget child) => MaterialApp(home: Scaffold(body: child));

  group('RiskCaseListBody', () {
    testWidgets('renders loading, empty, error, and success states', (tester) async {
      await tester.pumpWidget(host(RiskCaseListBody(
        state: const AsyncLoading<RiskCaseSummaryPage>(),
        onRetry: () {},
        onPage: (_) {},
        onOpen: (_) {},
      )));
      expect(find.byType(CircularProgressIndicator), findsOneWidget);

      await tester.pumpWidget(host(RiskCaseListBody(
        state: AsyncData(sampleListPage(items: [])),
        onRetry: () {},
        onPage: (_) {},
        onOpen: (_) {},
      )));
      expect(find.text('No risk cases match these filters.'), findsOneWidget);

      await tester.pumpWidget(host(RiskCaseListBody(
        state: AsyncError<RiskCaseSummaryPage>(unavailable, StackTrace.empty),
        onRetry: () {},
        onPage: (_) {},
        onOpen: (_) {},
      )));
      expect(find.text('API unavailable'), findsOneWidget);

      var opened = '';
      await tester.pumpWidget(host(RiskCaseListBody(
        state: AsyncData(sampleListPage()),
        onRetry: () {},
        onPage: (_) {},
        onOpen: (value) => opened = value,
      )));
      expect(find.byKey(const Key('risk-case-list')), findsOneWidget);
      await tester.tap(find.text(sampleSummary().caseNumber));
      expect(opened, sampleSummary().caseNumber);
    });
  });

  group('RiskCaseDetailBody', () {
    testWidgets('renders loading, error, detail, associations, and history',
        (tester) async {
      await tester.pumpWidget(host(RiskCaseDetailBody(
        state: const AsyncLoading<RiskCaseView>(),
        onRetry: () {},
        onAddNote: (_) {},
      )));
      expect(find.byType(CircularProgressIndicator), findsOneWidget);

      await tester.pumpWidget(host(RiskCaseDetailBody(
        state: AsyncError<RiskCaseView>(unavailable, StackTrace.empty),
        onRetry: () {},
        onAddNote: (_) {},
      )));
      expect(find.text('API unavailable'), findsOneWidget);

      await tester.pumpWidget(host(RiskCaseDetailBody(
        state: AsyncData(sampleView()),
        onRetry: () {},
        onAddNote: (_) {},
      )));
      expect(find.byKey(const Key('risk-case-detail')), findsOneWidget);
      expect(find.text('History timeline'), findsOneWidget);
      expect(find.textContaining('dec-36000000'), findsWidgets);
      expect(find.byKey(const Key('open-add-note')), findsOneWidget);
    });
  });

  group('AddNoteDialog', () {
    testWidgets('renders backend error and submits note with expected version',
        (tester) async {
      String? content;
      int? version;
      await tester.pumpWidget(MaterialApp(
        home: AddNoteDialog(
          expectedVersion: 7,
          isSubmitting: false,
          errorMessage: 'This case changed. The latest version was reloaded.',
          onSubmit: (submitted, expectedVersion) async {
            content = submitted;
            version = expectedVersion;
            return false;
          },
        ),
      ));

      expect(find.byKey(const Key('operation-error')), findsOneWidget);
      await tester.enterText(
        find.byKey(const Key('note-content')),
        'Investigated execution sequence',
      );
      await tester.tap(find.byKey(const Key('submit-note')));
      await tester.pump();

      expect(content, 'Investigated execution sequence');
      expect(version, 7);
    });
  });
}
