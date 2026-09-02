// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'risk_case_models.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;

/// @nodoc
mixin _$RiskCaseSummary {

 String get caseNumber; String get subjectRef; String get status; String get priority; String? get assigneeRef; DateTime get createdAt; DateTime get updatedAt; int get version;
/// Create a copy of RiskCaseSummary
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RiskCaseSummaryCopyWith<RiskCaseSummary> get copyWith => _$RiskCaseSummaryCopyWithImpl<RiskCaseSummary>(this as RiskCaseSummary, _$identity);

  /// Serializes this RiskCaseSummary to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RiskCaseSummary&&(identical(other.caseNumber, caseNumber) || other.caseNumber == caseNumber)&&(identical(other.subjectRef, subjectRef) || other.subjectRef == subjectRef)&&(identical(other.status, status) || other.status == status)&&(identical(other.priority, priority) || other.priority == priority)&&(identical(other.assigneeRef, assigneeRef) || other.assigneeRef == assigneeRef)&&(identical(other.createdAt, createdAt) || other.createdAt == createdAt)&&(identical(other.updatedAt, updatedAt) || other.updatedAt == updatedAt)&&(identical(other.version, version) || other.version == version));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,caseNumber,subjectRef,status,priority,assigneeRef,createdAt,updatedAt,version);

@override
String toString() {
  return 'RiskCaseSummary(caseNumber: $caseNumber, subjectRef: $subjectRef, status: $status, priority: $priority, assigneeRef: $assigneeRef, createdAt: $createdAt, updatedAt: $updatedAt, version: $version)';
}


}

