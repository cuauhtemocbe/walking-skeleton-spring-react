package com.miapp.cliente;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nombre;

    @Column(nullable = false, length = 13, unique = true)
    private String rfc;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    protected Cliente() {
    }

    Cliente(String nombre, String rfc, String razonSocial) {
        this.nombre = nombre;
        this.rfc = rfc;
        this.razonSocial = razonSocial;
    }

    Long getId() {
        return id;
    }

    String getNombre() {
        return nombre;
    }

    String getRfc() {
        return rfc;
    }

    String getRazonSocial() {
        return razonSocial;
    }

    void actualizar(String nombre, String rfc, String razonSocial) {
        this.nombre = nombre;
        this.rfc = rfc;
        this.razonSocial = razonSocial;
    }

}
