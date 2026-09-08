# StockPulse — Architecture Decision Record

## 0a. Frontend framework
**Context:** Brief allows React 18 or Angular 17, must be documented.
**Options:** React 18 (Vite) vs Angular 17.
**Decision:** React 18 with Vite. Faster to scaffold and iterate; component model maps directly onto product table, suggestion cards, purchase order workflows, and audit console tabs.
**Tradeoffs:** No built-in DI or routing — managed state cleanly with custom hooks and tab views.

## 0b. LLM provider & Provider abstraction
**Context:** Brief allows Gemini, Groq, or Ollama; OpenAI-compatible Qwen endpoint supported.
**Options:** Gemini 1.5 Flash, Groq + Llama 3.1, Ollama local, Qwen (OpenAI-compatible).
**Decision:** Interface-based `LLMGateway` (`QwenLLMGateway` real, `FakeLLMGateway` for testing). Strategy configurable via `commerce.strategy` property.
**Tradeoffs:** System works fully offline using deterministic engines when LLM key is absent.

## 1. Where does commerce logic live?
**Context:** Pricing and reorder decisions need a home that won't accumulate persistence, eventing, and business rules into one god-class.
**Options:** (a) Fat service class doing everything, (b) domain model methods on `Product`, (c) dedicated `CommerceAdvisor` components + `CommerceEngineService` orchestrator.
**Decision:** (c). `CommerceAdvisor` implementations hold pure decision logic. `CommerceEngineService` handles persistence, idempotency checks, and product-status side effects.

## 2. Unified AI call vs separate pricing/reorder calls
**Context:** Every trigger needs both a pricing and a reorder recommendation.
**Options:** (a) One `CommerceAdvisor.advise()` call returning both, (b) two independent calls/interfaces.
**Decision:** (a), unified. Single LLM round trip per trigger; merchandising judgment (raise price vs clear stock) is naturally informed by the same context as reorder quantity.

## 3. Runtime strategy switching
**Context:** `commerce.strategy` must switch active advisor without a restart.
**Decision:** Spring `Map<String, CommerceAdvisor>` autowired by bean name, read via `StrategyRegistry`.

## 4. Architecture Shift — Deterministic Calculation + Guardrails + LLM Explanation
**Context:** Production roadmap recommendation: LLM should NOT invent critical numerical business decisions.
**Options:** (a) Raw LLM output for numbers, (b) Deterministic formula calculation $\rightarrow$ Business Guardrails validation $\rightarrow$ Mathematical confidence scoring $\rightarrow$ LLM contextual explanation $\rightarrow$ Human approval.
**Decision:** (b). `DeterministicCommerceCalculator` computes numbers mathematically using expected lead-time demand, safety stock, incoming inventory, and MOQ. `PricingPolicyEngine` enforces policy guardrails. `LLMGateway` generates contextual plain-English reasoning for the validated numbers.

## 5. Business Guardrails Engine (`PricingPolicyEngine`)
**Context:** Simple max price bounds (e.g. 5x current price) are insufficient as a real commercial policy.
**Decision:** `PricingPolicyEngine` enforces:
- **Margin Floor**: Minimum $15\%$ margin ($\text{Price} \ge \text{Cost} \times 1.15$).
- **Category Caps**: Max $+15\%$ increase for `ELECTRONICS`, $+20\%$ for `APPAREL`, $+10\%$ for `HOME`.
- **Decrease Cap**: Max $-25\%$ decrease limit.
- **Price Change Cooldown**: Blocks new pricing recommendations if product price changed within last 24 hours.
- **Psychological Rounding**: Applies `.99` price endings.

## 6. Mathematical System Confidence Scoring
**Context:** LLM-generated confidence numbers are uncalibrated.
**Decision:** `ConfidenceScorer` computes system confidence ($0.0 - 1.0$) mathematically based on data completeness, stock scarcity ratio, velocity stability, and policy compliance.

## 7. Realistic Purchase Order & Fulfillment Workflow
**Context:** Accepting a reorder suggestion should not instantly inflate physical inventory on hand.
**Decision:** Accepting a reorder suggestion creates a `PurchaseOrder` in `CREATED` status and increments `Product.incomingStock`. Physical stock level (`stockLevel`) is only incremented when goods are physically received (`PATCH /purchase-orders/{id}/receive`).

## 8. Concurrency Protection & Optimistic Locking
**Context:** Simultaneous orders or stock updates could oversell inventory or cause race conditions.
**Decision:** Added `@Version` optimistic locking to `Product` entity. Concurrent updates throw `OptimisticLockingFailureException` and trigger transaction rollbacks.

## 9. Transactional Outbox Pattern for Durable Workflows
**Context:** In-memory Spring events can be lost if JVM restarts during recommendation processing.
**Decision:** `OutboxEvent` table records triggers inside the database transaction. Scheduled `OutboxWorker` polls pending events and executes recommendation processing with automatic retry handling.

## 10. Enterprise Auditability & Telemetry
**Context:** Real-world commerce platforms require complete audit trails for pricing, stock, and recommendation decisions.
**Decision:** Implemented audit entities and endpoints:
- `PriceHistory`: Logs timestamp, old price, new price, actor, reason, and suggestion ID.
- `InventoryTransaction`: Logs `SALE`, `SHIPMENT_RECEIVED`, and `STOCK_ADJUSTMENT` deltas.
- `RecommendationAudit`: Logs strategy used, calculated values, applied guardrails, confidence, and approval history.

## 11. Idempotency
**Context:** Repeated stock/order events for the same product and trigger reason must not create unbounded duplicate `PENDING` suggestions.
**Decision:** Uniqueness key is `(productId, triggerReason, suggestionType, status=PENDING)`. `CommerceEngineService` checks `existsByProduct_IdAndTriggerReasonAndStatus` before creation.

## 12. Suggestion Acceptance & Product State
**Context:** Accepting a pricing suggestion updates `currentPrice` and clears `PRICE_REVIEW_PENDING` back to `ACTIVE` only if no other pricing suggestion remains `PENDING`. Rejecting either changes only the suggestion's status.