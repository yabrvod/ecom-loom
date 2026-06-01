package com.study.ecommerce.catalog.domain;

import java.math.BigDecimal;

public class VariantWithStock {
    public Long id;
    public String sku;
    public String name;
    public BigDecimal price;
    public int stock;

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }
    public boolean isInStock() { return stock > 0; }
}
