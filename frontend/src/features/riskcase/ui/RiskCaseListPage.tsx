import { Button, Card, Flex, Form, Input, Select, Space, Typography } from 'antd';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { userFacingError } from '../../../core/api/errors';
import { EmptyState, ErrorState, LoadingState } from '../../../shared/AsyncState';
import {
  riskCasePriorities,
  riskCaseStatuses,
  type RiskCaseFilters,
} from '../api/riskCaseTypes';
import { useRiskCaseList } from '../model/riskCaseQueries';
import { RiskCaseTable } from './RiskCaseTable';

const pageSize = 20;

interface FilterForm {
  status?: RiskCaseFilters['status'];
  priority?: RiskCaseFilters['priority'];
  subjectRef?: string;
  assignee?: string;
}

export function RiskCaseListPage() {
  const navigate = useNavigate();
  const [filters, setFilters] = useState<RiskCaseFilters>({});
  const [page, setPage] = useState(0);
  const query = useRiskCaseList(filters, page, pageSize);

  const applyFilters = (values: FilterForm) => {
    setFilters({
      status: values.status,
      priority: values.priority,
      subjectRef: values.subjectRef?.trim() || undefined,
      assignee: values.assignee?.trim() || undefined,
    });
    setPage(0);
  };

  return (
    <Space direction="vertical" size="large" className="page-stack">
      <div>
        <Typography.Title level={2}>Risk cases</Typography.Title>
        <Typography.Text type="secondary">
          Bounded operator view · 20 cases per page · timestamps shown in UTC
        </Typography.Text>
      </div>

      <Card>
        <Form<FilterForm> layout="inline" onFinish={applyFilters} aria-label="Risk case filters">
          <Form.Item name="status" label="Status">
            <Select allowClear placeholder="All statuses" className="filter-select">
              {riskCaseStatuses.map((status) => (
                <Select.Option key={status} value={status}>
                  {status.replaceAll('_', ' ')}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="priority" label="Priority">
            <Select allowClear placeholder="All priorities" className="filter-select">
              {riskCasePriorities.map((priority) => (
                <Select.Option key={priority} value={priority}>
                  {priority}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>
          <Form.Item name="subjectRef" label="Subject">
            <Input placeholder="Exact subject reference" allowClear />
          </Form.Item>
          <Form.Item name="assignee" label="Assignee">
            <Input placeholder="Exact actor reference" allowClear />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit">
              Apply filters
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card className="data-card">
        {query.isPending ? <LoadingState label="Loading risk cases" /> : null}
        {query.isError ? (
          <ErrorState
            message={userFacingError(query.error, 'Risk cases could not be loaded.')}
            onRetry={() => void query.refetch()}
          />
        ) : null}
        {query.data?.items.length === 0 ? (
          <EmptyState description="No risk cases match these filters." />
        ) : null}
        {query.data && query.data.items.length > 0 ? (
          <RiskCaseTable
            cases={query.data.items}
            onOpen={(caseNumber) => navigate(`/cases/${encodeURIComponent(caseNumber)}`)}
          />
        ) : null}
        {query.data ? (
          <Flex justify="end" align="center" gap="middle" className="pagination-row">
            <Typography.Text>Page {query.data.page + 1}</Typography.Text>
            <Button disabled={page === 0} onClick={() => setPage((value) => value - 1)}>
              Previous
            </Button>
            <Button disabled={!query.data.hasNext} onClick={() => setPage((value) => value + 1)}>
              Next
            </Button>
          </Flex>
        ) : null}
      </Card>
    </Space>
  );
}
