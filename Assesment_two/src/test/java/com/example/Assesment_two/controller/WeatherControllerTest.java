package com.example.Assesment_two.controller;

import com.example.Assesment_two.model.weather.WeatherData;
import com.example.Assesment_two.model.weather.ForecastData;
import com.example.Assesment_two.service.WeatherService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WeatherControllerTest {

    @Mock
    private WeatherService weatherService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new WeatherController(weatherService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/weather/{city} should return 200 and weather data on success")
    void shouldGetCurrentWeatherSuccessfully() throws Exception {
        WeatherData mockData = new WeatherData();
        mockData.setCity("London");
        mockData.setCountry("United Kingdom");
        mockData.setTemperatureC(15.0);
        mockData.setConditionText("Sunny");
        mockData.setHumidity(65);

        when(weatherService.getCurrentWeather("London")).thenReturn(mockData);

        mockMvc.perform(get("/api/weather/London"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value("London"))
                .andExpect(jsonPath("$.country").value("United Kingdom"))
                .andExpect(jsonPath("$.temperatureC").value(15.0))
                .andExpect(jsonPath("$.conditionText").value("Sunny"))
                .andExpect(jsonPath("$.humidity").value(65));
    }

    @Test
    @DisplayName("GET /api/weather/{city} should return 400 when city is not found")
    void shouldReturn400WhenCityNotFound() throws Exception {
        when(weatherService.getCurrentWeather("InvalidCity123"))
                .thenThrow(new CityNotFoundException("No location found for city: InvalidCity123"));

        mockMvc.perform(get("/api/weather/InvalidCity123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("No location found for city: InvalidCity123"));
    }

    @Test
    @DisplayName("GET /api/weather/{city} should return 502 on Weather API error")
    void shouldReturn502OnWeatherApiError() throws Exception {
        when(weatherService.getCurrentWeather("London"))
                .thenThrow(new WeatherApiException("Invalid Weather API key"));

        mockMvc.perform(get("/api/weather/London"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("Invalid Weather API key"));
    }

    @Test
    @DisplayName("GET /api/weather/forecast/{city} should return 200 and forecast data")
    void shouldGetForecastSuccessfully() throws Exception {
        ForecastData.ForecastDayData day1 = new ForecastData.ForecastDayData();
        day1.setDate("2024-01-15");
        day1.setMaxTempC(12.0);
        day1.setMinTempC(8.0);
        day1.setAvgTempC(10.0);
        day1.setConditionText("Sunny");
        day1.setSunrise("07:50");
        day1.setSunset("16:20");

        ForecastData mockForecast = new ForecastData();
        mockForecast.setCity("London");
        mockForecast.setCountry("United Kingdom");
        mockForecast.setForecastDays(new ForecastData.ForecastDayData[]{day1});

        when(weatherService.getForecast(eq("London"), anyInt())).thenReturn(mockForecast);

        mockMvc.perform(get("/api/weather/forecast/London")
                        .param("days", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.city").value("London"))
                .andExpect(jsonPath("$.forecastDays[0].date").value("2024-01-15"))
                .andExpect(jsonPath("$.forecastDays[0].maxTempC").value(12.0))
                .andExpect(jsonPath("$.forecastDays[0].conditionText").value("Sunny"));
    }

    @Test
    @DisplayName("GET /api/weather/forecast/{city} should use default days=3")
    void shouldUseDefaultDaysWhenNotSpecified() throws Exception {
        ForecastData mockForecast = new ForecastData();
        mockForecast.setCity("London");
        mockForecast.setForecastDays(new ForecastData.ForecastDayData[]{});

        when(weatherService.getForecast(eq("London"), eq(3))).thenReturn(mockForecast);

        mockMvc.perform(get("/api/weather/forecast/London"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("London"));
    }

    @Test
    @DisplayName("GET /api/weather/forecast/{city}?days=15 should return 400 for invalid days")
    void shouldReturn400ForInvalidDays() throws Exception {
        when(weatherService.getForecast("London", 15))
                .thenThrow(new WeatherApiException("Days parameter must be between 1 and 14"));

        mockMvc.perform(get("/api/weather/forecast/London")
                        .param("days", "15"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Days parameter must be between 1 and 14"));
    }

    @Test
    @DisplayName("GET /api/weather/{city} should return 502 on unexpected service error")
    void shouldReturn502OnUnexpectedServiceError() throws Exception {
        when(weatherService.getCurrentWeather("London"))
                .thenThrow(new WeatherApiException("Unexpected service error"));

        mockMvc.perform(get("/api/weather/London"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }
}
