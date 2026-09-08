# StockPulse — AI Inventory & Dynamic Pricing Platform

> **Production-Grade Enterprise Architecture**  
> A reactive commerce advisor that automatically detects inventory threshold drops and demand velocity spikes, uses a deterministic optimization engine and business guardrails to calculate prices and reorder quantities, leverages LLMs for contextual explanations, and surfaces complete audit trails to merchandising.

---

## 🚀 Quickstart (Under 5 Minutes)

### Prerequisites
- **Java 17+**
- **Node.js 18+** & `npm`

---

### 1. Start Backend (`http://localhost:8080`)

```bash
cd backend
# Optional: export LLM_API_KEY=your-key (works fully offline with deterministic rules by default)
mvnw spring-boot:run
```

*The backend starts on `http://localhost:8080` with pre-seeded products in H2 in-memory database (`/h2-console`).*

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

## 💡 Production Architecture & Key Features

### 🏛️ 5-Step Execution Pipeline
```
  Order / Stock Event
         │
         ▼
  Transactional Inventory Service (Optimistic Locking & Outbox Event)
         │
         ▼
  Outbox Worker (Durable Event Processing)
         │
         ▼
  Deterministic Pricing & Replenishment Optimizer
         │
         ▼
  Business Guardrails Engine (Margin Floors, Category Caps, Cooldowns)
         │
         ▼
  LLM Explanation Layer (Contextual Reasoning for Validated Numbers)
         │
         ▼
  Human Approval / Purchase Order Fulfillment
```

1. **Deterministic Calculation**: Numerical prices and reorder quantities are computed mathematically using expected lead-time demand, safety stock, incoming inventory, and MOQ:
   $$\text{Reorder Quantity} = \max(\text{MOQ}, \text{LeadTimeDemand} + \text{SafetyStock} - \text{StockOnHand} - \text{IncomingStock})$$
2. **Business Guardrails Policy Engine (`PricingPolicyEngine`)**:
   - **Margin Floor**: Enforces $15\%$ minimum margin ($\text{Price} \ge \text{Cost} \times 1.15$).
   - **Category Increase Caps**: Enforces max $+15\%$ for `ELECTRONICS`, $+20\%$ for `APPAREL`, $+10\%$ for `HOME`.
   - **Decrease Cap**: Enforces max $-25\%$ price decrease.
   - **Price Change Cooldown**: Blocks repeated price changes within a 24-hour window.
   - **Psychological Rounding**: Applies `.99` price endings.
3. **Mathematical Confidence Scoring (`ConfidenceScorer`)**: Calculates system confidence ($0.0 - 1.0$) based on data completeness, stock scarcity ratio, and policy compliance.
4. **Realistic Purchase Order Workflow**: Accepting a reorder recommendation creates a `PurchaseOrder` in `CREATED` status and updates `incomingStock`. Ingesting physical shipments (`PATCH /purchase-orders/{id}/receive`) increments physical stock and decrements `incomingStock`.
5. **Complete Enterprise Auditability**: `PriceHistory`, `InventoryTransaction`, and `RecommendationAudit` telemetry logs all pricing changes, inventory movements, system confidence, and decision trails.
6. **Transactional Outbox Pattern**: `OutboxEvent` & `@Scheduled` `OutboxWorker` process signal triggers reliably without in-memory event loss across JVM restarts.

---

## 🎯 Live Demo Walkthrough

1. **Merchandising Console**:
   - Observe `PRD-003` (*Organic Cotton T-Shirt*), pre-seeded with low stock (8 units vs 15 threshold).
   - Click **"+ Add Product"** to add any new item dynamically.
   - Click **"Simulate Sale"** on `PRD-008` (*Hoodie*) to trigger demand velocity spike recommendations.
2. **Purchase Orders & Fulfillment Tab**:
   - Click **Accept** on a reorder recommendation $\rightarrow$ a Purchase Order is created in `CREATED` status with `incomingStock` updated.
   - Click **"Receive Shipment"** $\rightarrow$ physical stock increments, incoming stock decrements, and an `InventoryTransaction` is logged.
3. **Audit Trails & History Tab**:
   - Inspect historical logs for **Price History**, **Inventory Transactions**, and **Recommendation Audits**.

---

## 📡 API Endpoint Summary

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/products` | Retrieve catalog with status and category filters |
| `POST` | `/api/products` | Create a new product |
| `PATCH` | `/api/products/{id}/stock` | Update stock level (logs transaction & fires outbox trigger) |
| `POST` | `/api/products/{id}/orders` | Simulate an order sale (decrements stock & bumps velocity) |
| `POST` | `/api/products/{id}/suggest-pricing` | On-demand pricing recommendation |
| `POST` | `/api/products/{id}/suggest-reorder` | On-demand reorder recommendation |
| `GET` | `/api/suggestions/pending` | Fetch all pending suggestions |
| `PATCH` | `/api/suggestions/pricing/{id}/accept` | Approve pricing (updates price & logs `PriceHistory`) |
| `PATCH` | `/api/suggestions/reorder/{id}/accept` | Approve reorder (creates `PurchaseOrder` & updates `incomingStock`) |
| `GET` | `/api/purchase-orders` | List purchase orders |
| `PATCH` | `/api/purchase-orders/{id}/receive` | Receive shipment (increments physical stock & logs `InventoryTransaction`) |
| `GET` | `/api/audit/price-history` | Fetch price change history logs |
| `GET` | `/api/audit/inventory-transactions` | Fetch inventory transaction logs |
| `GET` | `/api/audit/recommendations` | Fetch recommendation decision audits |

---

## 📄 Architecture Decision Record (ADR)
See [ADR.md](./ADR.md) for detailed engineering decisions on deterministic calculation, policy guardrails, purchase order workflows, outbox events, and auditability.
