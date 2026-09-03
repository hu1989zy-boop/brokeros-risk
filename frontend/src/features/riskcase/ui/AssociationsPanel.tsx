import { Alert, Button, Card, Empty, List, Space, Tag, Typography } from 'antd';

import { humanize } from '../../../shared/format';
import {
  associationActionDescriptors,
  type CaseActionDescriptor,
} from '../actions/actionDescriptors';
import type { RiskCaseView } from '../api/riskCaseTypes';
import { projectAssociations } from '../model/associationProjection';

export function AssociationsPanel({
  view,
  onSelectAction,
}: {
  view: RiskCaseView;
  onSelectAction?: (descriptor: CaseActionDescriptor) => void;
}) {
  const projection = projectAssociations(view);
  return (
    <Card title="Associations" data-testid="associations-panel">
      <Alert
        className="dialog-alert"
        type="info"
        showIcon
        message="Bounded association view"
        description="Current decision is authoritative detail data. Evidence, associated decisions, actions, and outcome presence are reconstructed only from the loaded history page; the backend does not expose a complete current-association projection."
      />
      {view.history.nextCursor ? (
        <Alert
          className="dialog-alert"
          type="warning"
          showIcon
          message="More history exists; this association view may be incomplete."
        />
      ) : null}
      {onSelectAction ? (
        <section className="association-section" aria-label="Association actions">
          <Typography.Title level={5}>Association operations</Typography.Title>
          <Space wrap>
            {associationActionDescriptors.map((descriptor) => (
              <Button key={descriptor.id} onClick={() => onSelectAction(descriptor)}>
                {descriptor.label}
              </Button>
            ))}
          </Space>
        </section>
      ) : null}
      <AssociationList
        title="Evidence history"
        empty="No evidence association is visible in loaded history."
        items={projection.evidence.map((item) => ({
          key: item.evidenceRef,
          primary: item.evidenceRef,
          secondary: `Latest visible event: ${humanize(item.latestEventType)} at v${item.version}`,
        }))}
      />
      <AssociationList
        title="Associated decisions"
        empty="No decision association is visible in loaded case state."
        items={projection.decisions.map((reference) => ({
          key: reference,
          primary: reference,
          secondary:
            reference === view.detail.currentDecisionRef ? 'Current decision' : 'Associated',
          current: reference === view.detail.currentDecisionRef,
        }))}
      />
      <AssociationList
        title="Associated actions"
        empty="No action association is visible in loaded history."
        items={projection.actions.map((item) => ({
          key: item.actionRef,
          primary: item.actionRef,
          secondary: item.outcomeRecorded
            ? 'Outcome reference recorded (outcome ref is not exposed by history)'
            : item.active
              ? 'Associated'
              : 'Withdrawn',
        }))}
      />
    </Card>
  );
}

function AssociationList({
  title,
  empty,
  items,
}: {
  title: string;
  empty: string;
  items: Array<{ key: string; primary: string; secondary: string; current?: boolean }>;
}) {
  return (
    <section className="association-section">
      <Typography.Title level={5}>{title}</Typography.Title>
      {items.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={empty} />
      ) : (
        <List
          size="small"
          dataSource={items}
          renderItem={(item) => (
            <List.Item key={item.key}>
              <Space wrap>
                <Typography.Text code>{item.primary}</Typography.Text>
                {item.current ? <Tag color="blue">Current</Tag> : null}
                <Typography.Text type="secondary">{item.secondary}</Typography.Text>
              </Space>
            </List.Item>
          )}
        />
      )}
    </section>
  );
}
