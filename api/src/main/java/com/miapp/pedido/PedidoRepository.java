package com.miapp.pedido;

import org.springframework.data.jpa.repository.JpaRepository;

interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
