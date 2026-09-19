export type Severity = 'CRITICAL' | 'MAJOR' | 'MODERATE' | 'OPTIMIZATION';

export type Category = 
  | 'OSGI_DEPENDENCIES'
  | 'UI_THREAD_BLOCKING'
  | 'SWT_JFACE_COMPLIANCE'
  | 'LOGIC_AND_SYNC'
  | 'RESOURCE_LIFECYCLE';

export interface CodeIssue {
  id: string;
  title: string;
  severity: Severity;
  category: Category;
  filePath: string;
  lineNumbers?: string;
  summary: string;
  violation: string;
  impact: string;
  solution: string;
  originalSnippet: string;
  correctedSnippet: string;
}

export interface ReviewMetric {
  id: string;
  label: string;
  value: string;
  status: 'passed' | 'warning' | 'failed';
  description: string;
}

export interface ProjectFileInfo {
  path: string;
  title: string;
  description: string;
  status: 'modified' | 'critical_fix' | 'verified';
  issuesCount: number;
  originalCode: string;
  fixedCode: string;
  diffSummary: string;
}
