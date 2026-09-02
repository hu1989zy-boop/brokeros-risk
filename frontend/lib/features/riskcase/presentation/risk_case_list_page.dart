import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../core/api/api_contract.dart';
import '../../../core/auth/auth_controller.dart';
import '../../../shared/async_views.dart';
import '../application/risk_case_notifiers.dart';
import '../data/risk_case_models.dart';

class RiskCaseListPage extends ConsumerStatefulWidget {
  const RiskCaseListPage({super.key});

  @override
  ConsumerState<RiskCaseListPage> createState() => _RiskCaseListPageState();
}

class _RiskCaseListPageState extends ConsumerState<RiskCaseListPage> {
  String? _status;
  String? _priority;

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(riskCaseListProvider);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Risk Cases'),
        actions: [
          IconButton(
            tooltip: 'Sign out',
            onPressed: () => ref.read(authControllerProvider.notifier).logout(),
            icon: const Icon(Icons.logout),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 1180),
            child: Padding(
              padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Wrap(
                    spacing: 12,
                    runSpacing: 12,
                    children: [
                      SizedBox(
                        width: 220,
                        child: DropdownButtonFormField<String?>(
                          key: const Key('status-filter'),
                          initialValue: _status,
                          decoration: const InputDecoration(labelText: 'Status'),
                          items: const [
                            DropdownMenuItem(value: null, child: Text('All statuses')),
                            DropdownMenuItem(value: 'OPEN', child: Text('Open')),
                            DropdownMenuItem(
                              value: 'IN_REVIEW',
                              child: Text('In review'),
                            ),
                            DropdownMenuItem(
                              value: 'ACTION_REQUIRED',
                              child: Text('Action required'),
                            ),
                            DropdownMenuItem(value: 'RESOLVED', child: Text('Resolved')),
                            DropdownMenuItem(value: 'CLOSED', child: Text('Closed')),
                            DropdownMenuItem(
                              value: 'CANCELLED',
                              child: Text('Cancelled'),
                            ),
                          ],
                          onChanged: (value) => setState(() => _status = value),
                        ),
                      ),
                      SizedBox(
                        width: 220,
                        child: DropdownButtonFormField<String?>(
                          key: const Key('priority-filter'),
                          initialValue: _priority,
                          decoration: const InputDecoration(labelText: 'Priority'),
                          items: const [
                            DropdownMenuItem(value: null, child: Text('All priorities')),
                            DropdownMenuItem(value: 'LOW', child: Text('Low')),
                            DropdownMenuItem(value: 'NORMAL', child: Text('Normal')),
                            DropdownMenuItem(value: 'HIGH', child: Text('High')),
                            DropdownMenuItem(
                              value: 'CRITICAL',
                              child: Text('Critical'),
                            ),
                          ],
                          onChanged: (value) => setState(() => _priority = value),
                        ),
                      ),
                      FilledButton.icon(
                        onPressed: () => ref
                            .read(riskCaseListProvider.notifier)
                            .applyFilters(status: _status, priority: _priority),
                        icon: const Icon(Icons.filter_alt_outlined),
                        label: const Text('Apply filters'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),
                  Expanded(
                    child: RiskCaseListBody(
                      state: state,
                      onRetry: () => ref.read(riskCaseListProvider.notifier).load(),
                      onPage: (page) =>
                          ref.read(riskCaseListProvider.notifier).load(page: page),
                      onOpen: (caseNumber) => context.go('/cases/$caseNumber'),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class RiskCaseListBody extends StatelessWidget {
  const RiskCaseListBody({
    required this.state,
    required this.onRetry,
    required this.onPage,
    required this.onOpen,
    super.key,
  });

  final AsyncValue<RiskCaseSummaryPage> state;
  final VoidCallback onRetry;
  final ValueChanged<int> onPage;
  final ValueChanged<String> onOpen;

  @override
  Widget build(BuildContext context) {
    return state.when(
      loading: () => const LoadingView(label: 'Loading risk cases'),
      error: (error, stackTrace) => ErrorView(
        message: error is ApiFailure ? error.message : 'Risk cases could not be loaded.',
        onRetry: onRetry,
      ),
      data: (page) {
        if (page.items.isEmpty) {
          return const EmptyView(message: 'No risk cases match these filters.');
        }
        return Column(
          children: [
            Expanded(
              child: ListView.separated(
                key: const Key('risk-case-list'),
                itemCount: page.items.length,
                separatorBuilder: (context, index) => const SizedBox(height: 10),
                itemBuilder: (context, index) {
                  final item = page.items[index];
                  return Card(
                    child: ListTile(
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 20,
                        vertical: 10,
                      ),
                      title: Text(
                        item.caseNumber,
                        style: const TextStyle(fontWeight: FontWeight.w700),
                      ),
                      subtitle: Padding(
                        padding: const EdgeInsets.only(top: 6),
                        child: Text(
                          '${item.subjectRef}  •  ${item.assigneeRef ?? 'Unassigned'}\n'
                          'Updated ${_formatInstant(item.updatedAt)}',
                        ),
                      ),
                      trailing: Wrap(
                        spacing: 8,
                        crossAxisAlignment: WrapCrossAlignment.center,
                        children: [
                          Chip(label: Text(item.priority)),
                          _StatusChip(status: item.status),
                          const Icon(Icons.chevron_right),
                        ],
                      ),
                      onTap: () => onOpen(item.caseNumber),
                    ),
                  );
                },
              ),
            ),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                Text('Page ${page.page + 1}'),
                const SizedBox(width: 12),
                IconButton(
                  tooltip: 'Previous page',
                  onPressed: page.page == 0 ? null : () => onPage(page.page - 1),
                  icon: const Icon(Icons.chevron_left),
                ),
                IconButton(
                  tooltip: 'Next page',
                  onPressed: page.hasNext ? () => onPage(page.page + 1) : null,
                  icon: const Icon(Icons.chevron_right),
                ),
              ],
            ),
          ],
        );
      },
    );
  }
}

class _StatusChip extends StatelessWidget {
  const _StatusChip({required this.status});

  final String status;

  @override
  Widget build(BuildContext context) {
    final color = switch (status) {
      'ACTION_REQUIRED' => Colors.orange,
      'RESOLVED' || 'CLOSED' => Colors.green,
      'CANCELLED' => Colors.grey,
      _ => Theme.of(context).colorScheme.primary,
    };
    return Chip(
      avatar: Icon(Icons.circle, size: 10, color: color),
      label: Text(status.replaceAll('_', ' ')),
    );
  }
}

String _formatInstant(DateTime value) => value.toUtc().toIso8601String();
