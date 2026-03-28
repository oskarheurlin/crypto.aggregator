# Cryptocurrency Price Aggregator

A lightweight service that periodically fetches cryptocurrency prices from Coinbase and serves them through a REST API.

## Running Locally

### Prerequisites

- Java 17+

### Start the application

```bash
./gradlew bootRun
```

The server starts on `http://localhost:8080`

On startup, the service:
1. Validates configured symbols against the Coinbase Products API (fails fast if any are invalid)
2. Fetches prices immediately for all tracked symbols (in parallel)
3. Begins polling every 10 seconds

### Run tests

```bash
./gradlew test
```

## API Usage

### Get price for a symbol

```
GET /prices/{symbol}
```

Supported symbols: `BTC-USD`, `ETH-USD`, `ETH-BTC` (case-insensitive).

**Example:**

```bash
curl http://localhost:8080/prices/BTC-USD
```

**Response (200):**

```json
{
  "symbol": "BTC-USD",
  "price": 67542.01,
  "timestamp": "2026-03-28T12:00:00Z"
}
```

The `timestamp` field is the time this service last updated the stored price (ISO 8601 UTC), not the exchange's trade event time.

**Symbol not tracked (404):**

```bash
curl http://localhost:8080/prices/DOGE-USD
```

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Symbol not tracked: DOGE-USD. Available: [BTC-USD, ETH-USD, ETH-BTC]"
}
```

**No price available yet (404):**

If a tracked symbol has not been fetched yet:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "No price available yet for: BTC-USD"
}
```

## Configuration

All settings are in `application.yml` and can be overridden via environment variables:

| Setting | Default | Environment Variable |
|---------|---------|---------------------|
| Tracked symbols | BTC-USD, ETH-USD, ETH-BTC (all Coinbase) | — |
| Fetch interval | 10000ms | `PRICE_FETCH_INTERVAL_MS` |
| Coinbase base URL | `https://api.exchange.coinbase.com` | `COINBASE_API_BASE_URL` |
| HTTP timeout | 5000ms | `COINBASE_HTTP_TIMEOUT_MS` |

Each symbol is configured with an exchange mapping:

```yaml
app:
  symbols:
    - symbol: BTC-USD
      exchange: coinbase
    - symbol: ETH-USD
      exchange: coinbase
```

## Design Decisions

**Non-blocking throughout.** The service uses Spring WebFlux with Netty as the server and `WebClient` for HTTP calls. Kotlin coroutines provide structured concurrency — all symbols are fetched in parallel via `supervisorScope`, completing in the time of the slowest call rather than the sum. This gives consistent non-blocking I/O from client to server with no model mixing.

**Configuration-driven symbols with exchange routing.** Tracked symbols are defined in `application.yml`, each mapped to an exchange. The `ExchangeClientRouter` resolves which `ExchangeClient` implementation handles each symbol. Adding a new pair on an existing exchange is a config-only change.

**Startup validation.** On startup, the service calls the Coinbase Products API to verify all configured symbols are valid. If any symbol is not available on the exchange, the application fails to start. This catches configuration errors early.

**Extension points without over-abstraction.** `ExchangeClient` (interface) and `PriceStore` (interface) provide natural seams for adding exchanges or swapping storage. `ExchangeClientRouter` maps symbols to their exchange at startup. These are lightweight — one file each — but make the multi-exchange and horizontal-scaling paths concrete.

**No resilience libraries.** The 10-second polling loop acts as a natural retry mechanism — if a fetch fails, the next attempt is ~10 seconds later. Each symbol is fetched independently with per-symbol error isolation via `supervisorScope`, so one failure does not block others.

**`fixedDelay` over `fixedRate`.** The scheduler waits 10 seconds *after* the previous fetch completes before starting the next one. This naturally prevents overlapping fetch cycles without needing additional synchronization.

## Scaling for Multiple Exchanges and More Pairs

**More currency pairs:** Add entries to `app.symbols` in `application.yml` with the appropriate exchange. No code changes needed — the router validates and starts fetching them automatically.

**Multiple exchanges:** Implement the `ExchangeClient` interface (e.g., `BinanceClient`). The `ExchangeClientRouter` automatically picks up new implementations and routes symbols to the correct exchange based on config.

**Horizontal scaling:** Swap `InMemoryPriceStore` for a shared implementation (e.g., Redis) behind the `PriceStore` interface. This allows multiple service instances to write prices and serve reads from a common data source.
