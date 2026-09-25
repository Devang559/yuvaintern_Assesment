# Weather API Integration - Test Report & Documentation

**Project:** Assesment_two - Spring Boot Product Catalog API
**Date:** September 25, 2026
**API Integrated:** WeatherAPI.com (weatherapi.com)

---

## 1. Executive Summary

This document details the integration of the WeatherAPI.com public API into the existing Spring Boot Product Catalog REST API application. The integration adds weather data retrieval (current weather and forecast) as new endpoints, with comprehensive unit and integration tests covering success scenarios, error handling, edge cases, and external API interaction validation.

### Test Results Summary

| Test Class | Tests | Failures | Errors | Skipped | Status |
|---|---|---|---|---|---|
| AssesmentTwoApplicationTests | 1 | 0 | 0 | 0 | PASS |
| ProductControllerTest | 14 | 0 | 0 | 0 | PASS |
| WeatherControllerTest | 7 | 0 | 0 | 0 | PASS |
| WeatherServiceTest | 13 | 0 | 0 | 0 | PASS |
| WeatherControllerIntegrationTest | 8 | 0 | 0 | 0 | PASS |
| **Total** | **43** | **0** | **0** | **0** | **ALL PASS** |

---

## 2. Changes Made

### 2.1 Dependencies Added (pom.xml)

| Dependency | Purpose | Scope |
|---|---|---|
| `spring-boot-starter-validation` | Bean Validation (JSR-389) for `@Valid`, `@NotBlank` | compile |
| `spring-boot-starter-webflux` | Required for `WebClient` HTTP client | compile |
| `com.squareup.okhttp3:mockwebserver` | Mock HTTP server for testing API interactions | test |

### 2.2 Configuration Changes

**application.properties** - Added Weather API configuration:
```properties
weather.api.base-url=https://api.weatherapi.com/v1
weather.api.key=${WEATHER_API_KEY:DEMO_KEY}
weather.api.timeout-ms=5000
```

### 2.3 New Source Files

#### Model Classes (DTOs)

| File | Path | Description |
|---|---|---|
| WeatherApiResponse | `src/main/java/com/example/Assesment_two/model/weather/WeatherApiResponse.java` | Maps WeatherAPI.com JSON responses with nested classes for Location, CurrentWeather, Condition, Forecast, ForecastDay, ForecastDayDetails, Astro, Hour, and WeatherError |
| WeatherData | `src/main/java/com/example/Assesment_two/model/weather/WeatherData.java` | Internal DTO returned by the controller containing curated weather fields |
| ForecastData | `src/main/java/com/example/Assesment_two/model/weather/ForecastData.java` | Internal DTO returned by the forecast controller endpoint |

#### Configuration

| File | Path | Description |
|---|---|---|
| RestClientConfig | `src/main/java/com/example/Assesment_two/config/RestClientConfig.java` | Creates a `WebClient` bean configured for the Weather API base URL |

#### Service Layer

| File | Path | Description |
|---|---|---|
| WeatherService | `src/main/java/com/example/Assesment_two/service/WeatherService.java` | Calls WeatherAPI.com endpoints using `WebClient`, maps responses to internal DTOs, handles errors including rate limiting, invalid API key, and invalid city |

#### Controller Layer

| File | Path | Description |
|---|---|---|
| WeatherController | `src/main/java/com/example/Assesment_two/controller/WeatherController.java` | REST endpoints for weather data: `GET /api/weather/{city}` and `GET /api/weather/forecast/{city}?days=3` |
| WeatherApiException | `src/main/java/com/example/Assesment_two/controller/WeatherApiException.java` | Custom exception for Weather API errors (returns 502 Bad Gateway) |
| CityNotFoundException | `src/main/java/com/example/Assesment_two/controller/CityNotFoundException.java` | Custom exception for invalid city names (returns 400 Bad Request) |
| GlobalExceptionHandler | `src/main/java/com/example/Assesment_two/controller/GlobalExceptionHandler.java` | Global exception handler for all controllers, returns structured JSON error responses |

### 2.4 Modified Source Files

