package com.miapp.pedido.dto;

public record LineaPedidoInput(
    Long productoId,
    Integer cantidad
) {
}
