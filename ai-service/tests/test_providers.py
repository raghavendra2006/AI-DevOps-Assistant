import pytest
from app.providers.factory import LLMFactory
from app.providers.gemini import GeminiProvider
from app.providers.openai_provider import OpenAIProvider
from app.providers.ollama_provider import OllamaProvider


def test_llm_factory_instantiation():
    gemini = LLMFactory.get_provider("gemini")
    assert isinstance(gemini, GeminiProvider)

    openai_p = LLMFactory.get_provider("openai")
    assert isinstance(openai_p, OpenAIProvider)

    ollama = LLMFactory.get_provider("ollama")
    assert isinstance(ollama, OllamaProvider)


def test_llm_factory_unsupported_provider():
    with pytest.raises(ValueError, match="Unsupported LLM provider 'anthropic'"):
        LLMFactory.get_provider("anthropic")


@pytest.mark.asyncio
async def test_mock_provider_generation(mock_llm_provider):
    result = await mock_llm_provider.generate("Test prompt")
    assert "OOMKilled" in result
    assert mock_llm_provider.call_count == 1
