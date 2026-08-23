# StockPulse — Architecture Decision Record

## 0a. Frontend framework
**Context:** Brief allows React 18 or Angular 17, must be documented.
**Options:** React 18 (Vite) vs Angular 17.
**Decision:** React 18 with Vite. Faster to scaffold and iterate within a 3-hour window; component model maps directly onto the product table + suggestion cards without extra boilerplate.
**Tradeoffs:** No built-in DI, routing, or forms module — irrelevant here since the console has one screen and no routes.

## 0b. LLM provider
**Context:** Brief allows Gemini, Groq, or Ollama; we used an OpenAI-compatible Qwen endpoint instead.
**Options:** Gemini 1.5 Flash, Groq + Llama 3.1, Ollama local, Qwen (OpenAI-compatible).
**Decision:** Qwen via `/chat/completions`, same shape as the Groq/Ollama branches in the provided `LLMGateway` reference. `LLMGateway` is an interface (`QwenLLMGateway` real, `FakeLLMGateway` deterministic for dev/testing), so the provider is a config-level choice, not a code dependency.
**Tradeoffs:** Only one real provider implemented; adding Gemini later means one more `LLMGateway` implementation, no other changes.

## 1. Where does commerce logic live?
**Context:** Pricing and reorder decisions need a home that won't accumulate persistence, eventing, and business rules into one god-class.
**Options:** (a) Fat service class doing everything, (b) domain model methods on `Product`, (c) dedicated `CommerceAdvisor` components + a thin `CommerceEngineService` orchestrator.
**Decision:** (c). `CommerceAdvisor` implementations hold pure decision logic (no persistence, no I/O beyond the LLM call). `CommerceEngineService` owns persistence, idempotency checks, and product-status side effects. `ProductService`/`SuggestionService` own their own entity's lifecycle only.
**Tradeoffs:** More files/classes than a single service; worth it because each class has one reason to change, and it's what let the on-demand and async paths share `CommerceEngineService.generateSuggestions` verbatim.

## 2. Unified AI call vs separate pricing/reorder calls
**Context:** Every trigger needs both a pricing and a reorder recommendation.
**Options:** (a) One `CommerceAdvisor.advise()` call returning both, (b) two independent calls/interfaces.
**Decision:** (a), unified. One LLM round trip per trigger (cost/latency), and the merchandising judgment (raise price vs clear stock) is naturally informed by the same context as the reorder quantity — splitting them would mean duplicating the product/trigger context into two prompts for no real independence benefit.
**Tradeoffs:** A single malformed/failed AI response drops both recommendations to rule-based fallback together, not independently. Accepted this because the unified fallback (both-or-nothing rule-based) is simpler to reason about and test than partial fallback, and the rule-based baseline is solid enough on its own to not need per-field independence. On-demand endpoints (`/suggest-pricing`, `/suggest-reorder`) still generate both suggestion types under the hood even when only one was requested — documented as a deliberate consequence, not a bug.

## 3. Runtime strategy switching
**Context:** `commerce.strategy` must switch active advisor without a restart, and a sprint-2 `CompetitorAwareStrategy` must plug in without touching existing code.
**Options:** (a) if/else branching on a config value, (b) factory class, (c) Spring `Map<String, CommerceAdvisor>` autowired by bean name, read via a `StrategyRegistry`.
**Decision:** (c). Spring auto-populates the map from all `@Component("name")`-annotated `CommerceAdvisor` beans. `StrategyRegistry` reads `commerce.strategy` per call (not cached at startup), so a config change takes effect on the next request. A `CompetitorAwareStrategy` only needs `@Component("competitor-aware")` implementing `CommerceAdvisor` — the map grows automatically, `StrategyRegistry` needs zero changes.
**Tradeoffs:** Typos in `commerce.strategy` fail at request time rather than startup — acceptable for a 3-hour build; a `@PostConstruct` validation check would be the sprint-2 hardening.

## 4. LLM failure handling
**Context:** Timeouts, malformed JSON, and absurd values (e.g. $999,999) must not silently drop a recommendation.
**Options:** (a) Return null/empty on failure, (b) retry the LLM call, (c) fall back to the rule-based advisor for the complete pair.
**Decision:** (c). `AICommerceAdvisor.advise()` wraps the whole LLM→parse→validate pipeline in one try/catch; any exception (timeout, `AIParsingException`, `AIValidationException`) returns `fallback.advise(...)` — same method signature, caller never knows the difference. Validation checks: price positive and within a configurable multiple of current price (default 5x), reorder quantity a positive integer, confidence in [0,1], direction a valid enum.
**Tradeoffs:** No retry — a transient network blip goes straight to rule-based rather than trying again. Chose this for the demo window: a predictable fallback is more valuable to show working than a retry loop that might still fail live.

