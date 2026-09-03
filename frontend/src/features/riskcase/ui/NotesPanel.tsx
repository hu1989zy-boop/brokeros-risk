import { Button, Card, List, Space, Typography } from 'antd';

import { formatInstant } from '../../../shared/format';
import type { RiskCaseHistoryEntry } from '../api/riskCaseTypes';

function noteEntries(entries: RiskCaseHistoryEntry[]): RiskCaseHistoryEntry[] {
  return entries.filter((entry) => entry.eventType === 'NOTE' && entry.affectedRef !== null);
}

export function NotesPanel({
  entries,
  disabled = false,
  onCorrect,
}: {
  entries: RiskCaseHistoryEntry[];
  disabled?: boolean;
  onCorrect: (noteRef: string) => void;
}) {
  const notes = noteEntries(entries);
  return (
    <Card title="Investigation notes">
      {notes.length === 0 ? (
        <Typography.Text type="secondary">No investigation notes recorded.</Typography.Text>
      ) : (
        <List
          dataSource={notes}
          renderItem={(entry) => (
            <List.Item
              actions={[
                <Button
                  key="correct"
                  disabled={disabled}
                  onClick={() => onCorrect(entry.affectedRef!)}
                >
                  Correct
                </Button>,
              ]}
            >
              <List.Item.Meta
                title={<Typography.Text code>{entry.affectedRef}</Typography.Text>}
                description={
                  <Space direction="vertical" size={0}>
                    <Typography.Text type="secondary">
                      Version {entry.version} · {formatInstant(entry.occurredAt)}
                    </Typography.Text>
                    <Typography.Text type="secondary">Recorded by {entry.actorRef}</Typography.Text>
                  </Space>
                }
              />
            </List.Item>
          )}
        />
      )}
    </Card>
  );
}