/// @nodoc
abstract mixin class $RiskCaseSummaryCopyWith<$Res>  {
  factory $RiskCaseSummaryCopyWith(RiskCaseSummary value, $Res Function(RiskCaseSummary) _then) = _$RiskCaseSummaryCopyWithImpl;
@useResult
$Res call({
 String caseNumber, String subjectRef, String status, String priority, String? assigneeRef, DateTime createdAt, DateTime updatedAt, int version
});




}
/// @nodoc
class _$RiskCaseSummaryCopyWithImpl<$Res>
    implements $RiskCaseSummaryCopyWith<$Res> {
  _$RiskCaseSummaryCopyWithImpl(this._self, this._then);

  final RiskCaseSummary _self;
  final $Res Function(RiskCaseSummary) _then;

/// Create a copy of RiskCaseSummary
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? caseNumber = null,Object? subjectRef = null,Object? status = null,Object? priority = null,Object? assigneeRef = freezed,Object? createdAt = null,Object? updatedAt = null,Object? version = null,}) {
  return _then(_self.copyWith(
caseNumber: null == caseNumber ? _self.caseNumber : caseNumber // ignore: cast_nullable_to_non_nullable
as String,subjectRef: null == subjectRef ? _self.subjectRef : subjectRef // ignore: cast_nullable_to_non_nullable
as String,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as String,priority: null == priority ? _self.priority : priority // ignore: cast_nullable_to_non_nullable
as String,assigneeRef: freezed == assigneeRef ? _self.assigneeRef : assigneeRef // ignore: cast_nullable_to_non_nullable
as String?,createdAt: null == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as DateTime,updatedAt: null == updatedAt ? _self.updatedAt : updatedAt // ignore: cast_nullable_to_non_nullable
as DateTime,version: null == version ? _self.version : version // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [RiskCaseSummary].
extension RiskCaseSummaryPatterns on RiskCaseSummary {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RiskCaseSummary value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RiskCaseSummary() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RiskCaseSummary value)  $default,){
final _that = this;
switch (_that) {
case _RiskCaseSummary():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RiskCaseSummary value)?  $default,){
final _that = this;
switch (_that) {
case _RiskCaseSummary() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String caseNumber,  String subjectRef,  String status,  String priority,  String? assigneeRef,  DateTime createdAt,  DateTime updatedAt,  int version)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RiskCaseSummary() when $default != null:
return $default(_that.caseNumber,_that.subjectRef,_that.status,_that.priority,_that.assigneeRef,_that.createdAt,_that.updatedAt,_that.version);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String caseNumber,  String subjectRef,  String status,  String priority,  String? assigneeRef,  DateTime createdAt,  DateTime updatedAt,  int version)  $default,) {final _that = this;
switch (_that) {
case _RiskCaseSummary():
return $default(_that.caseNumber,_that.subjectRef,_that.status,_that.priority,_that.assigneeRef,_that.createdAt,_that.updatedAt,_that.version);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String caseNumber,  String subjectRef,  String status,  String priority,  String? assigneeRef,  DateTime createdAt,  DateTime updatedAt,  int version)?  $default,) {final _that = this;
switch (_that) {
case _RiskCaseSummary() when $default != null:
return $default(_that.caseNumber,_that.subjectRef,_that.status,_that.priority,_that.assigneeRef,_that.createdAt,_that.updatedAt,_that.version);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _RiskCaseSummary implements RiskCaseSummary {
  const _RiskCaseSummary({required this.caseNumber, required this.subjectRef, required this.status, required this.priority, this.assigneeRef, required this.createdAt, required this.updatedAt, required this.version});
  factory _RiskCaseSummary.fromJson(Map<String, dynamic> json) => _$RiskCaseSummaryFromJson(json);

@override final  String caseNumber;
@override final  String subjectRef;
@override final  String status;
@override final  String priority;
@override final  String? assigneeRef;
@override final  DateTime createdAt;
@override final  DateTime updatedAt;
@override final  int version;

/// Create a copy of RiskCaseSummary
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RiskCaseSummaryCopyWith<_RiskCaseSummary> get copyWith => __$RiskCaseSummaryCopyWithImpl<_RiskCaseSummary>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$RiskCaseSummaryToJson(this, );
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RiskCaseSummary&&(identical(other.caseNumber, caseNumber) || other.caseNumber == caseNumber)&&(identical(other.subjectRef, subjectRef) || other.subjectRef == subjectRef)&&(identical(other.status, status) || other.status == status)&&(identical(other.priority, priority) || other.priority == priority)&&(identical(other.assigneeRef, assigneeRef) || other.assigneeRef == assigneeRef)&&(identical(other.createdAt, createdAt) || other.createdAt == createdAt)&&(identical(other.updatedAt, updatedAt) || other.updatedAt == updatedAt)&&(identical(other.version, version) || other.version == version));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,caseNumber,subjectRef,status,priority,assigneeRef,createdAt,updatedAt,version);

@override
String toString() {
  return 'RiskCaseSummary(caseNumber: $caseNumber, subjectRef: $subjectRef, status: $status, priority: $priority, assigneeRef: $assigneeRef, createdAt: $createdAt, updatedAt: $updatedAt, version: $version)';
}


}

/// @nodoc
abstract mixin class _$RiskCaseSummaryCopyWith<$Res> implements $RiskCaseSummaryCopyWith<$Res> {
  factory _$RiskCaseSummaryCopyWith(_RiskCaseSummary value, $Res Function(_RiskCaseSummary) _then) = __$RiskCaseSummaryCopyWithImpl;
@override @useResult
$Res call({
 String caseNumber, String subjectRef, String status, String priority, String? assigneeRef, DateTime createdAt, DateTime updatedAt, int version
});




}
/// @nodoc
class __$RiskCaseSummaryCopyWithImpl<$Res>
    implements _$RiskCaseSummaryCopyWith<$Res> {
  __$RiskCaseSummaryCopyWithImpl(this._self, this._then);

  final _RiskCaseSummary _self;
  final $Res Function(_RiskCaseSummary) _then;

/// Create a copy of RiskCaseSummary
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? caseNumber = null,Object? subjectRef = null,Object? status = null,Object? priority = null,Object? assigneeRef = freezed,Object? createdAt = null,Object? updatedAt = null,Object? version = null,}) {
  return _then(_RiskCaseSummary(
caseNumber: null == caseNumber ? _self.caseNumber : caseNumber // ignore: cast_nullable_to_non_nullable
as String,subjectRef: null == subjectRef ? _self.subjectRef : subjectRef // ignore: cast_nullable_to_non_nullable
as String,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as String,priority: null == priority ? _self.priority : priority // ignore: cast_nullable_to_non_nullable
as String,assigneeRef: freezed == assigneeRef ? _self.assigneeRef : assigneeRef // ignore: cast_nullable_to_non_nullable
as String?,createdAt: null == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as DateTime,updatedAt: null == updatedAt ? _self.updatedAt : updatedAt // ignore: cast_nullable_to_non_nullable
as DateTime,version: null == version ? _self.version : version // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}


/// @nodoc
mixin _$RiskCaseSummaryPage {

 List<RiskCaseSummary> get items; int get page; int get size; bool get hasNext;
/// Create a copy of RiskCaseSummaryPage
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RiskCaseSummaryPageCopyWith<RiskCaseSummaryPage> get copyWith => _$RiskCaseSummaryPageCopyWithImpl<RiskCaseSummaryPage>(this as RiskCaseSummaryPage, _$identity);

  /// Serializes this RiskCaseSummaryPage to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RiskCaseSummaryPage&&const DeepCollectionEquality().equals(other.items, items)&&(identical(other.page, page) || other.page == page)&&(identical(other.size, size) || other.size == size)&&(identical(other.hasNext, hasNext) || other.hasNext == hasNext));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(items),page,size,hasNext);

@override
String toString() {
  return 'RiskCaseSummaryPage(items: $items, page: $page, size: $size, hasNext: $hasNext)';
}


}

/// @nodoc
abstract mixin class $RiskCaseSummaryPageCopyWith<$Res>  {
  factory $RiskCaseSummaryPageCopyWith(RiskCaseSummaryPage value, $Res Function(RiskCaseSummaryPage) _then) = _$RiskCaseSummaryPageCopyWithImpl;
@useResult
$Res call({
 List<RiskCaseSummary> items, int page, int size, bool hasNext
});




}
/// @nodoc
class _$RiskCaseSummaryPageCopyWithImpl<$Res>
    implements $RiskCaseSummaryPageCopyWith<$Res> {
  _$RiskCaseSummaryPageCopyWithImpl(this._self, this._then);

  final RiskCaseSummaryPage _self;
  final $Res Function(RiskCaseSummaryPage) _then;

/// Create a copy of RiskCaseSummaryPage
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? items = null,Object? page = null,Object? size = null,Object? hasNext = null,}) {
  return _then(_self.copyWith(
items: null == items ? _self.items : items // ignore: cast_nullable_to_non_nullable
as List<RiskCaseSummary>,page: null == page ? _self.page : page // ignore: cast_nullable_to_non_nullable
as int,size: null == size ? _self.size : size // ignore: cast_nullable_to_non_nullable
as int,hasNext: null == hasNext ? _self.hasNext : hasNext // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}

}


/// Adds pattern-matching-related methods to [RiskCaseSummaryPage].
extension RiskCaseSummaryPagePatterns on RiskCaseSummaryPage {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RiskCaseSummaryPage value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RiskCaseSummaryPage() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RiskCaseSummaryPage value)  $default,){
final _that = this;
switch (_that) {
case _RiskCaseSummaryPage():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RiskCaseSummaryPage value)?  $default,){
final _that = this;
switch (_that) {
case _RiskCaseSummaryPage() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<RiskCaseSummary> items,  int page,  int size,  bool hasNext)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RiskCaseSummaryPage() when $default != null:
return $default(_that.items,_that.page,_that.size,_that.hasNext);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<RiskCaseSummary> items,  int page,  int size,  bool hasNext)  $default,) {final _that = this;
switch (_that) {
case _RiskCaseSummaryPage():
return $default(_that.items,_that.page,_that.size,_that.hasNext);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<RiskCaseSummary> items,  int page,  int size,  bool hasNext)?  $default,) {final _that = this;
switch (_that) {
case _RiskCaseSummaryPage() when $default != null:
return $default(_that.items,_that.page,_that.size,_that.hasNext);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _RiskCaseSummaryPage implements RiskCaseSummaryPage {
  const _RiskCaseSummaryPage({required final  List<RiskCaseSummary> items, required this.page, required this.size, required this.hasNext}): _items = items;
  factory _RiskCaseSummaryPage.fromJson(Map<String, dynamic> json) => _$RiskCaseSummaryPageFromJson(json);

 final  List<RiskCaseSummary> _items;
@override List<RiskCaseSummary> get items {
  if (_items is EqualUnmodifiableListView) return _items;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_items);
}

@override final  int page;
@override final  int size;
@override final  bool hasNext;

/// Create a copy of RiskCaseSummaryPage
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RiskCaseSummaryPageCopyWith<_RiskCaseSummaryPage> get copyWith => __$RiskCaseSummaryPageCopyWithImpl<_RiskCaseSummaryPage>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$RiskCaseSummaryPageToJson(this, );
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RiskCaseSummaryPage&&const DeepCollectionEquality().equals(other._items, _items)&&(identical(other.page, page) || other.page == page)&&(identical(other.size, size) || other.size == size)&&(identical(other.hasNext, hasNext) || other.hasNext == hasNext));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_items),page,size,hasNext);

@override
String toString() {
  return 'RiskCaseSummaryPage(items: $items, page: $page, size: $size, hasNext: $hasNext)';
}


}

/// @nodoc
abstract mixin class _$RiskCaseSummaryPageCopyWith<$Res> implements $RiskCaseSummaryPageCopyWith<$Res> {
  factory _$RiskCaseSummaryPageCopyWith(_RiskCaseSummaryPage value, $Res Function(_RiskCaseSummaryPage) _then) = __$RiskCaseSummaryPageCopyWithImpl;
@override @useResult
$Res call({
 List<RiskCaseSummary> items, int page, int size, bool hasNext
});




}
/// @nodoc
class __$RiskCaseSummaryPageCopyWithImpl<$Res>
    implements _$RiskCaseSummaryPageCopyWith<$Res> {
  __$RiskCaseSummaryPageCopyWithImpl(this._self, this._then);

  final _RiskCaseSummaryPage _self;
  final $Res Function(_RiskCaseSummaryPage) _then;

/// Create a copy of RiskCaseSummaryPage
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? items = null,Object? page = null,Object? size = null,Object? hasNext = null,}) {
  return _then(_RiskCaseSummaryPage(
items: null == items ? _self._items : items // ignore: cast_nullable_to_non_nullable
as List<RiskCaseSummary>,page: null == page ? _self.page : page // ignore: cast_nullable_to_non_nullable
as int,size: null == size ? _self.size : size // ignore: cast_nullable_to_non_nullable
as int,hasNext: null == hasNext ? _self.hasNext : hasNext // ignore: cast_nullable_to_non_nullable
as bool,
  ));
}


}


