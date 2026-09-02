import { Alert, Button, Empty, Flex, Spin, Typography } from 'antd';
import type { ReactNode } from 'react';

export function LoadingState({ label }: { label: string }) {
  return (
    <Flex className="async-state" vertical align="center" justify="center" gap="middle">
      <Spin size="large" />
      <Typography.Text type="secondary">{label}</Typography.Text>
    </Flex>
  );
}

export function EmptyState({ description }: { description: string }) {
  return (
    <div className="async-state">
      <Empty description={description} />
    </div>
  );
}

export function ErrorState({
  message,
  onRetry,
}: {
  message: ReactNode;
  onRetry?: () => void;
}) {
  return (
    <div className="async-state">
      <Alert
        type="error"
        showIcon
        message="Unable to load data"
        description={message}
        action={
          onRetry ? (
            <Button onClick={onRetry} danger>
              Retry
            </Button>
          ) : undefined
        }
      />
    </div>
  );
}
