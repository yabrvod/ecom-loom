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
        Long cartId = resolveCartId(user, session);
        var items = cartService.getEnrichedItems(cartId);
        var total = cartService.calcTotal(items);
        model.addAttribute("items", items);
        model.addAttribute("total", total);
        return "cart/view";
    }

    @PostMapping("/agregar")
    public String addItem(@RequestParam Long variantId,
                          @RequestParam(defaultValue = "1") int quantity,
                          @AuthenticationPrincipal UserDetails user,
                          HttpSession session) {
        Long cartId = resolveCartId(user, session);
        cartService.addItem(cartId, variantId, quantity);
        return "redirect:/carrito";
    }

    @PostMapping("/actualizar")
    public String updateQuantity(@RequestParam Long variantId,
                                 @RequestParam int quantity,
                                 @AuthenticationPrincipal UserDetails user,
                                 HttpSession session) {
        Long cartId = resolveCartId(user, session);
        cartService.updateQuantity(cartId, variantId, quantity);
        return "redirect:/carrito";
    }

    @PostMapping("/eliminar/{variantId}")
    public String removeItem(@PathVariable Long variantId,
                             @AuthenticationPrincipal UserDetails user,
                             HttpSession session) {
        Long cartId = resolveCartId(user, session);
        cartService.removeItem(cartId, variantId);
        return "redirect:/carrito";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam String shippingName,
                           @RequestParam String shippingAddress,
                           @AuthenticationPrincipal UserDetails user,
                           HttpSession session,
                           RedirectAttributes redirect) {
        Long cartId = resolveCartId(user, session);
        Long userId = user != null
            ? userRepository.findByUsername(user.getUsername()).map(u -> u.getId()).orElse(null)
            : null;
        try {
            var order = checkoutService.checkout(cartId, userId, shippingName, shippingAddress);
            return "redirect:/checkout/confirmacion/" + order.getConfirmationToken();
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            return "redirect:/carrito";
        }
    }

    private Long resolveCartId(UserDetails user, HttpSession session) {
        if (user != null) {
            Long userId = userRepository.findByUsername(user.getUsername())
                .map(u -> u.getId()).orElse(null);
            if (userId != null) {
                Long cartId = cartService.getOrCreateForUser(userId);
                cartService.mergeSessionCart(session.getId(), cartId);
                return cartId;
            }
        }
        return cartService.getOrCreateForSession(session.getId());
    }
}