/// @nodoc
mixin _$RiskCaseDetail {

 String get caseNumber; String get subjectType; String get subjectRef; String get intakeSource; String get intakeSummary; String get status; String get priority; String? get assigneeRef; String? get assignedByRef; DateTime? get assignedAt; String? get currentDecisionRef; int get currentCycleNo; String get createdByRef; DateTime get createdAt; String get updatedByRef; DateTime get updatedAt; int get version;
/// Create a copy of RiskCaseDetail
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RiskCaseDetailCopyWith<RiskCaseDetail> get copyWith => _$RiskCaseDetailCopyWithImpl<RiskCaseDetail>(this as RiskCaseDetail, _$identity);

  /// Serializes this RiskCaseDetail to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RiskCaseDetail&&(identical(other.caseNumber, caseNumber) || other.caseNumber == caseNumber)&&(identical(other.subjectType, subjectType) || other.subjectType == subjectType)&&(identical(other.subjectRef, subjectRef) || other.subjectRef == subjectRef)&&(identical(other.intakeSource, intakeSource) || other.intakeSource == intakeSource)&&(identical(other.intakeSummary, intakeSummary) || other.intakeSummary == intakeSummary)&&(identical(other.status, status) || other.status == status)&&(identical(other.priority, priority) || other.priority == priority)&&(identical(other.assigneeRef, assigneeRef) || other.assigneeRef == assigneeRef)&&(identical(other.assignedByRef, assignedByRef) || other.assignedByRef == assignedByRef)&&(identical(other.assignedAt, assignedAt) || other.assignedAt == assignedAt)&&(identical(other.currentDecisionRef, currentDecisionRef) || other.currentDecisionRef == currentDecisionRef)&&(identical(other.currentCycleNo, currentCycleNo) || other.currentCycleNo == currentCycleNo)&&(identical(other.createdByRef, createdByRef) || other.createdByRef == createdByRef)&&(identical(other.createdAt, createdAt) || other.createdAt == createdAt)&&(identical(other.updatedByRef, updatedByRef) || other.updatedByRef == updatedByRef)&&(identical(other.updatedAt, updatedAt) || other.updatedAt == updatedAt)&&(identical(other.version, version) || other.version == version));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,caseNumber,subjectType,subjectRef,intakeSource,intakeSummary,status,priority,assigneeRef,assignedByRef,assignedAt,currentDecisionRef,currentCycleNo,createdByRef,createdAt,updatedByRef,updatedAt,version);

@override
String toString() {
  return 'RiskCaseDetail(caseNumber: $caseNumber, subjectType: $subjectType, subjectRef: $subjectRef, intakeSource: $intakeSource, intakeSummary: $intakeSummary, status: $status, priority: $priority, assigneeRef: $assigneeRef, assignedByRef: $assignedByRef, assignedAt: $assignedAt, currentDecisionRef: $currentDecisionRef, currentCycleNo: $currentCycleNo, createdByRef: $createdByRef, createdAt: $createdAt, updatedByRef: $updatedByRef, updatedAt: $updatedAt, version: $version)';
}


}

/// @nodoc
abstract mixin class $RiskCaseDetailCopyWith<$Res>  {
  factory $RiskCaseDetailCopyWith(RiskCaseDetail value, $Res Function(RiskCaseDetail) _then) = _$RiskCaseDetailCopyWithImpl;
@useResult
$Res call({
 String caseNumber, String subjectType, String subjectRef, String intakeSource, String intakeSummary, String status, String priority, String? assigneeRef, String? assignedByRef, DateTime? assignedAt, String? currentDecisionRef, int currentCycleNo, String createdByRef, DateTime createdAt, String updatedByRef, DateTime updatedAt, int version
});




}
/// @nodoc
class _$RiskCaseDetailCopyWithImpl<$Res>
    implements $RiskCaseDetailCopyWith<$Res> {
  _$RiskCaseDetailCopyWithImpl(this._self, this._then);

  final RiskCaseDetail _self;
  final $Res Function(RiskCaseDetail) _then;

/// Create a copy of RiskCaseDetail
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? caseNumber = null,Object? subjectType = null,Object? subjectRef = null,Object? intakeSource = null,Object? intakeSummary = null,Object? status = null,Object? priority = null,Object? assigneeRef = freezed,Object? assignedByRef = freezed,Object? assignedAt = freezed,Object? currentDecisionRef = freezed,Object? currentCycleNo = null,Object? createdByRef = null,Object? createdAt = null,Object? updatedByRef = null,Object? updatedAt = null,Object? version = null,}) {
  return _then(_self.copyWith(
caseNumber: null == caseNumber ? _self.caseNumber : caseNumber // ignore: cast_nullable_to_non_nullable
as String,subjectType: null == subjectType ? _self.subjectType : subjectType // ignore: cast_nullable_to_non_nullable
as String,subjectRef: null == subjectRef ? _self.subjectRef : subjectRef // ignore: cast_nullable_to_non_nullable
as String,intakeSource: null == intakeSource ? _self.intakeSource : intakeSource // ignore: cast_nullable_to_non_nullable
as String,intakeSummary: null == intakeSummary ? _self.intakeSummary : intakeSummary // ignore: cast_nullable_to_non_nullable
as String,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as String,priority: null == priority ? _self.priority : priority // ignore: cast_nullable_to_non_nullable
as String,assigneeRef: freezed == assigneeRef ? _self.assigneeRef : assigneeRef // ignore: cast_nullable_to_non_nullable
as String?,assignedByRef: freezed == assignedByRef ? _self.assignedByRef : assignedByRef // ignore: cast_nullable_to_non_nullable
as String?,assignedAt: freezed == assignedAt ? _self.assignedAt : assignedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,currentDecisionRef: freezed == currentDecisionRef ? _self.currentDecisionRef : currentDecisionRef // ignore: cast_nullable_to_non_nullable
as String?,currentCycleNo: null == currentCycleNo ? _self.currentCycleNo : currentCycleNo // ignore: cast_nullable_to_non_nullable
as int,createdByRef: null == createdByRef ? _self.createdByRef : createdByRef // ignore: cast_nullable_to_non_nullable
as String,createdAt: null == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as DateTime,updatedByRef: null == updatedByRef ? _self.updatedByRef : updatedByRef // ignore: cast_nullable_to_non_nullable
as String,updatedAt: null == updatedAt ? _self.updatedAt : updatedAt // ignore: cast_nullable_to_non_nullable
as DateTime,version: null == version ? _self.version : version // ignore: cast_nullable_to_non_nullable
as int,
  ));
}

}


