package com.study.ecommerce.cart.domain;

import java.math.BigDecimal;

public class CartItemView {
    public Long variantId;
    public String productName;
    public String variantName;
    public BigDecimal price;
    public int quantity;

    public BigDecimal getSubtotal() {
        return price != null ? price.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO;
    }

    public Long getVariantId() { return variantId; }
    public String getProductName() { return productName; }
    public String getVariantName() { return variantName; }
    public BigDecimal getPrice() { return price; }
    public int getQuantity() { return quantity; }
}
