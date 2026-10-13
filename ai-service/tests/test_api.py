from unittest.mock import patch
from app.schemas.analyze import DevOpsAnalysisResult


def test_health_endpoint(test_client):
    response = test_client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert "documents_indexed" in data


def test_search_endpoint_missing_api_key(test_client):
    response = test_client.post("/api/v1/search", json={"query": "test query", "top_k": 3})
    assert response.status_code == 401
    assert "Missing X-API-Key" in response.json()["detail"]


def test_search_endpoint_invalid_api_key(test_client):
    response = test_client.post(
        "/api/v1/search",
        json={"query": "test query", "top_k": 3},
        headers={"X-API-Key": "invalid-secret-key"}
    )
    assert response.status_code == 403
    assert "Invalid X-API-Key" in response.json()["detail"]


def test_search_endpoint_authorized(test_client, auth_headers):
    response = test_client.post(
        "/api/v1/search",
        json={"query": "kubernetes crashloopbackoff", "top_k": 2},
        headers=auth_headers
    )
    assert response.status_code == 200
    data = response.json()
    assert "results" in data
    assert data["query"] == "kubernetes crashloopbackoff"


def test_ingest_text_endpoint(test_client, auth_headers):
    payload = {
        "content": "# Test Ingestion Document\nThis document tests REST API ingestion into ChromaDB.",
        "source": "api_test.md",
        "product": "test_product",
        "version": "1.0",
        "url": "https://test.aidevops.io"
    }
    response = test_client.post("/api/v1/ingest/text", json=payload, headers=auth_headers)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert data["chunks_indexed"] >= 1


def test_analyze_endpoint_authorized(test_client, auth_headers, mock_llm_provider):
    with patch("app.api.routes.analyze.LLMFactory.get_provider", return_value=mock_llm_provider):
        payload = {
            "query": "Pod failed with OOMKilled exit code 137",
            "task_type": "log_analysis",
            "top_k": 2
        }
        response = test_client.post("/api/v1/analyze", json=payload, headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert "analysis" in data
        assert data["analysis"]["task"] == "log_analysis"
        assert data["analysis"]["severity"] == "CRITICAL"
        assert len(data["analysis"]["recommended_actions"]) >= 1
