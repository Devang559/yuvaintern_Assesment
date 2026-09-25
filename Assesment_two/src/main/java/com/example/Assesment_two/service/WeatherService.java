package com.example.Assesment_two.service;

import com.example.Assesment_two.controller.CityNotFoundException;
import com.example.Assesment_two.controller.WeatherApiException;
import com.example.Assesment_two.model.weather.ForecastData;
import com.example.Assesment_two.model.weather.WeatherApiResponse;
import com.example.Assesment_two.model.weather.WeatherData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherService {

    private final WebClient weatherWebClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${weather.api.key:DEMO_KEY}")
    private String apiKey;

    @Value("${weather.api.timeout-ms:5000}")
    private int timeoutMs;

    public WeatherData getCurrentWeather(String city) {
        log.info("Fetching current weather for city: {}", city);
        try {
            WeatherApiResponse response = weatherWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/current.json")
                            .queryParam("key", apiKey)
                            .queryParam("q", city)
                            .queryParam("aq", "no")
                            .build())
                    .retrieve()
                    .bodyToMono(WeatherApiResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .subscribeOn(Schedulers.boundedElastic())
                    .block();

            return processCurrentWeatherResponse(response, city);
        } catch (WeatherApiException | CityNotFoundException e) {
            throw e;
        } catch (WebClientResponseException e) {
            throw handleWebClientResponseException(e, city);
        } catch (Exception e) {
            if (e.getCause() instanceof WeatherApiException || e.getCause() instanceof CityNotFoundException) {
                throw (RuntimeException) e.getCause();
            }
            throw new WeatherApiException("Failed to fetch weather data for city: " + city + " - " + e.getMessage(), e);
        }
    }

    public ForecastData getForecast(String city, int days) {
        if (days < 1 || days > 14) {
            throw new WeatherApiException("Days parameter must be between 1 and 14");
        }

        log.info("Fetching {}-day forecast for city: {}", days, city);
        try {
            WeatherApiResponse response = weatherWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/forecast.json")
                            .queryParam("key", apiKey)
                            .queryParam("q", city)
                            .queryParam("aq", "no")
                            .queryParam("days", days)
                            .build())
                    .retrieve()
                    .bodyToMono(WeatherApiResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .subscribeOn(Schedulers.boundedElastic())
                    .block();

            return processForecastResponse(response, city);
        } catch (WeatherApiException | CityNotFoundException e) {
            throw e;
        } catch (WebClientResponseException e) {
            throw handleWebClientResponseException(e, city);
        } catch (Exception e) {
            if (e.getCause() instanceof WeatherApiException || e.getCause() instanceof CityNotFoundException) {
                throw (RuntimeException) e.getCause();
            }
            throw new WeatherApiException("Failed to fetch forecast for city: " + city + " - " + e.getMessage(), e);
        }
    }

    private WeatherData processCurrentWeatherResponse(WeatherApiResponse response, String city) {
        if (response == null) {
            throw new WeatherApiException("Received null response from Weather API for city: " + city);
        }
        if (response.getError() != null) {
            throw handleApiError(response.getError(), city);
        }
        log.info("Successfully fetched weather data for: {}",
                response.getLocation() != null ? response.getLocation().getName() : city);
        return mapToWeatherData(response);
    }

    private ForecastData processForecastResponse(WeatherApiResponse response, String city) {
        if (response == null) {
            throw new WeatherApiException("Received null response from Weather API for city: " + city);
        }
        if (response.getError() != null) {
            throw handleApiError(response.getError(), city);
        }
        log.info("Successfully fetched forecast data for: {}",
                response.getLocation() != null ? response.getLocation().getName() : city);
        return mapToForecastData(response);
    }

    private RuntimeException handleWebClientResponseException(WebClientResponseException e, String city) {
        int statusCode = e.getStatusCode().value();
        String responseBody = e.getResponseBodyAsString();

        log.error("WeatherAPI HTTP error [{}]: {}", statusCode, responseBody);

        try {
            WeatherApiResponse errorResponse = objectMapper.readValue(
                    responseBody, WeatherApiResponse.class);
            if (errorResponse.getError() != null) {
                return handleApiError(errorResponse.getError(), city);
            }
        } catch (Exception parseEx) {
            log.warn("Could not parse error response body: {}", parseEx.getMessage());
        }

        return switch (statusCode) {
            case 400, 404 -> new CityNotFoundException("No location found for city: " + city);
            case 401 -> new WeatherApiException("Invalid Weather API key");
            case 403 -> new WeatherApiException("API key quota exceeded or access forbidden");
            case 429 -> new WeatherApiException("Weather API rate limit exceeded");
            case 500, 502, 503 -> new WeatherApiException("Weather API server error (HTTP " + statusCode + ")");
            default -> new WeatherApiException("Weather API error [" + statusCode + "]: " + e.getMessage());
        };
    }

    private RuntimeException handleApiError(WeatherApiResponse.WeatherError error, String city) {
        if (error == null) {
            return new WeatherApiException("Weather API error for city: " + city);
        }

        int code = error.getCode() != null ? error.getCode() : 0;
        String message = error.getMessage();

        return switch (code) {
            case 1003, 1005, 1006 -> {
                log.error("WeatherAPI location error [{}]: {}", code, message);
                yield new CityNotFoundException("No location found for city: " + city + " - " + message);
            }
            case 1002, 2006 -> {
                log.error("WeatherAPI authentication error [{}]: {}", code, message);
                yield new WeatherApiException("Invalid or missing Weather API key: " + message);
            }
            case 2007, 2008 -> {
                log.error("WeatherAPI quota error [{}]: {}", code, message);
                yield new WeatherApiException("API key quota exceeded or disabled: " + message);
            }
            default -> {
                log.error("WeatherAPI error [{}]: {}", code, message);
                yield new WeatherApiException("Weather API error [" + code + "]: " + message);
            }
        };
    }

    private WeatherData mapToWeatherData(WeatherApiResponse response) {
        WeatherApiResponse.Location loc = response.getLocation();
        WeatherApiResponse.CurrentWeather cur = response.getCurrent();

        WeatherData data = new WeatherData();
        if (loc != null) {
            data.setCity(loc.getName());
            data.setRegion(loc.getRegion());
            data.setCountry(loc.getCountry());
            data.setLatitude(loc.getLat());
            data.setLongitude(loc.getLon());
            data.setTimezone(loc.getTzId());
            data.setLocalTime(loc.getLocaltime());
        }

        if (cur != null) {
            data.setLastUpdated(cur.getLastUpdated());
            data.setTemperatureC(cur.getTempC());
            data.setTemperatureF(cur.getTempF());
            data.setDay(cur.getIsDay() != null && cur.getIsDay() == 1);

            if (cur.getCondition() != null) {
                data.setConditionText(cur.getCondition().getText());
                data.setConditionIcon(cur.getCondition().getIcon());
                data.setConditionCode(cur.getCondition().getCode());
            }

            data.setWindMph(cur.getWindMph());
            data.setWindKph(cur.getWindKph());
            data.setWindDirection(cur.getWindDir());
            data.setHumidity(cur.getHumidity());
            data.setCloudCover(cur.getCloud());
            data.setFeelslikeC(cur.getFeelslikeC());
            data.setFeelslikeF(cur.getFeelslikeF());
            data.setVisibilityKm(cur.getVisKm());
            data.setUvIndex(cur.getUv());
            data.setGustMph(cur.getGustMph());
            data.setGustKph(cur.getGustKph());
        }

        return data;
    }

    private ForecastData mapToForecastData(WeatherApiResponse response) {
        WeatherApiResponse.Location loc = response.getLocation();

        ForecastData data = new ForecastData();
        if (loc != null) {
            data.setCity(loc.getName());
            data.setCountry(loc.getCountry());
            data.setRegion(loc.getRegion());
        }

        WeatherApiResponse.Forecast forecast = response.getForecast();
        if (forecast != null && forecast.getForecastDay() != null) {
            List<ForecastData.ForecastDayData> days = forecast.getForecastDay()
                    .stream()
                    .map(this::mapForecastDay)
                    .collect(Collectors.toList());
            data.setForecastDays(days.toArray(ForecastData.ForecastDayData[]::new));
        }

        return data;
    }

    private ForecastData.ForecastDayData mapForecastDay(WeatherApiResponse.ForecastDay fd) {
        ForecastData.ForecastDayData dayData = new ForecastData.ForecastDayData();
        dayData.setDate(fd.getDate());

        WeatherApiResponse.ForecastDayDetails details = fd.getDay();
        if (details != null) {
            dayData.setMaxTempC(details.getMaxTempC());
            dayData.setMinTempC(details.getMinTempC());
            dayData.setAvgTempC(details.getAvgTempC());
            dayData.setMaxWindKph(details.getMaxWindKph());
            dayData.setTotalPrecipMm(details.getTotalPrecipMm());
            dayData.setAvgHumidity(details.getAvgHumidity());
            dayData.setUvIndex(details.getUv());

            if (details.getCondition() != null) {
                dayData.setConditionText(details.getCondition().getText());
                dayData.setConditionIcon(details.getCondition().getIcon());
                dayData.setConditionCode(details.getCondition().getCode());
            }
        }

        if (fd.getAstro() != null) {
            dayData.setSunrise(fd.getAstro().getSunrise());
            dayData.setSunset(fd.getAstro().getSunset());
        }

        return dayData;
    }

    private void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    private void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
