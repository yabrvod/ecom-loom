package com.study.ecommerce.admin.web;

import org.jooq.DSLContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jooq.impl.DSL.*;

/**
 * Admin — todo con jOOQ: KPIs, reportes, stock bajo.
 * Queries de solo lectura con agregaciones complejas.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DSLContext dsl;

    public AdminController(DSLContext dsl) {
        this.dsl = dsl;
    }

    @GetMapping
    public String dashboard(Model model) {
        // KPIs con jOOQ
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalProducts", dsl.selectCount()
            .from(table("products"))
            .where(field("active").eq(true))
            .fetchOne(0, int.class));

        stats.put("ordersToday", dsl.selectCount()
            .from(table("orders"))
            .where(field("created_at").greaterOrEqual(
                currentLocalDateTime().minus(field("interval '1 day'"))
            ))
            .fetchOne(0, int.class));

        stats.put("totalRevenue", dsl.select(sum(field("total", BigDecimal.class)))
            .from(table("orders"))
            .where(field("status").eq("CONFIRMED"))
            .fetchOne(0, BigDecimal.class));

        stats.put("totalUsers", dsl.selectCount()
            .from(table("users"))
            .fetchOne(0, int.class));

        // Órdenes recientes con JOIN a users
        List<Map<String, Object>> recentOrders = dsl.select(
                    field("o.id"),
                    field("u.username"),
                    field("o.total"),
                    field("o.status"),
                    field("o.created_at")
                )
                .from(table("orders").as("o"))
                .leftJoin(table("users").as("u")).on(field("u.id").eq(field("o.user_id")))
                .orderBy(field("o.created_at").desc())
                .limit(10)
                .fetchMaps();

        // Stock bajo
        List<Map<String, Object>> lowStock = dsl.select(
                    field("p.name", String.class).as("productName"),
                    field("v.name", String.class).as("variantName"),
                    field("v.sku"),
                    field("i.stock")
                )
                .from(table("inventory").as("i"))
                .join(table("product_variants").as("v")).on(field("v.id").eq(field("i.variant_id")))
                .join(table("products").as("p")).on(field("p.id").eq(field("v.product_id")))
                .where(field("i.stock").lessOrEqual(5))
                .orderBy(field("i.stock"))
                .fetchMaps();

        model.addAttribute("stats", stats);
        model.addAttribute("recentOrders", recentOrders);
        model.addAttribute("lowStock", lowStock);
        return "admin/index";
    }
}
