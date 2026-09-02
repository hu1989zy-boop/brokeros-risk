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
import type { RiskCaseHistoryEntry, RiskCaseView } from '../api/riskCaseTypes';
import { useAddRiskCaseNote, useRiskCaseDetail } from '../model/riskCaseQueries';
import { AddNoteDialog } from './AddNoteDialog';

function associationEntries(entries: RiskCaseHistoryEntry[]): RiskCaseHistoryEntry[] {
  return entries.filter((entry) => entry.affectedRef !== null);
}

export function RiskCaseDetailPage() {
  const { caseNumber = '' } = useParams();
  const decodedCaseNumber = decodeURIComponent(caseNumber);
  const navigate = useNavigate();
  const query = useRiskCaseDetail(decodedCaseNumber);
  const mutation = useAddRiskCaseNote();
  const [noteOpen, setNoteOpen] = useState(false);
  const [operationMessage, setOperationMessage] = useState<string | null>(null);

  if (query.isPending) {
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
        <Space>
          <Button onClick={() => void query.refetch()}>Reload</Button>
          <Button type="primary" onClick={() => setNoteOpen(true)}>
            Add note
          </Button>
        </Space>
      </Flex>

      {operationMessage ? <Alert type="success" showIcon message={operationMessage} /> : null}
      <RiskCaseDetailContent view={view} />

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
    </Space>
  );
}

export function RiskCaseDetailContent({ view }: { view: RiskCaseView }) {
  const detail = view.detail;
  const associations = associationEntries(view.history.entries);
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

      <Card title="Association references in history">
        {associations.length === 0 ? (
          <Typography.Text type="secondary">No association reference events recorded.</Typography.Text>
        ) : (
          <Space wrap>
            {associations.map((entry, index) => (
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
