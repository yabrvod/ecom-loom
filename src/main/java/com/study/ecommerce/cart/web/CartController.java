package com.study.ecommerce.cart.web;

import com.study.ecommerce.cart.service.CartService;
import com.study.ecommerce.checkout.service.CheckoutService;
import com.study.ecommerce.shared.exception.BusinessException;
import com.study.ecommerce.shared.security.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/carrito")
public class CartController {

    private final CartService cartService;
    private final CheckoutService checkoutService;
    private final UserRepository userRepository;

    public CartController(CartService cartService, CheckoutService checkoutService,
                          UserRepository userRepository) {
        this.cartService = cartService;
        this.checkoutService = checkoutService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String view(@AuthenticationPrincipal UserDetails user,
                       HttpSession session, Model model) {
        var cart = resolveCart(user, session);
        var items = cartService.getEnrichedItems(cart);
        model.addAttribute("cart", cart);
        model.addAttribute("items", items);
        model.addAttribute("total", items.stream()
            .map(i -> i.getSubtotal())
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        return "cart/view";
    }

    @PostMapping("/agregar")
    public String addItem(@RequestParam Long variantId,
                          @RequestParam(defaultValue = "1") int quantity,
                          @AuthenticationPrincipal UserDetails user,
                          HttpSession session,
                          RedirectAttributes redirect) {
        var cart = resolveCart(user, session);
        cartService.addItem(cart.getId(), variantId, quantity);
        redirect.addFlashAttribute("mensaje", "Producto agregado al carrito");
        return "redirect:/carrito";
    }

    @PostMapping("/eliminar/{variantId}")
    public String removeItem(@PathVariable Long variantId,
                             @AuthenticationPrincipal UserDetails user,
                             HttpSession session) {
        var cart = resolveCart(user, session);
        cartService.removeItem(cart.getId(), variantId);
        return "redirect:/carrito";
    }

    @PostMapping("/actualizar")
    public String updateQuantity(@RequestParam Long variantId,
                                 @RequestParam int quantity,
                                 @AuthenticationPrincipal UserDetails user,
                                 HttpSession session) {
        var cart = resolveCart(user, session);
        cartService.updateQuantity(cart.getId(), variantId, quantity);
        return "redirect:/carrito";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam String shippingName,
                           @RequestParam String shippingAddress,
                           @AuthenticationPrincipal UserDetails user,
                           HttpSession session,
                           RedirectAttributes redirect) {
        var cart = resolveCart(user, session);
        Long userId = user != null
            ? userRepository.findByUsername(user.getUsername()).map(u -> u.getId()).orElse(null)
            : null;
        try {
            var order = checkoutService.checkout(cart.getId(), userId, shippingName, shippingAddress);
            return "redirect:/checkout/confirmacion/" + order.getConfirmationToken();
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/carrito";
        }
    }

    private com.study.ecommerce.cart.domain.Cart resolveCart(UserDetails user, HttpSession session) {
        if (user != null) {
            // Usuario autenticado: carrito persistido por userId
            // Al hacer login se fusiona el carrito anónimo (sessionId) si existía
            Long userId = userRepository.findByUsername(user.getUsername())
                .map(u -> u.getId())
                .orElse(null);
            if (userId != null) {
                var cart = cartService.getOrCreateForUser(userId);
                // Fusionar carrito anónimo si existe
                cartService.mergeSessionCart(session.getId(), cart);
                return cart;
            }
        }
        return cartService.getOrCreateForSession(session.getId());
    }
}
