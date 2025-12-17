package com.rds.app_restaurante.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

//Configuracion para el filtro de CORS
@Configuration
public class CorsConfig {

	@Value("${app.cors.allowed-origins:http://localhost:4200}")
	private String allowedOrigins;

	@Value("${app.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
	private String allowedMethods;

	//Bean para configurar el filtro de CORS
	@Bean
	public CorsFilter corsFilter() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowCredentials(true);
		
		// Crear lista de patrones permitidos
		List<String> patterns = new ArrayList<>();
		
		// Agregar orígenes desde application.yml
		if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
			patterns.addAll(Arrays.asList(allowedOrigins.split(",")));
		}
		
		// Agregar patrones para redes locales (si no están ya incluidos)
		// Esto permite acceso desde cualquier máquina en la misma red
		String[] localNetworkPatterns = {
			"http://localhost:*",
			"http://127.0.0.1:*",
			"http://192.168.*.*:*",    // Redes privadas clase C (192.168.0.0/16)
			"http://10.*.*.*:*",       // Redes privadas clase A (10.0.0.0/8)
			"http://172.16.*.*:*",     // Redes privadas clase B (172.16.0.0/12)
			"http://172.17.*.*:*",
			"http://172.18.*.*:*",
			"http://172.19.*.*:*",
			"http://172.20.*.*:*",
			"http://172.21.*.*:*",
			"http://172.22.*.*:*",
			"http://172.23.*.*:*",
			"http://172.24.*.*:*",
			"http://172.25.*.*:*",
			"http://172.26.*.*:*",
			"http://172.27.*.*:*",
			"http://172.28.*.*:*",
			"http://172.29.*.*:*",
			"http://172.30.*.*:*",
			"http://172.31.*.*:*"
		};
		
		// Agregar solo si no están ya en la lista
		for (String pattern : localNetworkPatterns) {
			if (!patterns.contains(pattern)) {
				patterns.add(pattern);
			}
		}
		
		config.setAllowedOriginPatterns(patterns);
		config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
		config.addAllowedHeader("*");
		config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
		config.setMaxAge(3600L); // Cache preflight por 1 hora

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return new CorsFilter(source);
	}
}

