import { Alert, Form, Input, Modal, Select, Typography, type FormRule } from 'antd';
import { useState } from 'react';

import type { ResultCode } from '../../../core/api/contracts';
import { ApiError, userFacingError } from '../../../core/api/errors';
import type { CaseActionDescriptor } from '../actions/actionDescriptors';
import {
  invalidReferences,
  type CaseActionFieldSpec,
  type CaseActionValues,
} from '../actions/actionInputs';

function isFormValidationFailure(value: unknown): value is { errorFields: unknown[] } {
  return (
    typeof value === 'object' &&
    value !== null &&
    'errorFields' in value &&
    Array.isArray((value as { errorFields: unknown }).errorFields)
  );
}

export function CaseActionDialog({
  descriptor,
  expectedVersion,
  submitting,
  onCancel,
  onSubmit,
}: {
  descriptor: CaseActionDescriptor;
  expectedVersion: number;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (values: CaseActionValues) => Promise<void>;
}) {
  const [form] = Form.useForm<CaseActionValues>();
  const [confirming, setConfirming] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [conflict, setConflict] = useState(false);

  const submit = async () => {
    try {
      const values = await form.validateFields();
      setErrorMessage(null);
      setConflict(false);
      if (descriptor.terminal && !confirming) {
        setConfirming(true);
        return;
      }
      await onSubmit(values);
      form.resetFields();
    } catch (error) {
      if (isFormValidationFailure(error)) return;
      if (error instanceof ApiError && error.code === 'RISK_CASE_VERSION_CONFLICT') {
        setConfirming(false);
        setConflict(true);
        setErrorMessage(
          'This case changed while you were editing. The latest version was reloaded; review your preserved input and retry.',
        );
        return;
      }
      setErrorMessage(actionErrorMessage(descriptor, error));
    }
  };

  return (
    <Modal
      title={confirming ? `Confirm: ${descriptor.label}` : descriptor.label}
      open
      okText={
        descriptor.terminal
          ? confirming
            ? `Confirm ${descriptor.label.toLowerCase()}`
            : 'Review action'
          : descriptor.label
      }
      cancelText={confirming ? 'Back' : 'Cancel'}
      confirmLoading={submitting}
      maskClosable={!submitting && !confirming}
      closable={!submitting}
      okButtonProps={{ disabled: submitting, danger: descriptor.terminal && confirming }}
      cancelButtonProps={{ disabled: submitting }}
      onCancel={() => {
        if (submitting) return;
        if (confirming) {
          setConfirming(false);
          return;
        }
        onCancel();
      }}
      onOk={() => void submit()}
      destroyOnHidden
    >
      <Typography.Paragraph type="secondary">
        The operation will be submitted against case version {expectedVersion}. Authorization and
        lifecycle validation remain server-side.
      </Typography.Paragraph>
      {confirming ? (
        <Alert
          className="dialog-alert"
          type="warning"
          showIcon
          message="Confirm terminal case operation"
          description={descriptor.confirmation}
        />
      ) : null}
      {errorMessage ? (
        <Alert
          className="dialog-alert"
          type={conflict ? 'warning' : 'error'}
          showIcon
          message={errorMessage}
        />
      ) : null}
      <Form<CaseActionValues> form={form} layout="vertical" preserve>
        {descriptor.fields.map((field) => (
          <Form.Item key={field.name} name={field.name} label={field.label} rules={rulesFor(field)}>
            {renderField(field, submitting || confirming)}
          </Form.Item>
        ))}
      </Form>
    </Modal>
  );
}

function renderField(field: CaseActionFieldSpec, disabled: boolean) {
  if (field.kind === 'select') {
    return (
      <Select
        disabled={disabled}
        options={field.options}
        placeholder={field.placeholder ?? `Select ${field.label.toLowerCase()}`}
      />
    );
  }
  if (field.kind === 'textarea' || field.kind === 'reference-list') {
    return (
      <Input.TextArea
        disabled={disabled}
        rows={field.kind === 'reference-list' ? 3 : 5}
        maxLength={field.maxLength}
        showCount={Boolean(field.maxLength)}
        placeholder={field.placeholder}
      />
    );
  }
  return (
    <Input disabled={disabled} maxLength={field.maxLength} placeholder={field.placeholder} />
  );
}

function rulesFor(field: CaseActionFieldSpec): FormRule[] {
  const rules: FormRule[] = [];
  if (field.required) {
    rules.push({ required: true, whitespace: true, message: `Enter ${field.label.toLowerCase()}.` });
  }
  if (field.maxLength) {
    rules.push({ max: field.maxLength, message: `${field.label} cannot exceed ${field.maxLength} characters.` });
  }
  if (field.pattern) {
    rules.push({ pattern: field.pattern, message: field.patternMessage });
  }
  if (field.referencePattern) {
    rules.push({
      validator: async (_rule, value: string | undefined) => {
        const invalid = invalidReferences(value, field.referencePattern!);
        if (invalid.length > 0) {
          throw new Error(`Invalid reference: ${invalid[0]}`);
        }
      },
    });
  }
  return rules;
}

function actionErrorMessage(descriptor: CaseActionDescriptor, error: unknown): string {
  if (error instanceof ApiError) {
    const override = descriptor.messages?.[error.code as ResultCode];
    return override ?? error.message;
  }
  return userFacingError(error, `${descriptor.label} could not be completed.`);
}
