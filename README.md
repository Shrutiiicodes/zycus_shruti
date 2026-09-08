# StockPulse — AI Inventory & Dynamic Pricing Engine

> **Hackathon Solution**  
> A reactive commerce advisor that automatically detects inventory threshold drops and demand velocity spikes, uses AI to recommend price adjustments and reorder quantities, and surfaces them to merchandising for human approval.

---

## 🚀 Quickstart (Under 5 Minutes)

### Prerequisites
- **Java 17+**
- **Node.js 18+** & `npm`

---

### 1. Start Backend (`http://localhost:8080`)

```bash
cd backend
# Set JAVA_HOME if needed
mvnw spring-boot:run
```

*The backend will automatically start on `http://localhost:8080` with pre-seeded products in H2 in-memory database (`/h2-console`).*

---

### 2. Start Frontend (`http://localhost:5173`)

In a separate terminal:

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173` in your browser.

---

## 💡 Architecture & Key Features

1. **Domain Model (`Product`, `PricingSuggestion`, `ReorderSuggestion`)**: Explicit state machines (`ACTIVE`, `PRICE_REVIEW_PENDING`, `OUT_OF_STOCK`) with Sprint 2 extension placeholders (`costPrice`, `supplierId`).
2. **Pluggable Commerce Engine**: Interface-driven strategy pattern (`CommerceAdvisor`). Easily switch between `rule-based` and `ai` (Qwen/Gemini/Groq) at runtime without restart (`commerce.strategy` property).
3. **Resilient AI Advisor**: Structured LLM context, strict output JSON parsing, bounds validation, and instant fallback to rule-based logic if LLM times out or returns invalid parameters.
4. **Agentic Recommendation Loop**: Event-driven decoupled architecture (`@EventListener` + `@Async`). Stock level changes and simulated sales fire async recommendation jobs for `INVENTORY_LOW` and `DEMAND_SPIKE`.
5. **Human-in-the-Loop Merchandising Console**: Interactive React console displaying confidence scores, detailed AI reasoning, trigger badges, and accept/reject controls.

---

## 🎯 Live Demo Walkthrough

1. **Observe Seeded State**: Product `PRD-003` (*Organic Cotton T-Shirt*) is seeded with **Stock: 8**, **Threshold: 15** (already low stock).
2. **Simulate a Sale**: Click **"Simulate Sale"** on `PRD-008` (*Hoodie — Heather Grey*) to bump its order velocity.
3. **Auto-Triggered Suggestions**: The background agentic loop evaluates inventory/demand signals and surfaces pending pricing and reorder suggestions with `INVENTORY_LOW` and `DEMAND_SPIKE` badges.
4. **Human Approval**: Click **Accept** on a pricing suggestion → `currentPrice` updates and product status returns to `ACTIVE`. Click **Accept** on a reorder suggestion → `stockLevel` increments (simulating inbound shipment).

---

## 📡 API Endpoint Summary

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/products` | Retrieve catalog with status and category filters |
| `POST` | `/api/products` | Create a new product |
| `PATCH` | `/api/products/{id}/stock` | Update stock level (triggers agentic loop if low) |
| `POST` | `/api/products/{id}/orders` | Simulate an order sale (decrements stock & bumps velocity) |
| `POST` | `/api/products/{id}/suggest-pricing` | On-demand pricing recommendation |
| `POST` | `/api/products/{id}/suggest-reorder` | On-demand reorder recommendation |
| `GET` | `/api/suggestions/pending` | Fetch all pending suggestions |
| `PATCH` | `/api/suggestions/pricing/{id}/accept` | Approve pricing recommendation |
| `PATCH` | `/api/suggestions/pricing/{id}/reject` | Reject pricing recommendation |
| `PATCH` | `/api/suggestions/reorder/{id}/accept` | Approve reorder recommendation |
| `PATCH` | `/api/suggestions/reorder/{id}/reject` | Reject reorder recommendation |

---

## 📄 Architecture Decision Record (ADR)
See [ADR.md](./ADR.md) for detailed decisions on commerce logic placement, strategy switchability, LLM resilience, and agentic loop decoupling.
