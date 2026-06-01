package com.study.ecommerce.checkout.service;

import com.study.ecommerce.cart.service.CartService;
import com.study.ecommerce.checkout.domain.Order;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.study.ecommerce.shared.exception.BusinessException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Checkout migrado de Hibernate a JDBC puro.
 * Elimina el overhead de ORM en el path transaccional más crítico.
 * Una sola @Transactional con JDBC — sin sesión JPA abierta.
 */
@Service
public class CheckoutTransactionService {

    private final NamedParameterJdbcTemplate jdbc;
    private final CartService cartService;

    public CheckoutTransactionService(NamedParameterJdbcTemplate jdbc, CartService cartService) {
        this.jdbc = jdbc;
        this.cartService = cartService;
    }

    @Transactional
    public Order execute(Long cartId, Long userId, String shippingName,
                         String shippingAddress, Map<Long, Map<String, Object>> variantMap) {

        // Calcular total
        BigDecimal total = variantMap.values().stream()
            .map(v -> (BigDecimal) v.get("price"))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calcular total real con cantidades
        var items = cartService.getEnrichedItems(cartId);
        total = items.stream()
            .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        UUID token = UUID.randomUUID();

        // INSERT order
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update("""
            INSERT INTO orders (user_id, status, total, shipping_name, shipping_address, confirmation_token)
            VALUES (:userId, 'CONFIRMED', :total, :shippingName, :shippingAddress, :token)
            """, new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("total", total)
            .addValue("shippingName", shippingName)
            .addValue("shippingAddress", shippingAddress)
            .addValue("token", token),
            keyHolder, new String[]{"id"});

        Long orderId = keyHolder.getKey().longValue();

        // INSERT order_items + descontar inventario
        for (var item : items) {
            var data = variantMap.get(item.getVariantId());

            jdbc.update("""
                INSERT INTO order_items (order_id, variant_id, product_name, variant_name, unit_price, quantity)
                VALUES (:orderId, :variantId, :productName, :variantName, :unitPrice, :quantity)
                """, new MapSqlParameterSource()
                .addValue("orderId", orderId)
                .addValue("variantId", item.getVariantId())
                .addValue("productName", item.getProductName())
                .addValue("variantName", item.getVariantName())
                .addValue("unitPrice", item.getPrice())
                .addValue("quantity", item.getQuantity()));

            int updated = jdbc.update("""
                UPDATE inventory SET stock = stock - :qty, updated_at = NOW()
                WHERE variant_id = :vid AND stock >= :qty
                """, new MapSqlParameterSource()
                .addValue("qty", item.getQuantity())
                .addValue("vid", item.getVariantId()));

            if (updated == 0)
                throw new BusinessException("Stock agotado para: " + item.getProductName());
        }

        // INSERT payment simulado
        jdbc.update("""
            INSERT INTO payments (order_id, status, method, amount, processed_at)
            VALUES (:orderId, 'APPROVED', 'SIMULATED', :amount, NOW())
            """, new MapSqlParameterSource()
            .addValue("orderId", orderId)
            .addValue("amount", total));

        // Vaciar carrito
        cartService.clearCart(cartId);

        // Retornar Order con los datos necesarios para el redirect
        return buildOrderResult(orderId, token, total, shippingName, shippingAddress);
    }

    private Order buildOrderResult(Long id, UUID token, BigDecimal total,
                                   String shippingName, String shippingAddress) {
        Order order = new Order(null, shippingName, shippingAddress);
        order.setId(id);
        order.setConfirmationToken(token);
        order.setTotal(total);
        return order;
    }
}
