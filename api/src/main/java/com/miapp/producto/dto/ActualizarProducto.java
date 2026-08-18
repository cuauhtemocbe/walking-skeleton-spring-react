package com.miapp.producto.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ActualizarProducto(
    @NotBlank @Size(max = 50) String codigo,
    @NotBlank @Size(max = 200) String nombre,
    @Positive BigDecimal precioUnitario
) {
}