| File | Changes |
|---|---|
| pom.xml | Added `spring-boot-starter-validation`, `spring-boot-starter-webflux`, `mockwebserver` dependencies |
| application.properties | Added Weather API base URL, API key, and timeout configuration |
| Product.java | Added `@NotBlank`, `@Size`, `@NotNull`, `@DecimalMin` validation annotations |
| ProductController.java | Added `@Valid` annotation to POST and PUT endpoints for input validation |

### 2.5 New Test Files

| File | Type | Tests | Description |
|---|---|---|---|
| WeatherServiceTest | Unit | 13 | Tests WeatherService with MockWebServer: success, error codes (400/401/403/429/500/503), malformed JSON, invalid days |
| WeatherControllerTest | Unit (MockMvc) | 7 | Tests WeatherController with mocked WeatherService: success, 400, 502, forecast, default days |
| WeatherControllerIntegrationTest | Integration | 8 | Tests full flow with MockWebServer: current weather, forecast, error handling, product CRUD coexistence |

### 2.6 Modified Test Files

| File | Changes |
|---|---|
| ProductControllerTest | Added `GlobalExceptionHandler` to MockMvc, added 6 new edge case tests (empty list, blank name, null price, zero price, non-existent update, non-existent delete) |
| AssesmentTwoApplicationTests | Simplified to context load test only |

---

## 3. Weather API Integration Details

### 3.1 API Selection

**WeatherAPI.com** (weatherapi.com) was selected because:
- Free tier: 1 million calls per month (no credit card required)
- Returns JSON responses with comprehensive weather data
- Provides both current weather and forecast endpoints
- Well-documented API with OpenAPI specification

### 3.2 Endpoints Integrated

| Endpoint | Method | Description |
|---|---|---|
| `GET /api/weather/{city}` | GET | Returns current weather data for the specified city |
| `GET /api/weather/forecast/{city}?days=3` | GET | Returns 3-day weather forecast (1-14 days configurable) |

### 3.3 External WeatherAPI.com Endpoints Called

| External Endpoint | Method |
|---|---|
| `https://api.weatherapi.com/v1/current.json?key={API_KEY}&q={city}&aq=no` | GET |
| `https://api.weatherapi.com/v1/forecast.json?key={API_KEY}&q={city}&aq=no&days={days}` | GET |

### 3.4 API Key Configuration

The API key is configured via environment variable:
```bash
export WEATHER_API_KEY=your_actual_api_key
```

Or in `application.properties`:
```properties
weather.api.key=YOUR_API_KEY
```

### 3.5 Error Code Handling

The WeatherService handles the following WeatherAPI.com error codes:

| Error Code | HTTP Status | Exception Thrown | User Message |
|---|---|---|---|
| 1003 | 400 | CityNotFoundException | "No location found for city: {city}" |
| 1005 | 400 | CityNotFoundException | "No location found for city: {city}" |
| 1006 | 400 | CityNotFoundException | "No location found for city: {city}" |
| 1002 | 401 | WeatherApiException | "Invalid or missing Weather API key" |
| 2006 | 401 | WeatherApiException | "Invalid or missing Weather API key" |
| 2007 | 403 | WeatherApiException | "API key quota exceeded or disabled" |
| 2008 | 403 | WeatherApiException | "API key quota exceeded or disabled" |
| 9999 | 500 | WeatherApiException | "Weather API error [9999]: ..." |
| 429 | 429 | WeatherApiException | "Weather API rate limit exceeded" |

---

## 4. API Endpoints

### 4.1 Product CRUD Endpoints (Existing)

Base path: `/api/products`

| Method | Endpoint | Description | Status Codes |
|---|---|---|---|
| GET | `/api/products` | Get all products | 200 OK |
| GET | `/api/products/{id}` | Get a product by ID | 200 OK, 404 Not Found |
| POST | `/api/products` | Create a product | 200 OK, 400 Bad Request |
| PUT | `/api/products/{id}` | Update a product | 200 OK, 404 Not Found |
| DELETE | `/api/products/{id}` | Delete a product | 204 No Content, 404 Not Found |

### 4.2 Weather Endpoints (New)

Base path: `/api/weather`

