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
        // Cerrar todas las conexiones antes del checkpoint
        // Evita que el checkpoint guarde sockets inválidos
        dataSource.getHikariPoolMXBean().softEvictConnections();
        Thread.sleep(500); // Dar tiempo a que se cierren
        System.out.println("[CRaC] Conexiones BD cerradas antes del checkpoint");
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) throws Exception {
        // HikariCP crea nuevas conexiones automáticamente al primer request
        // Solo log para confirmar que el restore fue exitoso
        System.out.println("[CRaC] Restore completado — conexiones BD se crearán al primer request");
    }
}
