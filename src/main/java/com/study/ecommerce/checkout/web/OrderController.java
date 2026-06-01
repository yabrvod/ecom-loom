package com.study.ecommerce.checkout.web;

import com.study.ecommerce.checkout.repository.OrderRepository;
import com.study.ecommerce.shared.exception.ResourceNotFoundException;
import com.study.ecommerce.shared.security.UserRepository;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class OrderController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public OrderController(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    // Confirmación pública con UUID — no expone ID secuencial
    @GetMapping("/checkout/confirmacion/{token}")
    public String confirmation(@PathVariable UUID token, Model model) {
        var order = orderRepository.findWithItemsByConfirmationToken(token)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada"));
        model.addAttribute("order", order);
        return "checkout/confirmation";
    }

    // Historial de órdenes — solo usuarios autenticados (ver SecurityConfig)
    @GetMapping("/ordenes/{id}")
    public String detail(@PathVariable Long id, Model model) {
        var order = orderRepository.findWithItemsById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada: " + id));
        model.addAttribute("order", order);
        return "checkout/confirmation";
    }
}
