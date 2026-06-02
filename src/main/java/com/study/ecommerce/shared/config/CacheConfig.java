package com.study.ecommerce.shared.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache("catalog",
            Caffeine.newBuilder().expireAfterWrite(60, TimeUnit.SECONDS).maximumSize(200).build());
        manager.registerCustomCache("product",
            Caffeine.newBuilder().expireAfterWrite(120, TimeUnit.SECONDS).maximumSize(500).build());
        manager.registerCustomCache("categories",
            Caffeine.newBuilder().expireAfterWrite(300, TimeUnit.SECONDS).maximumSize(50).build());
        // Cache de variantes — para checkout sin queries a BD en cada item
        // TTL 5 min: precios/nombres casi nunca cambian
        manager.registerCustomCache("variants",
            Caffeine.newBuilder().expireAfterWrite(300, TimeUnit.SECONDS).maximumSize(1000).build());
        return manager;
    }
}
