package com.example.Assesment_two.service;

import com.example.Assesment_two.controller.CityNotFoundException;
import com.example.Assesment_two.controller.WeatherApiException;
import com.example.Assesment_two.model.weather.ForecastData;
import com.example.Assesment_two.model.weather.WeatherData;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class WeatherServiceTest {

    private MockWebServer mockWebServer;
    private WeatherService weatherService;
    private WebClient webClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        weatherService = new WeatherService(webClient);
        ReflectionTestUtils.setField(weatherService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(weatherService, "timeoutMs", 5000);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Should return weather data on successful API call")
    void shouldReturnWeatherDataOnSuccess() throws InterruptedException {
        String jsonResponse = """
            {
                "location": {
                    "name": "London",
                    "region": "London",
                    "country": "United Kingdom",
                    "lat": 51.52,
                    "lon": -0.11,
                    "tz_id": "Europe/London",
                    "last_updated_epoch": 1705317600,
                    "last_updated": "2024-01-15 10:00",
                    "localtime_epoch": 1705317600,
                    "localtime": "2024-01-15 10:00"
                },
                "current": {
                    "last_updated_epoch": 1705317600,
                    "last_updated": "2024-01-15 10:00",
                    "temp_c": 10.0,
                    "temp_f": 50.0,
                    "is_day": 1,
                    "condition": {
                        "text": "Sunny",
                        "icon": "//cdn.weatherapi.com/weather/64x64/day/1000.png",
                        "code": 1000
                    },
                    "wind_mph": 5.6,
                    "wind_kph": 9.0,
                    "wind_degree": 180,
                    "wind_dir": "S",
                    "pressure_mb": 1015.0,
                    "precip_mm": 0.0,
                    "humidity": 75,
                    "cloud": 0,
                    "feelslike_c": 8.0,
                    "feelslike_f": 46.4,
                    "vis_km": 10.0,
                    "uv": 1.0,
                    "gust_mph": 8.0,
                    "gust_kph": 12.8
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(200));

        WeatherData result = weatherService.getCurrentWeather("London");

        assertNotNull(result);
        assertEquals("London", result.getCity());
        assertEquals("United Kingdom", result.getCountry());
        assertEquals(10.0, result.getTemperatureC());
        assertEquals(50.0, result.getTemperatureF());
        assertTrue(result.getDay());
        assertEquals("Sunny", result.getConditionText());
        assertEquals(75, result.getHumidity());

        RecordedRequest recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("GET", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("/current.json"));
        assertTrue(recordedRequest.getPath().contains("q=London"));
        assertTrue(recordedRequest.getPath().contains("key=test-api-key"));
    }

    @Test
    @DisplayName("Should throw CityNotFoundException for invalid city (error code 1006)")
    void shouldThrowCityNotFoundExceptionForInvalidCity() {
        String jsonResponse = """
            {
                "error": {
                    "code": 1006,
                    "message": "No location found matching parameter 'q'"
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(400));

        CityNotFoundException exception = assertThrows(CityNotFoundException.class,
                () -> weatherService.getCurrentWeather("InvalidCity123"));

        assertTrue(exception.getMessage().contains("InvalidCity123"));
    }

    @Test
    @DisplayName("Should throw CityNotFoundException when API returns error code 1003 (missing q param)")
    void shouldThrowCityNotFoundExceptionForMissingQueryParam() {
        String jsonResponse = """
            {
                "error": {
                    "code": 1003,
                    "message": "Parameter 'q' not provided"
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(400));

        CityNotFoundException exception = assertThrows(CityNotFoundException.class,
                () -> weatherService.getCurrentWeather(" "));

        assertTrue(exception.getMessage().contains("No location found"));
    }

    @Test
    @DisplayName("Should throw WeatherApiException for invalid API key (error code 1002)")
    void shouldThrowWeatherApiExceptionForInvalidApiKey() {
        String jsonResponse = """
            {
                "error": {
                    "code": 1002,
                    "message": "API key not provided"
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(401));

        WeatherApiException exception = assertThrows(WeatherApiException.class,
                () -> weatherService.getCurrentWeather("London"));

        assertTrue(exception.getMessage().contains("Invalid or missing Weather API key"));
    }

    @Test
    @DisplayName("Should throw WeatherApiException for rate limit exceeded (error code 2007)")
    void shouldThrowWeatherApiExceptionForRateLimitExceeded() {
        String jsonResponse = """
            {
                "error": {
                    "code": 2007,
                    "message": "API key has exceeded calls per month quota"
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(403));

        WeatherApiException exception = assertThrows(WeatherApiException.class,
                () -> weatherService.getCurrentWeather("London"));

        assertTrue(exception.getMessage().contains("quota exceeded"));
    }

    @Test
    @DisplayName("Should throw WeatherApiException for server error (500)")
    void shouldThrowWeatherApiExceptionForServerError() {
        String jsonResponse = """
            {
                "error": {
                    "code": 9999,
                    "message": "Internal application error"
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(500));

        WeatherApiException exception = assertThrows(WeatherApiException.class,
                () -> weatherService.getCurrentWeather("London"));

        assertNotNull(exception);
    }

    @Test
    @DisplayName("Should throw WeatherApiException for malformed JSON response")
    void shouldThrowWeatherApiExceptionForMalformedJson() {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{ this is not valid json !!!")
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(200));

        WeatherApiException exception = assertThrows(WeatherApiException.class,
                () -> weatherService.getCurrentWeather("London"));

        assertTrue(exception.getMessage().contains("Failed to fetch weather data"));
    }

    @Test
    @DisplayName("Should throw WeatherApiException on service unavailable (503)")
    void shouldThrowWeatherApiExceptionOnNetworkFailure() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(503)
                .setBody("{\"error\":{\"code\":9999,\"message\":\"Service unavailable\"}}"));

        WeatherApiException exception = assertThrows(WeatherApiException.class,
                () -> weatherService.getCurrentWeather("London"));

        assertNotNull(exception);
    }

    @Test
    @DisplayName("Should parse all weather fields correctly")
    void shouldParseAllWeatherFieldsCorrectly() {
        String jsonResponse = """
            {
                "location": {
                    "name": "New York",
                    "region": "NY",
                    "country": "USA",
                    "lat": 40.71,
                    "lon": -74.01,
                    "tz_id": "America/New_York",
                    "localtime": "2024-06-15 14:30"
                },
                "current": {
                    "last_updated": "2024-06-15 14:30",
                    "temp_c": 25.5,
                    "temp_f": 77.9,
                    "is_day": 1,
                    "condition": {
                        "text": "Partly cloudy",
                        "icon": "//cdn.weatherapi.com/weather/64x64/day/116.png",
                        "code": 1003
                    },
                    "wind_mph": 8.1,
                    "wind_kph": 13.0,
                    "wind_degree": 180,
                    "wind_dir": "S",
                    "pressure_mb": 1013.0,
                    "precip_mm": 0.0,
                    "humidity": 65,
                    "cloud": 40,
                    "feelslike_c": 27.0,
                    "feelslike_f": 80.6,
                    "vis_km": 10.0,
                    "uv": 5.0,
                    "gust_mph": 11.2,
                    "gust_kph": 18.0
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(200));

        WeatherData result = weatherService.getCurrentWeather("New York");

        assertNotNull(result);
        assertEquals("New York", result.getCity());
        assertEquals("USA", result.getCountry());
        assertEquals("NY", result.getRegion());
        assertEquals(40.71, result.getLatitude());
        assertEquals(-74.01, result.getLongitude());
        assertEquals("America/New_York", result.getTimezone());
        assertEquals("2024-06-15 14:30", result.getLocalTime());
        assertEquals(25.5, result.getTemperatureC());
        assertEquals(77.9, result.getTemperatureF());
        assertTrue(result.getDay());
        assertEquals("Partly cloudy", result.getConditionText());
        assertEquals("//cdn.weatherapi.com/weather/64x64/day/116.png", result.getConditionIcon());
        assertEquals(1003, result.getConditionCode());
        assertEquals(13.0, result.getWindKph());
        assertEquals(65, result.getHumidity());
        assertEquals(40, result.getCloudCover());
        assertEquals(27.0, result.getFeelslikeC());
        assertEquals(5.0, result.getUvIndex());
    }

    @Test
    @DisplayName("Should fetch forecast data successfully")
    void shouldFetchForecastDataSuccessfully() throws InterruptedException {
        String jsonResponse = """
            {
                "location": {
                    "name": "London",
                    "region": "London",
                    "country": "United Kingdom",
                    "lat": 51.52,
                    "lon": -0.11,
                    "tz_id": "Europe/London"
                },
                "forecast": {
                    "forecastday": [
                        {
                            "date": "2024-01-15",
                            "date_epoch": 1705276800,
                            "day": {
                                "maxtemp_c": 12.0,
                                "mintemp_c": 8.0,
                                "avgtemp_c": 10.0,
                                "maxwind_mph": 10.0,
                                "maxwind_kph": 16.0,
                                "totalprecip_mm": 0.5,
                                "avg_humidity": 75,
                                "condition": {
                                    "text": "Sunny",
                                    "icon": "//cdn.weatherapi.com/weather/64x64/day/1000.png",
                                    "code": 1000
                                },
                                "uv": 2.0
                            },
                            "astro": {
                                "sunrise": "07:50",
                                "sunset": "16:20"
                            }
                        },
                        {
                            "date": "2024-01-16",
                            "date_epoch": 1705363200,
                            "day": {
                                "maxtemp_c": 11.0,
                                "mintemp_c": 6.0,
                                "avgtemp_c": 8.5,
                                "maxwind_mph": 12.0,
                                "maxwind_kph": 19.3,
                                "totalprecip_mm": 1.2,
                                "avg_humidity": 80,
                                "condition": {
                                    "text": "Rain",
                                    "icon": "//cdn.weatherapi.com/weather/64x64/day/296.png",
                                    "code": 1003
                                },
                                "uv": 1.0
                            },
                            "astro": {
                                "sunrise": "07:48",
                                "sunset": "16:22"
                            }
                        }
                    ]
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(200));

        ForecastData result = weatherService.getForecast("London", 2);

        assertNotNull(result);
        assertEquals("London", result.getCity());
        assertEquals("United Kingdom", result.getCountry());
        assertEquals(2, result.getForecastDays().length);

        ForecastData.ForecastDayData day1 = result.getForecastDays()[0];
        assertEquals("2024-01-15", day1.getDate());
        assertEquals(12.0, day1.getMaxTempC());
        assertEquals(8.0, day1.getMinTempC());
        assertEquals(10.0, day1.getAvgTempC());
        assertEquals("Sunny", day1.getConditionText());
        assertEquals("07:50", day1.getSunrise());
        assertEquals("16:20", day1.getSunset());

        ForecastData.ForecastDayData day2 = result.getForecastDays()[1];
        assertEquals("2024-01-16", day2.getDate());
        assertEquals(11.0, day2.getMaxTempC());
        assertEquals("Rain", day2.getConditionText());

        RecordedRequest recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertTrue(recordedRequest.getPath().contains("/forecast.json"));
        assertTrue(recordedRequest.getPath().contains("days=2"));
    }

    @Test
    @DisplayName("Should throw WeatherApiException for invalid days parameter (out of range)")
    void shouldThrowWeatherApiExceptionForInvalidDays() {
        WeatherApiException exception = assertThrows(WeatherApiException.class,
                () -> weatherService.getForecast("London", 15));

        assertTrue(exception.getMessage().contains("Days parameter must be between 1 and 14"));
    }

    @Test
    @DisplayName("Should throw WeatherApiException for zero days parameter")
    void shouldThrowWeatherApiExceptionForZeroDays() {
        WeatherApiException exception = assertThrows(WeatherApiException.class,
                () -> weatherService.getForecast("London", 0));

        assertTrue(exception.getMessage().contains("Days parameter must be between 1 and 14"));
    }

    @Test
    @DisplayName("Should throw CityNotFoundException on forecast for invalid city")
    void shouldThrowCityNotFoundExceptionOnForecastForInvalidCity() {
        String jsonResponse = """
            {
                "error": {
                    "code": 1006,
                    "message": "No location found"
                }
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(400));

        CityNotFoundException exception = assertThrows(CityNotFoundException.class,
                () -> weatherService.getForecast("InvalidCity123", 3));

        assertTrue(exception.getMessage().contains("InvalidCity123"));
    }
}
