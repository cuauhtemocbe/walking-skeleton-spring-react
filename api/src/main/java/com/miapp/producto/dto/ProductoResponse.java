package com.miapp.producto.dto;

import java.math.BigDecimal;

public record ProductoResponse(
    Long id,
    String codigo,
    String nombre,
    BigDecimal precioUnitario
) {
}
