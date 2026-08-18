package com.miapp.pedido;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "pedido")
class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id", nullable = false)
    private Long clienteId;

    @Column(nullable = false)
    private LocalDate fecha;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoDetalle> detalles = new ArrayList<>();

    protected Pedido() {
    }

    Pedido(Long clienteId, LocalDate fecha) {
        this.clienteId = clienteId;
        this.fecha = fecha;
    }

    Long getId() {
        return id;
    }

    Long getClienteId() {
        return clienteId;
    }

    LocalDate getFecha() {
        return fecha;
    }

    List<PedidoDetalle> getDetalles() {
        return detalles;
    }

    void agregarDetalle(PedidoDetalle detalle) {
        detalles.add(detalle);
        detalle.asignarPedido(this);
    }

}
