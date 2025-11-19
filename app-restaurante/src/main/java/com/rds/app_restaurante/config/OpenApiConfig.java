package com.rds.app_restaurante.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//Configuracion para la documentacion de la API
@Configuration
public class OpenApiConfig {

	//Bean para configurar la documentacion de la API
	@Bean
	public OpenAPI restauranteOpenApi() {
		return new OpenAPI()
			.info(new Info()
				.title("Restaurante API")
				.description("API para gestionar el menú y pedidos del restaurante")
				.version("v1.0.0")
				.license(new License().name("MIT")))
			.externalDocs(new ExternalDocumentation()
				.description("Repositorio")
				.url("https://github.com/cleguizamo/RDS"));
	}
}

