package com.miapp.cliente.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarCliente(
    @NotBlank @Size(max = 200) String nombre,
    @NotBlank @Size(max = 13) String rfc,
    @NotBlank @Size(max = 200) String razonSocial
) {
}