| Method | Endpoint | Description | Status Codes |
|---|---|---|---|
| GET | `/api/weather/{city}` | Get current weather for a city | 200 OK, 400 Bad Request, 502 Bad Gateway |
| GET | `/api/weather/forecast/{city}?days=3` | Get weather forecast | 200 OK, 400 Bad Request, 502 Bad Gateway |

---

## 5. Sample API Requests & Responses

### 5.1 Current Weather

**Request:**
```bash
curl -X GET http://localhost:8080/api/weather/London
```

**Response (200 OK):**
```json
{
  "city": "London",
  "region": "London, Greater London",
  "country": "United Kingdom",
  "latitude": 51.52,
  "longitude": -0.11,
  "timezone": "Europe/London",
  "localTime": "2024-01-15 10:00",
  "lastUpdated": "2024-01-15 10:00",
  "temperatureC": 10.0,
  "temperatureF": 50.0,
  "day": true,
  "conditionText": "Sunny",
  "conditionIcon": "//cdn.weatherapi.com/weather/64x64/day/1000.png",
  "conditionCode": 1000,
  "windMph": 5.6,
  "windKph": 9.0,
  "windDirection": "S",
  "humidity": 75,
  "cloudCover": 0,
  "feelslikeC": 8.0,
  "feelslikeF": 46.4,
  "visibilityKm": 10.0,
  "uvIndex": 1.0,
  "gustMph": 8.0,
  "gustKph": 12.8
}
```

**Error Response (400 Bad Request - Invalid City):**
```json
{
  "timestamp": "2026-09-25T10:43:46.655692500",
  "status": 400,
  "error": "Bad Request",
  "message": "No location found for city: InvalidCityXYZ - No location found matching parameter 'q'"
}
```

**Error Response (502 Bad Gateway - Invalid API Key):**
```json
{
  "timestamp": "2026-09-25T10:43:47.230",
  "status": 502,
  "error": "Bad Gateway",
  "message": "Invalid or missing Weather API key: API key not provided"
}
```

### 5.2 Weather Forecast

**Request:**
```bash
curl -X GET "http://localhost:8080/api/weather/forecast/London?days=3"
```

**Response (200 OK):**
```json
{
  "city": "London",
  "country": "United Kingdom",
  "region": "London, Greater London",
  "forecastDays": [
    {
      "date": "2024-01-15",
      "maxTempC": 12.0,
      "minTempC": 8.0,
      "avgTempC": 10.0,
      "conditionText": "Sunny",
      "conditionIcon": "//cdn.weatherapi.com/weather/64x64/day/1000.png",
      "conditionCode": 1000,
      "maxWindKph": 16.0,
      "totalPrecipMm": 0.5,
      "avgHumidity": 75.0,
      "uvIndex": 2.0,
      "sunrise": "07:50",
      "sunset": "16:20"
    },
    {
      "date": "2024-01-16",
      "maxTempC": 11.0,
      "minTempC": 6.0,
      "avgTempC": 8.5,
      "conditionText": "Rain",
      "conditionIcon": "//cdn.weatherapi.com/weather/64x64/day/296.png",
      "conditionCode": 1003,
      "maxWindKph": 19.3,
      "totalPrecipMm": 1.2,
      "avgHumidity": 80.0,
      "uvIndex": 1.0,
      "sunrise": "07:48",
      "sunset": "16:22"
    },
    {
      "date": "2024-01-17",
      "maxTempC": 9.0,
      "minTempC": 5.0,
      "avgTempC": 7.0,
      "conditionText": "Light rain",
      "conditionIcon": "//cdn.weatherapi.com/weather/64x64/day/296.png",
      "conditionCode": 1003,
      "maxWindKph": 24.0,
      "totalPrecipMm": 2.0,
      "avgHumidity": 82.0,
      "uvIndex": 1.0,
      "sunrise": "07:46",
      "sunset": "16:24"
    }
  ]
}
```

### 5.3 Product CRUD (Updated with Validation)

