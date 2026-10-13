import pytest
from typing import Any, Optional
from fastapi.testclient import TestClient
from app.main import app
from app.core.config import settings
from app.providers.base import BaseLLMProvider
from app.rag.vector_store import VectorStore


class MockLLMProvider(BaseLLMProvider):
    """Predictable mock provider for unit & integration testing."""

    def __init__(self, responses: Optional[list] = None):
        self.responses = responses or [
            '{"summary": "Container in pod billing-api crashed due to OOMKilled condition.", "root_cause": "OOMKilled exit code 137", "severity": "CRITICAL", "impact": "Pod is failing readiness checks and restarting.", "fix_steps": ["Increase memory limit to 1.5Gi"], "commands": [{"command": "kubectl describe pod billing-api", "description": "Inspect pod termination status", "risk_level": "SAFE"}], "explanation": "Linux kernel killed container exceeding memory.", "confidence": 0.99, "references": []}'
        ]
        self.call_count = 0

    async def generate(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        temperature: float = 0.2,
        max_tokens: int = 2048,
        **kwargs: Any
    ) -> str:
        idx = min(self.call_count, len(self.responses) - 1)
        resp = self.responses[idx]
        self.call_count += 1
        return resp


@pytest.fixture
def mock_llm_provider():
    return MockLLMProvider()


@pytest.fixture
def in_memory_vector_store():
    return VectorStore(in_memory=True)


@pytest.fixture
def test_client():
    return TestClient(app)


@pytest.fixture
def auth_headers():
    return {"X-API-Key": settings.AI_SERVICE_API_KEY}
