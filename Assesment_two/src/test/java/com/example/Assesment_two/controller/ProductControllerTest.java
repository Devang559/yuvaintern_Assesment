package com.example.Assesment_two.controller;

import com.example.Assesment_two.model.Product;
import com.example.Assesment_two.repository.ProductRepository;
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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductRepository productRepository;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/products should return all products")
    void shouldGetAllProducts() throws Exception {
        List<Product> products = Arrays.asList(
                new Product(1L, "Laptop", new BigDecimal("999.99")),
                new Product(2L, "Mouse", new BigDecimal("19.99"))
        );
        given(productRepository.findAll()).willReturn(products);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[1].name").value("Mouse"));
    }

    @Test
    @DisplayName("GET /api/products should return empty list when no products exist")
    void shouldReturnEmptyListWhenNoProducts() throws Exception {
        given(productRepository.findAll()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("GET /api/products/{id} should return product by ID")
    void shouldGetProductById() throws Exception {
        Product product = new Product(1L, "Laptop", new BigDecimal("999.99"));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(999.99));
    }

    @Test
    @DisplayName("GET /api/products/{id} should return 404 when product not found")
    void shouldReturn404WhenProductNotFound() throws Exception {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("POST /api/products should create a new product")
    void shouldCreateProduct() throws Exception {
        Product product = new Product(null, "Laptop", new BigDecimal("999.99"));
        Product saved = new Product(1L, "Laptop", new BigDecimal("999.99"));
        given(productRepository.save(any(Product.class))).willReturn(saved);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    @DisplayName("POST /api/products should return 400 when name is blank")
    void shouldReturn400WhenProductNameIsBlank() throws Exception {
        Product product = new Product(null, "", new BigDecimal("999.99"));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(product)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/products should return 400 when name is null")
    void shouldReturn400WhenProductNameIsNull() throws Exception {
        String jsonPayload = "{\"id\": null, \"name\": null, \"price\": 999.99}";

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/products should return 400 when price is null")
    void shouldReturn400WhenProductPriceIsNull() throws Exception {
        String jsonPayload = "{\"id\": null, \"name\": \"Laptop\", \"price\": null}";

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/products should return 400 when price is zero")
    void shouldReturn400WhenProductPriceIsZero() throws Exception {
        String jsonPayload = "{\"id\": null, \"name\": \"Laptop\", \"price\": 0}";

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/products/{id} should update existing product")
    void shouldUpdateProduct() throws Exception {
        Product existing = new Product(1L, "Laptop", new BigDecimal("999.99"));
        Product updateRequest = new Product(1L, "Updated Laptop", new BigDecimal("1299.99"));
        given(productRepository.findById(1L)).willReturn(Optional.of(existing));
        given(productRepository.save(existing)).willReturn(updateRequest);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Laptop"))
                .andExpect(jsonPath("$.price").value(1299.99));
    }

    @Test
    @DisplayName("PUT /api/products/{id} should return 404 when product not found")
    void shouldReturn404WhenUpdatingNonExistentProduct() throws Exception {
        Product updateRequest = new Product(99L, "Updated Laptop", new BigDecimal("1299.99"));
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(put("/api/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT /api/products/{id} should return 400 when name is blank")
    void shouldReturn400WhenUpdatingWithBlankName() throws Exception {
        Product updateRequest = new Product(1L, "", new BigDecimal("1299.99"));

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /api/products/{id} should delete product and return 204")
    void shouldDeleteProduct() throws Exception {
        Product existing = new Product(1L, "Laptop", new BigDecimal("999.99"));
        given(productRepository.findById(1L)).willReturn(Optional.of(existing));
        doNothing().when(productRepository).delete(existing);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/products/{id} should return 404 when product not found")
    void shouldReturn404WhenDeletingNonExistentProduct() throws Exception {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(delete("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        verify(productRepository, never()).delete(any());
    }
}
