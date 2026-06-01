package com.study.ecommerce.checkout.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "variant_name", nullable = false, length = 100)
    private String variantName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private int quantity;

    protected OrderItem() {}

    public OrderItem(Order order, Long variantId, String productName,
                     String variantName, BigDecimal unitPrice, int quantity) {
        this.order = order;
        this.variantId = variantId;
        this.productName = productName;
        this.variantName = variantName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public Long getVariantId() { return variantId; }
    public String getProductName() { return productName; }
    public String getVariantName() { return variantName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getSubtotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
}
