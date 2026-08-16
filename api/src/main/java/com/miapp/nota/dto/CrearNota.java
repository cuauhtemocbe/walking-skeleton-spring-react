package com.miapp.nota.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearNota(
    @NotBlank @Size(max = 500) String texto
) {
}
