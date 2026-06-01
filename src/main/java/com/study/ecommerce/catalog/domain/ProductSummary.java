package com.study.ecommerce.catalog.domain;

import java.math.BigDecimal;

public class ProductSummary {
    public Long id;
    public String name;
    public String slug;
    public BigDecimal basePrice;
    public String categoryName;
    public Integer minStock;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public BigDecimal getBasePrice() { return basePrice; }
    public String getCategoryName() { return categoryName; }
    public Integer getMinStock() { return minStock; }
    public boolean isInStock() { return minStock != null && minStock > 0; }
}
