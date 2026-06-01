package com.study.ecommerce.cart.service;

import com.study.ecommerce.cart.domain.CartItemView;
import org.jooq.DSLContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;

/**
 * CartService migrado de Hibernate a jOOQ + JDBC.
 * Elimina el overhead de sesión JPA, dirty checking y proxy generation.
 * Cada operación es una query directa — equivalente al cartService.js de Node.
 */
@Service
public class CartService {

    private final DSLContext dsl;
    private final NamedParameterJdbcTemplate jdbc;

    public CartService(DSLContext dsl, NamedParameterJdbcTemplate jdbc) {
        this.dsl = dsl;
        this.jdbc = jdbc;
    }

    @Transactional
    public Long getOrCreateForSession(String sessionId) {
        var existing = dsl.select(field("id"))
            .from(table("carts"))
            .where(field("session_id").eq(sessionId))
            .fetchOne();
        if (existing != null) return existing.get(field("id", Long.class));

        return dsl.insertInto(table("carts"))
            .set(field("session_id"), sessionId)
            .returning(field("id"))
            .fetchOne()
            .get(field("id", Long.class));
    }

    @Transactional
    public Long getOrCreateForUser(Long userId) {
        var existing = dsl.select(field("id"))
            .from(table("carts"))
            .where(field("user_id").eq(userId))
            .fetchOne();
        if (existing != null) return existing.get(field("id", Long.class));

        return dsl.insertInto(table("carts"))
            .set(field("user_id"), userId)
            .returning(field("id"))
            .fetchOne()
            .get(field("id", Long.class));
    }

    @Transactional
    public void addItem(Long cartId, Long variantId, int quantity) {
        jdbc.update("""
            INSERT INTO cart_items (cart_id, variant_id, quantity)
            VALUES (:cartId, :variantId, :qty)
            ON CONFLICT (cart_id, variant_id)
            DO UPDATE SET quantity = cart_items.quantity + :qty
            """, new MapSqlParameterSource()
            .addValue("cartId", cartId)
            .addValue("variantId", variantId)
            .addValue("qty", quantity));

        jdbc.update("UPDATE carts SET updated_at = NOW() WHERE id = :id",
            new MapSqlParameterSource("id", cartId));
    }

    @Transactional
    public void removeItem(Long cartId, Long variantId) {
        jdbc.update("DELETE FROM cart_items WHERE cart_id = :cartId AND variant_id = :variantId",
            new MapSqlParameterSource()
                .addValue("cartId", cartId)
                .addValue("variantId", variantId));
    }

    @Transactional
    public void updateQuantity(Long cartId, Long variantId, int quantity) {
        if (quantity <= 0) {
            removeItem(cartId, variantId);
            return;
        }
        jdbc.update("UPDATE cart_items SET quantity = :qty WHERE cart_id = :cartId AND variant_id = :variantId",
            new MapSqlParameterSource()
                .addValue("qty", quantity)
                .addValue("cartId", cartId)
                .addValue("variantId", variantId));
    }

    // Enriquecer items con nombre y precio via jOOQ (una sola query con JOIN)
    @Transactional(readOnly = true)
    public List<CartItemView> getEnrichedItems(Long cartId) {
        return dsl.select(
                    field("ci.variant_id"),
                    field("ci.quantity"),
                    field("p.name", String.class),
                    field("v.name", String.class),
                    field("v.price", BigDecimal.class)
                )
                .from(table("cart_items").as("ci"))
                .join(table("product_variants").as("v")).on(field("v.id").eq(field("ci.variant_id")))
                .join(table("products").as("p")).on(field("p.id").eq(field("v.product_id")))
                .where(field("ci.cart_id").eq(cartId))
                .fetch(r -> {
                    CartItemView v = new CartItemView();
                    v.variantId    = r.get(0, Long.class);
                    v.quantity     = r.get(1, Integer.class);
                    v.productName  = r.get(2, String.class);
                    v.variantName  = r.get(3, String.class);
                    v.price        = r.get(4, BigDecimal.class);
                    return v;
                });
    }

    @Transactional(readOnly = true)
    public boolean isEmpty(Long cartId) {
        return dsl.selectCount()
            .from(table("cart_items"))
            .where(field("cart_id").eq(cartId))
            .fetchOne(0, int.class) == 0;
    }

    @Transactional(readOnly = true)
    public int totalItems(Long cartId) {
        Integer total = jdbc.queryForObject(
            "SELECT COALESCE(SUM(quantity),0) FROM cart_items WHERE cart_id = :id",
            new MapSqlParameterSource("id", cartId), Integer.class);
        return total != null ? total : 0;
    }

    @Transactional
    public void clearCart(Long cartId) {
        jdbc.update("DELETE FROM carts WHERE id = :id",
            new MapSqlParameterSource("id", cartId));
    }

    @Transactional
    public void mergeSessionCart(String sessionId, Long userCartId) {
        Long sessionCartId = dsl.select(field("id"))
            .from(table("carts"))
            .where(field("session_id").eq(sessionId))
            .fetchOneInto(Long.class);

        if (sessionCartId == null || sessionCartId.equals(userCartId)) return;

        // Fusionar items del carrito anónimo al carrito del usuario
        jdbc.update("""
            INSERT INTO cart_items (cart_id, variant_id, quantity)
            SELECT :userCartId, variant_id, quantity FROM cart_items WHERE cart_id = :sessionCartId
            ON CONFLICT (cart_id, variant_id)
            DO UPDATE SET quantity = cart_items.quantity + EXCLUDED.quantity
            """, new MapSqlParameterSource()
            .addValue("userCartId", userCartId)
            .addValue("sessionCartId", sessionCartId));

        jdbc.update("DELETE FROM carts WHERE id = :id",
            new MapSqlParameterSource("id", sessionCartId));
    }

    public BigDecimal calcTotal(List<CartItemView> items) {
        return items.stream()
            .map(CartItemView::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