/// Adds pattern-matching-related methods to [RiskCaseDetail].
extension RiskCaseDetailPatterns on RiskCaseDetail {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RiskCaseDetail value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RiskCaseDetail() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RiskCaseDetail value)  $default,){
final _that = this;
switch (_that) {
case _RiskCaseDetail():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RiskCaseDetail value)?  $default,){
final _that = this;
switch (_that) {
case _RiskCaseDetail() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String caseNumber,  String subjectType,  String subjectRef,  String intakeSource,  String intakeSummary,  String status,  String priority,  String? assigneeRef,  String? assignedByRef,  DateTime? assignedAt,  String? currentDecisionRef,  int currentCycleNo,  String createdByRef,  DateTime createdAt,  String updatedByRef,  DateTime updatedAt,  int version)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RiskCaseDetail() when $default != null:
return $default(_that.caseNumber,_that.subjectType,_that.subjectRef,_that.intakeSource,_that.intakeSummary,_that.status,_that.priority,_that.assigneeRef,_that.assignedByRef,_that.assignedAt,_that.currentDecisionRef,_that.currentCycleNo,_that.createdByRef,_that.createdAt,_that.updatedByRef,_that.updatedAt,_that.version);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String caseNumber,  String subjectType,  String subjectRef,  String intakeSource,  String intakeSummary,  String status,  String priority,  String? assigneeRef,  String? assignedByRef,  DateTime? assignedAt,  String? currentDecisionRef,  int currentCycleNo,  String createdByRef,  DateTime createdAt,  String updatedByRef,  DateTime updatedAt,  int version)  $default,) {final _that = this;
switch (_that) {
case _RiskCaseDetail():
return $default(_that.caseNumber,_that.subjectType,_that.subjectRef,_that.intakeSource,_that.intakeSummary,_that.status,_that.priority,_that.assigneeRef,_that.assignedByRef,_that.assignedAt,_that.currentDecisionRef,_that.currentCycleNo,_that.createdByRef,_that.createdAt,_that.updatedByRef,_that.updatedAt,_that.version);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String caseNumber,  String subjectType,  String subjectRef,  String intakeSource,  String intakeSummary,  String status,  String priority,  String? assigneeRef,  String? assignedByRef,  DateTime? assignedAt,  String? currentDecisionRef,  int currentCycleNo,  String createdByRef,  DateTime createdAt,  String updatedByRef,  DateTime updatedAt,  int version)?  $default,) {final _that = this;
switch (_that) {
case _RiskCaseDetail() when $default != null:
return $default(_that.caseNumber,_that.subjectType,_that.subjectRef,_that.intakeSource,_that.intakeSummary,_that.status,_that.priority,_that.assigneeRef,_that.assignedByRef,_that.assignedAt,_that.currentDecisionRef,_that.currentCycleNo,_that.createdByRef,_that.createdAt,_that.updatedByRef,_that.updatedAt,_that.version);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _RiskCaseDetail implements RiskCaseDetail {
  const _RiskCaseDetail({required this.caseNumber, required this.subjectType, required this.subjectRef, required this.intakeSource, required this.intakeSummary, required this.status, required this.priority, this.assigneeRef, this.assignedByRef, this.assignedAt, this.currentDecisionRef, required this.currentCycleNo, required this.createdByRef, required this.createdAt, required this.updatedByRef, required this.updatedAt, required this.version});
  factory _RiskCaseDetail.fromJson(Map<String, dynamic> json) => _$RiskCaseDetailFromJson(json);

@override final  String caseNumber;
@override final  String subjectType;
@override final  String subjectRef;
@override final  String intakeSource;
@override final  String intakeSummary;
@override final  String status;
@override final  String priority;
@override final  String? assigneeRef;
@override final  String? assignedByRef;
@override final  DateTime? assignedAt;
@override final  String? currentDecisionRef;
@override final  int currentCycleNo;
@override final  String createdByRef;
@override final  DateTime createdAt;
@override final  String updatedByRef;
@override final  DateTime updatedAt;
@override final  int version;

/// Create a copy of RiskCaseDetail
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RiskCaseDetailCopyWith<_RiskCaseDetail> get copyWith => __$RiskCaseDetailCopyWithImpl<_RiskCaseDetail>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$RiskCaseDetailToJson(this, );
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RiskCaseDetail&&(identical(other.caseNumber, caseNumber) || other.caseNumber == caseNumber)&&(identical(other.subjectType, subjectType) || other.subjectType == subjectType)&&(identical(other.subjectRef, subjectRef) || other.subjectRef == subjectRef)&&(identical(other.intakeSource, intakeSource) || other.intakeSource == intakeSource)&&(identical(other.intakeSummary, intakeSummary) || other.intakeSummary == intakeSummary)&&(identical(other.status, status) || other.status == status)&&(identical(other.priority, priority) || other.priority == priority)&&(identical(other.assigneeRef, assigneeRef) || other.assigneeRef == assigneeRef)&&(identical(other.assignedByRef, assignedByRef) || other.assignedByRef == assignedByRef)&&(identical(other.assignedAt, assignedAt) || other.assignedAt == assignedAt)&&(identical(other.currentDecisionRef, currentDecisionRef) || other.currentDecisionRef == currentDecisionRef)&&(identical(other.currentCycleNo, currentCycleNo) || other.currentCycleNo == currentCycleNo)&&(identical(other.createdByRef, createdByRef) || other.createdByRef == createdByRef)&&(identical(other.createdAt, createdAt) || other.createdAt == createdAt)&&(identical(other.updatedByRef, updatedByRef) || other.updatedByRef == updatedByRef)&&(identical(other.updatedAt, updatedAt) || other.updatedAt == updatedAt)&&(identical(other.version, version) || other.version == version));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,caseNumber,subjectType,subjectRef,intakeSource,intakeSummary,status,priority,assigneeRef,assignedByRef,assignedAt,currentDecisionRef,currentCycleNo,createdByRef,createdAt,updatedByRef,updatedAt,version);

@override
String toString() {
  return 'RiskCaseDetail(caseNumber: $caseNumber, subjectType: $subjectType, subjectRef: $subjectRef, intakeSource: $intakeSource, intakeSummary: $intakeSummary, status: $status, priority: $priority, assigneeRef: $assigneeRef, assignedByRef: $assignedByRef, assignedAt: $assignedAt, currentDecisionRef: $currentDecisionRef, currentCycleNo: $currentCycleNo, createdByRef: $createdByRef, createdAt: $createdAt, updatedByRef: $updatedByRef, updatedAt: $updatedAt, version: $version)';
}


}

