import { Alert, Card, Descriptions, Input, Spin, Typography } from 'antd';
import { useEffect, useRef } from 'react';

import { formatInstant, humanize } from '../../../shared/format';
import type { ReferenceKind } from '../actions/actionInputs';
import type { ReferencePreview } from '../api/referencePreview';
import { useReferencePreview } from '../model/useReferencePreview';

export function ReferenceInput({
  kind,
  value,
  id,
  disabled = false,
  required = false,
  onChange,
  onConfirmationChange,
}: {
  kind: ReferenceKind;
  value?: string;
  id?: string;
  disabled?: boolean;
  required?: boolean;
  onChange?: (value: string) => void;
  onConfirmationChange?: (confirmed: boolean) => void;
}) {
  const state = useReferencePreview(kind, value);
  const confirmed = state.status === 'valid' || (!required && !value?.trim());
  const reportedConfirmation = useRef<boolean | null>(null);

  useEffect(() => {
    if (reportedConfirmation.current !== confirmed) {
      reportedConfirmation.current = confirmed;
      onConfirmationChange?.(confirmed);
    }
  }, [confirmed, onConfirmationChange]);

  return (
    <div className="reference-input">
      <Input
        id={id}
        value={value}
        disabled={disabled}
        placeholder={placeholderFor(kind)}
        autoComplete="off"
        onChange={(event) => onChange?.(event.target.value)}
      />
      {state.status === 'loading' ? (
        <Typography.Text type="secondary">
          <Spin size="small" /> Checking reference…
        </Typography.Text>
      ) : null}
      {state.status === 'invalid-format' ? (
        <Alert type="warning" showIcon message={`Enter ${placeholderFor(kind)}.`} />
      ) : null}
      {state.status === 'not-found' || state.status === 'forbidden' || state.status === 'error' ? (
        <Alert type="error" showIcon message={state.message} />
      ) : null}
      {state.status === 'valid' ? <ReferencePreviewCard preview={state.preview} /> : null}
    </div>
  );
}

function ReferencePreviewCard({ preview }: { preview: ReferencePreview }) {
  const related =
    preview.kind === 'evidence' || preview.kind === 'decision'
      ? ['Subject', preview.subjectRef]
      : preview.kind === 'action'
        ? ['Decision', preview.decisionRef]
        : ['Action', preview.actionRef];
  const status =
    preview.kind === 'evidence' || preview.kind === 'action' ? preview.status : null;
  return (
    <Card size="small" title="Confirmed reference preview">
      <Descriptions size="small" column={1}>
        <Descriptions.Item label="Reference">{preview.reference}</Descriptions.Item>
        <Descriptions.Item label={related[0]}>{related[1]}</Descriptions.Item>
        <Descriptions.Item label="Source">{humanize(preview.source)}</Descriptions.Item>
        {status ? <Descriptions.Item label="Status">{humanize(status)}</Descriptions.Item> : null}
        <Descriptions.Item label="Recorded (UTC)">
          {formatInstant(preview.recordedAt)}
        </Descriptions.Item>
      </Descriptions>
    </Card>
  );
}

function placeholderFor(kind: ReferenceKind): string {
  const prefixes: Record<ReferenceKind, string> = {
    evidence: 'ev-<UUIDv4>',
    decision: 'dec-<UUIDv4>',
    action: 'act-<UUIDv4>',
    actionOutcome: 'aoc-<UUIDv4>',
  };
  return prefixes[kind];
}
