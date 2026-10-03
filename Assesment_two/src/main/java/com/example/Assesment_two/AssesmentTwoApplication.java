package com.example.Assesment_two;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AssesmentTwoApplication {

	public static void main(String[] args) {
		SpringApplication.run(AssesmentTwoApplication.class, args);
	}

}
