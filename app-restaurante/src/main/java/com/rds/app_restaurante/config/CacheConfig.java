package com.rds.app_restaurante.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "categories", 
                "subCategories",
                "products",
                "statistics"
        );
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES) // Cache expira después de 30 minutos
                .maximumSize(500) // Máximo 500 entradas en caché
                .recordStats() // Habilitar estadísticas de caché
        );
        
        return cacheManager;
    }
}

