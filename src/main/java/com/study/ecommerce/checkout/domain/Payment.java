package com.study.ecommerce.checkout.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.PENDING;

    @Column(nullable = false, length = 30)
    private String method = "SIMULATED";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    protected Payment() {}

    public Payment(Order order, BigDecimal amount) {
        this.order = order;
        this.amount = amount;
    }

    public void approve() {
        this.status = Status.APPROVED;
        this.processedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public Status getStatus() { return status; }
    public String getMethod() { return method; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
