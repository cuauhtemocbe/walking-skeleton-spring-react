package com.miapp.pedido.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PedidoResponse(
    Long id,
    Long clienteId,
    LocalDate fecha,
    List<LineaPedidoResponse> lineas,
    BigDecimal total
) {

    public record LineaPedidoResponse(
        Long productoId,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal
    ) {
    }

}
