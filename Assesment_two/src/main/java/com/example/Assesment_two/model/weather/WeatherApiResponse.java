package com.example.Assesment_two.model.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherApiResponse {

    private Location location;
    private CurrentWeather current;
    private Forecast forecast;
    private WeatherError error;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        private String name;
        private String region;
        private String country;
        private Double lat;
        private Double lon;

        @JsonProperty("tz_id")
        private String tzId;

        @JsonProperty("last_updated_epoch")
        private Long lastUpdatedEpoch;

        @JsonProperty("last_updated")
        private String lastUpdated;

        @JsonProperty("localtime_epoch")
        private Long localtimeEpoch;

        @JsonProperty("localtime")
        private String localtime;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CurrentWeather {
        @JsonProperty("last_updated_epoch")
        private Long lastUpdatedEpoch;

        @JsonProperty("last_updated")
        private String lastUpdated;

        @JsonProperty("temp_c")
        private Double tempC;

        @JsonProperty("temp_f")
        private Double tempF;

        @JsonProperty("is_day")
        private Integer isDay;

        private Condition condition;

        @JsonProperty("wind_mph")
        private Double windMph;

        @JsonProperty("wind_kph")
        private Double windKph;

        @JsonProperty("wind_degree")
        private Integer windDegree;

        @JsonProperty("wind_dir")
        private String windDir;

        @JsonProperty("pressure_mb")
        private Double pressureMb;

        @JsonProperty("precip_mm")
        private Double precipMm;

        private Integer humidity;
        private Integer cloud;

        @JsonProperty("feelslike_c")
        private Double feelslikeC;

        @JsonProperty("feelslike_f")
        private Double feelslikeF;

        @JsonProperty("vis_km")
        private Double visKm;

        private Double uv;

        @JsonProperty("gust_mph")
        private Double gustMph;

        @JsonProperty("gust_kph")
        private Double gustKph;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Condition {
        private String text;
        private String icon;
        private Integer code;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Forecast {
        @JsonProperty("forecastday")
        private List<ForecastDay> forecastDay;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ForecastDay {
        private String date;

        @JsonProperty("date_epoch")
        private Long dateEpoch;

        private ForecastDayDetails day;
        private Astro astro;
        private List<Hour> hour;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ForecastDayDetails {
        @JsonProperty("maxtemp_c")
        private Double maxTempC;

        @JsonProperty("mintemp_c")
        private Double minTempC;

        @JsonProperty("avgtemp_c")
        private Double avgTempC;

        @JsonProperty("maxwind_mph")
        private Double maxWindMph;

        @JsonProperty("maxwind_kph")
        private Double maxWindKph;

        @JsonProperty("totalprecip_mm")
        private Double totalPrecipMm;

        @JsonProperty("avg_humidity")
        private Double avgHumidity;

        private Condition condition;

        private Double uv;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Astro {
        private String sunrise;
        private String sunset;
        private String moonrise;
        private String moonset;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Hour {
        @JsonProperty("time_epoch")
        private Long timeEpoch;

        private String time;

        @JsonProperty("temp_c")
        private Double tempC;

        @JsonProperty("temp_f")
        private Double tempF;

        @JsonProperty("is_day")
        private Integer isDay;

        private Condition condition;

        @JsonProperty("wind_mph")
        private Double windMph;

        @JsonProperty("wind_kph")
        private Double windKph;

        private Integer humidity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherError {
        private Integer code;
        private String message;
    }
}
