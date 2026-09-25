package com.example.Assesment_two.model.weather;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForecastData {
    private String city;
    private String country;
    private String region;

    private ForecastDayData[] forecastDays;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ForecastDayData {
        private String date;
        private Double maxTempC;
        private Double minTempC;
        private Double avgTempC;
        private String conditionText;
        private String conditionIcon;
        private Integer conditionCode;
        private Double maxWindKph;
        private Double totalPrecipMm;
        private Double avgHumidity;
        private Double uvIndex;
        private String sunrise;
        private String sunset;
    }
}
