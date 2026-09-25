package com.example.Assesment_two.integration;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class WeatherControllerIntegrationTest {

    private static MockWebServer mockWebServer;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeAll
    static void startMockServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("weather.api.base-url", () -> mockWebServer.url("/").toString());
        registry.add("weather.api.key", () -> "test-api-key");
        registry.add("weather.api.timeout-ms", () -> "5000");
    }

    @AfterAll
    static void stopMockServer() throws IOException {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    private String loadLondonWeatherResponse() {
        return """
            {
                "location": {
                    "name": "London",
                    "region": "London, Greater London",
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
    }

    private String loadLondonForecastResponse() {
        return """
            {
                "location": {
                    "name": "London",
                    "region": "London, Greater London",
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
                        },
                        {
                            "date": "2024-01-17",
                            "date_epoch": 1705449600,
                            "day": {
                                "maxtemp_c": 9.0,
                                "mintemp_c": 5.0,
                                "avgtemp_c": 7.0,
                                "maxwind_mph": 15.0,
                                "maxwind_kph": 24.0,
                                "totalprecip_mm": 2.0,
                                "avg_humidity": 82,
                                "condition": {
                                    "text": "Light rain",
                                    "icon": "//cdn.weatherapi.com/weather/64x64/day/296.png",
                                    "code": 1003
                                },
                                "uv": 1.0
                            },
                            "astro": {
                                "sunrise": "07:46",
                                "sunset": "16:24"
                            }
                        }
                    ]
                }
            }
            """;
    }

    private String loadCityNotFoundErrorResponse() {
        return """
            {
                "error": {
                    "code": 1006,
                    "message": "No location found matching parameter 'q'"
                }
            }
            """;
    }

    private String loadInvalidKeyErrorResponse() {
        return """
            {
                "error": {
                    "code": 1002,
                    "message": "API key not provided"
                }
            }
            """;
    }

    @Test
    @DisplayName("Integration: GET /api/weather/{city} returns weather data from external API")
    void integrationTestGetCurrentWeather() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(loadLondonWeatherResponse())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(200));

        mockMvc.perform(get("/api/weather/London"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value("London"))
                .andExpect(jsonPath("$.country").value("United Kingdom"))
                .andExpect(jsonPath("$.temperatureC").value(10.0))
                .andExpect(jsonPath("$.conditionText").value("Sunny"))
                .andExpect(jsonPath("$.humidity").value(75));

        RecordedRequest recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(recordedRequest);
        Assertions.assertEquals("GET", recordedRequest.getMethod());
        Assertions.assertTrue(recordedRequest.getPath().contains("/current.json"));
        Assertions.assertTrue(recordedRequest.getPath().contains("q=London"));
    }

    @Test
    @DisplayName("Integration: GET /api/weather/{invalidCity} returns 400 Bad Request")
    void integrationTestCityNotFound() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(loadCityNotFoundErrorResponse())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(400));

        mockMvc.perform(get("/api/weather/InvalidCityXYZ"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("No location found")));

        RecordedRequest recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(recordedRequest);
        Assertions.assertTrue(recordedRequest.getPath().contains("/current.json"));
    }

    @Test
    @DisplayName("Integration: GET /api/weather/forecast/{city}?days=3 returns forecast data")
    void integrationTestGetForecast() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(loadLondonForecastResponse())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(200));

        mockMvc.perform(get("/api/weather/forecast/London")
                        .param("days", "3"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value("London"))
                .andExpect(jsonPath("$.forecastDays.length()").value(3))
                .andExpect(jsonPath("$.forecastDays[0].date").value("2024-01-15"))
                .andExpect(jsonPath("$.forecastDays[0].maxTempC").value(12.0))
                .andExpect(jsonPath("$.forecastDays[0].conditionText").value("Sunny"))
                .andExpect(jsonPath("$.forecastDays[1].date").value("2024-01-16"))
                .andExpect(jsonPath("$.forecastDays[1].conditionText").value("Rain"))
                .andExpect(jsonPath("$.forecastDays[2].date").value("2024-01-17"))
                .andExpect(jsonPath("$.forecastDays[0].sunrise").value("07:50"));

        RecordedRequest recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(recordedRequest);
        Assertions.assertTrue(recordedRequest.getPath().contains("/forecast.json"));
    }

    @Test
    @DisplayName("Integration: GET /api/weather/forecast/{city} uses default days=3")
    void integrationTestForecastDefaultDays() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(loadLondonForecastResponse())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(200));

        mockMvc.perform(get("/api/weather/forecast/London"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("London"))
                .andExpect(jsonPath("$.forecastDays.length()").value(3));

        RecordedRequest recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(recordedRequest);
    }

    @Test
    @DisplayName("Integration: Product CRUD still works alongside Weather API integration")
    void integrationTestProductCrudStillWorks() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Test Product\", \"price\": 19.99}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.price").value(19.99));

        mockMvc.perform(get("/api/products"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Product"));
    }

    @Test
    @DisplayName("Integration: Weather API error response returns structured error")
    void integrationTestWeatherApiErrorStructure() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(loadCityNotFoundErrorResponse())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(400));

        mockMvc.perform(get("/api/weather/NonExistentCity"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("Integration: Weather response contains all expected fields")
    void integrationTestWeatherResponseFields() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(loadLondonWeatherResponse())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(200));

        mockMvc.perform(get("/api/weather/London"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").exists())
                .andExpect(jsonPath("$.country").exists())
                .andExpect(jsonPath("$.region").exists())
                .andExpect(jsonPath("$.latitude").exists())
                .andExpect(jsonPath("$.longitude").exists())
                .andExpect(jsonPath("$.timezone").exists())
                .andExpect(jsonPath("$.localTime").exists())
                .andExpect(jsonPath("$.lastUpdated").exists())
                .andExpect(jsonPath("$.temperatureC").exists())
                .andExpect(jsonPath("$.temperatureF").exists())
                .andExpect(jsonPath("$.day").exists())
                .andExpect(jsonPath("$.conditionText").exists())
                .andExpect(jsonPath("$.conditionIcon").exists())
                .andExpect(jsonPath("$.windKph").exists())
                .andExpect(jsonPath("$.humidity").exists())
                .andExpect(jsonPath("$.feelslikeC").exists())
                .andExpect(jsonPath("$.uvIndex").exists());
    }

    @Test
    @DisplayName("Integration: Weather API returns 502 on invalid API key")
    void integrationTestInvalidApiKey() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setBody(loadInvalidKeyErrorResponse())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(401));

        mockMvc.perform(get("/api/weather/London"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value(containsString("Invalid or missing Weather API key")));

        RecordedRequest recordedRequest = mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        Assertions.assertNotNull(recordedRequest);
        Assertions.assertTrue(recordedRequest.getPath().contains("/current.json"));
    }
}
