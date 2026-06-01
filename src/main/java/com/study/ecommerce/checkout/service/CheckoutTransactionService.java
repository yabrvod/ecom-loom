package com.study.ecommerce.checkout.service;

import com.study.ecommerce.cart.domain.Cart;
import com.study.ecommerce.cart.repository.CartRepository;
import com.study.ecommerce.checkout.domain.Order;
import com.study.ecommerce.checkout.domain.Payment;
import com.study.ecommerce.checkout.repository.OrderRepository;
import com.study.ecommerce.shared.exception.BusinessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Fase 2 del checkout — en bean separado para que @Transactional
 * funcione correctamente (Spring AOP no intercepta llamadas internas).
 */
@Service
public class CheckoutTransactionService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final NamedParameterJdbcTemplate jdbc;

    public CheckoutTransactionService(CartRepository cartRepository,
                                      OrderRepository orderRepository,
                                      NamedParameterJdbcTemplate jdbc) {
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        this.jdbc = jdbc;
    }

    @Transactional
    public Order execute(Cart cart, Long userId, String shippingName,
                         String shippingAddress, Map<Long, Map<String, Object>> variantMap) {

        Order order = new Order(userId, shippingName, shippingAddress);

        for (var item : cart.getItems()) {
            var data = variantMap.get(item.getVariantId());

            int updated = jdbc.update("""
                UPDATE inventory SET stock = stock - :qty, updated_at = NOW()
                WHERE variant_id = :vid AND stock >= :qty
                """, new MapSqlParameterSource()
                    .addValue("qty", item.getQuantity())
                    .addValue("vid", item.getVariantId()));

            if (updated == 0)
                throw new BusinessException("Stock agotado para: " + data.get("product_name"));

            order.addItem(
                item.getVariantId(),
                (String) data.get("product_name"),
                (String) data.get("variant_name"),
                (BigDecimal) data.get("price"),
                item.getQuantity()
            );
        }

        Order saved = orderRepository.save(order);

        Payment payment = new Payment(saved, saved.getTotal());
        payment.approve();
        saved.setPayment(payment);
        saved.confirm();

        // Vaciar y eliminar el carrito — dirty checking + delete
        cart.getItems().clear();
        cartRepository.delete(cart);

        return saved;
    }
}
