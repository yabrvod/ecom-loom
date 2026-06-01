package com.study.ecommerce.checkout.service;

import com.study.ecommerce.cart.domain.Cart;
import com.study.ecommerce.cart.repository.CartRepository;
import com.study.ecommerce.checkout.domain.Order;
import com.study.ecommerce.shared.exception.BusinessException;
import com.study.ecommerce.shared.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/**
 * Checkout — dos fases claramente separadas:
 *
 * FASE 1 (fuera de transacción): leer datos del catálogo con JdbcTemplate
 * FASE 2 (dentro de @Transactional): descontar stock + crear orden con Hibernate
 *
 * Esto evita el conflicto de conexiones entre jOOQ y Hibernate
 * dentro de la misma transacción JPA.
 */
@Service
public class CheckoutService {

    private final CartRepository cartRepository;
    private final CheckoutTransactionService txService;
    private final NamedParameterJdbcTemplate jdbc;

    public CheckoutService(CartRepository cartRepository,
                           CheckoutTransactionService txService,
                           NamedParameterJdbcTemplate jdbc) {
        this.cartRepository = cartRepository;
        this.txService = txService;
        this.jdbc = jdbc;
    }

    public Order checkout(Long cartId, Long userId, String shippingName, String shippingAddress) {
        Cart cart = cartRepository.findWithItemsById(cartId)
            .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado"));

        if (cart.isEmpty())
            throw new BusinessException("El carrito está vacío");

        // FASE 1 — leer datos del catálogo sin transacción JPA
        var variantIds = cart.getItems().stream()
            .map(i -> i.getVariantId()).toList();

        List<Map<String, Object>> variantData = jdbc.queryForList("""
            SELECT v.id, v.name AS variant_name, v.price,
                   p.name AS product_name,
                   COALESCE(i.stock, 0) AS stock
            FROM product_variants v
            JOIN products p ON p.id = v.product_id
            LEFT JOIN inventory i ON i.variant_id = v.id
            WHERE v.id IN (:ids)
            """, new MapSqlParameterSource("ids", variantIds));

        // Indexar por variant_id
        var variantMap = variantData.stream()
            .collect(java.util.stream.Collectors.toMap(
                r -> ((Number) r.get("id")).longValue(), r -> r));

        // Validar stock antes de abrir transacción
        for (var item : cart.getItems()) {
            var data = variantMap.get(item.getVariantId());
            if (data == null)
                throw new BusinessException("Variante no encontrada: " + item.getVariantId());
            int stock = ((Number) data.get("stock")).intValue();
            if (stock < item.getQuantity())
                throw new BusinessException(
                    "Stock insuficiente para " + data.get("product_name") +
                    " - " + data.get("variant_name") +
                    " (disponible: " + stock + ")");
        }

        // FASE 2 — transacción JPA en bean separado (Spring AOP funciona correctamente)
        return txService.execute(cart, userId, shippingName, shippingAddress, variantMap);
    }
}
