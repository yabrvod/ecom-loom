package com.study.ecommerce.shared.config;

import com.zaxxer.hikari.HikariDataSource;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * CRaC Resource handler para HikariCP.
 *
 * Problema sin esto:
 *   El checkpoint guarda conexiones JDBC abiertas.
 *   Al restore, esas conexiones son inválidas → errores de BD.
 *
 * Solución:
 *   beforeCheckpoint → cierra todas las conexiones del pool
 *   afterRestore     → el pool crea conexiones nuevas automáticamente
 */
@Configuration
public class CracConfig implements Resource {

    @Autowired
    private HikariDataSource dataSource;

    @PostConstruct
    public void registerWithCrac() {
        // Registrar este bean como Resource de CRaC
        Core.getGlobalContext().register(this);
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        System.out.println("[CRaC] Cerrando pool HikariCP antes del checkpoint...");
        // Paso 1: soft evict (marca conexiones para cierre)
        dataSource.getHikariPoolMXBean().softEvictConnections();
        Thread.sleep(1000);
        // Paso 2: reducir pool a 0 — fuerza cierre de todas las conexiones
        dataSource.setMaximumPoolSize(0);
        dataSource.setMinimumIdle(0);
        Thread.sleep(2000);
        System.out.println("[CRaC] Pool cerrado. Conexiones activas: " +
            dataSource.getHikariPoolMXBean().getActiveConnections());
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        // Restaurar pool con configuración original
        dataSource.setMaximumPoolSize(30);
        dataSource.setMinimumIdle(5);
        System.out.println("[CRaC] Restore completado — pool restaurado, conexiones BD se crearán al primer request");
    }
}
