package com.miapp.nota;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "nota")
class Nota {

    @Id
    private UUID id;

    @Column(nullable = false, length = 500)
    private String texto;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    protected Nota() {
    }

    Nota(UUID id, String texto, Instant creadaEn) {
        this.id = id;
        this.texto = texto;
        this.creadaEn = creadaEn;
    }

    UUID getId() {
        return id;
    }

    String getTexto() {
        return texto;
    }

    Instant getCreadaEn() {
        return creadaEn;
    }

}
