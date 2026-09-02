import {
  type ColumnDef,
  flexRender,
  getCoreRowModel,
  getSortedRowModel,
  type SortingState,
  useReactTable,
} from '@tanstack/react-table';
import { Button, Tag, Tooltip } from 'antd';
import { useMemo, useState } from 'react';

import { formatInstant, humanize } from '../../../shared/format';
import type { RiskCasePriority, RiskCaseStatus, RiskCaseSummary } from '../api/riskCaseTypes';

const statusColors: Record<RiskCaseStatus, string> = {
  OPEN: 'blue',
  IN_REVIEW: 'cyan',
  ACTION_REQUIRED: 'orange',
  RESOLVED: 'green',
  CLOSED: 'green',
  CANCELLED: 'default',
};

const priorityColors: Record<RiskCasePriority, string> = {
  LOW: 'default',
  NORMAL: 'blue',
  HIGH: 'orange',
  CRITICAL: 'red',
};

export function RiskCaseTable({
  cases,
  onOpen,
}: {
  cases: RiskCaseSummary[];
  onOpen: (caseNumber: string) => void;
}) {
  const [sorting, setSorting] = useState<SortingState>([]);
  const columns = useMemo<ColumnDef<RiskCaseSummary>[]>(
    () => [
      {
        accessorKey: 'caseNumber',
        header: 'Case',
        cell: ({ row }) => (
          <Button type="link" className="table-link" onClick={() => onOpen(row.original.caseNumber)}>
            {row.original.caseNumber}
          </Button>
        ),
      },
      { accessorKey: 'subjectRef', header: 'Subject' },
      {
        accessorKey: 'status',
        header: 'Status',
        cell: ({ getValue }) => {
          const status = getValue<RiskCaseStatus>();
          return <Tag color={statusColors[status]}>{humanize(status)}</Tag>;
        },
      },
      {
        accessorKey: 'priority',
        header: 'Priority',
        cell: ({ getValue }) => {
          const priority = getValue<RiskCasePriority>();
          return <Tag color={priorityColors[priority]}>{humanize(priority)}</Tag>;
        },
      },
      {
        accessorKey: 'assigneeRef',
        header: 'Assignee',
        cell: ({ getValue }) => getValue<string | null>() ?? 'Unassigned',
      },
      {
        accessorKey: 'updatedAt',
        header: 'Updated (UTC)',
        cell: ({ getValue }) => formatInstant(getValue<string>()),
      },
    ],
    [onOpen],
  );
  const table = useReactTable({
    data: cases,
    columns,
    state: { sorting },
    onSortingChange: setSorting,
    getCoreRowModel: getCoreRowModel(),
    getSortedRowModel: getSortedRowModel(),
  });

  return (
    <div className="table-scroll" data-testid="risk-case-table">
      <table className="risk-table">
        <thead>
          {table.getHeaderGroups().map((group) => (
            <tr key={group.id}>
              {group.headers.map((header) => (
                <th key={header.id} scope="col">
                  {header.isPlaceholder ? null : (
                    <Tooltip title={header.column.getCanSort() ? 'Sort this page' : undefined}>
                      <button
                        type="button"
                        className="sort-header"
                        onClick={header.column.getToggleSortingHandler()}
                        disabled={!header.column.getCanSort()}
                      >
                        {flexRender(header.column.columnDef.header, header.getContext())}
                        {{ asc: ' ↑', desc: ' ↓' }[header.column.getIsSorted() as string] ?? ''}
                      </button>
                    </Tooltip>
                  )}
                </th>
              ))}
            </tr>
          ))}
        </thead>
        <tbody>
          {table.getRowModel().rows.map((row) => (
            <tr key={row.id}>
              {row.getVisibleCells().map((cell) => (
                <td key={cell.id}>{flexRender(cell.column.columnDef.cell, cell.getContext())}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
