export type IncidentStatus = 'OPEN' | 'ANALYZING' | 'ANALYZED' | 'RESOLVED' | 'FAILED';
export type Severity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type InputType = 'KUBERNETES' | 'DOCKER' | 'JENKINS' | 'TERRAFORM' | 'GENERIC';
export type RiskLevel = 'SAFE' | 'CAUTION' | 'DANGEROUS';

export interface RemediationCommand {
  id?: number;
  command: string;
  description: string;
  riskLevel: RiskLevel;
  stepOrder: number;
}

export interface AnalysisReference {
  id?: number;
  title: string;
  source: string;
  docId?: string;
  url?: string;
}

export interface IncidentSummary {
  id: number;
  title: string;
  status: IncidentStatus;
  severity: Severity;
  inputType: InputType;
  summary: string;
  confidence: number;
  isFallback: boolean;
  createdAt: string;
}

export interface IncidentDetail {
  id: number;
  title: string;
  status: IncidentStatus;
  severity: Severity;
  inputType: InputType;
  summary: string;
  rootCause: string;
  impact: string;
  explanation: string;
  confidence: number;
  modelUsed?: string;
  isFallback: boolean;
  sanitizedLog: string;
  redactedTokensCount: number;
  fixSteps: string[];
  commands: RemediationCommand[];
  references: AnalysisReference[];
  createdAt: string;
  updatedAt: string;
}

export interface IncidentAnalyzeRequest {
  content: string;
  title?: string;
  inputType?: string;
  context?: Record<string, any>;
}