**Request (POST - Create with validation):**
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "", "price": 999.99}'
```

**Response (400 Bad Request - Validation Error):**
```json
{
  "timestamp": "2026-09-25T10:43:46.700",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed: ..."
}
```

---

## 6. Testing Details

### 6.1 Test Strategy

| Test Type | Test Class | Scope |
|---|---|---|
| Unit Test | WeatherServiceTest | Tests WeatherService in isolation using MockWebServer to simulate WeatherAPI.com responses |
| Unit Test | WeatherControllerTest | Tests WeatherController with mocked WeatherService using MockMvc |
| Unit Test | ProductControllerTest | Tests ProductController with mocked ProductRepository and validation |
| Integration Test | WeatherControllerIntegrationTest | Tests full stack: MockMvc -> Controller -> Service -> WebClient -> MockWebServer -> JSON response |
| Context Test | AssesmentTwoApplicationTests | Verifies Spring application context loads correctly |

### 6.2 Test Coverage Details

#### WeatherServiceTest (13 tests) - Unit Tests

| Test | Description | Key Assertions |
|---|---|---|
| shouldReturnWeatherDataOnSuccess | Valid API response returns WeatherData | city, country, temp, humidity, condition |
| shouldThrowCityNotFoundExceptionForInvalidCity | Error code 1006 → CityNotFoundException | Exception type and message |
| shouldThrowCityNotFoundExceptionForMissingQueryParam | Error code 1003 → CityNotFoundException | Exception type and message |
| shouldThrowWeatherApiExceptionForInvalidApiKey | Error code 1002 → WeatherApiException | Error message contains "API key" |
| shouldThrowWeatherApiExceptionForRateLimitExceeded | Error code 2007 → WeatherApiException | Error message contains "quota" |
| shouldThrowWeatherApiExceptionForServerError | Error code 9999 → WeatherApiException | Exception type |
| shouldThrowWeatherApiExceptionForMalformedJson | Invalid JSON → WeatherApiException | Error message contains "Failed to fetch" |
| shouldThrowWeatherApiExceptionOnNetworkFailure | HTTP 503 → WeatherApiException | Exception type |
| shouldParseAllWeatherFieldsCorrectly | All JSON fields correctly mapped | 16 field assertions |
| shouldFetchForecastDataSuccessfully | 2-day forecast parsed correctly | date, temp, condition, sunrise/sunset |
| shouldThrowWeatherApiExceptionForInvalidDays | days=15 → WeatherApiException | Error message about range |
| shouldThrowWeatherApiExceptionForZeroDays | days=0 → WeatherApiException | Error message about range |
| shouldThrowCityNotFoundExceptionOnForecastForInvalidCity | Forecast with invalid city | Exception type |

#### WeatherControllerTest (7 tests) - Unit Tests

| Test | Description | Key Assertions |
|---|---|---|
| shouldGetCurrentWeatherSuccessfully | Mocked WeatherService returns WeatherData | 200 status, city, country, temp |
| shouldReturn400WhenCityNotFound | CityNotFoundException → 400 | Status, message |
| shouldReturn502OnWeatherApiError | WeatherApiException → 502 | Status, message |
| shouldGetForecastSuccessfully | Mocked service returns ForecastData | 200 status, city, forecastDays |
| shouldUseDefaultDaysWhenNotSpecified | Default days=3 used | 200 status, city |
| shouldReturn400ForInvalidDays | days=15 → 502 (error from service) | Status, message |
| shouldReturn502OnUnexpectedServiceError | WeatherApiException → 502 | Status |

#### ProductControllerTest (14 tests) - Unit Tests

| Test | Description | Key Assertions |
|---|---|---|
| shouldGetAllProducts | Returns list of products | 200 status, product names |
| shouldReturnEmptyListWhenNoProducts | Empty product list | 200 status, empty JSON array |
| shouldGetProductById | Returns product by ID | 200 status, name, price |
| shouldReturn404WhenProductNotFound | Non-existent ID → 404 | Status 404, error message |
| shouldCreateProduct | Creates new product | 200 status, id, name |
| shouldReturn400WhenProductNameIsBlank | Blank name → 400 | Status 400 |
| shouldReturn400WhenProductNameIsNull | Null name → 400 | Status 400 |
| shouldReturn400WhenProductPriceIsNull | Null price → 400 | Status 400 |
| shouldReturn400WhenProductPriceIsZero | Zero price → 400 | Status 400 |
| shouldUpdateProduct | Updates existing product | 200 status, updated name |
| shouldReturn404WhenUpdatingNonExistentProduct | Non-existent ID → 404 | Status 404, never calls save |
| shouldReturn400WhenUpdatingWithBlankName | Blank name on update → 400 | Status 400 |
| shouldDeleteProduct | Deletes product | 204 status |
| shouldReturn404WhenDeletingNonExistentProduct | Non-existent ID → 404 | Status 404, never calls delete |

#### WeatherControllerIntegrationTest (8 tests) - Integration Tests

| Test | Description | Key Assertions |
|---|---|---|
| integrationTestGetCurrentWeather | Full flow: MVC → Service → API → Response | 200 status, weather data fields, request URL |
| integrationTestCityNotFound | Invalid city through full stack | 400 status, structured error |
| integrationTestGetForecast | Full flow: forecast endpoint | 200 status, 3 forecast days, specific dates |
| integrationTestForecastDefaultDays | Default days=3 used | 200 status, 3 forecast days |
| integrationTestProductCrudStillWorks | Product CRUD still functional | 200 status, product data |
| integrationTestWeatherApiErrorStructure | Error response structure | timestamp, status, error, message |
| integrationTestWeatherResponseFields | All response fields present | 18 field existence assertions |
| integrationTestInvalidApiKey | Invalid API key → 502 | 502 status, error message |

### 6.3 How to Run Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=WeatherServiceTest

# Run only integration tests
./mvnw test -Dtest=WeatherControllerIntegrationTest

# Run with verbose output
./mvnw test -Dtest=WeatherControllerIntegrationTest -X
```

