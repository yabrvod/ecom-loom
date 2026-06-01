package com.study.ecommerce.catalog.domain;

import java.math.BigDecimal;
import java.util.List;

public class ProductDetail {
    public Long id;
    public String name;
    public String slug;
    public String description;
    public BigDecimal basePrice;
    public String categoryName;
    public String categorySlug;
    private List<VariantWithStock> variants;

    public List<VariantWithStock> getVariants() { return variants; }
    public void setVariants(List<VariantWithStock> variants) { this.variants = variants; }
    public boolean hasStock() {
        return variants != null && variants.stream().anyMatch(v -> v.stock > 0);
    }
}
