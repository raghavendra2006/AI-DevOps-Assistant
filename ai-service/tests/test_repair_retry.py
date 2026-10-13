import pytest
from pydantic import BaseModel
from typing import List
from tests.conftest import MockLLMProvider


class SimpleDiagnostic(BaseModel):
    issue: str
    code: int
    fixes: List[str]


@pytest.mark.asyncio
async def test_structured_output_first_attempt():
    valid_json = '{"issue": "Memory Limit Exceeded", "code": 137, "fixes": ["Increase limit"]}'
    provider = MockLLMProvider(responses=[valid_json])

    result = await provider.generate_structured("Analyze", SimpleDiagnostic)
    assert result.issue == "Memory Limit Exceeded"
    assert result.code == 137
    assert len(result.fixes) == 1
    assert provider.call_count == 1


@pytest.mark.asyncio
async def test_structured_output_markdown_extraction():
    markdown_wrapped = '```json\n{"issue": "State Lock Error", "code": 500, "fixes": ["Force unlock"]}\n```'
    provider = MockLLMProvider(responses=[markdown_wrapped])

    result = await provider.generate_structured("Analyze", SimpleDiagnostic)
    assert result.issue == "State Lock Error"
    assert result.code == 500
    assert provider.call_count == 1


@pytest.mark.asyncio
async def test_structured_output_repair_retry_success():
    # First response has invalid JSON syntax, second response is repaired valid JSON
    malformed_json = '{"issue": "Broken syntax, no quotes or close brace'
    repaired_json = '{"issue": "Syntax Repaired", "code": 200, "fixes": ["All good"]}'
    provider = MockLLMProvider(responses=[malformed_json, repaired_json])

    result = await provider.generate_structured("Analyze", SimpleDiagnostic, max_repair_retries=2)
    assert result.issue == "Syntax Repaired"
    assert result.code == 200
    assert provider.call_count == 2


@pytest.mark.asyncio
async def test_structured_output_repair_retry_exhaustion():
    # All responses are invalid
    malformed_1 = 'Invalid output'
    malformed_2 = 'Still invalid'
    provider = MockLLMProvider(responses=[malformed_1, malformed_2])

    with pytest.raises(ValueError, match="Failed to validate LLM output against schema"):
        await provider.generate_structured("Analyze", SimpleDiagnostic, max_repair_retries=1)

    assert provider.call_count == 2
