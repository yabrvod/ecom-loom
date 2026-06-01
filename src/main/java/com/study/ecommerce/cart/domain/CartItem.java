package com.study.ecommerce.cart.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "variant_id", nullable = false)
    private Long variantId;

    @Column(nullable = false)
    private int quantity;

    protected CartItem() {}

    public CartItem(Cart cart, Long variantId, int quantity) {
        this.cart = cart;
        this.variantId = variantId;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public Cart getCart() { return cart; }
    public Long getVariantId() { return variantId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
