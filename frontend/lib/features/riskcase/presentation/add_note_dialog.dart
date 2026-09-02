import 'package:flutter/material.dart';

class AddNoteDialog extends StatefulWidget {
  const AddNoteDialog({
    required this.expectedVersion,
    required this.isSubmitting,
    required this.onSubmit,
    this.errorMessage,
    super.key,
  });

  final int expectedVersion;
  final bool isSubmitting;
  final String? errorMessage;
  final Future<bool> Function(String content, int expectedVersion) onSubmit;

  @override
  State<AddNoteDialog> createState() => _AddNoteDialogState();
}

class _AddNoteDialogState extends State<AddNoteDialog> {
  final _controller = TextEditingController();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: const Text('Add investigation note'),
      content: SizedBox(
        width: 520,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextField(
              key: const Key('note-content'),
              controller: _controller,
              autofocus: true,
              minLines: 4,
              maxLines: 8,
              maxLength: 4000,
              decoration: const InputDecoration(
                labelText: 'Investigation note',
                hintText: 'Record an objective investigation observation',
              ),
            ),
            if (widget.errorMessage != null)
              Text(
                widget.errorMessage!,
                key: const Key('operation-error'),
                style: TextStyle(color: Theme.of(context).colorScheme.error),
              ),
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: widget.isSubmitting ? null : () => Navigator.pop(context),
          child: const Text('Cancel'),
        ),
        FilledButton(
          key: const Key('submit-note'),
          onPressed: widget.isSubmitting
              ? null
              : () async {
                  final content = _controller.text.trim();
                  if (content.isEmpty) {
                    return;
                  }
                  final success = await widget.onSubmit(
                    content,
                    widget.expectedVersion,
                  );
                  if (success && context.mounted) {
                    Navigator.pop(context);
                  }
                },
          child: widget.isSubmitting
              ? const SizedBox.square(
                  dimension: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Text('Add note'),
        ),
      ],
    );
  }
}
