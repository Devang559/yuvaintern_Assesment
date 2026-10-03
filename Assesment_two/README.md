# Product Catalog REST API & Weather Data Integration

A Spring Boot RESTful API for managing a product catalog with full CRUD operations
backed by an in-memory H2 database, plus Weather API integration providing current weather
and forecast data via WeatherAPI.com.

## Features

- **Product Catalog**: Full CRUD operations for a `Product` entity with Bean Validation
- **Weather API Integration**: Retrieve current weather and forecast data from WeatherAPI.com
- **Caching**: Caffeine-based response caching for Weather API (30-minute TTL)
- **Retry Logic**: Configurable exponential backoff retry for transient failures
- **Monitoring**: Spring Boot Actuator endpoints for health and metrics
- **Error Handling**: Structured JSON error responses with appropriate HTTP status codes
- **Performance Optimized**: 42% test execution time reduction after optimization
- **Comprehensive Testing**: 48 tests (unit + integration + performance) covering all scenarios

## Domain model

### Product

A `Product` has:

| Field   | Type      | Description           | Validation          |
|---------|-----------|-----------------------|---------------------|
| id      | Long      | Auto-generated ID     | N/A (server-set)    |
| name    | String    | Product name          | `@NotBlank`, `@Size(max=255)` |
| price   | BigDecimal| Product price         | `@NotNull`, `@DecimalMin("0.01")` |

#### Example payload

```json
{
  "id": 1,
  "name": "Laptop",
  "price": 999.99
}
```

## API endpoints

### Product Catalog (`/api/products`)

| Method   | Endpoint            | Description               | Status codes      |
|----------|---------------------|----------------------------|-------------------|
| GET      | `/api/products`     | Get all products           | 200 OK            |
| GET      | `/api/products/{id}`| Get a single product       | 200 OK, 404 Not Found |
| POST     | `/api/products`     | Create a new product       | 200 OK, 400 Bad Request |
| PUT      | `/api/products/{id}`| Update an existing product | 200 OK, 404 Not Found |
| DELETE   | `/api/products/{id}`| Delete a product           | 204 No Content, 404 Not Found |

### Weather API (`/api/weather`)

| Method   | Endpoint                          | Description               | Status codes      |
|----------|-----------------------------------|----------------------------|-------------------|
| GET      | `/api/weather/{city}`            | Get current weather        | 200 OK, 400 Bad Request, 502 Bad Gateway |
| GET      | `/api/weather/forecast/{city}?days=3` | Get weather forecast    | 200 OK, 400 Bad Request, 502 Bad Gateway |

## Configuration

### Weather API Key

Set your WeatherAPI.com API key via environment variable or application.properties:

```bash
# Linux / macOS
export WEATHER_API_KEY=your_api_key_here

# Windows
set WEATHER_API_KEY=your_api_key_here
```

Or in `src/main/resources/application.properties`:
```properties
weather.api.key=YOUR_API_KEY
weather.api.base-url=https://api.weatherapi.com/v1
weather.api.timeout-ms=5000
weather.api.retry-attempts=3
weather.api.retry-backoff-ms=500
```

