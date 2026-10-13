from fastapi import APIRouter
from app.api.routes import health, search, analyze, ingest

api_router = APIRouter()

# Include health check
api_router.include_router(health.router)

# Include v1 endpoints
api_router.include_router(search.router, prefix="/api/v1")
api_router.include_router(analyze.router, prefix="/api/v1")
api_router.include_router(ingest.router, prefix="/api/v1")
