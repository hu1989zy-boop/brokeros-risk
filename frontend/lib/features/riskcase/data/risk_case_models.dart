import 'package:freezed_annotation/freezed_annotation.dart';

part 'risk_case_models.freezed.dart';
part 'risk_case_models.g.dart';

@freezed
abstract class RiskCaseSummary with _$RiskCaseSummary {
  const factory RiskCaseSummary({
    required String caseNumber,
    required String subjectRef,
    required String status,
    required String priority,
    String? assigneeRef,
    required DateTime createdAt,
    required DateTime updatedAt,
    required int version,
  }) = _RiskCaseSummary;

  factory RiskCaseSummary.fromJson(Map<String, dynamic> json) =>
      _$RiskCaseSummaryFromJson(json);
}

@freezed
abstract class RiskCaseSummaryPage with _$RiskCaseSummaryPage {
  const factory RiskCaseSummaryPage({
    required List<RiskCaseSummary> items,
    required int page,
    required int size,
    required bool hasNext,
  }) = _RiskCaseSummaryPage;

  factory RiskCaseSummaryPage.fromJson(Map<String, dynamic> json) =>
      _$RiskCaseSummaryPageFromJson(json);
}

@freezed
abstract class RiskCaseDetail with _$RiskCaseDetail {
  const factory RiskCaseDetail({
    required String caseNumber,
    required String subjectType,
    required String subjectRef,
    required String intakeSource,
    required String intakeSummary,
    required String status,
    required String priority,
    String? assigneeRef,
    String? assignedByRef,
    DateTime? assignedAt,
    String? currentDecisionRef,
    required int currentCycleNo,
    required String createdByRef,
    required DateTime createdAt,
    required String updatedByRef,
    required DateTime updatedAt,
    required int version,
  }) = _RiskCaseDetail;

  factory RiskCaseDetail.fromJson(Map<String, dynamic> json) =>
      _$RiskCaseDetailFromJson(json);
}

@freezed
abstract class RiskCaseHistoryEntry with _$RiskCaseHistoryEntry {
  const factory RiskCaseHistoryEntry({
    required int version,
    required String eventType,
    String? affectedRef,
    required String actorRef,
    required DateTime occurredAt,
  }) = _RiskCaseHistoryEntry;

  factory RiskCaseHistoryEntry.fromJson(Map<String, dynamic> json) =>
      _$RiskCaseHistoryEntryFromJson(json);
}

@freezed
abstract class RiskCaseHistoryPage with _$RiskCaseHistoryPage {
  const factory RiskCaseHistoryPage({
    required List<RiskCaseHistoryEntry> entries,
    String? nextCursor,
  }) = _RiskCaseHistoryPage;

  factory RiskCaseHistoryPage.fromJson(Map<String, dynamic> json) =>
      _$RiskCaseHistoryPageFromJson(json);
}

@freezed
abstract class RiskCaseNote with _$RiskCaseNote {
  const factory RiskCaseNote({
    required String noteRef,
    String? supersedesNoteRef,
    required int version,
    required String createdByRef,
    required DateTime createdAt,
  }) = _RiskCaseNote;

  factory RiskCaseNote.fromJson(Map<String, dynamic> json) =>
      _$RiskCaseNoteFromJson(json);
}

@freezed
abstract class RiskCaseView with _$RiskCaseView {
  const factory RiskCaseView({
    required RiskCaseDetail detail,
    required RiskCaseHistoryPage history,
  }) = _RiskCaseView;
}

class RiskCaseListQuery {
  const RiskCaseListQuery({
    this.status,
    this.priority,
    this.subjectRef,
    this.assignee,
  });

  final String? status;
  final String? priority;
  final String? subjectRef;
  final String? assignee;

  Map<String, dynamic> toQueryParameters({
    required int page,
    required int size,
  }) {
    return <String, dynamic>{
      if (status != null) 'status': status,
      if (priority != null) 'priority': priority,
      if (subjectRef != null) 'subjectRef': subjectRef,
      if (assignee != null) 'assignee': assignee,
      'page': page,
      'size': size,
    };
  }
}
