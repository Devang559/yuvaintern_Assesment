package com.example.Assesment_two.model.weather;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherData {
    private String city;
    private String region;
    private String country;
    private Double latitude;
    private Double longitude;
    private String timezone;
    private String localTime;
    private String lastUpdated;

    private Double temperatureC;
    private Double temperatureF;
    private Boolean day;
    private String conditionText;
    private String conditionIcon;
    private Integer conditionCode;
    private Double windMph;
    private Double windKph;
    private String windDirection;
    private Integer humidity;
    private Integer cloudCover;
    private Double feelslikeC;
    private Double feelslikeF;
    private Double visibilityKm;
    private Double uvIndex;
    private Double gustMph;
    private Double gustKph;
}