### 6.4 Test Report Location

Maven Surefire generates HTML and XML test reports in:
- `target/surefire-reports/` (XML and TXT reports)
- `target/site/jacoco/` (JaCoCo coverage report, if enabled)

---

## 7. Architecture

### 7.1 Component Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                        Client (curl/Postman)                  │
└───────────────┬───────────────────────┬───────────────────────┘
                │                       │
        ┌───────▼───────┐       ┌───────▼───────┐
        │ProductController│     │WeatherController│
        │  (/api/products)  │   │  (/api/weather)│
        └───────┬───────┘       └───────┬───────┘
                │                       │
        ┌───────▼───────┐       ┌───────▼───────┐
        │ProductRepository│   │  WeatherService   │
        │  (Spring Data) │   │  (WebClient)    │
        └───────┬───────┘       └───────┬───────┘
                │                       │
        ┌───────▼───────┐               │
        │  H2 Database  │       ┌───────▼──────────┐
        │  (in-memory)  │       │  WeatherAPI.com     │
        └───────────────┘       │  (External API)    │
                                └────────────────────┘
```

### 7.2 Request Flow for Weather Endpoint

1. Client sends GET request to `/api/weather/{city}`
2. `WeatherController` receives the request
3. `WeatherController` calls `WeatherService.getCurrentWeather(city)`
4. `WeatherService` uses `WebClient` to call `https://api.weatherapi.com/v1/current.json?q={city}&key={API_KEY}`
5. WeatherAPI.com returns JSON response
6. `WeatherService` maps the JSON to `WeatherApiResponse` DTO, then to `WeatherData`
7. `WeatherController` returns `WeatherData` as JSON response
8. If any error occurs, `GlobalExceptionHandler` catches it and returns structured error JSON

---

## 8. How to Test Locally

### 8.1 Prerequisites

