from fastapi import APIRouter, Depends
from app.core.security import verify_api_key
from app.schemas.search import SearchRequest, SearchResponse, SearchResultItem
from app.rag.retriever import Retriever

router = APIRouter(prefix="/search", tags=["RAG Retrieval"])


@router.post("", response_model=SearchResponse)
async def search_documents(
    request: SearchRequest,
    _api_key: str = Depends(verify_api_key),
):
    """
    Performs semantic vector search against indexed DevOps documentation with metadata filtering.
    Requires X-API-Key authentication.
    """
    retriever = Retriever()
    raw_results = retriever.retrieve(
        query=request.query,
        top_k=request.top_k,
        filters=request.filters,
    )

    items = [
        SearchResultItem(
            content=r["content"],
            metadata=r["metadata"],
            score=r["score"],
        )
        for r in raw_results
    ]

    return SearchResponse(
        query=request.query,
        total_results=len(items),
        results=items,
    )
