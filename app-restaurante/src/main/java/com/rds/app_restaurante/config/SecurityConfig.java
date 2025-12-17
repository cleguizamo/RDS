package com.rds.app_restaurante.config;

import com.rds.app_restaurante.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

//Configuracion para la seguridad de la API
//Configuracion para el encriptador de contraseñas
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final Environment environment;
    
    @Value("${app.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}")
    private String allowedOrigins;
    
    //Bean para configurar el encriptador de contraseñas
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); //Devuelve el encriptador de contraseñas
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Determinar si estamos en producción
        boolean isProduction = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        
        if (isProduction && allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            // En producción: usar solo los orígenes específicos configurados
            List<String> origins = Arrays.asList(allowedOrigins.split(","));
            origins.forEach(origin -> {
                origin = origin.trim();
                if (origin.startsWith("https://") || origin.startsWith("http://")) {
                    configuration.addAllowedOrigin(origin);
                }
            });
        } else {
            // En desarrollo: permitir localhost y redes locales
            configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.*.*:*",     // Redes privadas clase C
                "http://10.*.*.*:*",        // Redes privadas clase A
                "http://172.16.*.*:*",      // Redes privadas clase B
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
            ));
        }
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        configuration.setMaxAge(3600L); // Cache preflight por 1 hora
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    //Bean para configurar la seguridad HTTP con JWT
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // IMPORTANTE: Agregar el filtro JWT ANTES de UsernamePasswordAuthenticationFilter
            // Esto asegura que el SecurityContext se establezca antes de la evaluación de autorización
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/api", "/error").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // Usar hasAnyRole para permitir que el método del controlador también verifique el rol
                // Esto da doble protección: a nivel de SecurityFilterChain y a nivel de método
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN")
                .requestMatchers("/api/employee/**").hasAnyRole("EMPLOYEE")
                .requestMatchers("/api/client/**").hasAnyRole("CLIENT")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}