/// @nodoc
abstract mixin class _$RiskCaseDetailCopyWith<$Res> implements $RiskCaseDetailCopyWith<$Res> {
  factory _$RiskCaseDetailCopyWith(_RiskCaseDetail value, $Res Function(_RiskCaseDetail) _then) = __$RiskCaseDetailCopyWithImpl;
@override @useResult
$Res call({
 String caseNumber, String subjectType, String subjectRef, String intakeSource, String intakeSummary, String status, String priority, String? assigneeRef, String? assignedByRef, DateTime? assignedAt, String? currentDecisionRef, int currentCycleNo, String createdByRef, DateTime createdAt, String updatedByRef, DateTime updatedAt, int version
});




}
/// @nodoc
class __$RiskCaseDetailCopyWithImpl<$Res>
    implements _$RiskCaseDetailCopyWith<$Res> {
  __$RiskCaseDetailCopyWithImpl(this._self, this._then);

  final _RiskCaseDetail _self;
  final $Res Function(_RiskCaseDetail) _then;

/// Create a copy of RiskCaseDetail
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? caseNumber = null,Object? subjectType = null,Object? subjectRef = null,Object? intakeSource = null,Object? intakeSummary = null,Object? status = null,Object? priority = null,Object? assigneeRef = freezed,Object? assignedByRef = freezed,Object? assignedAt = freezed,Object? currentDecisionRef = freezed,Object? currentCycleNo = null,Object? createdByRef = null,Object? createdAt = null,Object? updatedByRef = null,Object? updatedAt = null,Object? version = null,}) {
  return _then(_RiskCaseDetail(
caseNumber: null == caseNumber ? _self.caseNumber : caseNumber // ignore: cast_nullable_to_non_nullable
as String,subjectType: null == subjectType ? _self.subjectType : subjectType // ignore: cast_nullable_to_non_nullable
as String,subjectRef: null == subjectRef ? _self.subjectRef : subjectRef // ignore: cast_nullable_to_non_nullable
as String,intakeSource: null == intakeSource ? _self.intakeSource : intakeSource // ignore: cast_nullable_to_non_nullable
as String,intakeSummary: null == intakeSummary ? _self.intakeSummary : intakeSummary // ignore: cast_nullable_to_non_nullable
as String,status: null == status ? _self.status : status // ignore: cast_nullable_to_non_nullable
as String,priority: null == priority ? _self.priority : priority // ignore: cast_nullable_to_non_nullable
as String,assigneeRef: freezed == assigneeRef ? _self.assigneeRef : assigneeRef // ignore: cast_nullable_to_non_nullable
as String?,assignedByRef: freezed == assignedByRef ? _self.assignedByRef : assignedByRef // ignore: cast_nullable_to_non_nullable
as String?,assignedAt: freezed == assignedAt ? _self.assignedAt : assignedAt // ignore: cast_nullable_to_non_nullable
as DateTime?,currentDecisionRef: freezed == currentDecisionRef ? _self.currentDecisionRef : currentDecisionRef // ignore: cast_nullable_to_non_nullable
as String?,currentCycleNo: null == currentCycleNo ? _self.currentCycleNo : currentCycleNo // ignore: cast_nullable_to_non_nullable
as int,createdByRef: null == createdByRef ? _self.createdByRef : createdByRef // ignore: cast_nullable_to_non_nullable
as String,createdAt: null == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as DateTime,updatedByRef: null == updatedByRef ? _self.updatedByRef : updatedByRef // ignore: cast_nullable_to_non_nullable
as String,updatedAt: null == updatedAt ? _self.updatedAt : updatedAt // ignore: cast_nullable_to_non_nullable
as DateTime,version: null == version ? _self.version : version // ignore: cast_nullable_to_non_nullable
as int,
  ));
}


}


/// @nodoc
mixin _$RiskCaseHistoryEntry {

 int get version; String get eventType; String? get affectedRef; String get actorRef; DateTime get occurredAt;
/// Create a copy of RiskCaseHistoryEntry
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RiskCaseHistoryEntryCopyWith<RiskCaseHistoryEntry> get copyWith => _$RiskCaseHistoryEntryCopyWithImpl<RiskCaseHistoryEntry>(this as RiskCaseHistoryEntry, _$identity);

  /// Serializes this RiskCaseHistoryEntry to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RiskCaseHistoryEntry&&(identical(other.version, version) || other.version == version)&&(identical(other.eventType, eventType) || other.eventType == eventType)&&(identical(other.affectedRef, affectedRef) || other.affectedRef == affectedRef)&&(identical(other.actorRef, actorRef) || other.actorRef == actorRef)&&(identical(other.occurredAt, occurredAt) || other.occurredAt == occurredAt));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,version,eventType,affectedRef,actorRef,occurredAt);

@override
String toString() {
  return 'RiskCaseHistoryEntry(version: $version, eventType: $eventType, affectedRef: $affectedRef, actorRef: $actorRef, occurredAt: $occurredAt)';
}


}

/// @nodoc
abstract mixin class $RiskCaseHistoryEntryCopyWith<$Res>  {
  factory $RiskCaseHistoryEntryCopyWith(RiskCaseHistoryEntry value, $Res Function(RiskCaseHistoryEntry) _then) = _$RiskCaseHistoryEntryCopyWithImpl;
@useResult
$Res call({
 int version, String eventType, String? affectedRef, String actorRef, DateTime occurredAt
});




}
/// @nodoc
class _$RiskCaseHistoryEntryCopyWithImpl<$Res>
    implements $RiskCaseHistoryEntryCopyWith<$Res> {
  _$RiskCaseHistoryEntryCopyWithImpl(this._self, this._then);

  final RiskCaseHistoryEntry _self;
  final $Res Function(RiskCaseHistoryEntry) _then;

/// Create a copy of RiskCaseHistoryEntry
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? version = null,Object? eventType = null,Object? affectedRef = freezed,Object? actorRef = null,Object? occurredAt = null,}) {
  return _then(_self.copyWith(
version: null == version ? _self.version : version // ignore: cast_nullable_to_non_nullable
as int,eventType: null == eventType ? _self.eventType : eventType // ignore: cast_nullable_to_non_nullable
as String,affectedRef: freezed == affectedRef ? _self.affectedRef : affectedRef // ignore: cast_nullable_to_non_nullable
as String?,actorRef: null == actorRef ? _self.actorRef : actorRef // ignore: cast_nullable_to_non_nullable
as String,occurredAt: null == occurredAt ? _self.occurredAt : occurredAt // ignore: cast_nullable_to_non_nullable
as DateTime,
  ));
}

}


/// Adds pattern-matching-related methods to [RiskCaseHistoryEntry].
extension RiskCaseHistoryEntryPatterns on RiskCaseHistoryEntry {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RiskCaseHistoryEntry value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RiskCaseHistoryEntry() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RiskCaseHistoryEntry value)  $default,){
final _that = this;
switch (_that) {
case _RiskCaseHistoryEntry():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RiskCaseHistoryEntry value)?  $default,){
final _that = this;
switch (_that) {
case _RiskCaseHistoryEntry() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int version,  String eventType,  String? affectedRef,  String actorRef,  DateTime occurredAt)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RiskCaseHistoryEntry() when $default != null:
return $default(_that.version,_that.eventType,_that.affectedRef,_that.actorRef,_that.occurredAt);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int version,  String eventType,  String? affectedRef,  String actorRef,  DateTime occurredAt)  $default,) {final _that = this;
switch (_that) {
case _RiskCaseHistoryEntry():
return $default(_that.version,_that.eventType,_that.affectedRef,_that.actorRef,_that.occurredAt);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int version,  String eventType,  String? affectedRef,  String actorRef,  DateTime occurredAt)?  $default,) {final _that = this;
switch (_that) {
case _RiskCaseHistoryEntry() when $default != null:
return $default(_that.version,_that.eventType,_that.affectedRef,_that.actorRef,_that.occurredAt);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _RiskCaseHistoryEntry implements RiskCaseHistoryEntry {
  const _RiskCaseHistoryEntry({required this.version, required this.eventType, this.affectedRef, required this.actorRef, required this.occurredAt});
  factory _RiskCaseHistoryEntry.fromJson(Map<String, dynamic> json) => _$RiskCaseHistoryEntryFromJson(json);

@override final  int version;
@override final  String eventType;
@override final  String? affectedRef;
@override final  String actorRef;
@override final  DateTime occurredAt;

/// Create a copy of RiskCaseHistoryEntry
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RiskCaseHistoryEntryCopyWith<_RiskCaseHistoryEntry> get copyWith => __$RiskCaseHistoryEntryCopyWithImpl<_RiskCaseHistoryEntry>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$RiskCaseHistoryEntryToJson(this, );
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RiskCaseHistoryEntry&&(identical(other.version, version) || other.version == version)&&(identical(other.eventType, eventType) || other.eventType == eventType)&&(identical(other.affectedRef, affectedRef) || other.affectedRef == affectedRef)&&(identical(other.actorRef, actorRef) || other.actorRef == actorRef)&&(identical(other.occurredAt, occurredAt) || other.occurredAt == occurredAt));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,version,eventType,affectedRef,actorRef,occurredAt);

@override
String toString() {
  return 'RiskCaseHistoryEntry(version: $version, eventType: $eventType, affectedRef: $affectedRef, actorRef: $actorRef, occurredAt: $occurredAt)';
}


}

