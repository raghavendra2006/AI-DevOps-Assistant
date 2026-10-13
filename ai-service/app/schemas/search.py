from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field


class SearchRequest(BaseModel):
    query: str = Field(..., min_length=2, description="Semantic search query or error snippet")
    top_k: int = Field(5, ge=1, le=20, description="Maximum number of relevant chunks to retrieve")
    filters: Optional[Dict[str, Any]] = Field(None, description="Metadata dictionary filter (e.g. {'product': 'kubernetes'})")


class SearchResultItem(BaseModel):
    content: str
    metadata: Dict[str, Any]
    score: float


class SearchResponse(BaseModel):
    query: str
    total_results: int
    results: List[SearchResultItem]
