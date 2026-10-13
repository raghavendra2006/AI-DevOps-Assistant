from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


class AnalyzeRequest(BaseModel):
    query: str = Field(..., min_length=3, description="DevOps log snippet, manifest, or query to analyze")
    task_type: str = Field("log_analysis", description="Task classification: log_analysis, manifest_review, command_help, pipeline_fix")
    context: Optional[str] = Field(None, description="Optional extra user-provided context or terminal output")
    filters: Optional[Dict[str, Any]] = Field(None, description="Metadata filters for RAG retrieval")
    top_k: int = Field(4, ge=1, le=10, description="Number of reference documentation chunks to inject")


class DevOpsAnalysisResult(BaseModel):
    task: str = Field(..., description="Classification of the analyzed DevOps task")
    root_cause: str = Field(..., description="Root cause explanation or technical diagnosis")
    severity: str = Field("HIGH", description="Severity level: LOW, MEDIUM, HIGH, CRITICAL")
    affected_component: str = Field(..., description="Component or subsystem involved (e.g. k8s_pod, terraform_state)")
    recommended_actions: List[str] = Field(..., description="Concrete, actionable steps to resolve the issue")
    confidence_score: float = Field(0.9, ge=0.0, le=1.0, description="Model self-assessed confidence level")
    citations: List[str] = Field(default_factory=list, description="Referenced documentation sources")


class AnalyzeResponse(BaseModel):
    query: str
    analysis: DevOpsAnalysisResult
    retrieved_sources_count: int
    provider_used: str
