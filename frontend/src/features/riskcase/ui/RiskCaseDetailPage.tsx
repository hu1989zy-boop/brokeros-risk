import {
  Alert,
  Button,
  Card,
  Descriptions,
  Flex,
  Space,
  Tag,
  Timeline,
  Typography,
} from 'antd';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { userFacingError } from '../../../core/api/errors';
import { ErrorState, LoadingState } from '../../../shared/AsyncState';
import { formatInstant, humanize } from '../../../shared/format';
import {
  descriptorFor,
  type CaseActionDescriptor,
} from '../actions/actionDescriptors';
import { useCaseAction } from '../actions/useCaseAction';
import type { RiskCaseAssociations, RiskCaseView } from '../api/riskCaseTypes';
import { associationHistoryEntries, onCaseReferenceOptions } from '../model/associationProjection';
import {
  useAddRiskCaseNote,
  useRiskCaseAssociations,
  useRiskCaseDetail,
} from '../model/riskCaseQueries';
import { AddNoteDialog } from './AddNoteDialog';
import { AssociationsPanel } from './AssociationsPanel';
import { CaseActionDialog } from './CaseActionDialog';
import { CaseActionsBar } from './CaseActionsBar';
import { NotesPanel } from './NotesPanel';

interface SelectedAction {
  descriptor: CaseActionDescriptor;
  noteRef?: string;
}

export function RiskCaseDetailPage() {
  const { caseNumber = '' } = useParams();
  const decodedCaseNumber = decodeURIComponent(caseNumber);
  const navigate = useNavigate();
  const query = useRiskCaseDetail(decodedCaseNumber);
  const associationsQuery = useRiskCaseAssociations(decodedCaseNumber);
  const mutation = useAddRiskCaseNote();
  const [noteOpen, setNoteOpen] = useState(false);
  const [selectedAction, setSelectedAction] = useState<SelectedAction | null>(null);
  const [operationMessage, setOperationMessage] = useState<string | null>(null);

  if (query.isPending || associationsQuery.isPending) {
    return <LoadingState label="Loading risk case detail" />;
  }
  if (query.isError) {
    return (
      <ErrorState
        message={userFacingError(query.error, 'Risk case detail could not be loaded.')}
        onRetry={() => void query.refetch()}
      />
    );
  }
  if (associationsQuery.isError) {
    return (
      <ErrorState
        message={userFacingError(
          associationsQuery.error,
          'Risk case associations could not be loaded.',
        )}
        onRetry={() => void associationsQuery.refetch()}
      />
    );
  }

  const view = query.data;
  return (
    <Space direction="vertical" size="large" className="page-stack">
      <Flex justify="space-between" align="start" wrap="wrap" gap="middle">
        <div>
          <Button type="link" className="back-link" onClick={() => navigate('/cases')}>
            ← Back to risk cases
          </Button>
          <Typography.Title level={2}>{view.detail.caseNumber}</Typography.Title>
          <Space wrap>
            <Tag color="blue">{humanize(view.detail.status)}</Tag>
            <Tag color={view.detail.priority === 'CRITICAL' ? 'red' : 'orange'}>
              {humanize(view.detail.priority)} priority
            </Tag>
            <Typography.Text type="secondary">Version {view.detail.version}</Typography.Text>
          </Space>
        </div>
        <Space direction="vertical" align="end">
          <Space>
            <Button
              onClick={() => void Promise.all([
                query.refetch(), associationsQuery.refetch(),
              ])}
            >
              Reload
            </Button>
            <Button type="primary" onClick={() => setNoteOpen(true)}>
              Add note
            </Button>
          </Space>
          <CaseActionsBar
            status={view.detail.status}
            onSelect={(descriptor) => {
              setOperationMessage(null);
              setSelectedAction({ descriptor });
            }}
          />
        </Space>
      </Flex>

      {operationMessage ? <Alert type="success" showIcon message={operationMessage} /> : null}
      <RiskCaseDetailContent
        view={view}
        associations={associationsQuery.data}
        onAssociationAction={(descriptor) => {
          setOperationMessage(null);
          setSelectedAction({ descriptor });
        }}
        onCorrectNote={(noteRef) => {
          setOperationMessage(null);
          setSelectedAction({ descriptor: descriptorFor('correctNote'), noteRef });
        }}
      />

      {noteOpen ? (
        <AddNoteDialog
          open
          expectedVersion={view.detail.version}
          submitting={mutation.isPending}
          onCancel={() => {
            if (!mutation.isPending) setNoteOpen(false);
          }}
          onSubmit={async (content, expectedVersion) => {
            const note = await mutation.mutateAsync({
              caseNumber: view.detail.caseNumber,
              content,
              expectedVersion,
            });
            setOperationMessage(`Investigation note ${note.noteRef} was added.`);
            setNoteOpen(false);
          }}
          onVersionConflict={async () => {
            await query.refetch();
          }}
        />
      ) : null}

      {selectedAction ? (
        <CaseActionFlow
          key={`${selectedAction.descriptor.id}-${selectedAction.noteRef ?? ''}`}
          selected={selectedAction}
          caseNumber={view.detail.caseNumber}
          expectedVersion={view.detail.version}
          associations={associationsQuery.data}
          onCancel={() => setSelectedAction(null)}
          onSuccess={(message) => {
            setOperationMessage(message);
            setSelectedAction(null);
          }}
        />
      ) : null}
    </Space>
  );
}

