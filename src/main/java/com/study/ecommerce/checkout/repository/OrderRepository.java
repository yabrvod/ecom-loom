package com.study.ecommerce.checkout.repository;

import com.study.ecommerce.checkout.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"items", "payment"})
    Optional<Order> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"items", "payment"})
    Optional<Order> findWithItemsByConfirmationToken(UUID token);
}
