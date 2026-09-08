# StockPulse Backend

Production-Grade AI-assisted inventory and dynamic pricing engine for ShopStream.

## Run it (under 5 minutes)

```bash
cd backend
# Optional: export LLM_API_KEY=your-key (rule-based & deterministic engine works fully offline)
mvnw spring-boot:run
```

Runs on `http://localhost:8080`. H2 console at `/h2-console` (JDBC URL `jdbc:h2:mem:stockpulse`). Seed data (`data.sql`) loads automatically on startup — 8 products across Electronics, Apparel, and Home goods.

## Architecture Highlights
1. **5-Step Execution Pipeline**: Deterministic Calculation $\rightarrow$ Pricing Policy Engine Guardrails $\rightarrow$ Mathematical Confidence Scoring $\rightarrow$ LLM Explanation Layer $\rightarrow$ Human Approval.
2. **Business Guardrails (`PricingPolicyEngine`)**: Margin floor ($15\%$), Category caps ($+15\%$ Electronics, $+20\%$ Apparel, $+10\%$ Home), max $-25\%$ decrease limit, 24h price change cooldown, and `.99` psychological rounding.
3. **Purchase Order Workflow**: Accepts reorders into `PurchaseOrder` objects (`incomingStock`), and ingests physical inventory upon shipment receipt (`PATCH /purchase-orders/{id}/receive`).
4. **Audit Trails**: Full historical logging for `PriceHistory`, `InventoryTransaction`, and `RecommendationAudit`.
5. **Transactional Outbox Pattern**: `OutboxEvent` and `@Scheduled` `OutboxWorker` ensure zero event loss across process restarts.

## Endpoints
- `POST /products`, `GET /products?status=&category=`, `GET /products/{id}`
- `PATCH /products/{id}/stock`, `POST /products/{id}/orders`
- `POST /products/{id}/suggest-pricing`, `POST /products/{id}/suggest-reorder`
- `GET /pricing-suggestions?status=`, `GET /reorder-suggestions?status=`
- `PATCH /pricing-suggestions/{id}`, `PATCH /reorder-suggestions/{id}`
- `GET /purchase-orders`, `PATCH /purchase-orders/{id}/receive`
- `GET /audit/price-history`, `GET /api/audit/inventory-transactions`, `GET /api/audit/recommendations`