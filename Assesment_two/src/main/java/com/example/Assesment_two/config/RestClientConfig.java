package com.example.Assesment_two.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class RestClientConfig {

    @Bean
    public WebClient weatherWebClient(
            @Value("${weather.api.base-url:https://api.weatherapi.com/v1}") String baseUrl) {
        log.info("Configuring Weather API WebClient with base URL: {}", baseUrl);
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
