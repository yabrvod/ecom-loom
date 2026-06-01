package com.study.ecommerce.cart.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Hibernate — carrito es estado mutable con relaciones en cascada.
 * Dirty checking maneja automáticamente los cambios en items.
 */
@Entity
@Table(name = "carts")
public class Cart {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id")
    private String sessionId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CartItem> items = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public void addItem(Long variantId, int quantity) {
        items.stream()
             .filter(i -> i.getVariantId().equals(variantId))
             .findFirst()
             .ifPresentOrElse(
                 i -> i.setQuantity(i.getQuantity() + quantity),
                 () -> items.add(new CartItem(this, variantId, quantity))
             );
        updatedAt = LocalDateTime.now();
    }

    public void removeItem(Long variantId) {
        items.removeIf(i -> i.getVariantId().equals(variantId));
        updatedAt = LocalDateTime.now();
    }

    public void updateQuantity(Long variantId, int quantity) {
        if (quantity <= 0) { removeItem(variantId); return; }
        items.stream()
             .filter(i -> i.getVariantId().equals(variantId))
             .findFirst()
             .ifPresent(i -> i.setQuantity(quantity));
        updatedAt = LocalDateTime.now();
    }

    public boolean isEmpty() { return items.isEmpty(); }
    public int totalItems() { return items.stream().mapToInt(CartItem::getQuantity).sum(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public List<CartItem> getItems() { return items; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
