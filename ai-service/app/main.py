from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings
from app.core.logging import setup_logging, logger
from app.api.router import api_router
from app.rag.vector_store import VectorStore


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup
    setup_logging(settings.LOG_LEVEL)
    logger.info(f"Starting {settings.PROJECT_NAME} [{settings.ENVIRONMENT}]")
    logger.info(f"Active LLM Provider: {settings.LLM_PROVIDER}")

    # Ensure vector store directory exists
    try:
        vs = VectorStore()
        logger.info(f"ChromaDB initialized. Current documents indexed: {vs.count()}")
    except Exception as e:
        logger.warning(f"ChromaDB initialization notice: {e}")

    yield

    # Shutdown
    logger.info(f"Shutting down {settings.PROJECT_NAME}")


app = FastAPI(
    title=settings.PROJECT_NAME,
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount Routes
app.include_router(api_router)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
