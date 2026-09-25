package com.example.Assesment_two.controller;

import com.example.Assesment_two.model.weather.WeatherData;
import com.example.Assesment_two.model.weather.ForecastData;
import com.example.Assesment_two.service.WeatherService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@Validated
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/{city}")
    public ResponseEntity<WeatherData> getCurrentWeather(@PathVariable @NotBlank String city) {
        WeatherData weatherData = weatherService.getCurrentWeather(city.trim());
        return ResponseEntity.ok(weatherData);
    }

    @GetMapping("/forecast/{city}")
    public ResponseEntity<ForecastData> getForecast(
            @PathVariable @NotBlank String city,
            @RequestParam(defaultValue = "3") @Min(1) @Max(14) int days) {
        ForecastData forecastData = weatherService.getForecast(city.trim(), days);
        return ResponseEntity.ok(forecastData);
    }
}
