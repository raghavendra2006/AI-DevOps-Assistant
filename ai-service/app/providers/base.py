from abc import ABC, abstractmethod
from typing import Any, Dict, Optional, Type, TypeVar
import json
import re
from pydantic import BaseModel, ValidationError
from app.core.logging import logger

T = TypeVar("T", bound=BaseModel)


class BaseLLMProvider(ABC):
    """Abstract Base Class for all LLM providers (Gemini, OpenAI, Ollama)."""

    @abstractmethod
    async def generate(
        self,
        prompt: str,
        system_prompt: Optional[str] = None,
        temperature: float = 0.2,
        max_tokens: int = 2048,
        **kwargs: Any
    ) -> str:
        """Generates plain text completion from the provider."""
        pass

    async def generate_structured(
        self,
        prompt: str,
        schema: Type[T],
        system_prompt: Optional[str] = None,
        temperature: float = 0.1,
        max_repair_retries: int = 2,
        **kwargs: Any
    ) -> T:
        """
        Generates structured JSON validated against a Pydantic schema,
        with an automated self-healing repair-retry loop for malformed outputs.
        """
        schema_json = json.dumps(schema.model_json_schema(), indent=2)
        augmented_system = (
            (system_prompt or "")
            + "\n\nCRITICAL: You must reply STRICTLY with a valid JSON object matching this schema. "
            + "Do NOT wrap with markdown code blocks, do NOT prepend or append explanations.\n"
            + f"JSON SCHEMA:\n{schema_json}"
        ).strip()

        current_prompt = prompt
        attempts = 0

        while attempts <= max_repair_retries:
            raw_output = await self.generate(
                prompt=current_prompt,
                system_prompt=augmented_system,
                temperature=temperature,
                **kwargs
            )

            # Strip markdown formatting if present
            cleaned_output = self._extract_json_substring(raw_output)

            try:
                parsed_instance = schema.model_validate_json(cleaned_output)
                return parsed_instance
            except (ValidationError, json.JSONDecodeError) as err:
                attempts += 1
                logger.warning(
                    f"LLM output validation failed (attempt {attempts}/{max_repair_retries + 1}): {err}"
                )
                if attempts > max_repair_retries:
                    logger.error(f"Failed to produce valid structured output after {attempts} attempts.")
                    raise ValueError(
                        f"Failed to validate LLM output against schema {schema.__name__}: {err}. "
                        f"Raw Output: {raw_output}"
                    ) from err

                # Formulate targeted repair prompt
                current_prompt = (
                    f"Original Task: {prompt}\n\n"
                    f"Your previous output failed validation with error:\n{str(err)}\n\n"
                    f"Previous Malformed Output:\n{raw_output}\n\n"
                    f"Please repair the output and return ONLY valid JSON matching the schema."
                )

        raise RuntimeError("Unreachable repair retry termination")

    @staticmethod
    def _extract_json_substring(text: str) -> str:
        """Extracts JSON substring from raw markdown code blocks or text."""
        text = text.strip()
        # Remove ```json ... ``` blocks
        if text.startswith("```"):
            pattern = r"```(?:json)?\s*([\s\S]*?)\s*```"
            match = re.search(pattern, text)
            if match:
                return match.group(1).strip()

        # If wrapped between { and }
        first_brace = text.find("{")
        last_brace = text.rfind("}")
        if first_brace != -1 and last_brace != -1 and last_brace > first_brace:
            return text[first_brace : last_brace + 1]

        return text
