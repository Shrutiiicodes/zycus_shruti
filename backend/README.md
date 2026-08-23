# StockPulse

AI-assisted inventory and dynamic pricing engine — reactive commerce advisor for ShopStream.

## Run it (under 5 minutes)

### Backend
```bash
cd backend
export LLM_API_KEY=your-qwen-api-key   # optional — rule-based works without it
./mvnw spring-boot:run
```
Runs on `http://localhost:8080`. H2 console at `/h2-console` (JDBC URL `jdbc:h2:mem:stockpulse`). Seed data (`data.sql`) loads automatically on startup — 8 products, including one already below its reorder threshold.

To switch to the AI strategy, set `commerce.strategy=ai` in `application.properties` (or override via env var `COMMERCE_STRATEGY=ai`) — no restart needed if changed as a Spring Cloud Config-style external property; with a plain `.properties` file a restart is required to pick it up on this build.

### Frontend
```bash
cd frontend
npm install
npm run dev
```
Runs on `http://localhost:5173`, calls the backend at `http://localhost:8080`.

## Demo path
1. Open the console — PRD-003 (T-shirt) already shows `PRICE_REVIEW_PENDING` from seed data.
2. Click **Simulate sale** on PRD-003 a couple more times, or `POST /products/PRD-003/orders` — stock is already below threshold (8 vs 15), so `INVENTORY_LOW` suggestions appear within the next poll cycle (~4s).
3. Click **Accept** on the pricing suggestion — `currentPrice` updates in the product table.
4. For a demand spike: hit **Simulate sale** on PRD-008 (Hoodie) several times quickly — velocity (15) is well above its category average, crossing the 3x spike multiplier and firing `DEMAND_SPIKE` suggestions.

## Architecture
See `ADR.md` for the full record. Summary: stock/order mutation → `TriggerEvaluator` checks both conditions → `ApplicationEventPublisher` fires a signal per satisfied condition → async listener runs the active `CommerceAdvisor` (rule-based or AI, switchable via `commerce.strategy`) → suggestions persist as `PENDING` → merchandising accepts/rejects in the console → accept updates the product's price or stock.

## Endpoints
- `POST /products`, `GET /products?status=&category=`, `GET /products/{id}`
- `PATCH /products/{id}/stock`, `POST /products/{id}/orders`
- `POST /products/{id}/suggest-pricing`, `POST /products/{id}/suggest-reorder`
- `GET /pricing-suggestions?status=`, `GET /reorder-suggestions?status=`
- `PATCH /pricing-suggestions/{id}`, `PATCH /reorder-suggestions/{id}` — body `{"decision":"ACCEPT"|"REJECT"}`

## Known limitations (see ADR "Deliberate exclusions")
No SSE streaming, no UI ceiling features, no LLM retry/backoff, no suggestion cooldown window.