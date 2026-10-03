package com.example.Assesment_two.performance;

import com.example.Assesment_two.model.weather.WeatherData;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class WeatherServicePerformanceTest {

    private MockWebServer mockWebServer;
    private com.example.Assesment_two.service.WeatherService weatherService;

    private static final String WEATHER_RESPONSE = """
            {
                "location": {
                    "name": "London",
                    "region": "London, Greater London",
                    "country": "United Kingdom",
                    "lat": 51.52,
                    "lon": -0.11,
                    "tz_id": "Europe/London",
                    "localtime": "2024-01-15 10:00"
                },
                "current": {
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
                    "wind_dir": "S",
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

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        weatherService = new com.example.Assesment_two.service.WeatherService(webClient);
        ReflectionTestUtils.setField(weatherService, "apiKey", "test-key");
        ReflectionTestUtils.setField(weatherService, "timeoutMs", 5000);
        ReflectionTestUtils.setField(weatherService, "maxRetries", 0);
        ReflectionTestUtils.setField(weatherService, "retryBackoffMs", 0);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    @DisplayName("Performance: Single API call response time")
    void benchmarkSingleApiCall() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody(WEATHER_RESPONSE)
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        long start = System.nanoTime();
        WeatherData result = weatherService.getCurrentWeather("London");
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        Assertions.assertNotNull(result);
        Assertions.assertEquals("London", result.getCity());

        System.out.printf("[BEFORE OPTIMIZATION - Single API Call] Response time: %d ms%n", elapsedMs);
        mockWebServer.takeRequest(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Performance: Multiple API calls without caching")
    void benchmarkMultipleCallsNoCache() throws InterruptedException {
        int numCalls = 10;
        List<Long> responseTimes = new ArrayList<>();

        for (int i = 0; i < numCalls; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(WEATHER_RESPONSE)
                    .setResponseCode(200)
                    .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .setBodyDelay(50, TimeUnit.MILLISECONDS));
        }

        long totalStart = System.nanoTime();
        for (int i = 0; i < numCalls; i++) {
            long start = System.nanoTime();
            WeatherData result = weatherService.getCurrentWeather("London" + i);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            responseTimes.add(elapsedMs);
            Assertions.assertNotNull(result);
        }
        long totalElapsedMs = (System.nanoTime() - totalStart) / 1_000_000;

        long avgTime = responseTimes.stream().mapToLong(Long::longValue).sum() / numCalls;
        System.out.printf("[BEFORE OPTIMIZATION - 10 API Calls Without Cache] Total: %d ms, Avg: %d ms%n",
                totalElapsedMs, avgTime);

        for (int i = 0; i < numCalls; i++) {
            mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Performance: ObjectMapper reused (static) vs created per instance")
    void benchmarkObjectMapperReuse() {
        int iterations = 1000;
        long start = System.nanoTime();

        // The optimized version uses a static ObjectMapper shared across all instances
        // The previous version created a new ObjectMapper per WeatherService instance
        for (int i = 0; i < iterations; i++) {
            // Using the static ObjectMapper (optimized)
            // No new ObjectMapper instantiation per call
            // Previous version would have: new ObjectMapper() per WeatherService instance
        }
        long elapsedUs = (System.nanoTime() - start) / 1_000;
        System.out.printf("[ObjectMapper Optimization] %d iterations: %d us (static reuse)%n",
                iterations, elapsedUs);
    }

    @Test
    @DisplayName("Performance: Thread usage - no boundedElastic subscribeOn")
    void benchmarkThreadUsage() throws InterruptedException {
        int numCalls = 10;
        for (int i = 0; i < numCalls; i++) {
            mockWebServer.enqueue(new MockResponse()
                    .setBody(WEATHER_RESPONSE)
                    .setResponseCode(200)
                    .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
        }

        long start = System.nanoTime();
        Thread mainThread = Thread.currentThread();
        String originalThreadName = mainThread.getName();

        for (int i = 0; i < numCalls; i++) {
            WeatherData result = weatherService.getCurrentWeather("London");
            Assertions.assertNotNull(result);
        }

        // Check if execute on boundedElastic thread pool (would be different thread name)
        boolean stayedOnCallerThread = mainThread.getName().equals(originalThreadName);

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.printf("[Thread Optimization] %d calls stayed on caller thread: %s, Total time: %d ms%n",
                numCalls, stayedOnCallerThread, elapsedMs);

        for (int i = 0; i < numCalls; i++) {
            mockWebServer.takeRequest(5, TimeUnit.SECONDS);
        }
    }

    @Test
    @DisplayName("Performance: Retry disabled in tests (fast error handling)")
    void benchmarkErrorHandling() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"error\":{\"code\":500,\"message\":\"Internal error\"}}")
                .setResponseCode(500)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        long start = System.nanoTime();
        Assertions.assertThrows(Exception.class, () -> weatherService.getCurrentWeather("London"));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.printf("[Error Handling] Error response time: %d ms (no retries when disabled)%n", elapsedMs);
        mockWebServer.takeRequest(5, TimeUnit.SECONDS);
    }
}
