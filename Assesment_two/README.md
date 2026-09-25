# Product Catalog REST API & Weather Data Integration

A Spring Boot RESTful API for managing a product catalog with full CRUD operations
backed by an in-memory H2 database, plus Weather API integration providing current weather
and forecast data via WeatherAPI.com.

## Features

- **Product Catalog**: Full CRUD operations for a `Product` entity with Bean Validation
- **Weather API Integration**: Retrieve current weather and forecast data from WeatherAPI.com
- **Error Handling**: Structured JSON error responses with appropriate HTTP status codes
- **Comprehensive Testing**: 43 tests (unit + integration) covering all scenarios

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
  "timestamp": "2026-09-25T10:30:00.000+00:00",
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
  "timestamp": "2026-09-25T10:30:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "No location found for city: InvalidCityXYZ"
}
```

API error (502):
```json
{
  "timestamp": "2026-09-25T10:30:00.100+00:00",
  "status": 502,
  "error": "Bad Gateway",
  "message": "Invalid or missing Weather API key: API key not provided"
}
```

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| Spring Boot | 4.1.1 | Application framework |
| Spring WebFlux | (from Boot 4.1.1) | WebClient for external API calls |
| Spring Data JPA | (from Boot 4.1.1) | Product repository |
| Hibernate Validator | 9.1.3.Final | Bean validation |
| Jackson | (from Boot 4.1.1) | JSON serialization/deserialization |
| H2 Database | (from Boot 4.1.1) | In-memory database |
| Lombok | (from Boot 4.1.1) | Boilerplate code reduction |
| MockWebServer | 4.12.0 | Mock HTTP server for API tests |

## Running the tests

```bash
./mvnw test
```

Expected output:
```
[INFO] Tests run: 43, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Test breakdown

| Test Class | Type | Tests |
|---|---|---|
| AssesmentTwoApplicationTests | Context test | 1 |
| ProductControllerTest | Unit (MockMvc) | 14 |
| WeatherControllerTest | Unit (MockMvc) | 7 |
| WeatherServiceTest | Unit (MockWebServer) | 13 |
| WeatherControllerIntegrationTest | Integration (MockWebServer) | 8 |
| **Total** | | **43** |

### Run specific tests

```bash
# Run all weather-related tests
./mvnw test -Dtest="*Weather*"

# Run integration tests only
./mvnw test -Dtest=WeatherControllerIntegrationTest

# Run product tests
./mvnw test -Dtest=ProductControllerTest

# Run with verbose output
./mvnw test -X
```

## Project structure

```
Assesment_two/
├── src/
│   ├── main/
│   │   ├── java/com/example/Assesment_two/
│   │   │   ├── AssesmentTwoApplication.java
│   │   │   ├── config/
│   │   │   │   └── RestClientConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── ProductController.java          (with validation)
│   │   │   │   ├── WeatherController.java          (NEW)
│   │   │   │   ├── GlobalExceptionHandler.java     (NEW)
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
│   │   │       └── WeatherService.java             (NEW)
│   │   └── resources/
│   │       └── application.properties             (updated with Weather API config)
│   └── test/
│       └── java/com/example/Assesment_two/
│           ├── AssesmentTwoApplicationTests.java
│           ├── controller/
│           │   ├── ProductControllerTest.java        (updated - 14 tests)
│           │   └── WeatherControllerTest.java        (NEW - 7 tests)
│           ├── integration/
│           │   └── WeatherControllerIntegrationTest.java (NEW - 8 tests)
│           └── service/
│               └── WeatherServiceTest.java           (NEW - 13 tests)
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

## Documentation

Comprehensive test report and documentation is available in `pdf_task1/Weather_API_Integration_Test_Report.md`, covering:
- Detailed test descriptions and assertions
- Architecture diagrams and request flow
- Error code handling matrix
- Local testing instructions