## 5. Agentic loop trigger and decoupling
**Context:** Stock/order mutations must not block on suggestion generation, and both trigger types need independent, precise evaluation.
**Options:** (a) Scheduled poller, (b) synchronous suggestion generation inline in the request, (c) `ApplicationEventPublisher` + `@EventListener @Async`.
**Decision:** (c). `TriggerEvaluator` checks both `INVENTORY_LOW` (stock < threshold) and `DEMAND_SPIKE` (velocity > 3x category average) independently after any stock/order mutation and publishes one `ProductSignalEvent` per satisfied condition. `RecommendationEventListener` runs on a dedicated thread pool (`AsyncConfig`), re-fetches the product, and calls the same `CommerceEngineService.generateSuggestions` the on-demand endpoints use.
**Tradeoffs:** In-memory event bus — a JVM restart between publish and listener execution would drop the signal. Acceptable for a single-instance demo; a durable queue (Kafka/outbox pattern) is the sprint-2/3 answer if this needs to survive restarts or scale horizontally.

## 6. Idempotency
**Context:** Repeated stock/order events for the same product and trigger reason must not create unbounded duplicate `PENDING` suggestions.
**Decision:** Uniqueness key is `(productId, triggerReason, suggestionType, status=PENDING)`. `CommerceEngineService` checks `existsByProduct_IdAndTriggerReasonAndStatus` independently for pricing and reorder before creating either — so a product can have a `PENDING` `INVENTORY_LOW` pricing suggestion and a `PENDING` `DEMAND_SPIKE` pricing suggestion simultaneously (different keys), but never two `PENDING` `INVENTORY_LOW` pricing suggestions.
**Tradeoffs:** Once a suggestion is accepted/rejected, the next qualifying event creates a fresh one — no cooldown window. Sprint 2's "price change cooldown" roadmap item is exactly this gap, deliberately deferred.

## 7. Suggestion acceptance and product state
**Context:** Pricing and reorder suggestions can coexist per product with independent approval states; accepting one changes `Product` state.
**Decision:** `SuggestionStatus` (workflow: PENDING/ACCEPTED/REJECTED) lives entirely on the suggestion entities; `Product.status` reflects commerce state only. Accepting a pricing suggestion updates `currentPrice` and clears `PRICE_REVIEW_PENDING` back to `ACTIVE` **only if no other pricing suggestion is still PENDING** for that product. Accepting a reorder suggestion increments `stockLevel` (simulating inbound shipment) and does **not** touch `Product.status` — reorder has no review-pending state of its own. Rejecting either changes only the suggestion's status, never the product.
**Tradeoffs:** If both a pricing and reorder suggestion are pending and only the reorder is accepted, the product stays `PRICE_REVIEW_PENDING` until the pricing suggestion is separately decided — intentional, since the price hasn't actually changed yet.

## Extensibility (sprint 2 seams, pointing at actual code)
- `Product.costPrice` / `Product.supplierId` — nullable columns already present in the entity and seed schema, unused today.
- `CompetitorAwareStrategy` — implements `CommerceAdvisor`, registers as `@Component("competitor-aware")`; `StrategyRegistry`'s `Map<String, CommerceAdvisor>` picks it up with zero changes elsewhere.
- Margin floor validation — slots into `AIResponseValidator.validate()` alongside the existing price-bound check once `costPrice` is populated.
- Price change cooldown — slots into `CommerceEngineService`'s existing dedup check as an additional time-window condition.

## Deliberate exclusions (priority decisions, not time excuses)
- **SSE token streaming** (bonus, +5pts) — cut to protect the agentic loop and ADR, which the brief explicitly weights higher.
- **UI ceiling** (catalog board, margin display, price history, heatmap) — floor UI only; backend/systems design carries more weight per the rubric.
- **Retry logic on LLM failure** — single attempt then fallback, no exponential backoff — predictable behavior mattered more than resilience for a 3-hour demo.
- **Cooldown window on repeated triggers** — explicitly sprint 2 scope per the brief's own roadmap.