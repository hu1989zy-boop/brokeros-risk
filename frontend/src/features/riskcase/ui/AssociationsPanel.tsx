import { Button, Card, Empty, List, Space, Tag, Typography } from 'antd';

import { humanize } from '../../../shared/format';
import {
  associationActionDescriptors,
  type CaseActionDescriptor,
} from '../actions/actionDescriptors';
import type { RiskCaseAssociations } from '../api/riskCaseTypes';

export function AssociationsPanel({
  associations,
  onSelectAction,
}: {
  associations: RiskCaseAssociations;
  onSelectAction?: (descriptor: CaseActionDescriptor) => void;
}) {
  return (
    <Card title="Associations" data-testid="associations-panel">
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
        title="Evidence associations"
        empty="No evidence associations are recorded on this case."
        items={associations.evidenceAssociations.map((item) => ({
          key: item.eventRef,
          primary: item.evidenceRef,
          secondary: `${humanize(item.disposition)} · ${item.source}${
            item.replacementEvidenceRef
              ? ` · replacement ${item.replacementEvidenceRef}`
              : ''
          } · event ${item.eventRef}`,
        }))}
      />
      <AssociationList
        title="Associated decisions"
        empty="No decisions are associated with this case."
        items={associations.decisions.map((decision) => ({
          key: decision.decisionRef,
          primary: decision.decisionRef,
          secondary: decision.current ? 'Current decision' : 'Associated',
          current: decision.current,
        }))}
      />
      <AssociationList
        title="Associated actions"
        empty="No effective actions are associated with this case."
        items={associations.actions.map((item) => ({
          key: item.actionRef,
          primary: item.actionRef,
          secondary: item.outcomeRefs.length > 0
            ? `Outcomes: ${item.outcomeRefs.join(', ')}`
            : 'No outcome reference recorded',
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
