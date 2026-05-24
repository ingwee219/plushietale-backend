package com.plushietale.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing  // createdAt, updatedAt 자동 관리 활성화
public class PlushieTaleApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlushieTaleApplication.class, args);
	}
}
