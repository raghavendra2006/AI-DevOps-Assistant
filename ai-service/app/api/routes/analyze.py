from fastapi import APIRouter, Depends
from app.core.security import verify_api_key
from app.core.config import settings
from app.schemas.analyze import AnalyzeRequest, AnalyzeResponse, DevOpsAnalysisResult
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
    Analyzes a DevOps error log, manifest, or command query using RAG context injection
    and validates the LLM output into a structured schema with an automated self-healing repair loop.
    Requires X-API-Key authentication.
    """
    retriever = Retriever()
    retrieved_chunks = retriever.retrieve(
        query=request.query,
        top_k=request.top_k,
        filters=request.filters,
    )

    full_query = request.query
    if request.context:
        full_query += f"\n\nADDITIONAL ENVIRONMENT CONTEXT:\n{request.context}"

    prompt = PromptBuilder.build_rag_prompt(
        query=full_query,
        context_chunks=retrieved_chunks,
        include_few_shots=True,
    )
    system_prompt = PromptBuilder.build_system_prompt()

    provider = LLMFactory.get_provider()
    analysis_result = await provider.generate_structured(
        prompt=prompt,
        schema=DevOpsAnalysisResult,
        system_prompt=system_prompt,
        max_repair_retries=2,
    )

    # Attach citation metadata
    citations = []
    for chunk in retrieved_chunks:
        meta = chunk.get("metadata", {})
        source_label = f"{meta.get('product', 'doc').upper()}: {meta.get('source', 'unknown')}"
        if meta.get("url"):
            source_label += f" ({meta['url']})"
        if source_label not in citations:
            citations.append(source_label)

    if citations and not analysis_result.citations:
        analysis_result.citations = citations

    return AnalyzeResponse(
        query=request.query,
        analysis=analysis_result,
        retrieved_sources_count=len(retrieved_chunks),
        provider_used=settings.LLM_PROVIDER,
    )
