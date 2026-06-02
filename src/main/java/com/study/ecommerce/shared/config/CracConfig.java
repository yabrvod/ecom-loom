package com.study.ecommerce.shared.config;

import org.springframework.context.annotation.Configuration;

/**
 * CRaC configuration.
 *
 * Con spring-boot-starter-crac, Spring Boot 3.2+ registra automáticamente:
 *   - HikariCheckpointRestoreLifecycle: cierra/reabre conexiones JDBC
 *   - RedisConnectionFactoryBeanPostProcessor: maneja conexiones Redis
 *   - TomcatCRaCLifecycle: pausa/reanuda el servidor HTTP
 *
 * No se necesita implementación manual — el starter lo hace todo.
 * Esta clase queda como documentación de la integración.
 */
@Configuration
public class CracConfig {
    // Spring Boot starter-crac registra todos los lifecycles necesarios
    // automáticamente via auto-configuration
}
