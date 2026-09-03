import {
  riskCasePriorities,
  riskCaseResolutionOutcomes,
} from '../api/riskCaseTypes';

export type CaseActionFieldName =
  | 'actionRefs'
  | 'assigneeRef'
  | 'content'
  | 'duplicateCaseNumber'
  | 'evidenceRefs'
  | 'outcome'
  | 'priority'
  | 'reason'
  | 'resolutionSummary';

export type CaseActionFieldKind = 'text' | 'textarea' | 'select' | 'reference-list';

export interface CaseActionFieldOption {
  label: string;
  value: string;
}

export interface CaseActionFieldSpec {
  name: CaseActionFieldName;
  label: string;
  kind: CaseActionFieldKind;
  required: boolean;
  maxLength?: number;
  placeholder?: string;
  options?: CaseActionFieldOption[];
  pattern?: RegExp;
  patternMessage?: string;
  referencePattern?: RegExp;
}

export type CaseActionValues = Partial<Record<CaseActionFieldName, string>>;

export const canonicalUuidV4Pattern =
  /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/;
export const caseNumberPattern = new RegExp(`^RC-${canonicalUuidV4Pattern.source.slice(1, -1)}$`);
export const evidenceRefPattern = new RegExp(
  `^ev-${canonicalUuidV4Pattern.source.slice(1, -1)}$`,
);
export const actionRefPattern = new RegExp(
  `^act-${canonicalUuidV4Pattern.source.slice(1, -1)}$`,
);

export const priorityOptions: CaseActionFieldOption[] = riskCasePriorities.map((priority) => ({
  label: titleCase(priority),
  value: priority,
}));

export const resolutionOutcomeOptions: CaseActionFieldOption[] = riskCaseResolutionOutcomes.map(
  (outcome) => ({ label: titleCase(outcome), value: outcome }),
);

export function splitReferenceList(value: string | undefined): string[] {
  if (!value?.trim()) return [];
  return [...new Set(value.split(/[\n,]+/).map((item) => item.trim()).filter(Boolean))];
}

export function invalidReferences(value: string | undefined, pattern: RegExp): string[] {
  return splitReferenceList(value).filter((reference) => !pattern.test(reference));
}

function titleCase(value: string): string {
  return value
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}
