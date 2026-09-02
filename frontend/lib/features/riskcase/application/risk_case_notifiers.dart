import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_riverpod/legacy.dart';

import '../../../core/api/api_contract.dart';
import '../data/risk_case_models.dart';
import '../data/risk_case_repository.dart';
import 'risk_case_providers.dart';

final riskCaseListProvider = StateNotifierProvider.autoDispose<
    RiskCaseListNotifier, AsyncValue<RiskCaseSummaryPage>>((ref) {
  return RiskCaseListNotifier(ref.watch(riskCaseRepositoryProvider))..load();
});

final riskCaseDetailProvider = StateNotifierProvider.autoDispose.family<
    RiskCaseDetailNotifier, AsyncValue<RiskCaseView>, String>((ref, caseNumber) {
  return RiskCaseDetailNotifier(
    ref.watch(riskCaseRepositoryProvider),
    caseNumber,
  )..load();
});

final riskCaseOperationProvider = StateNotifierProvider.autoDispose.family<
    RiskCaseOperationNotifier, RiskCaseOperationState, String>((ref, caseNumber) {
  return RiskCaseOperationNotifier(
    ref.watch(riskCaseRepositoryProvider),
    caseNumber,
    onReload: () => ref.read(riskCaseDetailProvider(caseNumber).notifier).load(),
  );
});

class RiskCaseListNotifier extends StateNotifier<AsyncValue<RiskCaseSummaryPage>> {
  RiskCaseListNotifier(this._repository)
      : super(const AsyncLoading<RiskCaseSummaryPage>());

  static const pageSize = 20;
  final RiskCaseRepository _repository;
  RiskCaseListQuery _query = const RiskCaseListQuery();

  Future<void> load({int page = 0}) async {
    state = const AsyncLoading<RiskCaseSummaryPage>();
    final result = await _repository.listCases(
      _query,
      page: page,
      size: pageSize,
    );
    state = switch (result) {
      ApiSuccess<RiskCaseSummaryPage>(:final value) => AsyncData(value),
      ApiError<RiskCaseSummaryPage>(:final failure) =>
        AsyncError(failure, StackTrace.current),
    };
  }

  Future<void> applyFilters({String? status, String? priority}) async {
    _query = RiskCaseListQuery(status: status, priority: priority);
    await load();
  }
}

class RiskCaseDetailNotifier extends StateNotifier<AsyncValue<RiskCaseView>> {
  RiskCaseDetailNotifier(this._repository, this.caseNumber)
      : super(const AsyncLoading<RiskCaseView>());

  final RiskCaseRepository _repository;
  final String caseNumber;

  Future<void> load() async {
    state = const AsyncLoading<RiskCaseView>();
    final result = await _repository.getCase(caseNumber);
    state = switch (result) {
      ApiSuccess<RiskCaseView>(:final value) => AsyncData(value),
      ApiError<RiskCaseView>(:final failure) =>
        AsyncError(failure, StackTrace.current),
    };
  }
}

class RiskCaseOperationState {
  const RiskCaseOperationState({
    this.isSubmitting = false,
    this.message,
    this.isError = false,
    this.isVersionConflict = false,
  });

  final bool isSubmitting;
  final String? message;
  final bool isError;
  final bool isVersionConflict;
}

class RiskCaseOperationNotifier extends StateNotifier<RiskCaseOperationState> {
  RiskCaseOperationNotifier(
    this._repository,
    this.caseNumber, {
    required Future<void> Function() onReload,
  })  : _onReload = onReload,
        super(const RiskCaseOperationState());

  final RiskCaseRepository _repository;
  final String caseNumber;
  final Future<void> Function() _onReload;

  Future<bool> addNote({
    required String content,
    required int expectedVersion,
  }) async {
    state = const RiskCaseOperationState(isSubmitting: true);
    final result = await _repository.addNote(
      caseNumber: caseNumber,
      content: content,
      expectedVersion: expectedVersion,
    );
    switch (result) {
      case ApiSuccess<RiskCaseNote>():
        await _onReload();
        state = const RiskCaseOperationState(message: 'Investigation note added.');
        return true;
      case ApiError<RiskCaseNote>(:final failure):
        if (failure.isVersionConflict) {
          await _onReload();
          state = const RiskCaseOperationState(
            message: 'This case changed. The latest version was reloaded.',
            isError: true,
            isVersionConflict: true,
          );
        } else {
          state = RiskCaseOperationState(
            message: failure.message,
            isError: true,
          );
        }
        return false;
    }
  }

  void clearMessage() {
    state = const RiskCaseOperationState();
  }
}
