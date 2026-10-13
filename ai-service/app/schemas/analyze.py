from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field, model_validator


class CommandItem(BaseModel):
    command: str = Field(..., description="Executable terminal/CLI command")
    description: str = Field(..., description="Purpose and effect of the command")
    risk_level: str = Field("SAFE", description="Risk level: SAFE, CAUTION, or DANGEROUS")


class ReferenceItem(BaseModel):
    title: str = Field(..., description="Document or runbook title")
    source: str = Field("knowledge_base", description="Source repository or authority")
    doc_id: Optional[str] = Field(None, description="Document identifier")
    url: Optional[str] = Field(None, description="Direct URL or documentation link")


class DevOpsAnalysisResult(BaseModel):
    summary: str = Field(..., description="Concise high-level executive summary of the incident")
    root_cause: str = Field(..., description="Technical root cause explanation")
    severity: str = Field("HIGH", description="Severity classification: LOW, MEDIUM, HIGH, CRITICAL")
    impact: str = Field(..., description="Operational impact on systems, traffic, and dependencies")
    fix_steps: List[str] = Field(..., description="Ordered concrete steps to resolve the issue")
    commands: List[CommandItem] = Field(default_factory=list, description="Diagnostic and remediation CLI commands")
    explanation: str = Field(..., description="In-depth technical explanation of the failure mechanics")
    confidence: float = Field(0.9, ge=0.0, le=1.0, description="Model confidence score between 0.0 and 1.0")
    references: List[ReferenceItem] = Field(default_factory=list, description="Referenced documentation citations")


class AnalyzeRequest(BaseModel):
    content: Optional[str] = Field(None, description="DevOps log snippet, manifest, or query to analyze")
    query: Optional[str] = Field(None, description="Alias for content (backward compatibility)")
    input_type: Optional[str] = Field("auto", description="Input type hint: auto, kubernetes, docker, jenkins, terraform, generic")
    context: Optional[Any] = Field(None, description="Environment context (cluster, namespace, service, environment)")
    options: Optional[Dict[str, Any]] = Field(None, description="Model tuning options (temperature, model_preference)")
    filters: Optional[Dict[str, Any]] = Field(None, description="Metadata filters for RAG retrieval")
    top_k: int = Field(4, ge=1, le=10, description="Number of reference documentation chunks to inject")

    @model_validator(mode="after")
    def validate_content_or_query(self):
        if not self.content and not self.query:
            raise ValueError("Either 'content' or 'query' must be provided.")
        if not self.content and self.query:
            self.content = self.query
        return self


class AnalyzeResponse(BaseModel):
    summary: str
    root_cause: str
    severity: str
    impact: str
    fix_steps: List[str]
    commands: List[CommandItem]
    explanation: str
    confidence: float
    references: List[ReferenceItem]
    detected_input_type: str
    sanitized_tokens_count: int
    provider_used: str
