package com.study.ecommerce.cart.service;

import com.study.ecommerce.cart.domain.Cart;
import com.study.ecommerce.cart.domain.CartItemView;
import com.study.ecommerce.cart.repository.CartRepository;
import com.study.ecommerce.shared.exception.ResourceNotFoundException;
import org.jooq.DSLContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final DSLContext dsl;

    public CartService(CartRepository cartRepository, DSLContext dsl) {
        this.cartRepository = cartRepository;
        this.dsl = dsl;
    }

    @Transactional
    public Cart getOrCreateForUser(Long userId) {
        return cartRepository.findByUserId(userId)
            .orElseGet(() -> {
                Cart c = new Cart();
                c.setUserId(userId);
                return cartRepository.save(c);
            });
    }

    @Transactional
    public Cart getOrCreateForSession(String sessionId) {
        return cartRepository.findBySessionId(sessionId)
            .orElseGet(() -> {
                Cart c = new Cart();
                c.setSessionId(sessionId);
                return cartRepository.save(c);
            });
    }

    @Transactional
    public Cart addItem(Long cartId, Long variantId, int quantity) {
        Cart cart = cartRepository.findWithItemsById(cartId)
            .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado: " + cartId));
        cart.addItem(variantId, quantity);
        return cart;
    }

    @Transactional
    public Cart removeItem(Long cartId, Long variantId) {
        Cart cart = cartRepository.findWithItemsById(cartId)
            .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado: " + cartId));
        cart.removeItem(variantId);
        return cart;
    }

    @Transactional
    public Cart updateQuantity(Long cartId, Long variantId, int quantity) {
        Cart cart = cartRepository.findById(cartId)
            .orElseThrow(() -> new ResourceNotFoundException("Carrito no encontrado: " + cartId));
        cart.updateQuantity(variantId, quantity);
        return cart;
    }

    // Enriquecer items del carrito con nombre y precio via jOOQ
    @Transactional(readOnly = true)
    public List<CartItemView> getEnrichedItems(Cart cart) {
        if (cart.getItems().isEmpty()) return List.of();

        var variantIds = cart.getItems().stream()
            .map(i -> i.getVariantId())
            .collect(Collectors.toList());

        var variantData = dsl.select(
                    field("v.id").as("variantId"),
                    field("p.name", String.class).as("productName"),
                    field("v.name", String.class).as("variantName"),
                    field("v.price", BigDecimal.class).as("price")
                )
                .from(table("product_variants").as("v"))
                .join(table("products").as("p")).on(field("p.id").eq(field("v.product_id")))
                .where(field("v.id").in(variantIds))
                .fetchMap(field("variantId", Long.class));

        return cart.getItems().stream().map(item -> {
            var view = new CartItemView();
            view.variantId = item.getVariantId();
            view.quantity = item.getQuantity();
            var data = variantData.get(item.getVariantId());
            if (data != null) {
                view.productName = data.get("productName", String.class);
                view.variantName = data.get("variantName", String.class);
                view.price = data.get("price", BigDecimal.class);
            }
            return view;
        }).collect(Collectors.toList());
    }

    // Fusiona el carrito anónimo (sessionId) al carrito del usuario al hacer login
    @Transactional
    public void mergeSessionCart(String sessionId, Cart userCart) {
        cartRepository.findBySessionId(sessionId).ifPresent(sessionCart -> {
            if (!sessionCart.getId().equals(userCart.getId())) {
                sessionCart.getItems().forEach(item ->
                    userCart.addItem(item.getVariantId(), item.getQuantity())
                );
                cartRepository.delete(sessionCart);
            }
        });
    }
}
