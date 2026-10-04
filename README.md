# BuyTogether

BuyTogether turns scattered group-chat shopping messages into one shared, editable shopping order.

## Problem

Group shopping requests get buried in chat. People repeat products, change quantities, and leave others guessing what to buy.

## Solution

Create a shopping group, paste the conversation, and analyze it with the Gemini API using **Gemma 4**. The app keeps each person's source request, flags missing quantities, normalizes obvious product variations, and builds a combined order that can be edited and checked off.

> Gemma 4 is used for natural-language shopping-request extraction and normalization. Java performs validation, aggregation and business logic.

The UI makes the transformation visible:

## Architecture

```text
React + TypeScript + Vite
        │ REST / JSON
        ▼
Spring Boot (Java 21) ───── Gemini API (Gemma 4)
        │
        ▼
    PostgreSQL
```

Gemma extracts a request's speaker, original message, product, normalized product name, quantity, unit, confidence, and ambiguity. It is instructed not to infer quantities or calculate totals. The backend validates its response, normalizes product spelling/plurals, performs aggregation, and persists the group's requests and shared order.

## Tech stack

- Backend: Java 21, Spring Boot, Spring Web, Spring Data JPA, Bean Validation, Maven
- Database: PostgreSQL (Docker Compose)
- AI: Gemini API with configurable Gemma model
- Frontend: React, TypeScript, Vite, Tailwind CSS, Axios

## Gemma 4 integration

The backend calls `https://generativelanguage.googleapis.com/v1beta/models/{GEMMA_MODEL}:generateContent` with the backend-only `GEMINI_API_KEY` header. It requests JSON output and parses the returned `requests` array. The API key is never sent to the browser.

- Default model: **`gemma-4-26b-a4b-it`**
- Override with `GEMMA_MODEL`
- Live analysis requires a valid Gemini API key and access to the configured model. No mock or local fallback is used by the production analyze endpoint.

## API overview

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/groups` | Create a group |
| `GET` | `/api/groups` | List groups |
| `GET` | `/api/groups/{id}` | Get a group's items and source requests |
| `POST` | `/api/groups/{id}/analyze` | Send chat to Gemma and rebuild the shared order |
| `GET` | `/api/groups/{id}/items` | List order items |
| `POST` | `/api/groups/{id}/items` | Add an order item |
| `PATCH` | `/api/items/{id}` | Edit an order item |
| `DELETE` | `/api/items/{id}` | Delete an order item |
| `PATCH` | `/api/items/{id}/status` | Mark an item open/completed |

## Demo conversation

```text
Rahul: I need 3 blue pens for the workshop.
Priya: Can someone get me 2 spiral notebooks?
Aman: I need five blue pens too.
Neha: I'll need one spiral notebook.
Rahul: Also get me one black marker.
Aman: Actually make that two black markers for me.
```

The structured requests preserve each person's quantities. Java aggregation should produce:

- Blue Pen → 8 (Rahul 3, Aman 5)
- Spiral Notebook → 3 (Priya 2, Neha 1)
- Black Marker → 3 (Rahul 1, Aman 2)

An unspecified quantity (for example, “I need some pens”) is shown as **Quantity required** and excluded from totals until clarified.

## Tests and build

The backend test profile uses an in-memory H2 database and mocks `GemmaService` for normal application tests. Production uses PostgreSQL and the real Gemini API client.

```powershell
cd backend/gemma-app
.\mvnw.cmd clean test
cd ..\..\frontend
npm run build
```
