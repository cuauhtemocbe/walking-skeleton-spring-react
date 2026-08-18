package com.miapp.pedido.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CrearPedido(
    @NotNull Long clienteId,
    @NotEmpty List<LineaPedidoInput> lineas
) {
}
