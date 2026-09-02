import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/api/api_contract.dart';
import '../../../shared/async_views.dart';
import '../application/risk_case_notifiers.dart';
import '../data/risk_case_models.dart';
import 'add_note_dialog.dart';

class RiskCaseDetailPage extends ConsumerWidget {
  const RiskCaseDetailPage({required this.caseNumber, super.key});

  final String caseNumber;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(riskCaseDetailProvider(caseNumber));
    final operation = ref.watch(riskCaseOperationProvider(caseNumber));
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          tooltip: 'Back to risk cases',
          onPressed: () => context.go('/cases'),
          icon: const Icon(Icons.arrow_back),
        ),
        title: Text(caseNumber),
        actions: [
          IconButton(
            tooltip: 'Reload case',
            onPressed: () => ref.read(riskCaseDetailProvider(caseNumber).notifier).load(),
            icon: const Icon(Icons.refresh),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 1180),
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: RiskCaseDetailBody(
                state: state,
                operationMessage: operation.message,
                onRetry: () =>
                    ref.read(riskCaseDetailProvider(caseNumber).notifier).load(),
                onAddNote: (version) async {
                  ref.read(riskCaseOperationProvider(caseNumber).notifier).clearMessage();
                  await showDialog<void>(
                    context: context,
                    barrierDismissible: !operation.isSubmitting,
                    builder: (dialogContext) {
                      return Consumer(builder: (context, ref, child) {
                        final currentOperation =
                            ref.watch(riskCaseOperationProvider(caseNumber));
                        return AddNoteDialog(
                          expectedVersion: version,
                          isSubmitting: currentOperation.isSubmitting,
                          errorMessage: currentOperation.isError
                              ? currentOperation.message
                              : null,
                          onSubmit: (content, expectedVersion) => ref
                              .read(riskCaseOperationProvider(caseNumber).notifier)
                              .addNote(
                                content: content,
                                expectedVersion: expectedVersion,
                              ),
                        );
                      });
                    },
                  );
                },
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class RiskCaseDetailBody extends StatelessWidget {
  const RiskCaseDetailBody({
    required this.state,
    required this.onRetry,
    required this.onAddNote,
    this.operationMessage,
    super.key,
  });

  final AsyncValue<RiskCaseView> state;
  final VoidCallback onRetry;
  final ValueChanged<int> onAddNote;
  final String? operationMessage;

  @override
  Widget build(BuildContext context) {
    return state.when(
      loading: () => const LoadingView(label: 'Loading risk case detail'),
      error: (error, stackTrace) => ErrorView(
        message: error is ApiFailure ? error.message : 'Risk case detail could not be loaded.',
        onRetry: onRetry,
      ),
      data: (view) {
        final detail = view.detail;
        final associationEntries = view.history.entries
            .where((entry) => entry.affectedRef != null)
            .toList(growable: false);
        return ListView(
          key: const Key('risk-case-detail'),
          children: [
            if (operationMessage != null) ...[
              MaterialBanner(
                content: Text(operationMessage!),
                actions: const [SizedBox.shrink()],
              ),
              const SizedBox(height: 16),
            ],
            Wrap(
              alignment: WrapAlignment.spaceBetween,
              crossAxisAlignment: WrapCrossAlignment.center,
              runSpacing: 12,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      detail.caseNumber,
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    const SizedBox(height: 4),
                    Text('${detail.status.replaceAll('_', ' ')} • ${detail.priority}'),
                  ],
                ),
                FilledButton.icon(
                  key: const Key('open-add-note'),
                  onPressed: () => onAddNote(detail.version),
                  icon: const Icon(Icons.note_add_outlined),
                  label: const Text('Add note'),
                ),
              ],
            ),
            const SizedBox(height: 20),
            _SectionCard(
              title: 'Case detail',
              child: Wrap(
                spacing: 32,
                runSpacing: 18,
                children: [
                  _Field(label: 'Subject', value: detail.subjectRef),
                  _Field(label: 'Assignee', value: detail.assigneeRef ?? 'Unassigned'),
                  _Field(label: 'Cycle', value: '${detail.currentCycleNo}'),
                  _Field(label: 'Version', value: '${detail.version}'),
                  _Field(label: 'Intake', value: detail.intakeSource),
                  _Field(
                    label: 'Current decision',
                    value: detail.currentDecisionRef ?? 'None',
                  ),
                  _Field(label: 'Updated', value: _formatInstant(detail.updatedAt)),
                ],
              ),
            ),
            const SizedBox(height: 16),
            _SectionCard(
              title: 'Intake summary',
              child: Text(detail.intakeSummary),
            ),
            const SizedBox(height: 16),
            _SectionCard(
              title: 'Association references in history',
              child: associationEntries.isEmpty
                  ? const Text('No association reference events recorded.')
                  : Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: associationEntries
                          .map((entry) => Chip(
                                label: Text('${entry.eventType}: ${entry.affectedRef}'),
                              ))
                          .toList(growable: false),
                    ),
            ),
            const SizedBox(height: 16),
            _SectionCard(
              title: 'History timeline',
              child: view.history.entries.isEmpty
                  ? const Text('No history entries recorded.')
                  : Column(
                      children: view.history.entries
                          .map((entry) => ListTile(
                                leading: CircleAvatar(child: Text('${entry.version}')),
                                title: Text(entry.eventType.replaceAll('_', ' ')),
                                subtitle: Text(
                                  '${entry.affectedRef ?? 'Case'}\n'
                                  '${_formatInstant(entry.occurredAt)} • ${entry.actorRef}',
                                ),
                              ))
                          .toList(growable: false),
                    ),
            ),
          ],
        );
      },
    );
  }
}

class _SectionCard extends StatelessWidget {
  const _SectionCard({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(title, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 14),
            child,
          ],
        ),
      ),
    );
  }
}

class _Field extends StatelessWidget {
  const _Field({required this.label, required this.value});

  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      width: 240,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: Theme.of(context).textTheme.labelMedium),
          const SizedBox(height: 4),
          SelectableText(value),
        ],
      ),
    );
  }
}

String _formatInstant(DateTime value) => value.toUtc().toIso8601String();
