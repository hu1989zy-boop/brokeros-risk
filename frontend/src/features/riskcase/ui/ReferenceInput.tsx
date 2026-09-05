import { Alert, Card, Descriptions, Input, Segmented, Select, Spin, Typography } from 'antd';
import { useEffect, useRef, useState } from 'react';

import { formatInstant, humanize } from '../../../shared/format';
import type { ReferenceKind } from '../actions/actionInputs';
import type { ReferenceBrowseScope, ReferenceListItem } from '../api/referenceList';
import type { ReferencePreview } from '../api/referencePreview';
import {
  useActionList,
  useDecisionList,
  useEvidenceList,
  useOutcomeList,
} from '../model/useReferenceList';
import { useReferencePreview } from '../model/useReferencePreview';

export function ReferenceInput({
  kind,
  value,
  id,
  disabled = false,
  required = false,
  onChange,
  onConfirmationChange,
  browseScope,
}: {
  kind: ReferenceKind;
  value?: string;
  id?: string;
  disabled?: boolean;
  required?: boolean;
  onChange?: (value: string) => void;
  onConfirmationChange?: (confirmed: boolean) => void;
  browseScope?: ReferenceBrowseScope;
}) {
  const scopeKey = scopeKeyFor(kind, browseScope);
  const browseAvailable = Boolean(scopeKey);
  const [mode, setMode] = useState<'browse' | 'manual'>(browseAvailable ? 'browse' : 'manual');
  const priorBrowseAvailable = useRef(browseAvailable);
  const evidenceList = useEvidenceList(kind === 'evidence' ? browseScope?.subjectRef : undefined);
  const decisionList = useDecisionList(kind === 'decision' ? browseScope?.subjectRef : undefined);
  const actionList = useActionList(kind === 'action' ? browseScope?.decisionRef : undefined);
  const outcomeList = useOutcomeList(kind === 'actionOutcome' ? browseScope?.actionRef : undefined);
  const listQuery =
    kind === 'evidence'
      ? evidenceList
      : kind === 'decision'
        ? decisionList
        : kind === 'action'
          ? actionList
          : outcomeList;
  const state = useReferencePreview(kind, value);
  const confirmed = state.status === 'valid' || (!required && !value?.trim());
  const reportedConfirmation = useRef<boolean | null>(null);

  useEffect(() => {
    if (reportedConfirmation.current !== confirmed) {
      reportedConfirmation.current = confirmed;
      onConfirmationChange?.(confirmed);
    }
  }, [confirmed, onConfirmationChange]);

  useEffect(() => {
    if (!browseAvailable) {
      setMode('manual');
    } else if (!priorBrowseAvailable.current) {
      setMode('browse');
    }
    priorBrowseAvailable.current = browseAvailable;
  }, [browseAvailable]);

  return (
    <div className="reference-input">
      {browseAvailable ? (
        <Segmented
          aria-label="Reference selection mode"
          disabled={disabled}
          value={mode}
          options={[
            { label: 'Browse', value: 'browse' },
            { label: 'Enter manually', value: 'manual' },
          ]}
          onChange={(nextMode) => setMode(nextMode as 'browse' | 'manual')}
        />
      ) : null}
      {mode === 'browse' && browseAvailable ? (
        <>
          <Select
            id={id}
            value={value || undefined}
            disabled={disabled}
            loading={listQuery.isPending}
            showSearch
            optionFilterProp="label"
            placeholder={`Browse ${kindLabel(kind)} references`}
            options={(listQuery.data ?? []).map((item) => ({
              value: item.reference,
              label: listLabel(item),
            }))}
            notFoundContent={
              listQuery.isPending
                ? 'Loading references…'
                : 'No references are available in this case scope.'
            }
            onChange={(reference) => onChange?.(reference)}
          />
          {listQuery.isError ? (
            <Alert
              type="error"
              showIcon
              message="Scoped references could not be loaded. You can enter a reference manually."
            />
          ) : null}
        </>
      ) : (
        <Input
          id={id}
          value={value}
          disabled={disabled}
          placeholder={placeholderFor(kind)}
          autoComplete="off"
          onChange={(event) => onChange?.(event.target.value)}
        />
      )}
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

function scopeKeyFor(
  kind: ReferenceKind,
  browseScope: ReferenceBrowseScope | undefined,
): string | undefined {
  if (kind === 'evidence' || kind === 'decision') return browseScope?.subjectRef;
  if (kind === 'action') return browseScope?.decisionRef;
  return browseScope?.actionRef;
}

function kindLabel(kind: ReferenceKind): string {
  return kind === 'actionOutcome' ? 'action outcome' : kind;
}

function listLabel(item: ReferenceListItem): string {
  const status = item.kind === 'evidence' || item.kind === 'action' ? ` · ${item.status}` : '';
  return `${item.reference} · ${formatInstant(item.recordedAt)}${status}`;
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