/// @nodoc
abstract mixin class _$RiskCaseHistoryEntryCopyWith<$Res> implements $RiskCaseHistoryEntryCopyWith<$Res> {
  factory _$RiskCaseHistoryEntryCopyWith(_RiskCaseHistoryEntry value, $Res Function(_RiskCaseHistoryEntry) _then) = __$RiskCaseHistoryEntryCopyWithImpl;
@override @useResult
$Res call({
 int version, String eventType, String? affectedRef, String actorRef, DateTime occurredAt
});




}
/// @nodoc
class __$RiskCaseHistoryEntryCopyWithImpl<$Res>
    implements _$RiskCaseHistoryEntryCopyWith<$Res> {
  __$RiskCaseHistoryEntryCopyWithImpl(this._self, this._then);

  final _RiskCaseHistoryEntry _self;
  final $Res Function(_RiskCaseHistoryEntry) _then;

/// Create a copy of RiskCaseHistoryEntry
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? version = null,Object? eventType = null,Object? affectedRef = freezed,Object? actorRef = null,Object? occurredAt = null,}) {
  return _then(_RiskCaseHistoryEntry(
version: null == version ? _self.version : version // ignore: cast_nullable_to_non_nullable
as int,eventType: null == eventType ? _self.eventType : eventType // ignore: cast_nullable_to_non_nullable
as String,affectedRef: freezed == affectedRef ? _self.affectedRef : affectedRef // ignore: cast_nullable_to_non_nullable
as String?,actorRef: null == actorRef ? _self.actorRef : actorRef // ignore: cast_nullable_to_non_nullable
as String,occurredAt: null == occurredAt ? _self.occurredAt : occurredAt // ignore: cast_nullable_to_non_nullable
as DateTime,
  ));
}


}


/// @nodoc
mixin _$RiskCaseHistoryPage {

 List<RiskCaseHistoryEntry> get entries; String? get nextCursor;
/// Create a copy of RiskCaseHistoryPage
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RiskCaseHistoryPageCopyWith<RiskCaseHistoryPage> get copyWith => _$RiskCaseHistoryPageCopyWithImpl<RiskCaseHistoryPage>(this as RiskCaseHistoryPage, _$identity);

  /// Serializes this RiskCaseHistoryPage to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RiskCaseHistoryPage&&const DeepCollectionEquality().equals(other.entries, entries)&&(identical(other.nextCursor, nextCursor) || other.nextCursor == nextCursor));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(entries),nextCursor);

@override
String toString() {
  return 'RiskCaseHistoryPage(entries: $entries, nextCursor: $nextCursor)';
}


}

/// @nodoc
abstract mixin class $RiskCaseHistoryPageCopyWith<$Res>  {
  factory $RiskCaseHistoryPageCopyWith(RiskCaseHistoryPage value, $Res Function(RiskCaseHistoryPage) _then) = _$RiskCaseHistoryPageCopyWithImpl;
@useResult
$Res call({
 List<RiskCaseHistoryEntry> entries, String? nextCursor
});




}
/// @nodoc
class _$RiskCaseHistoryPageCopyWithImpl<$Res>
    implements $RiskCaseHistoryPageCopyWith<$Res> {
  _$RiskCaseHistoryPageCopyWithImpl(this._self, this._then);

  final RiskCaseHistoryPage _self;
  final $Res Function(RiskCaseHistoryPage) _then;

/// Create a copy of RiskCaseHistoryPage
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? entries = null,Object? nextCursor = freezed,}) {
  return _then(_self.copyWith(
entries: null == entries ? _self.entries : entries // ignore: cast_nullable_to_non_nullable
as List<RiskCaseHistoryEntry>,nextCursor: freezed == nextCursor ? _self.nextCursor : nextCursor // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}

}


/// Adds pattern-matching-related methods to [RiskCaseHistoryPage].
extension RiskCaseHistoryPagePatterns on RiskCaseHistoryPage {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RiskCaseHistoryPage value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RiskCaseHistoryPage() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RiskCaseHistoryPage value)  $default,){
final _that = this;
switch (_that) {
case _RiskCaseHistoryPage():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RiskCaseHistoryPage value)?  $default,){
final _that = this;
switch (_that) {
case _RiskCaseHistoryPage() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( List<RiskCaseHistoryEntry> entries,  String? nextCursor)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RiskCaseHistoryPage() when $default != null:
return $default(_that.entries,_that.nextCursor);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( List<RiskCaseHistoryEntry> entries,  String? nextCursor)  $default,) {final _that = this;
switch (_that) {
case _RiskCaseHistoryPage():
return $default(_that.entries,_that.nextCursor);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( List<RiskCaseHistoryEntry> entries,  String? nextCursor)?  $default,) {final _that = this;
switch (_that) {
case _RiskCaseHistoryPage() when $default != null:
return $default(_that.entries,_that.nextCursor);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _RiskCaseHistoryPage implements RiskCaseHistoryPage {
  const _RiskCaseHistoryPage({required final  List<RiskCaseHistoryEntry> entries, this.nextCursor}): _entries = entries;
  factory _RiskCaseHistoryPage.fromJson(Map<String, dynamic> json) => _$RiskCaseHistoryPageFromJson(json);

 final  List<RiskCaseHistoryEntry> _entries;
@override List<RiskCaseHistoryEntry> get entries {
  if (_entries is EqualUnmodifiableListView) return _entries;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_entries);
}

@override final  String? nextCursor;

/// Create a copy of RiskCaseHistoryPage
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RiskCaseHistoryPageCopyWith<_RiskCaseHistoryPage> get copyWith => __$RiskCaseHistoryPageCopyWithImpl<_RiskCaseHistoryPage>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$RiskCaseHistoryPageToJson(this, );
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RiskCaseHistoryPage&&const DeepCollectionEquality().equals(other._entries, _entries)&&(identical(other.nextCursor, nextCursor) || other.nextCursor == nextCursor));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,const DeepCollectionEquality().hash(_entries),nextCursor);

@override
String toString() {
  return 'RiskCaseHistoryPage(entries: $entries, nextCursor: $nextCursor)';
}


}

/// @nodoc
abstract mixin class _$RiskCaseHistoryPageCopyWith<$Res> implements $RiskCaseHistoryPageCopyWith<$Res> {
  factory _$RiskCaseHistoryPageCopyWith(_RiskCaseHistoryPage value, $Res Function(_RiskCaseHistoryPage) _then) = __$RiskCaseHistoryPageCopyWithImpl;
@override @useResult
$Res call({
 List<RiskCaseHistoryEntry> entries, String? nextCursor
});




}
/// @nodoc
class __$RiskCaseHistoryPageCopyWithImpl<$Res>
    implements _$RiskCaseHistoryPageCopyWith<$Res> {
  __$RiskCaseHistoryPageCopyWithImpl(this._self, this._then);

  final _RiskCaseHistoryPage _self;
  final $Res Function(_RiskCaseHistoryPage) _then;

/// Create a copy of RiskCaseHistoryPage
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? entries = null,Object? nextCursor = freezed,}) {
  return _then(_RiskCaseHistoryPage(
entries: null == entries ? _self._entries : entries // ignore: cast_nullable_to_non_nullable
as List<RiskCaseHistoryEntry>,nextCursor: freezed == nextCursor ? _self.nextCursor : nextCursor // ignore: cast_nullable_to_non_nullable
as String?,
  ));
}


}


