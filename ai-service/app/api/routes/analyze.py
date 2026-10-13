import json
from fastapi import APIRouter, Depends
from app.core.security import verify_api_key
from app.core.config import settings
from app.schemas.analyze import (
    AnalyzeRequest,
    AnalyzeResponse,
    DevOpsAnalysisResult,
    ReferenceItem,
    CommandItem,
)
from app.ml.classifier import InputClassifier
from app.ml.preprocessor import LogPreprocessor
from app.ml.command_risk import CommandRiskClassifier
from app.rag.retriever import Retriever
from app.prompts.builder import PromptBuilder
from app.providers.factory import LLMFactory

router = APIRouter(prefix="/analyze", tags=["AI DevOps Analysis"])


@router.post("", response_model=AnalyzeResponse)
async def analyze_devops_issue(
    request: AnalyzeRequest,
    _api_key: str = Depends(verify_api_key),
):
    """
    Analyzes a DevOps error log or incident using Phase 0 contract:
    1. Input classification (kubernetes, docker, jenkins, terraform, generic).
    2. Secret redaction and log truncation.
    3. RAG vector retrieval from ChromaDB.
    4. Structured LLM diagnosis with repair-retry loop.
    5. Deterministic command risk classification guardrail (SAFE, CAUTION, DANGEROUS).
    """
    raw_content = request.content or request.query or ""

    # 1. Classification
    detected_type, _ = InputClassifier.classify(raw_content, user_hint=request.input_type)

    # 2. Secret Redaction & Truncation
    sanitized_content, redacted_count = LogPreprocessor.redact_secrets(raw_content)
    processed_content = LogPreprocessor.truncate_log(sanitized_content)

    # 3. RAG Retrieval with metadata filters if detected
    retriever = Retriever()
    filters = request.filters or {}
    if not filters and detected_type in ("kubernetes", "docker", "jenkins", "terraform"):
        # Prioritize matching product if available
        filters = {"product": detected_type}

    try:
        retrieved_chunks = retriever.retrieve(
            query=processed_content[:500],
            top_k=request.top_k,
            filters=filters,
        )
    except Exception:
        # Fallback to no filter if product filter returned empty or errored
        retrieved_chunks = retriever.retrieve(
            query=processed_content[:500],
            top_k=request.top_k,
            filters=None,
        )

    # 4. Prompt Assembly
    context_str = ""
    if request.context:
        if isinstance(request.context, dict):
            context_str = json.dumps(request.context, indent=2)
        else:
            context_str = str(request.context)

    full_query = f"TECHNOLOGY: {detected_type.upper()}\n\nLOG:\n{processed_content}"
    if context_str:
        full_query += f"\n\nENVIRONMENT CONTEXT:\n{context_str}"

    prompt = PromptBuilder.build_rag_prompt(
        query=full_query,
        context_chunks=retrieved_chunks,
        include_few_shots=True,
    )
    system_prompt = PromptBuilder.build_system_prompt()

    # 5. LLM Structured Generation
    provider = LLMFactory.get_provider()
    analysis_result: DevOpsAnalysisResult = await provider.generate_structured(
        prompt=prompt,
        schema=DevOpsAnalysisResult,
        system_prompt=system_prompt,
        max_repair_retries=2,
    )

    # 6. Deterministic Command Risk Guardrail
    for cmd in analysis_result.commands:
        cmd.risk_level = CommandRiskClassifier.classify_command(cmd.command, cmd.risk_level)

    # 7. Citations & References formatting
    references: list[ReferenceItem] = []
    for chunk in retrieved_chunks:
        meta = chunk.get("metadata", {})
        title = meta.get("source", "DevOps Knowledge Base")
        source = meta.get("product", "knowledge_base")
        url = meta.get("url")
        references.append(
            ReferenceItem(
                title=title,
                source=source,
                doc_id=chunk.get("id"),
                url=url,
            )
        )

    if not analysis_result.references and references:
        analysis_result.references = references

    return AnalyzeResponse(
        summary=analysis_result.summary,
        root_cause=analysis_result.root_cause,
        severity=analysis_result.severity,
        impact=analysis_result.impact,
        fix_steps=analysis_result.fix_steps,
        commands=analysis_result.commands,
        explanation=analysis_result.explanation,
        confidence=analysis_result.confidence,
        references=analysis_result.references,
        detected_input_type=detected_type,
        sanitized_tokens_count=redacted_count,
        provider_used=settings.LLM_PROVIDER,
    )
