# Translation Service

> **Status:** Development / Testing only — will be replaced with a cloud provider in production.

A Spring Boot micro-service that translates text into one or more target languages by calling a
locally hosted [Ollama](https://ollama.com) LLM (default model: **mistral**).

---

## Table of Contents

- [Architecture](#architecture)
- [API Reference](#api-reference)
- [Quick Start — Docker Compose](#quick-start--docker-compose)
- [Quick Start — Gradle (no Docker)](#quick-start--gradle-no-docker)
- [Configuration](#configuration)
- [Caching](#caching)
- [Health Check](#health-check)
- [Running Tests](#running-tests)
- [Replacing the Backend](#replacing-the-backend)
- [Limitations](#limitations)

---

## Architecture

```
Caller ──► POST /api/translate
              │
              ▼
    TranslationController
              │
              ▼
    TranslationService (interface)
              │
              ▼
    OllamaTranslationService          ◄── Caffeine cache (24 h TTL)
              │
              ▼
    OllamaClient ──► POST http://ollama:11434/api/generate
                              │
                              ▼
                        Ollama LLM (mistral)
```

All public interfaces live in the `service` and `model` packages. Swapping the backend requires
only a new `TranslationService` `@Bean` — the controller and DTOs remain unchanged.

---

## API Reference

### `POST /api/translate`

**Content-Type:** `application/json` (UTF-8)

#### Request Body

| Field              | Type            | Required | Description                               |
|--------------------|-----------------|----------|-------------------------------------------|
| `textToTranslate`  | `string`        | ✅       | Source text (max 50 000 characters)       |
| `sourceLanguage`   | `string`        | ✅       | BCP-47 source language code (e.g. `"en"`) |
| `languageCodes`    | `array<string>` | ✅       | One or more BCP-47 target language codes  |

```json
{
  "textToTranslate": "Hello world. Please translate this document.",
  "sourceLanguage": "en",
  "languageCodes": ["ja", "ru"]
}
```

#### Response Body (200 OK)

```json
{
  "cognitiveServicesUsage": {
    "visionImageTypeTransactionCount": 0,
    "translatorTranslateTextCharacterCount": 88
  },
  "imageAnalysisResults": null,
  "translationResults": [
    {
      "translatedTimestamp": 1776694594,
      "languageCode": "ja",
      "value": "こんにちは、世界。この文書を翻訳してください。"
    },
    {
      "translatedTimestamp": 1776694596,
      "languageCode": "ru",
      "value": "Привет мир. Пожалуйста, переведите этот документ."
    }
  ],
  "recognizedText": null,
  "summarizedText": null,
  "tenantId": null,
  "attachmentId": null,
  "extractOnDisk": false
}
```

#### Error Responses (RFC 7807 Problem Detail)

| Status | When                                       |
|--------|--------------------------------------------|
| 400    | Blank/missing required field               |
| 422    | Non-recoverable translation failure        |
| 500    | Unexpected internal error                  |

---

## Quick Start — Docker Compose

### Prerequisites

- Docker Desktop (or Docker Engine with BuildKit)
- `./gradlew :translation-service:bootJar` must have been run at least once

### 1 — Build the JAR

```bash
# From api/ directory
./gradlew :translation-service:bootJar
```

### 2 — Pull the Mistral model (~4 GB, first run only)

```bash
docker compose -f docker-compose.dev.yml run --rm ollama-init
```

### 3 — Start the stack

```bash
docker compose -f docker-compose.dev.yml up
```

### 4 — Test the API

```bash
curl -s -X POST http://localhost:8083/api/translate \
  -H 'Content-Type: application/json' \
  -d '{
    "textToTranslate": "Hello world. Please translate this document.",
    "sourceLanguage": "en",
    "languageCodes": ["ja"]
  }' | jq
```

### 5 — Stop

```bash
docker compose -f docker-compose.dev.yml down
# To also remove the Ollama model volume (frees ~4 GB):
docker compose -f docker-compose.dev.yml down -v
```

---

## Quick Start — Gradle (no Docker)

Requires Ollama installed locally: <https://ollama.com/download>

```bash
# Pull the model once
ollama pull mistral

# Start Ollama (if not running)
ollama serve

# Run the service (from api/ directory)
./gradlew :translation-service:bootRun
```

The service starts on **port 8083** with `application-dev.yml` using `http://localhost:11434`.

---

## Configuration

All properties are under the `ollama.*` namespace:

| Property                        | Default                    | Description                             |
|---------------------------------|----------------------------|-----------------------------------------|
| `ollama.base-url`               | `http://localhost:11434`   | URL of the Ollama REST API              |
| `ollama.model`                  | `mistral`                  | Ollama model tag to use                 |
| `ollama.timeout-seconds`        | `120`                      | Max seconds to wait for generation      |
| `ollama.connect-timeout-seconds`| `10`                       | TCP connect timeout                     |

Override via environment variables in Docker Compose:

```yaml
environment:
  - OLLAMA_BASE_URL=http://my-ollama-host:11434
  - OLLAMA_MODEL=llama3
```

### Switching Models

Any model available in your Ollama installation can be used:

```bash
ollama pull llama3   # or gemma2, phi3, etc.
```

Then set `OLLAMA_MODEL=llama3`.

---

## Caching

Results are cached in-memory using [Caffeine](https://github.com/ben-manes/caffeine):

| Cache           | Key                             | Max entries | TTL     |
|-----------------|---------------------------------|-------------|---------|
| `translations`  | `text + srcLang + targetLang`   | 2 000       | 24 h    |
| `ollama-health` | `"ping"`                        | 1           | 30 s    |

Cache statistics are recorded and visible via Micrometer/Actuator metrics.

---

## Health Check

```bash
GET http://localhost:8083/actuator/health
```

Response when healthy:

```json
{
  "status": "UP",
  "components": {
    "ollama": {
      "status": "UP"
    }
  }
}
```

---

## Running Tests

```bash
# Unit tests only (no Docker required)
./gradlew :translation-service:test

# Full check including JaCoCo coverage (≥ 70 %)
./gradlew :translation-service:check
```

---

## Replacing the Backend

1. Create a new class implementing `TranslationService`.
2. Annotate it with `@Service` and `@Primary` (or use a profile-conditional `@Bean`).
3. The controller, models, and cache layer require **zero changes**.

Example skeleton:

```java
@Primary
@Service
@Profile("production")
public class AzureTranslationService implements TranslationService {
    @Override
    public TranslationResponse translate(TranslationRequest request) {
        // call Azure AI Translator REST API
    }
}
```

---

## Limitations

- **Not production-ready**: no authentication; intended for dev/test only.
- **Model quality**: Mistral translation quality varies by language pair. For high-quality output use a dedicated translation service.
- **Latency**: LLM inference is slow (~5–30 s per language). Caching mitigates this for repeated requests.
- **Memory**: Ollama requires ~6–8 GB RAM to run Mistral. Ensure Docker has sufficient memory allocated.

