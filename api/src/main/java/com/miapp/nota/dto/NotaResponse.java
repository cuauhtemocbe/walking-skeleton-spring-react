package com.miapp.nota.dto;

import java.time.Instant;
import java.util.UUID;

public record NotaResponse(
    UUID id,
    String texto,
    Instant creadaEn
) {
}
