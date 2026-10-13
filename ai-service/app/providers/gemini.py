from typing import Any, Optional
from tenacity import retry, stop_after_attempt, wait_exponential
from app.providers.base import BaseLLMProvider
from app.core.config import settings
from app.core.logging import logger

try:
    from google import genai
    from google.genai import types
    USE_GENAI_SDK = True
except ImportError:
    import google.generativeai as genai
    USE_GENAI_SDK = False


class GeminiProvider(BaseLLMProvider):
    """Google Gemini LLM Provider with exponential backoff retry handling."""

    def __init__(self, api_key: Optional[str] = None, model_name: Optional[str] = None):
        self.api_key = api_key or settings.GEMINI_API_KEY
        self.model_name = model_name or settings.GEMINI_MODEL
        self.client = None

        if self.api_key:
            if USE_GENAI_SDK:
                self.client = genai.Client(api_key=self.api_key)
            else:
                genai.configure(api_key=self.api_key)
        else:
            logger.warning("GEMINI_API_KEY is not configured. Live Gemini calls will fail unless provided.")

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=1, max=10),
        reraise=True
    )
    async def generate(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        temperature: float = 0.2,
        max_tokens: int = 2048,
        **kwargs: Any
    ) -> str:
        if not self.api_key:
            raise ValueError("Cannot invoke Gemini: GEMINI_API_KEY is not configured.")

        if USE_GENAI_SDK and self.client:
            config = types.GenerateContentConfig(
                temperature=temperature,
                max_output_tokens=max_tokens,
                system_instruction=system_prompt if system_prompt else None,
            )
            response = await self.client.aio.models.generate_content(
                model=self.model_name,
                contents=prompt,
                config=config,
            )
            return (response.text or "").strip()
        else:
            model = genai.GenerativeModel(
                model_name=self.model_name,
                system_instruction=system_prompt if system_prompt else None,
                generation_config=genai.GenerationConfig(
                    temperature=temperature,
                    max_output_tokens=max_tokens,
                )
            )
            response = await model.generate_content_async(prompt)
            return (response.text or "").strip()