/// @nodoc
mixin _$RiskCaseNote {

 String get noteRef; String? get supersedesNoteRef; int get version; String get createdByRef; DateTime get createdAt;
/// Create a copy of RiskCaseNote
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RiskCaseNoteCopyWith<RiskCaseNote> get copyWith => _$RiskCaseNoteCopyWithImpl<RiskCaseNote>(this as RiskCaseNote, _$identity);

  /// Serializes this RiskCaseNote to a JSON map.
  Map<String, dynamic> toJson();


@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RiskCaseNote&&(identical(other.noteRef, noteRef) || other.noteRef == noteRef)&&(identical(other.supersedesNoteRef, supersedesNoteRef) || other.supersedesNoteRef == supersedesNoteRef)&&(identical(other.version, version) || other.version == version)&&(identical(other.createdByRef, createdByRef) || other.createdByRef == createdByRef)&&(identical(other.createdAt, createdAt) || other.createdAt == createdAt));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,noteRef,supersedesNoteRef,version,createdByRef,createdAt);

@override
String toString() {
  return 'RiskCaseNote(noteRef: $noteRef, supersedesNoteRef: $supersedesNoteRef, version: $version, createdByRef: $createdByRef, createdAt: $createdAt)';
}


}

/// @nodoc
abstract mixin class $RiskCaseNoteCopyWith<$Res>  {
  factory $RiskCaseNoteCopyWith(RiskCaseNote value, $Res Function(RiskCaseNote) _then) = _$RiskCaseNoteCopyWithImpl;
@useResult
$Res call({
 String noteRef, String? supersedesNoteRef, int version, String createdByRef, DateTime createdAt
});




}
/// @nodoc
class _$RiskCaseNoteCopyWithImpl<$Res>
    implements $RiskCaseNoteCopyWith<$Res> {
  _$RiskCaseNoteCopyWithImpl(this._self, this._then);

  final RiskCaseNote _self;
  final $Res Function(RiskCaseNote) _then;

/// Create a copy of RiskCaseNote
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? noteRef = null,Object? supersedesNoteRef = freezed,Object? version = null,Object? createdByRef = null,Object? createdAt = null,}) {
  return _then(_self.copyWith(
noteRef: null == noteRef ? _self.noteRef : noteRef // ignore: cast_nullable_to_non_nullable
as String,supersedesNoteRef: freezed == supersedesNoteRef ? _self.supersedesNoteRef : supersedesNoteRef // ignore: cast_nullable_to_non_nullable
as String?,version: null == version ? _self.version : version // ignore: cast_nullable_to_non_nullable
as int,createdByRef: null == createdByRef ? _self.createdByRef : createdByRef // ignore: cast_nullable_to_non_nullable
as String,createdAt: null == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as DateTime,
  ));
}

}


/// Adds pattern-matching-related methods to [RiskCaseNote].
extension RiskCaseNotePatterns on RiskCaseNote {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RiskCaseNote value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RiskCaseNote() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RiskCaseNote value)  $default,){
final _that = this;
switch (_that) {
case _RiskCaseNote():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RiskCaseNote value)?  $default,){
final _that = this;
switch (_that) {
case _RiskCaseNote() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( String noteRef,  String? supersedesNoteRef,  int version,  String createdByRef,  DateTime createdAt)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RiskCaseNote() when $default != null:
return $default(_that.noteRef,_that.supersedesNoteRef,_that.version,_that.createdByRef,_that.createdAt);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( String noteRef,  String? supersedesNoteRef,  int version,  String createdByRef,  DateTime createdAt)  $default,) {final _that = this;
switch (_that) {
case _RiskCaseNote():
return $default(_that.noteRef,_that.supersedesNoteRef,_that.version,_that.createdByRef,_that.createdAt);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( String noteRef,  String? supersedesNoteRef,  int version,  String createdByRef,  DateTime createdAt)?  $default,) {final _that = this;
switch (_that) {
case _RiskCaseNote() when $default != null:
return $default(_that.noteRef,_that.supersedesNoteRef,_that.version,_that.createdByRef,_that.createdAt);case _:
  return null;

}
}

}

/// @nodoc
@JsonSerializable()

class _RiskCaseNote implements RiskCaseNote {
  const _RiskCaseNote({required this.noteRef, this.supersedesNoteRef, required this.version, required this.createdByRef, required this.createdAt});
  factory _RiskCaseNote.fromJson(Map<String, dynamic> json) => _$RiskCaseNoteFromJson(json);

@override final  String noteRef;
@override final  String? supersedesNoteRef;
@override final  int version;
@override final  String createdByRef;
@override final  DateTime createdAt;

/// Create a copy of RiskCaseNote
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RiskCaseNoteCopyWith<_RiskCaseNote> get copyWith => __$RiskCaseNoteCopyWithImpl<_RiskCaseNote>(this, _$identity);

@override
Map<String, dynamic> toJson() {
  return _$RiskCaseNoteToJson(this, );
}

@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RiskCaseNote&&(identical(other.noteRef, noteRef) || other.noteRef == noteRef)&&(identical(other.supersedesNoteRef, supersedesNoteRef) || other.supersedesNoteRef == supersedesNoteRef)&&(identical(other.version, version) || other.version == version)&&(identical(other.createdByRef, createdByRef) || other.createdByRef == createdByRef)&&(identical(other.createdAt, createdAt) || other.createdAt == createdAt));
}

@JsonKey(includeFromJson: false, includeToJson: false)
@override
int get hashCode => Object.hash(runtimeType,noteRef,supersedesNoteRef,version,createdByRef,createdAt);

@override
String toString() {
  return 'RiskCaseNote(noteRef: $noteRef, supersedesNoteRef: $supersedesNoteRef, version: $version, createdByRef: $createdByRef, createdAt: $createdAt)';
}


}

