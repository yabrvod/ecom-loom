package com.study.ecommerce.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String businessError(BusinessException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "error/400";
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String serverError(RuntimeException ex, Model model,
                              @org.springframework.beans.factory.annotation.Value("${spring.profiles.active:default}") String profile) {
        // En prod nunca exponer detalles de la excepción
        boolean isProd = profile.contains("prod");
        model.addAttribute("error", isProd
            ? "Ha ocurrido un error inesperado."
            : ex.getClass().getSimpleName() + ": " + ex.getMessage());
        return "error/500";
    }
}
