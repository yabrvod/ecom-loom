package com.study.ecommerce.catalog.web;

import com.study.ecommerce.catalog.service.CatalogService;
import com.study.ecommerce.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/")
    public String index(@RequestParam(required = false) String q,
                        @RequestParam(required = false) String category,
                        @RequestParam(required = false) BigDecimal maxPrice,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        var products = catalogService.list(category, q, maxPrice, page);
        var total = catalogService.count(category, q, maxPrice);
        var categories = catalogService.listCategories();

        model.addAttribute("products", products);
        model.addAttribute("total", total);
        model.addAttribute("categories", categories);
        model.addAttribute("q", q);
        model.addAttribute("category", category);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("page", page);
        model.addAttribute("totalPages", (int) Math.ceil(total / 12.0));
        return "catalog/index";
    }

    @GetMapping("/productos/{slug}")
    public String detail(@PathVariable String slug, Model model) {
        var product = catalogService.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + slug));
        model.addAttribute("product", product);
        model.addAttribute("categories", catalogService.listCategories());
        return "catalog/detail";
    }
}
