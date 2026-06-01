package com.study.ecommerce.checkout.service;

import com.study.ecommerce.cart.service.CartService;
import com.study.ecommerce.checkout.domain.Order;
import com.study.ecommerce.shared.exception.BusinessException;
import com.study.ecommerce.shared.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CheckoutService {

    private final CartService cartService;
    private final CheckoutTransactionService txService;
    private final NamedParameterJdbcTemplate jdbc;

    public CheckoutService(CartService cartService,
                           CheckoutTransactionService txService,
                           NamedParameterJdbcTemplate jdbc) {
        this.cartService = cartService;
        this.txService = txService;
        this.jdbc = jdbc;
    }

    public Order checkout(Long cartId, Long userId, String shippingName, String shippingAddress) {
        if (cartService.isEmpty(cartId))
            throw new BusinessException("El carrito está vacío");

        var items = cartService.getEnrichedItems(cartId);
        var variantIds = items.stream().map(i -> i.getVariantId()).toList();

        // FASE 1 — leer catálogo + validar stock (sin transacción)
        List<Map<String, Object>> variantData = jdbc.queryForList("""
            SELECT v.id, v.name AS variant_name, v.price,
                   p.name AS product_name,
                   COALESCE(i.stock, 0) AS stock
            FROM product_variants v
            JOIN products p ON p.id = v.product_id
            LEFT JOIN inventory i ON i.variant_id = v.id
            WHERE v.id IN (:ids)
            """, new MapSqlParameterSource("ids", variantIds));

        var variantMap = variantData.stream()
            .collect(java.util.stream.Collectors.toMap(
                r -> ((Number) r.get("id")).longValue(), r -> r));

        for (var item : items) {
            var data = variantMap.get(item.getVariantId());
            if (data == null)
                throw new ResourceNotFoundException("Variante no encontrada: " + item.getVariantId());
            int stock = ((Number) data.get("stock")).intValue();
            if (stock < item.getQuantity())
                throw new BusinessException(
                    "Stock insuficiente para " + data.get("product_name") +
                    " - " + data.get("variant_name") +
                    " (disponible: " + stock + ")");
        }

        // FASE 2 — transacción JDBC pura (sin Hibernate)
        return txService.execute(cartId, userId, shippingName, shippingAddress, variantMap);
    }
}
