// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'risk_case_models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

_RiskCaseSummary _$RiskCaseSummaryFromJson(Map<String, dynamic> json) =>
    _RiskCaseSummary(
      caseNumber: json['caseNumber'] as String,
      subjectRef: json['subjectRef'] as String,
      status: json['status'] as String,
      priority: json['priority'] as String,
      assigneeRef: json['assigneeRef'] as String?,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedAt: DateTime.parse(json['updatedAt'] as String),
      version: (json['version'] as num).toInt(),
    );

Map<String, dynamic> _$RiskCaseSummaryToJson(_RiskCaseSummary instance) =>
    <String, dynamic>{
      'caseNumber': instance.caseNumber,
      'subjectRef': instance.subjectRef,
      'status': instance.status,
      'priority': instance.priority,
      'assigneeRef': instance.assigneeRef,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedAt': instance.updatedAt.toIso8601String(),
      'version': instance.version,
    };

_RiskCaseSummaryPage _$RiskCaseSummaryPageFromJson(Map<String, dynamic> json) =>
    _RiskCaseSummaryPage(
      items: (json['items'] as List<dynamic>)
          .map((e) => RiskCaseSummary.fromJson(e as Map<String, dynamic>))
          .toList(),
      page: (json['page'] as num).toInt(),
      size: (json['size'] as num).toInt(),
      hasNext: json['hasNext'] as bool,
    );

Map<String, dynamic> _$RiskCaseSummaryPageToJson(
  _RiskCaseSummaryPage instance,
) => <String, dynamic>{
  'items': instance.items,
  'page': instance.page,
  'size': instance.size,
  'hasNext': instance.hasNext,
};

_RiskCaseDetail _$RiskCaseDetailFromJson(Map<String, dynamic> json) =>
    _RiskCaseDetail(
      caseNumber: json['caseNumber'] as String,
      subjectType: json['subjectType'] as String,
      subjectRef: json['subjectRef'] as String,
      intakeSource: json['intakeSource'] as String,
      intakeSummary: json['intakeSummary'] as String,
      status: json['status'] as String,
      priority: json['priority'] as String,
      assigneeRef: json['assigneeRef'] as String?,
      assignedByRef: json['assignedByRef'] as String?,
      assignedAt: json['assignedAt'] == null
          ? null
          : DateTime.parse(json['assignedAt'] as String),
      currentDecisionRef: json['currentDecisionRef'] as String?,
      currentCycleNo: (json['currentCycleNo'] as num).toInt(),
      createdByRef: json['createdByRef'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      updatedByRef: json['updatedByRef'] as String,
      updatedAt: DateTime.parse(json['updatedAt'] as String),
      version: (json['version'] as num).toInt(),
    );

Map<String, dynamic> _$RiskCaseDetailToJson(_RiskCaseDetail instance) =>
    <String, dynamic>{
      'caseNumber': instance.caseNumber,
      'subjectType': instance.subjectType,
      'subjectRef': instance.subjectRef,
      'intakeSource': instance.intakeSource,
      'intakeSummary': instance.intakeSummary,
      'status': instance.status,
      'priority': instance.priority,
      'assigneeRef': instance.assigneeRef,
      'assignedByRef': instance.assignedByRef,
      'assignedAt': instance.assignedAt?.toIso8601String(),
      'currentDecisionRef': instance.currentDecisionRef,
      'currentCycleNo': instance.currentCycleNo,
      'createdByRef': instance.createdByRef,
      'createdAt': instance.createdAt.toIso8601String(),
      'updatedByRef': instance.updatedByRef,
      'updatedAt': instance.updatedAt.toIso8601String(),
      'version': instance.version,
    };

_RiskCaseHistoryEntry _$RiskCaseHistoryEntryFromJson(
  Map<String, dynamic> json,
) => _RiskCaseHistoryEntry(
  version: (json['version'] as num).toInt(),
  eventType: json['eventType'] as String,
  affectedRef: json['affectedRef'] as String?,
  actorRef: json['actorRef'] as String,
  occurredAt: DateTime.parse(json['occurredAt'] as String),
);

Map<String, dynamic> _$RiskCaseHistoryEntryToJson(
  _RiskCaseHistoryEntry instance,
) => <String, dynamic>{
  'version': instance.version,
  'eventType': instance.eventType,
  'affectedRef': instance.affectedRef,
  'actorRef': instance.actorRef,
  'occurredAt': instance.occurredAt.toIso8601String(),
};

_RiskCaseHistoryPage _$RiskCaseHistoryPageFromJson(Map<String, dynamic> json) =>
    _RiskCaseHistoryPage(
      entries: (json['entries'] as List<dynamic>)
          .map((e) => RiskCaseHistoryEntry.fromJson(e as Map<String, dynamic>))
          .toList(),
      nextCursor: json['nextCursor'] as String?,
    );

Map<String, dynamic> _$RiskCaseHistoryPageToJson(
  _RiskCaseHistoryPage instance,
) => <String, dynamic>{
  'entries': instance.entries,
  'nextCursor': instance.nextCursor,
};

_RiskCaseNote _$RiskCaseNoteFromJson(Map<String, dynamic> json) =>
    _RiskCaseNote(
      noteRef: json['noteRef'] as String,
      supersedesNoteRef: json['supersedesNoteRef'] as String?,
      version: (json['version'] as num).toInt(),
      createdByRef: json['createdByRef'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
    );

Map<String, dynamic> _$RiskCaseNoteToJson(_RiskCaseNote instance) =>
    <String, dynamic>{
      'noteRef': instance.noteRef,
      'supersedesNoteRef': instance.supersedesNoteRef,
      'version': instance.version,
      'createdByRef': instance.createdByRef,
      'createdAt': instance.createdAt.toIso8601String(),
    };
