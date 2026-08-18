package com.miapp.cliente.dto;

public record ClienteResponse(
    Long id,
    String nombre,
    String rfc,
    String razonSocial
) {
}
