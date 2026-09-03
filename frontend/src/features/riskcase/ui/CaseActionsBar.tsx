import { Button, Space, Typography } from 'antd';

import { actionsForStatus, type CaseActionDescriptor } from '../actions/actionDescriptors';
import type { RiskCaseStatus } from '../api/riskCaseTypes';

export function CaseActionsBar({
  status,
  disabled = false,
  onSelect,
}: {
  status: RiskCaseStatus;
  disabled?: boolean;
  onSelect: (descriptor: CaseActionDescriptor) => void;
}) {
  const actions = actionsForStatus(status);
  return (
    <div className="case-actions" aria-label="Available case actions">
      <Typography.Text type="secondary">Available for this status</Typography.Text>
      <Space wrap>
        {actions.map((descriptor) => (
          <Button
            key={descriptor.id}
            disabled={disabled}
            danger={descriptor.id === 'cancel'}
            onClick={() => onSelect(descriptor)}
          >
            {descriptor.label}
          </Button>
        ))}
      </Space>
    </div>
  );
}
