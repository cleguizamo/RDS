package com.rds.app_restaurante;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AppRestauranteApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppRestauranteApplication.class, args);
	}

}