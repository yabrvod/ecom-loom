package com.study.ecommerce.catalog.service;

import com.study.ecommerce.catalog.domain.ProductDetail;
import com.study.ecommerce.catalog.domain.ProductSummary;
import com.study.ecommerce.catalog.repository.CatalogRepository;
import org.jooq.DSLContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.jooq.impl.DSL.*;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final CatalogRepository catalog;
    private final DSLContext dsl;

    public CatalogService(CatalogRepository catalog, DSLContext dsl) {
        this.catalog = catalog;
        this.dsl = dsl;
    }

    @Cacheable(value = "catalog", key = "#categorySlug + '-' + #search + '-' + #maxPrice + '-' + #page")
    public List<ProductSummary> list(String categorySlug, String search, BigDecimal maxPrice, int page) {
        return catalog.findAll(categorySlug, search, maxPrice, 12, page * 12);
    }

    public int count(String categorySlug, String search, BigDecimal maxPrice) {
        return catalog.countActive(categorySlug, search, maxPrice);
    }

    @Cacheable(value = "product", key = "#slug")
    public Optional<ProductDetail> findBySlug(String slug) {
        return catalog.findBySlug(slug);
    }

    @Cacheable("categories")
    public List<Map<String, Object>> listCategories() {
        return dsl.select(field("id"), field("name"), field("slug"))
                  .from(table("categories"))
                  .orderBy(field("name"))
                  .fetchMaps();
    }
}
