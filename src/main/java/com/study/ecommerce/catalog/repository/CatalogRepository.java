package com.study.ecommerce.catalog.repository;

import com.study.ecommerce.catalog.domain.ProductDetail;
import com.study.ecommerce.catalog.domain.ProductSummary;
import com.study.ecommerce.catalog.domain.VariantWithStock;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

/**
 * jOOQ — catálogo de solo lectura con queries complejas.
 * Filtros dinámicos, JOINs con inventario, búsqueda full-text.
 * Type-safe: renombrar columna → error en compile time.
 */
@Repository
public class CatalogRepository {

    private final DSLContext dsl;

    public CatalogRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<ProductSummary> findAll(String categorySlug, String search,
                                        BigDecimal maxPrice, int limit, int offset) {
        var q = dsl.select(
                    field("p.id"),
                    field("p.name", String.class).as("name"),
                    field("p.slug", String.class).as("slug"),
                    field("p.base_price", BigDecimal.class).as("basePrice"),
                    field("c.name", String.class).as("categoryName"),
                    min(field("i.stock", Integer.class)).as("minStock")
                )
                .from(table("products").as("p"))
                .join(table("categories").as("c")).on(field("c.id").eq(field("p.category_id")))
                .leftJoin(table("product_variants").as("v")).on(field("v.product_id").eq(field("p.id")))
                .leftJoin(table("inventory").as("i")).on(field("i.variant_id").eq(field("v.id")))
                .where(field("p.active").eq(true));

        if (categorySlug != null && !categorySlug.isBlank())
            q = q.and(field("c.slug").eq(categorySlug));

        if (search != null && !search.isBlank()) {
            String like = "%" + search.toLowerCase() + "%";
            q = q.and(field("lower(p.name)").like(like)
                 .or(field("lower(p.description)").like(like)));
        }

        if (maxPrice != null)
            q = q.and(field("p.base_price").lessOrEqual(maxPrice));

        return q.groupBy(field("p.id"), field("p.name"), field("p.slug"),
                         field("p.base_price"), field("p.created_at"), field("c.name"))
                .orderBy(field("p.created_at").desc())
                .limit(limit).offset(offset)
                .fetchInto(ProductSummary.class);
    }

    public Optional<ProductDetail> findBySlug(String slug) {
        var product = dsl.select(
                    field("p.id").as("id"),
                    field("p.name", String.class).as("name"),
                    field("p.slug", String.class).as("slug"),
                    field("p.description", String.class).as("description"),
                    field("p.base_price", BigDecimal.class).as("basePrice"),
                    field("c.name", String.class).as("categoryName"),
                    field("c.slug", String.class).as("categorySlug")
                )
                .from(table("products").as("p"))
                .join(table("categories").as("c")).on(field("c.id").eq(field("p.category_id")))
                .where(field("p.slug").eq(slug).and(field("p.active").eq(true)))
                .fetchOneInto(ProductDetail.class);

        if (product == null) return Optional.empty();

        List<VariantWithStock> variants = dsl.select(
                    field("v.id").as("id"),
                    field("v.sku").as("sku"),
                    field("v.name", String.class).as("name"),
                    field("v.price", BigDecimal.class).as("price"),
                    coalesce(field("i.stock", Integer.class), 0).as("stock")
                )
                .from(table("product_variants").as("v"))
                .leftJoin(table("inventory").as("i")).on(field("i.variant_id").eq(field("v.id")))
                .where(field("v.product_id").eq(product.id).and(field("v.active").eq(true)))
                .orderBy(field("v.price"))
                .fetchInto(VariantWithStock.class);

        product.setVariants(variants);
        return Optional.of(product);
    }

    public int countActive(String categorySlug, String search, BigDecimal maxPrice) {
        var q = dsl.selectCount()
                .from(table("products").as("p"))
                .join(table("categories").as("c")).on(field("c.id").eq(field("p.category_id")))
                .where(field("p.active").eq(true));

        if (categorySlug != null && !categorySlug.isBlank())
            q = q.and(field("c.slug").eq(categorySlug));

        if (search != null && !search.isBlank()) {
            String like = "%" + search.toLowerCase() + "%";
            q = q.and(field("lower(p.name)").like(like)
                 .or(field("lower(p.description)").like(like)));
        }
        if (maxPrice != null)
            q = q.and(field("p.base_price").lessOrEqual(maxPrice));

        return q.fetchOne(0, int.class);
    }

    public List<Map<String, Object>> findCategories() {
        return dsl.select(field("id"), field("name"), field("slug"))
                  .from(table("categories"))
                  .orderBy(field("name"))
                  .fetchMaps();
    }
}