Get a free API key at [weatherapi.com](https://www.weatherapi.com/).

## Running the application

```bash
./mvnw spring-boot:run
```

Once started:
- Product API: `http://localhost:8080/api/products`
- Weather API: `http://localhost:8080/api/weather/{city}`

## Example requests & responses

### Product Catalog

#### Get all products

```bash
curl -X GET http://localhost:8080/api/products
```

```json
[
  { "id": 1, "name": "Laptop", "price": 999.99 },
  { "id": 2, "name": "Mouse", "price": 19.99 }
]
```

#### Get a product by ID

```bash
curl -X GET http://localhost:8080/api/products/1
```

```json
{ "id": 1, "name": "Laptop", "price": 999.99 }
```

If the product does not exist:

```http
HTTP/1.1 404 Not Found
```

#### Create a product

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Laptop", "price": 999.99}'
```

```json
{ "id": 1, "name": "Laptop", "price": 999.99 }
```

#### Update a product

```bash
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Updated Laptop", "price": 1299.99}'
```

```json
{ "id": 1, "name": "Updated Laptop", "price": 1299.99 }
```

#### Delete a product

```bash
curl -X DELETE http://localhost:8080/api/products/1
```

```http
HTTP/1.1 204 No Content
```

#### Validation error response (400)

```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "", "price": 0}'
```

```json
{
  "timestamp": "2026-09-25T10:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: ..."
}
```

### Weather API

#### Current weather

```bash
curl -X GET http://localhost:8080/api/weather/London
```

```json
{
  "city": "London",
  "region": "London, Greater London",
  "country": "United Kingdom",
  "temperatureC": 10.0,
  "temperatureF": 50.0,
  "conditionText": "Sunny",
  "humidity": 75,
  "windKph": 9.0,
  "uvIndex": 1.0
}
```

#### 3-day forecast

```bash
curl -X GET "http://localhost:8080/api/weather/forecast/London?days=3"
```

```json
{
  "city": "London",
  "country": "United Kingdom",
  "forecastDays": [
    {
      "date": "2024-01-15",
      "maxTempC": 12.0,
      "minTempC": 8.0,
      "conditionText": "Sunny",
      "uvIndex": 2.0,
      "sunrise": "07:50",
      "sunset": "16:20"
    }
  ]
}
```

#### Weather error responses

Invalid city (400):
```json
{
  "timestamp": "2026-09-25T10:30:00.000Z",
  "status": 400,
  "error": "Bad Request",
  "message": "No location found for city: InvalidCityXYZ"
}
```

API error (502):
```json
{
  "timestamp": "2026-09-25T10:30:00.100Z",
  "status": 502,
  "error": "Bad Gateway",
  "message": "Invalid or missing Weather API key: API key not provided"
}
```

## Performance Optimizations

This project has been optimized for performance. See `pdf_task1/Performance_Optimization_Report.md` for detailed metrics.

### Key Optimizations

| Optimization | Impact |
|---|---|
| Caffeine caching (30min TTL) | ~99% faster for repeated weather requests |
| Removed reactive thread pool overhead | Eliminated thread context switches |
| Static ObjectMapper | Reduced GC pressure |
| Exponential backoff retries | Improved resilience to transient failures |
| H2 create-drop + open-in-view=false | 15% faster startup |
| HikariCP pool configuration | Optimized connection reuse |

### Performance Metrics

| Metric | Before | After | Improvement |
|---|---|---|---|
| Test execution time | 20.87s | 11.31s | 44% |
| Application startup | 8.65s | 7.36s | 15% |
| Cached API call latency | 200-500ms | <1ms | 99%+ |

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| Spring Boot | 4.1.1 | Application framework |
| Spring WebFlux | (from Boot 4.1.1) | WebClient for external API calls |
| Spring Cache | (from Boot 4.1.1) | Spring Cache abstraction for caching |
| Caffeine | 3.x (managed by Boot) | In-memory cache provider |
| Spring Data JPA | (from Boot 4.1.1) | Product repository |
| Hibernate Validator | 9.1.3.Final | Bean validation |
| Jackson | (from Boot 4.1.1) | JSON serialization/deserialization |
| Spring Boot Actuator | (from Boot 4.1.1) | Monitoring and metrics |
| H2 Database | (from Boot 4.1.1) | In-memory database |
| Lombok | (from Boot 4.1.1) | Boilerplate code reduction |
| MockWebServer | 4.12.0 | Mock HTTP server for API tests |

## Running the tests

```bash
./mvnw test
```

Expected output:
```
[INFO] Tests run: 48, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Test breakdown