/// @nodoc
abstract mixin class _$RiskCaseNoteCopyWith<$Res> implements $RiskCaseNoteCopyWith<$Res> {
  factory _$RiskCaseNoteCopyWith(_RiskCaseNote value, $Res Function(_RiskCaseNote) _then) = __$RiskCaseNoteCopyWithImpl;
@override @useResult
$Res call({
 String noteRef, String? supersedesNoteRef, int version, String createdByRef, DateTime createdAt
});




}
/// @nodoc
class __$RiskCaseNoteCopyWithImpl<$Res>
    implements _$RiskCaseNoteCopyWith<$Res> {
  __$RiskCaseNoteCopyWithImpl(this._self, this._then);

  final _RiskCaseNote _self;
  final $Res Function(_RiskCaseNote) _then;

/// Create a copy of RiskCaseNote
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? noteRef = null,Object? supersedesNoteRef = freezed,Object? version = null,Object? createdByRef = null,Object? createdAt = null,}) {
  return _then(_RiskCaseNote(
noteRef: null == noteRef ? _self.noteRef : noteRef // ignore: cast_nullable_to_non_nullable
as String,supersedesNoteRef: freezed == supersedesNoteRef ? _self.supersedesNoteRef : supersedesNoteRef // ignore: cast_nullable_to_non_nullable
as String?,version: null == version ? _self.version : version // ignore: cast_nullable_to_non_nullable
as int,createdByRef: null == createdByRef ? _self.createdByRef : createdByRef // ignore: cast_nullable_to_non_nullable
as String,createdAt: null == createdAt ? _self.createdAt : createdAt // ignore: cast_nullable_to_non_nullable
as DateTime,
  ));
}


}

/// @nodoc
mixin _$RiskCaseView {

 RiskCaseDetail get detail; RiskCaseHistoryPage get history;
/// Create a copy of RiskCaseView
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$RiskCaseViewCopyWith<RiskCaseView> get copyWith => _$RiskCaseViewCopyWithImpl<RiskCaseView>(this as RiskCaseView, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is RiskCaseView&&(identical(other.detail, detail) || other.detail == detail)&&(identical(other.history, history) || other.history == history));
}


@override
int get hashCode => Object.hash(runtimeType,detail,history);

@override
String toString() {
  return 'RiskCaseView(detail: $detail, history: $history)';
}


}

/// @nodoc
abstract mixin class $RiskCaseViewCopyWith<$Res>  {
  factory $RiskCaseViewCopyWith(RiskCaseView value, $Res Function(RiskCaseView) _then) = _$RiskCaseViewCopyWithImpl;
@useResult
$Res call({
 RiskCaseDetail detail, RiskCaseHistoryPage history
});


$RiskCaseDetailCopyWith<$Res> get detail;$RiskCaseHistoryPageCopyWith<$Res> get history;

}
/// @nodoc
class _$RiskCaseViewCopyWithImpl<$Res>
    implements $RiskCaseViewCopyWith<$Res> {
  _$RiskCaseViewCopyWithImpl(this._self, this._then);

  final RiskCaseView _self;
  final $Res Function(RiskCaseView) _then;

/// Create a copy of RiskCaseView
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? detail = null,Object? history = null,}) {
  return _then(_self.copyWith(
detail: null == detail ? _self.detail : detail // ignore: cast_nullable_to_non_nullable
as RiskCaseDetail,history: null == history ? _self.history : history // ignore: cast_nullable_to_non_nullable
as RiskCaseHistoryPage,
  ));
}
/// Create a copy of RiskCaseView
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$RiskCaseDetailCopyWith<$Res> get detail {
  
  return $RiskCaseDetailCopyWith<$Res>(_self.detail, (value) {
    return _then(_self.copyWith(detail: value));
  });
}/// Create a copy of RiskCaseView
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$RiskCaseHistoryPageCopyWith<$Res> get history {
  
  return $RiskCaseHistoryPageCopyWith<$Res>(_self.history, (value) {
    return _then(_self.copyWith(history: value));
  });
}
}


/// Adds pattern-matching-related methods to [RiskCaseView].
extension RiskCaseViewPatterns on RiskCaseView {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _RiskCaseView value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _RiskCaseView() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _RiskCaseView value)  $default,){
final _that = this;
switch (_that) {
case _RiskCaseView():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _RiskCaseView value)?  $default,){
final _that = this;
switch (_that) {
case _RiskCaseView() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( RiskCaseDetail detail,  RiskCaseHistoryPage history)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _RiskCaseView() when $default != null:
return $default(_that.detail,_that.history);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( RiskCaseDetail detail,  RiskCaseHistoryPage history)  $default,) {final _that = this;
switch (_that) {
case _RiskCaseView():
return $default(_that.detail,_that.history);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( RiskCaseDetail detail,  RiskCaseHistoryPage history)?  $default,) {final _that = this;
switch (_that) {
case _RiskCaseView() when $default != null:
return $default(_that.detail,_that.history);case _:
  return null;

}
}

}

/// @nodoc


class _RiskCaseView implements RiskCaseView {
  const _RiskCaseView({required this.detail, required this.history});
  

@override final  RiskCaseDetail detail;
@override final  RiskCaseHistoryPage history;

/// Create a copy of RiskCaseView
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$RiskCaseViewCopyWith<_RiskCaseView> get copyWith => __$RiskCaseViewCopyWithImpl<_RiskCaseView>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _RiskCaseView&&(identical(other.detail, detail) || other.detail == detail)&&(identical(other.history, history) || other.history == history));
}


@override
int get hashCode => Object.hash(runtimeType,detail,history);

@override
String toString() {
  return 'RiskCaseView(detail: $detail, history: $history)';
}


}

/// @nodoc
abstract mixin class _$RiskCaseViewCopyWith<$Res> implements $RiskCaseViewCopyWith<$Res> {
  factory _$RiskCaseViewCopyWith(_RiskCaseView value, $Res Function(_RiskCaseView) _then) = __$RiskCaseViewCopyWithImpl;
@override @useResult
$Res call({
 RiskCaseDetail detail, RiskCaseHistoryPage history
});


@override $RiskCaseDetailCopyWith<$Res> get detail;@override $RiskCaseHistoryPageCopyWith<$Res> get history;

}
/// @nodoc
class __$RiskCaseViewCopyWithImpl<$Res>
    implements _$RiskCaseViewCopyWith<$Res> {
  __$RiskCaseViewCopyWithImpl(this._self, this._then);

  final _RiskCaseView _self;
  final $Res Function(_RiskCaseView) _then;

/// Create a copy of RiskCaseView
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? detail = null,Object? history = null,}) {
  return _then(_RiskCaseView(
detail: null == detail ? _self.detail : detail // ignore: cast_nullable_to_non_nullable
as RiskCaseDetail,history: null == history ? _self.history : history // ignore: cast_nullable_to_non_nullable
as RiskCaseHistoryPage,
  ));
}

/// Create a copy of RiskCaseView
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$RiskCaseDetailCopyWith<$Res> get detail {
  
  return $RiskCaseDetailCopyWith<$Res>(_self.detail, (value) {
    return _then(_self.copyWith(detail: value));
  });
}/// Create a copy of RiskCaseView
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$RiskCaseHistoryPageCopyWith<$Res> get history {
  
  return $RiskCaseHistoryPageCopyWith<$Res>(_self.history, (value) {
    return _then(_self.copyWith(history: value));
  });
}
}

// dart format on
