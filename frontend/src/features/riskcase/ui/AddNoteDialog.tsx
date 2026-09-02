import { Alert, Form, Input, Modal, Typography } from 'antd';
import { useEffect, useState } from 'react';

import { ApiError, userFacingError } from '../../../core/api/errors';

interface NoteForm {
  content: string;
}

function isFormValidationFailure(value: unknown): value is { errorFields: unknown[] } {
  return (
    typeof value === 'object' &&
    value !== null &&
    'errorFields' in value &&
    Array.isArray((value as { errorFields: unknown }).errorFields)
  );
}

export function AddNoteDialog({
  open,
  expectedVersion,
  submitting,
  onCancel,
  onSubmit,
  onVersionConflict,
}: {
  open: boolean;
  expectedVersion: number;
  submitting: boolean;
  onCancel: () => void;
  onSubmit: (content: string, expectedVersion: number) => Promise<void>;
  onVersionConflict: () => Promise<void>;
}) {
  const [form] = Form.useForm<NoteForm>();
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!open) {
      form.resetFields();
      setErrorMessage(null);
    }
  }, [form, open]);

  const submit = async () => {
    try {
      const values = await form.validateFields();
      setErrorMessage(null);
      await onSubmit(values.content.trim(), expectedVersion);
      form.resetFields();
    } catch (error) {
      if (isFormValidationFailure(error)) {
        return;
      }
      if (error instanceof ApiError && error.code === 'RISK_CASE_VERSION_CONFLICT') {
        setErrorMessage('This case changed while you were editing. The latest version was reloaded; review and submit again.');
        await onVersionConflict();
        return;
      }
      setErrorMessage(userFacingError(error, 'The investigation note could not be added.'));
    }
  };

  return (
    <Modal
      title="Add investigation note"
      open={open}
      okText="Add note"
      cancelText="Cancel"
      confirmLoading={submitting}
      maskClosable={!submitting}
      closable={!submitting}
      okButtonProps={{ disabled: submitting }}
      cancelButtonProps={{ disabled: submitting }}
      onCancel={onCancel}
      onOk={() => void submit()}
      destroyOnHidden
    >
      <Typography.Paragraph type="secondary">
        The note will be submitted against case version {expectedVersion}. The backend remains the
        authority for validation and concurrency.
      </Typography.Paragraph>
      {errorMessage ? (
        <Alert className="dialog-alert" type="error" showIcon message={errorMessage} />
      ) : null}
      <Form<NoteForm> form={form} layout="vertical" preserve={false}>
        <Form.Item
          name="content"
          label="Investigation note"
          rules={[
            { required: true, whitespace: true, message: 'Enter an investigation note.' },
            { max: 4000, message: 'Investigation notes cannot exceed 4,000 characters.' },
          ]}
        >
          <Input.TextArea
            autoFocus
            disabled={submitting}
            rows={6}
            maxLength={4000}
            showCount
            placeholder="Record an objective investigation observation"
          />
        </Form.Item>
      </Form>
    </Modal>
  );
}