function CaseActionFlow({
  selected,
  caseNumber,
  expectedVersion,
  associations,
  onCancel,
  onSuccess,
}: {
  selected: SelectedAction;
  caseNumber: string;
  expectedVersion: number;
  associations: RiskCaseAssociations;
  onCancel: () => void;
  onSuccess: (message: string) => void;
}) {
  const action = useCaseAction(selected.descriptor, {
    caseNumber,
    expectedVersion,
    noteRef: selected.noteRef,
  });
  return (
    <CaseActionDialog
      descriptor={selected.descriptor}
      expectedVersion={expectedVersion}
      submitting={action.isPending}
      onCaseOptions={onCaseReferenceOptions(associations)}
      onCancel={onCancel}
      onSubmit={async (values) => {
        const result = await action.run(values);
        if ('noteRef' in result) {
          onSuccess(`Investigation note ${result.noteRef} was corrected.`);
          return;
        }
        onSuccess(`${selected.descriptor.label} completed.`);
      }}
    />
  );
}

export function RiskCaseDetailContent({
  view,
  associations,
  onAssociationAction,
  onCorrectNote = () => undefined,
}: {
  view: RiskCaseView;
  associations: RiskCaseAssociations;
  onAssociationAction?: (descriptor: CaseActionDescriptor) => void;
  onCorrectNote?: (noteRef: string) => void;
}) {
  const detail = view.detail;
  const associationEntries = associationHistoryEntries(view.history.entries);
  return (
    <>
      <Card title="Case detail">
        <Descriptions bordered size="small" column={{ xs: 1, sm: 2, lg: 3 }}>
          <Descriptions.Item label="Subject type">{detail.subjectType}</Descriptions.Item>
          <Descriptions.Item label="Subject reference">{detail.subjectRef}</Descriptions.Item>
          <Descriptions.Item label="Assignee">{detail.assigneeRef ?? 'Unassigned'}</Descriptions.Item>
          <Descriptions.Item label="Intake source">{detail.intakeSource}</Descriptions.Item>
          <Descriptions.Item label="Current cycle">{detail.currentCycleNo}</Descriptions.Item>
          <Descriptions.Item label="Current decision">
            {detail.currentDecisionRef ?? 'None'}
          </Descriptions.Item>
          <Descriptions.Item label="Created (UTC)">{formatInstant(detail.createdAt)}</Descriptions.Item>
          <Descriptions.Item label="Updated (UTC)">{formatInstant(detail.updatedAt)}</Descriptions.Item>
          <Descriptions.Item label="Updated by">{detail.updatedByRef}</Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="Intake summary">
        <Typography.Paragraph className="preserve-lines">{detail.intakeSummary}</Typography.Paragraph>
      </Card>

      <NotesPanel entries={view.history.entries} onCorrect={onCorrectNote} />

      <AssociationsPanel associations={associations} onSelectAction={onAssociationAction} />

      <Card title="Association references in history">
        {associationEntries.length === 0 ? (
          <Typography.Text type="secondary">No association reference events recorded.</Typography.Text>
        ) : (
          <Space wrap>
            {associationEntries.map((entry, index) => (
              <Tag key={`${entry.version}-${entry.eventType}-${entry.affectedRef}-${index}`}>
                {humanize(entry.eventType)}: {entry.affectedRef}
              </Tag>
            ))}
          </Space>
        )}
      </Card>

      <Card title="History timeline">
        {view.history.entries.length === 0 ? (
          <Typography.Text type="secondary">No history entries recorded.</Typography.Text>
        ) : (
          <Timeline
            items={view.history.entries.map((entry) => ({
              children: (
                <div>
                  <Typography.Text strong>
                    v{entry.version} · {humanize(entry.eventType)}
                  </Typography.Text>
                  <br />
                  <Typography.Text>{entry.affectedRef ?? 'Case'}</Typography.Text>
                  <br />
                  <Typography.Text type="secondary">
                    {formatInstant(entry.occurredAt)} · {entry.actorRef}
                  </Typography.Text>
                </div>
              ),
            }))}
          />
        )}
        {view.history.nextCursor ? (
          <Alert
            type="info"
            showIcon
            message="Showing the first 100 history entries. More history is available."
          />
        ) : null}
      </Card>
    </>
  );
}