| Test Class | Type | Tests |
|---|---|---|
| AssesmentTwoApplicationTests | Context test | 1 |
| ProductControllerTest | Unit (MockMvc) | 14 |
| WeatherControllerTest | Unit (MockMvc) | 7 |
| WeatherControllerIntegrationTest | Integration (MockWebServer) | 8 |
| WeatherServiceTest | Unit (MockWebServer) | 13 |
| WeatherServicePerformanceTest | Performance benchmark | 5 |
| **Total** | | **48** |

### Run specific tests

```bash
# Run all weather-related tests
./mvnw test -Dtest="*Weather*"

# Run integration tests only
./mvnw test -Dtest=WeatherControllerIntegrationTest

# Run product tests
./mvnw test -Dtest=ProductControllerTest

# Run performance benchmarks
./mvnw test -Dtest=WeatherServicePerformanceTest

# Run with verbose output
./mvnw test -X
```

## Project structure

```
Assesment_two/
├── src/
│   ├── main/
│   │   ├── java/com/example/Assesment_two/
│   │   │   ├── AssesmentTwoApplication.java          (with @EnableCaching)
│   │   │   ├── config/
│   │   │   │   ├── CacheConfig.java                  (NEW - programmatic Caffeine cache config)
│   │   │   │   └── RestClientConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── ProductController.java          (with validation)
│   │   │   │   ├── WeatherController.java          (NEW)
│   │   │   │   ├── GlobalExceptionHandler.java     (NEW - UTC timestamps)
│   │   │   │   ├── WeatherApiException.java        (NEW)
│   │   │   │   ├── CityNotFoundException.java      (NEW)
│   │   │   │   └── ProductNotFoundException.java
│   │   │   ├── model/
│   │   │   │   ├── Product.java                    (with validation annotations)
│   │   │   │   └── weather/
│   │   │   │       ├── WeatherApiResponse.java     (NEW)
│   │   │   │       ├── WeatherData.java            (NEW)
│   │   │   │       └── ForecastData.java           (NEW)
│   │   │   ├── repository/
│   │   │   │   └── ProductRepository.java
│   │   │   └── service/
│   │   │       └── WeatherService.java             (NEW - caching, retry, timing logs)
│   │   └── resources/
│   │       └── application.properties             (updated - cache, HikariCP, actuator config)
│   └── test/
│       └── java/com/example/Assesment_two/
│           ├── AssesmentTwoApplicationTests.java
│           ├── controller/
│           │   ├── ProductControllerTest.java        (updated - 14 tests)
│           │   └── WeatherControllerTest.java        (NEW - 7 tests)
│           ├── integration/
│           │   └── WeatherControllerIntegrationTest.java (NEW - 8 tests)
│           ├── performance/
│           │   └── WeatherServicePerformanceTest.java   (NEW - 5 benchmark tests)
│           └── service/
│               └── WeatherServiceTest.java           (NEW - 13 tests)
├── .github/workflows/
│   └── deploy.yml                                  (NEW - CI/CD pipeline)
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

### Actuator endpoints

After starting the application, monitor health and metrics:

```bash
# Health check
curl http://localhost:8080/actuator/health

# Application info
curl http://localhost:8080/actuator/info

# Cache statistics
curl http://localhost:8080/actuator/caches

# Metrics
curl http://localhost:8080/actuator/metrics
```

## CI/CD

The project includes a GitHub Actions workflow at `.github/workflows/deploy.yml` that:

1. **Build & Test**: Compiles, runs all 48 tests on every push/PR
2. **Release**: Creates a release with source ZIP and JAR artifacts on tag pushes
3. **Deploy**: Optionally deploys to a server via SSH on main/master branch pushes

```yaml
# Trigger on push to main or tag starting with v
# Run tests
# Package as JAR
# Create GitHub Release with ZIP and JAR
# (Optional) Deploy to server
```

## Documentation

Documentation is available in `pdf_task1/`:

| File | Description |
|---|---|
| `Weather_API_Integration_Test_Report.md` | Weather API integration test report |
| `Performance_Optimization_Report.md` | Performance analysis, bugs found, before/after metrics |
