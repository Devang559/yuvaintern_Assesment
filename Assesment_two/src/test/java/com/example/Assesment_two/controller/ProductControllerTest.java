package com.example.Assesment_two.controller;

import com.example.Assesment_two.model.Product;
import com.example.Assesment_two.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
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
                .build();
    }

    @Test
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
    void shouldGetProductById() throws Exception {
        Product product = new Product(1L, "Laptop", new BigDecimal("999.99"));
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.price").value(999.99));
    }

    @Test
    void shouldReturn404WhenProductNotFound() throws Exception {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
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
    void shouldDeleteProduct() throws Exception {
        Product existing = new Product(1L, "Laptop", new BigDecimal("999.99"));
        given(productRepository.findById(1L)).willReturn(Optional.of(existing));
        doNothing().when(productRepository).delete(existing);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
    }
}