- Java 17+
- Maven (or use the included Maven wrapper)
- WeatherAPI.com API key (free tier at https://www.weatherapi.com/)

### 8.2 Get a WeatherAPI Key

1. Visit https://www.weatherapi.com/
2. Sign up for a free account
3. Navigate to "API Key" in your dashboard
4. Copy your API key

### 8.3 Configure the API Key

Set the environment variable:
```bash
# Linux/macOS
export WEATHER_API_KEY=your_api_key_here

# Windows
set WEATHER_API_KEY=your_api_key_here
```

Or edit `application.properties` directly:
```properties
weather.api.key=your_api_key_here
```

### 8.4 Run the Application

```bash
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`

### 8.5 Test the Endpoints

#### Current Weather
```bash
curl -X GET http://localhost:8080/api/weather/London
```

#### Weather Forecast (3 days)
```bash
curl -X GET "http://localhost:8080/api/weather/forecast/London?days=3"
```

#### Product CRUD (verify existing functionality still works)
```bash
# Create a product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Laptop", "price": 999.99}'

# Get all products
curl -X GET http://localhost:8080/api/products

# Get a product by ID
curl -X GET http://localhost:8080/api/products/1

# Update a product
curl -X PUT http://localhost:8080/api/products/1 \
  -H "Content-Type: application/json" \
  -d '{"name": "Updated Laptop", "price": 1299.99}'

# Delete a product
curl -X DELETE http://localhost:8080/api/products/1
```

### 8.6 Run Tests

```bash
./mvnw test
```

Expected output:
```
[INFO] Tests run: 43, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 9. Project Structure

```
Assesment_two/
├── src/
│   ├── main/
│   │   ├── java/com/example/Assesment_two/
│   │   │   ├── AssesmentTwoApplication.java
│   │   │   ├── config/
│   │   │   │   └── RestClientConfig.java
│   │   │   ├── controller/
│   │   │   │   ├── ProductController.java (modified - added @Valid)
│   │   │   │   ├── ProductNotFoundException.java
│   │   │   │   ├── WeatherController.java (NEW)
│   │   │   │   ├── WeatherApiException.java (NEW)
│   │   │   │   ├── CityNotFoundException.java (NEW)
│   │   │   │   └── GlobalExceptionHandler.java (NEW)
│   │   │   ├── model/
│   │   │   │   ├── Product.java (modified - added validation)
│   │   │   │   └── weather/
│   │   │   │       ├── WeatherApiResponse.java (NEW)
│   │   │   │       ├── WeatherData.java (NEW)
│   │   │   │       └── ForecastData.java (NEW)
│   │   │   ├── repository/
│   │   │   │   └── ProductRepository.java
│   │   │   └── service/
│   │   │       └── WeatherService.java (NEW)
│   │   └── resources/
│   │       └── application.properties (modified)
│   └── test/
│       └── java/com/example/Assesment_two/
│           ├── AssesmentTwoApplicationTests.java (modified)
│           ├── controller/
│           │   ├── ProductControllerTest.java (modified - 14 tests)
│           │   └── WeatherControllerTest.java (NEW - 7 tests)
│           ├── integration/
│           │   └── WeatherControllerIntegrationTest.java (NEW - 8 tests)
│           └── service/
│               └── WeatherServiceTest.java (NEW - 13 tests)
└── pom.xml (modified)
```

---

## 10. Dependencies Used

| Library | Version | Purpose |
|---|---|---|
| Spring Boot | 4.1.1 | Application framework |
| Spring WebFlux | (from Boot 4.1.1) | WebClient for external API calls |
| Spring Data JPA | (from Boot 4.1.1) | Product repository |
| Hibernate Validator | 9.1.3.Final | Bean validation |
| Jackson | (from Boot 4.1.1) | JSON serialization/deserialization |
| H2 Database | (from Boot 4.1.1) | In-memory database |
| Lombok | (from Boot 4.1.1) | Boilerplate code reduction |
| JUnit 5 | (from spring-boot-starter-test) | Testing framework |
| Mockito | (from spring-boot-starter-test) | Mocking for unit tests |
| MockWebServer | 4.12.0 | Mock HTTP server for API tests |

---

## 11. Conclusion

The WeatherAPI.com integration has been successfully implemented and tested. The application now provides both product management APIs and weather data retrieval capabilities. All 43 tests pass, covering:

- **Unit tests** (34 tests): Isolated testing of WeatherService, WeatherController, and ProductController
- **Integration tests** (8 tests): Full-stack testing with MockWebServer simulating real API responses
- **Edge cases**: Invalid cities, API key errors, rate limiting, server errors, malformed JSON, network failures, and input validation

The test suite uses MockWebServer to simulate external API responses without making real API calls, ensuring tests are fast, reliable, and do not consume API quota.
