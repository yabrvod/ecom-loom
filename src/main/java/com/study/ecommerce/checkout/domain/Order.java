package com.study.ecommerce.checkout.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Hibernate — Order es el agregado raíz del checkout.
 * Una transacción toca: order + order_items + inventory + payment.
 * Hibernate maneja cascades, dirty checking y rollback automático.
 */
@Entity
@Table(name = "orders")
public class Order {

    public enum Status { PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "confirmation_token", nullable = false, unique = true)
    private UUID confirmationToken = UUID.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "shipping_name", length = 150)
    private String shippingName;

    @Column(name = "shipping_address")
    private String shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, optional = true)
    private Payment payment;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    protected Order() {}

    public Order(Long userId, String shippingName, String shippingAddress) {
        this.userId = userId;
        this.shippingName = shippingName;
        this.shippingAddress = shippingAddress;
    }

    public void addItem(Long variantId, String productName, String variantName,
                        BigDecimal unitPrice, int quantity) {
        items.add(new OrderItem(this, variantId, productName, variantName, unitPrice, quantity));
        recalculateTotal();
    }

    private void recalculateTotal() {
        this.total = items.stream()
            .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void confirm() {
        this.status = Status.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public UUID getConfirmationToken() { return confirmationToken; }
    public Status getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public String getShippingName() { return shippingName; }
    public String getShippingAddress() { return shippingAddress; }
    public List<OrderItem> getItems() { return items; }
    public Payment getPayment() { return payment; }
    public void setPayment(Payment payment) { this.payment = payment; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    // Setters para construcción desde JDBC (sin Hibernate)
    public void setId(Long id) { this.id = id; }
    public void setConfirmationToken(UUID confirmationToken) { this.confirmationToken = confirmationToken; }
    public void setTotal(java.math.BigDecimal total) { this.total = total; }
}
